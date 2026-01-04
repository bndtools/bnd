package org.bndtools.builder.classpath;

import static java.util.stream.Collectors.toList;
import static org.bndtools.builder.classpath.BndContainer.TEST;
import static org.bndtools.builder.classpath.BndContainer.hasAttribute;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.api.ModelListener;
import org.bndtools.builder.BuildLogger;
import org.bndtools.builder.BuilderPlugin;
import org.bndtools.utils.jar.PseudoJar;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.index.DiskIndex;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

import aQute.bnd.build.CircularDependencyException;
import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.RepoCollector;
import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.service.reporter.Reporter.SetLocation;
import bndtools.central.Central;
import bndtools.preferences.BndPreferences;

/**
 * ClasspathContainerInitializer for the aQute.bnd.classpath.container name.
 * <p>
 * Used in the .classpath file of bnd project to couple the bnd -buildpath into
 * the Eclipse IDE.
 */
public class BndContainerInitializer extends ClasspathContainerInitializer implements ModelListener {

	private static final ILogger												logger				= Logger
		.getLogger(BndContainerInitializer.class);
	private static final ClasspathContainerSerializationHelper<BndContainer>	serializationHelper	= new ClasspathContainerSerializationHelper<>();

	public BndContainerInitializer() {
		super();
		Central.onCnfWorkspace(workspace -> Central.getInstance()
			.addModelListener(BndContainerInitializer.this));
		JavaRuntime.addContainerResolver(new BndContainerRuntimeClasspathEntryResolver(),
			BndtoolsConstants.BND_CLASSPATH_ID.segment(0));
	}

	@Override
	public void initialize(IPath containerPath, final IJavaProject javaProject) throws CoreException {
		IProject project = javaProject.getProject();

		/*
		 * If workspace is already initialized or there is no saved container
		 * information, then update the container now.
		 */
		File containerFile = getContainerFile(project);
		if (Central.hasCnfWorkspace() || !containerFile.isFile()) {
			Updater updater = new Updater(project, javaProject);
			updater.updateClasspathContainer(true);
			return;
		}

		/*
		 * Read the saved container information and update the container now.
		 * Request an update using the project information after the workspace
		 * is initialized.
		 */
		BndContainer container = loadClasspathContainer(project);
		Updater.setClasspathContainer(javaProject, container);
		Central.onCnfWorkspace(
			workspace -> requestClasspathContainerUpdate(BndtoolsConstants.BND_CLASSPATH_ID, javaProject, null));
	}

	@Override
	public boolean canUpdateClasspathContainer(IPath containerPath, IJavaProject javaProject) {
		return true;
	}

	@Override
	public void requestClasspathContainerUpdate(IPath containerPath, IJavaProject javaProject,
		IClasspathContainer containerSuggestion) throws CoreException {
		IProject project = javaProject.getProject();
		if (containerSuggestion != null) {
			BndContainerSourceManager.saveAttachedSources(project, containerSuggestion.getClasspathEntries());
		}

		Updater updater = new Updater(project, javaProject);
		updater.updateClasspathContainer(false);
	}

	@Override
	public String getDescription(IPath containerPath, IJavaProject project) {
		return BndContainer.DESCRIPTION;
	}

	/**
	 * ModelListener modelChanged method.
	 */
	@Override
	public void modelChanged(Project model) throws Exception {
		IJavaProject javaProject = Central.getJavaProject(model);
		if (javaProject == null) {
			return; // bnd project is not loaded in the workspace
		}
		requestClasspathContainerUpdate(javaProject);
	}

	/**
	 * Return the BndContainer for the project, if there is one. This will not
	 * create one if there is not already one.
	 *
	 * @param javaProject The java project of interest. Must not be null.
	 * @return The BndContainer for the java project.
	 */
	public static IClasspathContainer getClasspathContainer(IJavaProject javaProject) {
		return JavaModelManager.getJavaModelManager()
			.containerGet(javaProject, BndtoolsConstants.BND_CLASSPATH_ID);
	}

	/**
	 * Request the BndContainer for the project, if there is one, be updated.
	 * This will not create one if there is not already one.
	 *
	 * @param javaProject The java project of interest. Must not be null.
	 * @return true if the classpath container was updated.
	 * @throws CoreException
	 */
	public static boolean requestClasspathContainerUpdate(IJavaProject javaProject) throws CoreException {
		IClasspathContainer oldContainer = getClasspathContainer(javaProject);
		if (oldContainer == null) {
			return false; // project does not have a BndContainer
		}
		ClasspathContainerInitializer initializer = JavaCore
			.getClasspathContainerInitializer(BndtoolsConstants.BND_CLASSPATH_ID.segment(0));
		if (initializer != null) {
			initializer.requestClasspathContainerUpdate(BndtoolsConstants.BND_CLASSPATH_ID, javaProject, null);
		}
		return getClasspathContainer(javaProject) != oldContainer;
	}

	private static BndContainer loadClasspathContainer(IProject project) {
		File containerFile = getContainerFile(project);
		if (!containerFile.isFile()) {
			return new BndContainer.Builder().build();
		}
		try {
			return serializationHelper.readClasspathContainer(containerFile);
		} catch (Exception e) {
			logger.logError("Unable to load serialized classpath container from file " + containerFile, e);
			IO.delete(containerFile);
			return new BndContainer.Builder().build();
		}
	}

	static void storeClasspathContainer(IProject project, BndContainer container) {
		File containerFile = getContainerFile(project);
		try {
			serializationHelper.writeClasspathContainer(container, containerFile);
		} catch (Exception e) {
			logger.logError("Unable to store serialized classpath container in file " + containerFile, e);
			IO.delete(containerFile);
		}
	}

	private static File getContainerFile(IProject p) {
		return IO.getFile(BuilderPlugin.getInstance()
			.getStateLocation()
			.toFile(), p.getName() + ".container");
	}

	/**
	 * This is a hack to prevent duplicate results in the Open Type dialog in
	 * Eclipse.
	 * <p>
	 * </p>
	 * This creates a (per project) projectname-empty.index file at
	 * /eclipseworkspace/.metadata/.plugins/bndtools.builder/empty.index and
	 * writes the version of your current running Eclipse into it. See
	 * {@link DiskIndex#SIGNATURE} The reason for this "hack" is that this
	 * version can change with newer Eclipse versions. The reason we need to
	 * create this file per project is, that the Eclipse indexer is heavily
	 * parallelized and keeps track which index is in use. Thus every project
	 * needs to have its own empty.index, even though this is kinda redundant.
	 * <p>
	 * See the comment in this file where {@link Updater#EMPTY_INDEX} is added
	 * to
	 *
	 * <pre>
	 * extraAttrs.add(EMPTY_INDEX)
	 * </pre>
	 *
	 * @param empty_index_file
	 */
	private static void writeDummyEmptyIndexFile(File empty_index_file) {
		// see the file 'example-empty.index' in this package for why we are
		// writing what we write here.

		byte[] prefix = {
			0x00, 0x13
		}; // ^@ ^S
		byte[] suffix = {
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
		};

		try (FileOutputStream out = new FileOutputStream(empty_index_file)) {

			// get the SIGNATURE constant via reflection, because we need to
			// avoid that it is baked in at compile time: we need the version
			// from the runtime eclipse instance
			String version = (String) DiskIndex.class.getField("SIGNATURE")
				.get(null);

			if (version == null) {
				return;
			}
			byte[] versionBytes = version.getBytes(StandardCharsets.US_ASCII);

			out.write(prefix);
			out.write(versionBytes);
			out.write(suffix);
			out.flush();
		} catch (Exception e) {
			logger.logError("Error writing empty.index", e);
		}
	}

	private static class Updater {
		private static final IAccessRule			DISCOURAGED			= JavaCore.newAccessRule(new Path("**"),
			IAccessRule.K_DISCOURAGED | IAccessRule.IGNORE_IF_BETTER);

		private static final IClasspathAttribute	WITHOUT_TEST_CODE	= JavaCore
			.newClasspathAttribute("without_test_code", Boolean.TRUE.toString());
		private static final Pattern				packagePattern		= Pattern.compile("(?<=^|\\.)\\*(?=\\.|$)|\\.");
		private static final Map<File, JarInfo>		jarInfo				= Collections
			.synchronizedMap(new WeakHashMap<>());

		private final IProject						project;
		private final IJavaProject					javaProject;
		private final IWorkspaceRoot				root;
		private final Project						model;
		private final BndContainer.Builder			builder;

		private final File							empty_index_file;
		private final IClasspathAttribute			EMPTY_INDEX;

		Updater(IProject project, IJavaProject javaProject) {
			assert project != null;
			assert javaProject != null;
			this.project = project;
			this.javaProject = javaProject;
			this.root = project.getWorkspace()
				.getRoot();
			this.builder = new BndContainer.Builder();
			Project p = null;
			try {
				p = Central.getProject(project);
			} catch (Exception e) {
				// this can happen during first project creation in an empty
				// workspace
				logger.logInfo("Unable to get bnd project for project " + project.getName(), e);
			}
			this.model = p;

			empty_index_file = IO.getFile(BuilderPlugin.getInstance()
				.getStateLocation()
				.toFile(), "empty-" + project.getName() + ".index");
			EMPTY_INDEX = JavaCore.newClasspathAttribute(IClasspathAttribute.INDEX_LOCATION_ATTRIBUTE_NAME,
				"file://" + empty_index_file.getAbsolutePath());
			writeDummyEmptyIndexFile(empty_index_file);
		}

		void updateClasspathContainer(boolean init) throws CoreException {
			if (model == null) { // this can happen during new project creation
				setClasspathContainer(javaProject, builder.build());
				return;
			}

			try {
				model.getWorkspace()
					.readLocked(this::calculateProjectClasspath);
			} catch (Exception e) {
				SetLocation error = error("Unable to calculate classpath for project %s", e, project.getName());
				logger.logError(error.location().message, e);
			}

			builder.entries(BndContainerSourceManager.loadAttachedSources(project, builder.entries()));

			if (!init) {
				IClasspathContainer container = getClasspathContainer(javaProject);
				if (container instanceof BndContainer) {
					BndContainer bndContainer = (BndContainer) container;
					List<IClasspathEntry> currentClasspath = Arrays.asList(bndContainer.getClasspathEntries());
					if (builder.entries()
						.equals(currentClasspath) && (builder.lastModified() <= bndContainer.lastModified())) {
						bndContainer.refresh();
						return; // no change; so no need for new container
					}
				}
			}

			BndContainer bndContainer = builder.build();
			bndContainer.refresh();
			setClasspathContainer(javaProject, bndContainer);
			storeClasspathContainer(project, bndContainer);
		}

		static void setClasspathContainer(IJavaProject javaProject, BndContainer container) throws JavaModelException {
			BndPreferences prefs = new BndPreferences();
			if (prefs.getBuildLogging() == BuildLogger.LOG_FULL) {
				StringBuilder sb = new StringBuilder();
				sb.append(container.getDescription())
					.append(" for ")
					.append(javaProject.getProject()
						.getName())
					.append("\n\n=== Compile Classpath ===");
				for (IClasspathEntry cpe : container.getClasspathEntries()) {
					sb.append("\n--- ")
						.append(cpe);
				}
				sb.append("\n\n=== Runtime Classpath ===");
				for (IRuntimeClasspathEntry cpe : container.getRuntimeClasspathEntries()) {
					sb.append("\n--- ")
						.append(cpe);
				}
				logger.logInfo(sb.append("\n")
					.toString(), null);
			}

			JavaCore.setClasspathContainer(BndtoolsConstants.BND_CLASSPATH_ID, new IJavaProject[] {
				javaProject
			}, new IClasspathContainer[] {
				container
			}, null);
		}

		private Void calculateProjectClasspath() throws CoreException {
			if (!project.isOpen()) {
				return null;
			}

			try {
				Collection<Container> containers = model.getBuildpath();
				calculateContainersClasspath(Constants.BUILDPATH, containers);

				containers = model.getTestpath();
				calculateContainersClasspath(Constants.TESTPATH, containers);

				containers = model.getBootclasspath();
				calculateContainersClasspath(Constants.BUILDPATH, containers);

				// handle ${repo} reference. This is especially needed for
				// sub-bundles (bundles which wrap other (non-osgi) jars
				// and were we want to have source attachments for Eclipse
				containers = RepoCollector.collectRepoReferences(model);
				calculateContainersClasspath(Constants.BUILDPATH, containers);
				containers = model.getSubProjects()
					.stream()
					.map(sp -> {

						try {
							return RepoCollector.collectRepoReferences(sp);
						} catch (IOException e) {
							throw Exceptions.duck(e);
						}
					})
					.flatMap(Collection::stream)
					.toList();
				calculateContainersClasspath(Constants.BUILDPATH, containers);

			} catch (CircularDependencyException e) {
				error("Circular dependency during classpath calculation: %s", e, e.getMessage());
				builder.entries(Collections.emptyList());
			} catch (Exception e) {
				error("Unexpected error during classpath calculation: %s", e, e.getMessage());
				builder.entries(Collections.emptyList());
			}
			return null;
		}

		private void calculateContainersClasspath(String instruction, Collection<Container> containers)
			throws CoreException {
			boolean testattr = instruction.equals(Constants.TESTPATH) && Arrays.stream(javaProject.getRawClasspath())
				.filter(cpe -> cpe.getEntryKind() == IClasspathEntry.CPE_SOURCE)
				.anyMatch(cpe -> hasAttribute(cpe, TEST));
			for (Container c : containers) {
				File file = c.getFile();
				assert file.isAbsolute();

				if (!file.exists()) {
					switch (c.getType()) {
						case REPO :
							error(c, instruction, "Repository file %s does not exist", file);
							break;
						case LIBRARY :
							error(c, instruction, "Library file %s does not exist", file);
							break;
						case PROJECT :
							error(c, instruction, "Project bundle %s does not exist", file);
							break;
						case EXTERNAL :
							error(c, instruction, "External file %s does not exist", file);
							break;
						default :
							break;
					}
				}

				IPath path = null;
				try {
					path = fileToPath(file);
				} catch (Exception e) {
					error(c, instruction, "Failed to convert file %s to Eclipse path: %s", e, file, e.getMessage());
					continue;
				}
				IResource resource = Central.toResource(file);
				if (resource != null) {
					builder.refresh(resource);
				}

				List<IClasspathAttribute> extraAttrs = calculateContainerAttributes(c);
				if (testattr) {
					extraAttrs.add(TEST);
				}
				List<IAccessRule> accessRules = calculateContainerAccessRules(c);

				switch (c.getType()) {
					case PROJECT :
						if (testattr) {
							extraAttrs.add(WITHOUT_TEST_CODE);
						}
						IProject otherProject = root.getFile(path)
							.getProject();
						if (isVersionProject(c)) { // version=project
							addProjectEntry(otherProject.getFullPath(), accessRules, extraAttrs);
						} else { // version=latest or version=snapshot
							/*
							 * When the source value is not equal to "project"
							 * (the default value), we make the source folder
							 * discouraged. This can be necessary when using the
							 * Eclipse Transformer Bnd Analyzer plugin which can
							 * make the class files in the jar incompatible with
							 * the class files in the source output folder. We
							 * tried to always make the source folder
							 * discouraged which worked fine for compiling but
							 * it messed up the Open Type Hierarchy support in
							 * Eclipse. See
							 * https://github.com/bndtools/bnd/issues/5250. So
							 * we maintain the prior behavior unless source is
							 * set to a value other than "project". e.g.
							 * source=none
							 */
							String source = c.getAttributes()
								.getOrDefault("source", "project");
							List<IAccessRule> projectAccessRules = Objects.equals(source, "project") ? accessRules
								: Collections.emptyList();
							/*
							 * Add project entry for source code before library
							 * entry for generated jar.
							 */
							addProjectEntry(otherProject.getFullPath(), projectAccessRules, extraAttrs);
							/*
							 * Supply an empty index for the generated JAR of a
							 * workspace project dependency. This prevents the
							 * non-editable source files in the generated jar
							 * from appearing in the Open Type dialog. Also see
							 * method writeDummyEmptyIndexFile()
							 */
							extraAttrs.add(EMPTY_INDEX);
							/*
							 * Compute source attachment path for library entry.
							 */
							IPath sourceAttachmentPath = calculateSourceAttachmentPath(path, file);
							IPath sourceAttachmentRootPath = null; // default
							if (sourceAttachmentPath == null) {
								IJavaProject otherJavaProject = JavaCore.create(otherProject);
								if (otherJavaProject != null) {
									for (IClasspathEntry raw : otherJavaProject.getRawClasspath()) {
										if ((raw.getEntryKind() == IClasspathEntry.CPE_SOURCE)
											&& !hasAttribute(raw, TEST)) {
											sourceAttachmentPath = raw.getSourceAttachmentPath();
											if (sourceAttachmentPath != null) {
												sourceAttachmentRootPath = raw.getSourceAttachmentRootPath();
											} else {
												sourceAttachmentPath = raw.getPath();
											}
											break;
										}
									}
								}
							}
							addLibraryEntry(path, file, accessRules, extraAttrs, sourceAttachmentPath,
								sourceAttachmentRootPath);
						}
						break;
					default :
						IPath sourceAttachmentPath = calculateSourceAttachmentPath(path, file);
						IPath sourceAttachmentRootPath = null; // default
						addLibraryEntry(path, file, accessRules, extraAttrs, sourceAttachmentPath,
							sourceAttachmentRootPath);
						break;
				}
			}
		}

		private void addProjectEntry(IPath path, List<IAccessRule> accessRules, List<IClasspathAttribute> extraAttrs) {
			List<IClasspathEntry> classpath = builder.entries();
			for (int i = 0; i < classpath.size(); i++) {
				IClasspathEntry entry = classpath.get(i);
				if (entry.getEntryKind() != IClasspathEntry.CPE_PROJECT) {
					continue;
				}
				if (!entry.getPath()
					.equals(path)) {
					continue;
				}

				// Found a project entry for the project
				List<IAccessRule> oldAccessRules = Arrays.asList(entry.getAccessRules());
				int last = oldAccessRules.size() - 1;
				if (last < 0) {
					return; // project entry already has full access
				}
				List<IAccessRule> combinedAccessRules = null;
				if (accessRules != null) { // if not full access request
					combinedAccessRules = new ArrayList<>(oldAccessRules);
					if (DISCOURAGED.equals(combinedAccessRules.get(last))) {
						combinedAccessRules.remove(last);
					}
					combinedAccessRules.addAll(accessRules);
				}
				IClasspathEntry projectEntry = JavaCore.newProjectEntry(path, toAccessRulesArray(combinedAccessRules),
					false, entry.getExtraAttributes(), false);
				builder.entry(i, projectEntry);
				return;
			}
			// Add a new project entry for the project
			IClasspathEntry projectEntry = JavaCore.newProjectEntry(path, toAccessRulesArray(accessRules), false,
				toClasspathAttributesArray(extraAttrs), false);
			builder.entry(projectEntry);
		}

		private IPath calculateSourceAttachmentPath(IPath path, File file) {
			JarInfo info = getJarInfo(file);
			return info.hasSource ? path : null;
		}

		private JarInfo getJarInfo(File file) {
			final long lastModified = file.lastModified();
			JarInfo info = jarInfo.get(file);
			if ((info != null) && (lastModified == info.lastModified)) {
				return info;
			}
			info = new JarInfo();
			if (!file.exists()) {
				return info;
			}
			info.lastModified = lastModified;
			try (PseudoJar jar = new PseudoJar(file)) {
				Manifest mf = jar.readManifest();
				if ((mf != null) && (mf.getMainAttributes()
					.getValue(Constants.BUNDLE_MANIFESTVERSION) != null)) {
					Parameters exportPkgs = new Parameters(mf.getMainAttributes()
						.getValue(Constants.EXPORT_PACKAGE));
					Set<String> exports = exportPkgs.keySet();
					info.exports = exports.toArray(new String[0]);
				}
				for (String entry = jar.nextEntry(); entry != null; entry = jar.nextEntry()) {
					if (entry.startsWith("OSGI-OPT/src/")) {
						// use library path as source attachment path
						info.hasSource = true;
						break;
					}
				}
			} catch (IOException e) {
				logger.logInfo("Failed to read " + file, e);
			}
			jarInfo.put(file, info);
			return info;
		}

		private void addLibraryEntry(IPath path, File file, List<IAccessRule> accessRules,
			List<IClasspathAttribute> extraAttrs, IPath sourceAttachmentPath, IPath sourceAttachmentRootPath) {

			if (file.isFile() && !file.getName()
				.toLowerCase()
				.endsWith(".jar")) {
				// non .jar files are no library entries (it is possible that
				// the ${repo} macro references non jar files which could end up
				// here
				return;
			}

			IClasspathEntry libraryEntry = JavaCore.newLibraryEntry(path, sourceAttachmentPath,
				sourceAttachmentRootPath, toAccessRulesArray(accessRules), toClasspathAttributesArray(extraAttrs),
				false);
			builder.entry(libraryEntry);
			builder.updateLastModified(file.lastModified());
		}

		private List<IClasspathAttribute> calculateContainerAttributes(Container c) {
			List<IClasspathAttribute> attrs = new ArrayList<>();
			attrs.add(JavaCore.newClasspathAttribute("bsn", c.getBundleSymbolicName()));
			attrs.add(JavaCore.newClasspathAttribute("type", c.getType()
				.name()));
			attrs.add(JavaCore.newClasspathAttribute("project", c.getProject()
				.getName()));

			String version = c.getAttributes()
				.get(Constants.VERSION_ATTRIBUTE);
			if (version != null) {
				attrs.add(JavaCore.newClasspathAttribute(Constants.VERSION_ATTRIBUTE, version));
			}
			else {
				version = c.getVersion();

				if (version != null) {
					attrs.add(JavaCore.newClasspathAttribute(Constants.VERSION_ATTRIBUTE, version));
				}
			}

			String packages = c.getAttributes()
				.get("packages");
			if (packages != null) {
				attrs.add(JavaCore.newClasspathAttribute("packages", packages));
			}

			return attrs;
		}

		private List<IAccessRule> calculateContainerAccessRules(Container c) {
			String packageList = c.getAttributes()
				.get("packages");
			if (packageList != null) {
				// Use packages=* for full access
				return Strings.splitAsStream(packageList)
					.map(exportPkg -> {
						int ruleKind = IAccessRule.K_ACCESSIBLE;
						if (exportPkg.startsWith("!")) {
							exportPkg = exportPkg.substring(1);
							ruleKind = IAccessRule.K_NON_ACCESSIBLE | IAccessRule.IGNORE_IF_BETTER;
						}
						Matcher m = packagePattern.matcher(exportPkg);
						StringBuilder pathStr = new StringBuilder(exportPkg.length() + 1);
						int start = 0;
						for (; m.find(); start = m.end()) {
							pathStr.append(exportPkg, start, m.start())
								.append(m.group()
									.equals("*") ? "**" : "/");
						}
						pathStr.append(exportPkg, start, exportPkg.length())
							.append("/*");
						return JavaCore.newAccessRule(new Path(pathStr.toString()), ruleKind);
					})
					.collect(toList());
			}

			switch (c.getType()) {
				case PROJECT :
					// if version=project, try Project for exports
					if (isVersionProject(c)) {
						return calculateProjectAccessRules(c.getProject());
					}
					//$FALL-THROUGH$
				case REPO :
				case EXTERNAL :
					JarInfo info = getJarInfo(c.getFile());
					if (info.exports == null) {
						break; // no export; so full access
					}
					List<IAccessRule> accessRules = new ArrayList<>();
					for (String exportPkg : info.exports) {
						String pathStr = exportPkg.replace('.', '/') + "/*";
						accessRules.add(JavaCore.newAccessRule(new Path(pathStr), IAccessRule.K_ACCESSIBLE));
					}
					return accessRules;
				default :
					break;
			}

			return null; // full access
		}

		private List<IAccessRule> calculateProjectAccessRules(Project p) {
			File accessPatternsFile = getAccessPatternsFile(p);
			String oldAccessPatterns = "";
			boolean exists = accessPatternsFile.exists();
			if (exists) { // read persisted access patterns
				try {
					oldAccessPatterns = IO.collect(accessPatternsFile);
					builder.updateLastModified(accessPatternsFile.lastModified());
				} catch (final IOException e) {
					logger.logError("Failed to read access patterns file for project " + p.getName(), e);
				}
			}

			if (p.getContained()
				.isEmpty()) {
				// project not recently built; use persisted access patterns
				if (!exists) {
					return null; // no persisted access patterns; full access
				}
				String[] patterns = oldAccessPatterns.split(",");
				List<IAccessRule> accessRules = new ArrayList<>(patterns.length);
				// if not empty, there are access patterns
				if (!oldAccessPatterns.isEmpty()) {
					for (String pathStr : patterns) {
						accessRules.add(JavaCore.newAccessRule(new Path(pathStr), IAccessRule.K_ACCESSIBLE));
					}
				}
				return accessRules;
			}

			Set<PackageRef> exportPkgs = p.getExports()
				.keySet();
			List<IAccessRule> accessRules = new ArrayList<>(exportPkgs.size());
			StringBuilder sb = new StringBuilder(oldAccessPatterns.length());
			for (PackageRef exportPkg : exportPkgs) {
				String pathStr = exportPkg.getBinary() + "/*";
				if (sb.length() > 0) {
					sb.append(',');
				}
				sb.append(pathStr);
				accessRules.add(JavaCore.newAccessRule(new Path(pathStr), IAccessRule.K_ACCESSIBLE));
			}

			String newAccessPatterns = sb.toString();
			// if state changed; persist updated access patterns
			if (!exists || !newAccessPatterns.equals(oldAccessPatterns)) {
				try {
					IO.store(newAccessPatterns, accessPatternsFile);
					builder.updateLastModified(accessPatternsFile.lastModified());
				} catch (final IOException e) {
					logger.logError("Failed to write access patterns file for project " + p.getName(), e);
				}
			}

			return accessRules;
		}

		private File getAccessPatternsFile(Project p) {
			return IO.getFile(BuilderPlugin.getInstance()
				.getStateLocation()
				.toFile(), p.getName() + ".accesspatterns");
		}

		private IAccessRule[] toAccessRulesArray(List<IAccessRule> rules) {
			if (rules == null) {
				return null;
			}
			final int size = rules.size();
			IAccessRule[] accessRules = rules.toArray(new IAccessRule[size + 1]);
			accessRules[size] = DISCOURAGED;
			return accessRules;
		}

		private IClasspathAttribute[] toClasspathAttributesArray(List<IClasspathAttribute> attrs) {
			if (attrs == null) {
				return null;
			}
			IClasspathAttribute[] attrsArray = attrs.toArray(new IClasspathAttribute[0]);
			return attrsArray;
		}

		private IPath fileToPath(File file) throws Exception {
			IPath path = Central.toPath(file);
			if (path == null)
				path = Path.fromOSString(file.getAbsolutePath());
			return path;
		}

		private boolean isVersionProject(Container c) {
			return Constants.VERSION_ATTR_PROJECT.equals(c.getAttributes()
				.get(Constants.VERSION_ATTRIBUTE));
		}

		private SetLocation error(String message, Throwable t, Object... args) {
			return model.error(message, t, args)
				.context(model.getName())
				.header(Constants.BUILDPATH)
				.file(model.getPropertiesFile()
					.getAbsolutePath());
		}

		private SetLocation error(Container c, String header, String message, Object... args) {
			return model.error(message, args)
				.context(c.getBundleSymbolicName())
				.header(header)
				.file(model.getPropertiesFile()
					.getAbsolutePath());
		}

		private SetLocation error(Container c, String header, String message, Throwable t, Object... args) {
			return model.error(message, t, args)
				.context(c.getBundleSymbolicName())
				.header(header)
				.file(model.getPropertiesFile()
					.getAbsolutePath());
		}
	}



	private static class JarInfo {
		boolean		hasSource;
		String[]	exports;
		long		lastModified;

		JarInfo() {}
	}
}
