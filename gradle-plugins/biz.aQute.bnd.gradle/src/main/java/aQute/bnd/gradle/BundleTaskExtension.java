package aQute.bnd.gradle;

import static aQute.bnd.gradle.BndUtils.builtBy;
import static aQute.bnd.gradle.BndUtils.jarLibraryElements;
import static aQute.bnd.gradle.BndUtils.logReport;
import static aQute.bnd.gradle.BndUtils.sourceSets;
import static aQute.bnd.gradle.BndUtils.unwrap;
import static aQute.bnd.gradle.BndUtils.unwrapFile;
import static aQute.bnd.gradle.BndUtils.unwrapFileOptional;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.gradle.api.tasks.PathSensitivity.RELATIVE;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.zip.ZipFile;

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
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.java.archives.internal.DefaultManifest;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.ClasspathNormalizer;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.work.NormalizeLineEndings;

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
 * <li>properties - Properties that are available for evaluation of the bnd
 * instructions. The default is the properties of the task and project
 * objects.</li>
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
	private final MapProperty<String, Object>	properties;
	private final Provider<UTF8Properties>      projectAndTaskProperties;
	private final Provider<String>				defaultBundleSymbolicName;
	private final Provider<String>				defaultBundleVersion;

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
	 * @return The provider for the bnd instructions.
	 */
	@Input
	@org.gradle.api.tasks.Optional
	public Provider<String> getBnd() {
		return bnd;
	}

	/**
	 * Properties that the Gradle configuration makes available for evaluation
	 * of the bnd instructions.
	 *
	 * @return Properties available for evaluation of the bnd instructions.
	 */
	@Input
	public MapProperty<String, Object> getProperties() {
		return properties;
	}

	/**
	 * Properties of the Gradle project and task that are referenced in the
	 * bndfile or in {@link #getProperties()}.
	 * <p>
	 * The following properties are set by the builder and are also available:
	 * <dl>
	 * <dt>{@code project.dir}</dt>
	 * <dd>The project directory.</dd>
	 * <dt>{@code project.output}</dt>
	 * <dd>The build directory.</dd>
	 * <dt>{@code project.buildpath}</dt>
	 * <dd>The project buildpath.</dd>
	 * <dt>{@code project.sourcepath<}</dt>
	 * <dd>The project sourcepath.</dd>
	 * </dl>
	 * <p>
	 * @return Properties available for evalutation of the bnd instructions.
	 */
	@Input
	public Provider<UTF8Properties> getProjectAndTaskProperties() {
		return projectAndTaskProperties;
	}

	private final ConfigurableFileCollection		allSource;

	private final org.gradle.api.tasks.bundling.Jar	task;
	private final ProjectLayout						layout;
	private final File								buildFile;
	private final ListProperty<CharSequence>		instructions;
	private final DirectoryProperty		            outputDirectory;

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
		outputDirectory = objects.directoryProperty();
		SourceSet mainSourceSet = sourceSets(project).getByName(SourceSet.MAIN_SOURCE_SET_NAME);
		setSourceSet(mainSourceSet);
		classpath(jarLibraryElements(task, mainSourceSet.getCompileClasspathConfigurationName()));
		properties = objects.mapProperty(String.class, Object.class);
		projectAndTaskProperties = project.provider(this::computeProjectAndTaskProperties);
		defaultBundleSymbolicName = task.getArchiveBaseName()
			.zip(task.getArchiveClassifier(), (baseName, classifier) -> classifier.isEmpty() ? baseName : baseName + "-" + classifier);
		defaultBundleVersion = task.getArchiveVersion()
			.orElse("0")
			.map(version -> MavenVersion.parseMavenString(version)
				.getOSGiVersion()
				.toString());
		// need to programmatically add to inputs since @InputFiles in an
		// extension is not processed
		task.getInputs()
			.files(getClasspath())
			.withNormalizer(ClasspathNormalizer.class)
			.withPropertyName("classpath");
		task.getInputs()
			.file(getBndfile())
			.optional()
			.withPathSensitivity(RELATIVE)
			.withPropertyName("bndfile")
			.normalizeLineEndings();
		task.getInputs()
			.property("bnd", getBnd());
		task.getInputs()
			.property("properties", getProperties());
		task.getInputs()
			.property("gradleProperties", getProjectAndTaskProperties());
		task.getInputs()
			.property("default Bundle-SymbolicName", getDefaultBundleSymbolicName());
		task.getInputs()
			.property("default Bundle-Version", getDefaultBundleVersion());
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
		getOutputDirectory().value(getTask().getProject()
			.getTasks()
			.named(sourceSet.getCompileJavaTaskName(), AbstractCompile.class)
			.flatMap(AbstractCompile::getDestinationDirectory));
	}

	ConfigurableFileCollection getAllSource() {
		return allSource;
	}

	File getBuildFile() {
		return buildFile;
	}

	DirectoryProperty getOutputDirectory() {
		return outputDirectory;
	}

	/**
	 * The default value for the Bundle-SymbolicName manifest header.
	 * <p>
	 * If the Bundle-SymbolicName manifest header is not set in the bnd instructions,
	 * the value of this provider will be used.
	 *
	 * @return The provider for the default Bundle-SymbolicName manifest header.
	 */
	Provider<String> getDefaultBundleSymbolicName() {
		return defaultBundleSymbolicName;
	}

	/**
	 * The default value for the Bundle-Version manifest header.
	 * <p>
	 * If the Bundle-Version manifest header is not set in the bnd instructions,
	 * the value of this provider will be used.
	 *
	 * @return The provider for the default Bundle-Version manifest header.
	 */
	Provider<String> getDefaultBundleVersion() {
		return defaultBundleVersion;
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

	/**
	 * Extract properties of the Gradle {@link Project} and {@link Task} that are
	 * referenced by macros in {@link #getBndfile()} or {@link #getProperties()}.
	 * This is done to avoid using the Gradle types at runtime, to allow use of
	 * the configuration cache.
	 * @return A {@link UTF8Properties} containing properties extracted from the
	 * project and task, with values converted to strings.
	 */
	private UTF8Properties computeProjectAndTaskProperties() {
		final var customBeanProperties = new BeanProperties();
		customBeanProperties.putAll(unwrap(getProperties()));
		try (Processor processor = new Processor(customBeanProperties, false)) {
			// Read the bndfile or bnd string if it exists. Ignore errors, since we'll read it again later.
			processor.setProperties(loadBndInstructions(processor));

			final var gradleProperties = new BeanProperties();
			gradleProperties.put("project", getTask().getProject());
			gradleProperties.put("task", getTask());

			final UTF8Properties foundProperties = new UTF8Properties();
			processor.getMacroReferences(Processor.MacroReference.UNKNOWN).forEach(propertyName -> {
				String propertyValue = gradleProperties.getProperty(propertyName);
				if (propertyValue != null) {
					foundProperties.setProperty(propertyName, propertyValue);
				}
			});
			foundProperties.setProperty("project.dir", getProjectDir().getCanonicalPath());
			foundProperties.setProperty("project.output", getOutputDir().getCanonicalPath());
			foundProperties.setProperty("project.sourcepath", getSourcepath().getAsPath());
			foundProperties.setProperty("project.buildpath", getBuildpath().getAsPath());
			getTask().getLogger().debug("Computed properties for task {}: {}", getTask().getName(), foundProperties);
			return foundProperties;
		} catch (IOException e) {
			throw Exceptions.duck(e);
		}
	}

	private Properties loadBndInstructions(Processor processor) throws IOException {
		Optional<File> bndfile = unwrapFileOptional(getBndfile()).filter(File::isFile);
		if (bndfile.isPresent()) {
			return processor.loadProperties(bndfile.get());
		} else {
			final UTF8Properties props = new UTF8Properties();
			String bnd = unwrap(getBnd());
			if (!bnd.isEmpty()) {
				props.load(bnd, getBuildFile(), processor);
			}
			return props.replaceHere(getProjectDir());
		}
	}

	private class BuildAction implements Action<Task> {
		@Override
		public void execute(Task t) {
			try {
				File projectDir = getProjectDir();
				FileCollection sourcepath = getSourcepath();
				Optional<org.gradle.api.java.archives.Manifest> taskManifest = Optional
					.ofNullable(getTask().getManifest());
				// create Builder
				Properties gradleProperties = new BeanProperties();
				gradleProperties.putAll(unwrap(getProperties()));
				try (Builder builder = new Builder(new Processor(gradleProperties, false))) {
					builder.setProperties(unwrap(getProjectAndTaskProperties()));

					// load bnd properties
					File temporaryBndFile = File.createTempFile("bnd", ".bnd", getTask().getTemporaryDir());
					try (Writer writer = IO.writer(temporaryBndFile)) {
						// write any task manifest entries into the tmp bnd
						// file
						Optional<UTF8Properties> properties = taskManifest.map(manifest -> MapStream
							.ofNullable(manifest.getEffectiveManifest()
								.getAttributes())
							.filterKey(key -> !Objects.equals(key, "Manifest-Version"))
							.mapValue(this::unwrapAttributeValue)
							.collect(MapStream.toMap((k1, k2) -> {
								throw new IllegalStateException("Duplicate key " + k1);
							}, UTF8Properties::new)));
						if (properties.isPresent()) {
							properties.get()
								.replaceHere(projectDir)
								.store(writer, null);
						}
						// if the bnd file exists, add its contents to the
						// tmp bnd file
						loadBndInstructions(builder).store(writer, null);
					}
					// this will cause project.dir property to be set
					builder.setProperties(temporaryBndFile, projectDir);
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

					// Include entire contents of Jar task generated jar
					// (except the manifest)
					File archiveCopyFile = new File(getTask().getTemporaryDir(), archiveFileName);
					IO.copy(archiveFile, archiveCopyFile);
					Jar bundleJar = new Jar(archiveFileName, archiveCopyFile);
					String reproducible = builder.getProperty(Constants.REPRODUCIBLE);
					if (Objects.isNull(reproducible)) {
						if (!getTask().isPreserveFileTimestamps()) {
							builder.setProperty(Constants.REPRODUCIBLE, Boolean.TRUE.toString());
						}
					}
					String compression = builder.getProperty(Constants.COMPRESSION);
					if (Objects.isNull(compression)) {
						builder.setProperty(Constants.COMPRESSION, switch (getTask().getEntryCompression()) {
							case STORED -> Jar.Compression.STORE.name();
							case DEFLATED -> Jar.Compression.DEFLATE.name();
						});
					}
					bundleJar.updateModified(archiveFile.lastModified(), "time of Jar task generated jar");
					bundleJar.setManifest(new Manifest());
					builder.setJar(bundleJar);

					// set builder classpath
					builder.setClasspath(getBuildpath().getFiles()
						.toArray(new File[0]));
					getTask().getLogger()
						.debug("builder classpath: {}", builder.getClasspath()
							.stream()
							.map(Jar::getSource)
							.collect(toList()));
					// set builder sourcepath
					builder.setSourcepath(sourcepath.getFiles()
						.toArray(new File[0]));
					getTask().getLogger()
						.debug("builder sourcepath: {}", builder.getSourcePath());
					// set bundle symbolic name if necessary
					String bundleSymbolicName = builder.getProperty(Constants.BUNDLE_SYMBOLICNAME);
					if (isEmpty(bundleSymbolicName)) {
						bundleSymbolicName = unwrap(getDefaultBundleSymbolicName());
						builder.setProperty(Constants.BUNDLE_SYMBOLICNAME, bundleSymbolicName);
					}

					// set bundle version if necessary
					String bundleVersion = builder.getProperty(Constants.BUNDLE_VERSION);
					if (isEmpty(bundleVersion)) {
						bundleVersion = unwrap(getDefaultBundleVersion());
						builder.setProperty(Constants.BUNDLE_VERSION, bundleVersion);
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
					// Set effective manifest from generated manifest
					Manifest builtManifest = builtJar.getManifest();
					taskManifest.ifPresent(
						manifest -> manifest.from(mergeManifest(builtManifest), merge -> merge.eachEntry(details -> {
							if (details.getMergeValue() == null) {
								// exclude if entry not in merge manifest
								details.exclude();
							}
						})));
					logReport(builder, getTask().getLogger());
					if (!builder.isOk()) {
						failTask("Bundle " + archiveFileName + " has errors", archiveFile);
					}
				}
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		}

		private org.gradle.api.java.archives.Manifest mergeManifest(Manifest builtManifest) {
			org.gradle.api.java.archives.Manifest mergeManifest = new DefaultManifest(null);
			mergeManifest.attributes(new AttributesMap(builtManifest.getMainAttributes()));
			builtManifest.getEntries()
				.forEach((section, attrs) -> mergeManifest.attributes(new AttributesMap(attrs), section));
			return mergeManifest;
		}

		private String unwrapAttributeValue(Object value) {
			while (value instanceof Provider<?> provider) {
				value = provider.getOrNull();
			}
			if (value == null) {
				return null;
			}
			return value.toString();
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

	private FileCollection getBuildpath() {
		FileCollection buildpath = getClasspath().filter(file -> {
			if (!file.exists()) {
				return false;
			}
			if (file.isDirectory()) {
				return true;
			}
			try (ZipFile zip = new ZipFile(file)) {
				// make sure it is a valid zip file and not a pom
				zip.entries();
			} catch (IOException e) {
				return false;
			}
			return true;
		});
		return buildpath;
	}

	private FileCollection getSourcepath() {
		return getAllSource().filter(File::exists);
	}

	private File getProjectDir() {
		return unwrapFile(getLayout().getProjectDirectory());
	}

	private File getOutputDir() {
		return unwrapFile(getOutputDirectory());
	}

	static final class AttributesMap extends AbstractMap<String, Object> {
		final java.util.jar.Attributes source;

		AttributesMap(java.util.jar.Attributes source) {
			this.source = requireNonNull(source);
		}

		@Override
		public Set<Entry<String, Object>> entrySet() {
			Set<Entry<Object, Object>> entrySet = source.entrySet();
			return new AbstractSet<>() {
				@Override
				public Iterator<Entry<String, Object>> iterator() {
					Iterator<Entry<Object, Object>> iterator = entrySet.iterator();
					return new Iterator<>() {
						@Override
						public boolean hasNext() {
							return iterator.hasNext();
						}

						@Override
						public Entry<String, Object> next() {
							Entry<Object, Object> next = iterator.next();
							return new AbstractMap.SimpleImmutableEntry<>(next.getKey()
								.toString(), next.getValue());
						}
					};
				}

				@Override
				public int size() {
					return entrySet.size();
				}
			};
		}
	}
}
