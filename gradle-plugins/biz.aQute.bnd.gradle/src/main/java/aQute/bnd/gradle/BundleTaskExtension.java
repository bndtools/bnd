package aQute.bnd.gradle;

import static aQute.bnd.gradle.BndUtils.builtBy;
import static aQute.bnd.gradle.BndUtils.isGradleCompatible;
import static aQute.bnd.gradle.BndUtils.jarLibraryElements;
import static aQute.bnd.gradle.BndUtils.logReport;
import static aQute.bnd.gradle.BndUtils.sourceSets;
import static aQute.bnd.gradle.BndUtils.unwrap;
import static aQute.bnd.gradle.BndUtils.unwrapFile;
import static aQute.bnd.gradle.BndUtils.unwrapFileOptional;
import static aQute.bnd.gradle.BndUtils.unwrapOptional;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.gradle.api.tasks.PathSensitivity.RELATIVE;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.jar.Manifest;
import java.util.zip.ZipFile;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.ClasspathNormalizer;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskInputFilePropertyBuilder;
import org.gradle.work.NormalizeLineEndings;

import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.stream.MapStream;
import aQute.bnd.version.MavenVersion;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.lib.utf8properties.UTF8Properties;

/**
 * BundleTaskExtension for Gradle.
 * <p>
 * Adds {@code bundle} extension to bundle builder tasks.
 * <p>
 * Properties:
 * <ul>
 * <li>bndfile - This is the name of the bnd file to use to make the bundle. The
 * bndfile does not need to exist. It will override headers in the jar task's
 * manifest.</li>
 * <li>bnd - This is a string holding a bnd file to use to make the bundle. It
 * will override headers in the jar task's manifest. This properties is ignored
 * if bndfile is specified and the specified file exists.</li>
 * <li>sourceSet - This is the SourceSet to use for the bnd builder. It defaults
 * to "project.sourceSets.main".</li>
 * <li>classpath - This is the FileCollection to use for the buildpath for the
 * bnd builder. It defaults to "project.sourceSets.main.compileClasspath".</li>
 * </ul>
 */
public class BundleTaskExtension {
	/**
	 * Name of the extension.
	 */
	public static final String					NAME	= "bundle";

	private final RegularFileProperty			bndfile;
	private final ConfigurableFileCollection	classpath;
	private final Provider<String>				bnd;

	/**
	 * The bndfile property.
	 * <p>
	 * A bnd file containing bnd instructions for this project.
	 *
	 * @return The property for the bndfile.
	 */
	@InputFile
	@PathSensitive(RELATIVE)
	@NormalizeLineEndings
	@org.gradle.api.tasks.Optional
	public RegularFileProperty getBndfile() {
		return bndfile;
	}

	/**
	 * The classpath property.
	 * <p>
	 * The default value is sourceSets.main.compileClasspath.
	 *
	 * @return The property for the classpath.
	 */
	@InputFiles
	@Classpath
	public ConfigurableFileCollection getClasspath() {
		return classpath;
	}

	/**
	 * The bnd property.
	 * <p>
	 * If the bndfile property points an existing file, this property is
	 * ignored. Otherwise, the bnd instructions in this property will be used.
	 *
	 * @return The property for the bnd instructions.
	 */
	@Input
	@org.gradle.api.tasks.Optional
	public Provider<String> getBnd() {
		return bnd;
	}

	private final ConfigurableFileCollection		allSource;

	private final org.gradle.api.tasks.bundling.Jar	task;
	private final ProjectLayout						layout;
	private final File								buildFile;
	private final ListProperty<CharSequence>		instructions;

	/**
	 * Create a BundleTaskExtension for the specified Jar task.
	 * <p>
	 * This also sets the default values for the added properties and adds the
	 * bnd file to the task inputs.
	 *
	 * @param task The Jar task for this extension.
	 */
	public BundleTaskExtension(org.gradle.api.tasks.bundling.Jar task) {
		this.task = task;
		Project project = task.getProject();
		layout = project.getLayout();
		ObjectFactory objects = project.getObjects();
		buildFile = project.getBuildFile();
		bndfile = objects.fileProperty();
		instructions = objects.listProperty(CharSequence.class)
			.empty();
		bnd = instructions.map(list -> Strings.join("\n", list));
		classpath = objects.fileCollection();
		allSource = objects.fileCollection();
		SourceSet mainSourceSet = sourceSets(project).getByName(SourceSet.MAIN_SOURCE_SET_NAME);
		setSourceSet(mainSourceSet);
		classpath(mainSourceSet.getCompileClasspath());
		// need to programmatically add to inputs since @InputFiles in a
		// extension is not processed
		task.getInputs()
			.files(getClasspath())
			.withNormalizer(ClasspathNormalizer.class)
			.withPropertyName("classpath");
		TaskInputFilePropertyBuilder bndfileInput = task.getInputs()
			.file(getBndfile())
			.optional()
			.withPathSensitivity(RELATIVE)
			.withPropertyName("bndfile");
		if (isGradleCompatible("7.2")) {
			bndfileInput.normalizeLineEndings();
		}
		task.getInputs()
			.property("bnd", getBnd());
	}

	/**
	 * Set the bnd property from a multi-line string.
	 *
	 * @param line bnd instructions.
	 */
	public void setBnd(CharSequence line) {
		instructions.empty();
		bnd(line);
	}

	/**
	 * Add instructions to the bnd property from a list of multi-line strings.
	 *
	 * @param lines bnd instructions.
	 */
	public void bnd(CharSequence... lines) {
		instructions.addAll(lines);
	}

	/**
	 * Set the bnd property from a multi-line string using a {@link Provider}.
	 *
	 * @param lines A provider of bnd instructions.
	 */
	public void setBnd(Provider<? extends CharSequence> lines) {
		instructions.empty();
		bnd(lines);
	}

	/**
	 * Add a multi-line string of instructions to the bnd property using a
	 * {@link Provider}.
	 *
	 * @param lines A provider bnd instructions.
	 */
	public void bnd(Provider<? extends CharSequence> lines) {
		instructions.add(lines);
	}

	/**
	 * Set the bnd property from a map.
	 *
	 * @param map A map of bnd instructions.
	 */
	public void setBnd(Map<String, ?> map) {
		instructions.empty();
		bnd(map);
	}

	/**
	 * Add instructions to the bnd property from a map.
	 *
	 * @param map A map of bnd instructions.
	 */
	public void bnd(Map<String, ?> map) {
		map.forEach((key, value) -> instructions.add(key + "=" + value));
	}

	/**
	 * Add files to the classpath.
	 *
	 * @param paths The arguments will be handled using
	 *            ConfigurableFileCollection.from().
	 * @return The property for the classpath.
	 */
	public ConfigurableFileCollection classpath(Object... paths) {
		return builtBy(getClasspath().from(paths), paths);
	}

	/**
	 * Set the classpath property.
	 *
	 * @param path The argument will be handled using
	 *            ConfigurableFileCollection.from().
	 */
	public void setClasspath(Object path) {
		getClasspath().setFrom(Collections.emptyList());
		getClasspath().setBuiltBy(Collections.emptyList());
		classpath(path);
	}

	/**
	 * Set the sourceSet.
	 *
	 * @param sourceSet A sourceSet to use to find source code.
	 */
	public void setSourceSet(SourceSet sourceSet) {
		getAllSource().setFrom(sourceSet.getAllSource()
			.getSourceDirectories());
		jarLibraryElements(getTask(), sourceSet.getCompileClasspathConfigurationName());
	}

	ConfigurableFileCollection getAllSource() {
		return allSource;
	}

	File getBuildFile() {
		return buildFile;
	}

	ProjectLayout getLayout() {
		return layout;
	}

	org.gradle.api.tasks.bundling.Jar getTask() {
		return task;
	}

	/**
	 * Return the Action to build the bundle for the task.
	 *
	 * @return The Action to build the bundle for the task.
	 */
	public Action<Task> buildAction() {
		return new BuildAction();
	}

	private class BuildAction implements Action<Task> {
		@Override
		public void execute(Task t) {
			try {
				File projectDir = unwrapFile(getLayout().getProjectDirectory());
				File buildDir = unwrapFile(getLayout().getBuildDirectory());
				File buildFile = getBuildFile();
				FileCollection sourcepath = getAllSource().filter(file -> file.exists());
				// create Builder
				Properties gradleProperties = new BeanProperties();
				gradleProperties.put("task", getTask());
				gradleProperties.put("project", getTask().getProject());
				try (Builder builder = new Builder(new Processor(gradleProperties, false))) {
					// load bnd properties
					File temporaryBndFile = File.createTempFile("bnd", ".bnd", getTask().getTemporaryDir());
					try (Writer writer = IO.writer(temporaryBndFile)) {
						// write any task manifest entries into the tmp bnd
						// file
						MapStream.of(getTask().getManifest()
							.getEffectiveManifest()
							.getAttributes())
							.filterKey(key -> !Objects.equals(key, "Manifest-Version"))
							.mapValue(Object::toString)
							.collect(MapStream.toMap((k1, k2) -> {
								throw new IllegalStateException("Duplicate key " + k1);
							}, UTF8Properties::new))
							.replaceHere(projectDir)
							.store(writer, null);
						// if the bnd file exists, add its contents to the
						// tmp bnd file
						Optional<File> bndfile = unwrapFileOptional(getBndfile()).filter(File::isFile);
						if (bndfile.isPresent()) {
							builder.loadProperties(bndfile.get())
								.store(writer, null);
						} else {
							String bnd = unwrap(getBnd());
							if (!bnd.isEmpty()) {
								UTF8Properties props = new UTF8Properties();
								props.load(bnd, buildFile, builder);
								props.replaceHere(projectDir)
									.store(writer, null);
							}
						}
					}
					// this will cause project.dir property to be set
					builder.setProperties(temporaryBndFile, projectDir);
					builder.setProperty("project.output", buildDir.getCanonicalPath());
					// If no bundle to be built, we have nothing to do
					if (builder.is(Constants.NOBUNDLES)) {
						return;
					}
					// Reject sub-bundle projects
					if (!Objects.equals(builder.getSubBuilders(), singletonList(builder))) {
						throw new GradleException("Sub-bundles are not supported by this task");
					}
					File archiveFile = unwrapFile(getTask().getArchiveFile());
					String archiveFileName = unwrap(getTask().getArchiveFileName());
					String archiveBaseName = unwrap(getTask().getArchiveBaseName());
					String archiveClassifier = unwrap(getTask().getArchiveClassifier());
					String archiveVersion = unwrapOptional(getTask().getArchiveVersion()).orElse(null);

					// Include entire contents of Jar task generated jar
					// (except the
					// manifest)
					File archiveCopyFile = new File(getTask().getTemporaryDir(), archiveFileName);
					IO.copy(archiveFile, archiveCopyFile);
					Jar bundleJar = new Jar(archiveFileName, archiveCopyFile);
					String reproducible = builder.getProperty(Constants.REPRODUCIBLE);
					bundleJar.setReproducible(Objects.nonNull(reproducible) ? Processor.isTrue(reproducible)
						: !getTask().isPreserveFileTimestamps());
					bundleJar.updateModified(archiveFile.lastModified(), "time of Jar task generated jar");
					bundleJar.setManifest(new Manifest());
					builder.setJar(bundleJar);

					// set builder classpath
					FileCollection buildpath = getClasspath().filter(file -> {
						if (!file.exists()) {
							return false;
						}
						if (file.isDirectory()) {
							return true;
						}
						try (ZipFile zip = new ZipFile(file)) {
							// make sure it is a valid zip file and not a
							// pom
							zip.entries();
						} catch (IOException e) {
							return false;
						}
						return true;
					});
					builder.setProperty("project.buildpath", buildpath.getAsPath());
					builder.setClasspath(buildpath.getFiles()
						.toArray(new File[0]));
					getTask().getLogger()
						.debug("builder classpath: {}", builder.getClasspath()
							.stream()
							.map(Jar::getSource)
							.collect(toList()));
					// set builder sourcepath
					builder.setProperty("project.sourcepath", sourcepath.getAsPath());
					builder.setSourcepath(sourcepath.getFiles()
						.toArray(new File[0]));
					getTask().getLogger()
						.debug("builder sourcepath: {}", builder.getSourcePath());
					// set bundle symbolic name from tasks's archiveBaseName
					// property if
					// necessary
					String bundleSymbolicName = builder.getProperty(Constants.BUNDLE_SYMBOLICNAME);
					if (isEmpty(bundleSymbolicName)) {
						bundleSymbolicName = archiveClassifier.isEmpty() ? archiveBaseName
							: archiveBaseName + "-" + archiveClassifier;
						builder.setProperty(Constants.BUNDLE_SYMBOLICNAME, bundleSymbolicName);
					}

					// set bundle version from task's archiveVersion if
					// necessary
					String bundleVersion = builder.getProperty(Constants.BUNDLE_VERSION);
					if (isEmpty(bundleVersion)) {
						builder.setProperty(Constants.BUNDLE_VERSION, MavenVersion.parseMavenString(archiveVersion)
							.getOSGiVersion()
							.toString());
					}

					getTask().getLogger()
						.debug("builder properties: {}", builder.getProperties());

					// Build bundle
					Jar builtJar = builder.build();
					if (!builder.isOk()) {
						// if we already have an error; fail now
						logReport(builder, getTask().getLogger());
						failTask("Bundle " + archiveFileName + " has errors", archiveFile);
					}

					// Write out the bundle
					builtJar.write(archiveFile);
					long now = System.currentTimeMillis();
					archiveFile.setLastModified(now);

					logReport(builder, getTask().getLogger());
					if (!builder.isOk()) {
						failTask("Bundle " + archiveFileName + " has errors", archiveFile);
					}
				}
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		}

		private void failTask(String msg, File archiveFile) {
			IO.delete(archiveFile);
			throw new GradleException(msg);
		}

		private boolean isEmpty(String header) {
			return Objects.isNull(header) || header.trim()
				.isEmpty() || Constants.EMPTY_HEADER.equals(header);
		}
	}
}
