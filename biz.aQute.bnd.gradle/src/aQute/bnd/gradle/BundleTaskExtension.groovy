package aQute.bnd.gradle

import static aQute.bnd.gradle.BndUtils.builtBy
import static aQute.bnd.gradle.BndUtils.jarLibraryElements
import static aQute.bnd.gradle.BndUtils.logReport
import static aQute.bnd.gradle.BndUtils.unwrap
import static java.util.Collections.singletonList
import static org.gradle.api.tasks.PathSensitivity.RELATIVE

import java.util.Properties
import java.util.jar.Manifest
import java.util.zip.ZipException
import java.util.zip.ZipFile

import aQute.bnd.osgi.Builder
import aQute.bnd.osgi.Constants
import aQute.bnd.osgi.Jar
import aQute.bnd.osgi.Processor
import aQute.bnd.version.MavenVersion
import aQute.lib.io.IO
import aQute.lib.strings.Strings
import aQute.lib.utf8properties.UTF8Properties

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.ClasspathNormalizer
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.SourceSet

/**
 * BundleTaskExtension for Gradle.
 *
 * <p>
 * Adds {@code bundle} extension to bundle builder tasks.
 *
 * <p>
 * Properties:
 * <ul>
 * <li>bndfile - This is the name of the bnd file to use to make the bundle.
 * The bndfile does not need
 * to exist. It will override headers in the jar task's manifest.</li>
 * <li>bnd - This is a string holding a bnd file to use to make the bundle.
 * It will override headers in the jar task's manifest.
 * This properties is ignored if bndfile is specified and the specified file
 * exists.</li>
 * <li>sourceSet - This is the SourceSet to use for the
 * bnd builder. It defaults to "project.sourceSets.main".</li>
 * <li>classpath - This is the FileCollection to use for the buildpath
 * for the bnd builder. It defaults to "project.sourceSets.main.compileClasspath".</li>
 * </ul>
 */
class BundleTaskExtension {
	public static final String NAME = "bundle"

	/**
	 * The bndfile property.
	 *
	 * <p>
	 * A bnd file containing bnd instructions for this project.
	 */
	@InputFile
	@PathSensitive(RELATIVE)
	@Optional
	final RegularFileProperty bndfile

	/**
	 * The classpath property.
	 *
	 * <p>
	 * The default value is sourceSets.main.compileClasspath.
	 */
	@InputFiles
	@Classpath
	final ConfigurableFileCollection classpath

	/**
	 * The bnd property.
	 * <p>
	 * If the bndfile property points an existing file, this property is ignored.
	 * Otherwise, the bnd instructions in this property will be used.
	 */
	@Input
	@Optional
	final Provider<String> bnd

	private final ConfigurableFileCollection allSource

	private final org.gradle.api.tasks.bundling.Jar task
	private final ProjectLayout layout
	private final ObjectFactory objects
	private final File buildFile
	private final ListProperty<CharSequence> instructions

	/**
	 * Create a BundleTaskExtension for the specified Jar task.
	 *
	 * <p>
	 * This also sets the default values for the added properties
	 * and adds the bnd file to the task inputs.
	 */
	BundleTaskExtension(org.gradle.api.tasks.bundling.Jar task) {
		this.task = task
		Project project = task.getProject()
		layout = project.getLayout()
		objects = project.getObjects()
		buildFile = project.getBuildFile()
		bndfile = objects.fileProperty()
		instructions = objects.listProperty(CharSequence.class).empty()
		bnd = instructions.map(list -> Strings.join("\n", list))
		classpath = objects.fileCollection()
		allSource = objects.fileCollection()
		SourceSet sourceSet = project.sourceSets.main
		setSourceSet(sourceSet)
		classpath(sourceSet.getCompileClasspath())
		// need to programmatically add to inputs since @InputFiles in a extension is not processed
		task.getInputs().files(getClasspath()).withNormalizer(ClasspathNormalizer.class).withPropertyName("classpath")
		task.getInputs().file(getBndfile()).optional().withPathSensitivity(RELATIVE).withPropertyName("bndfile")
		task.getInputs().property("bnd", getBnd())
	}

	/**
	 * Set the bnd property from a multi-line string.
	 */
	public void setBnd(CharSequence line) {
		instructions.empty()
		bnd(line)
	}

	/**
	 * Add instructions to the bnd property from a list of multi-line strings.
	 */
	public void bnd(CharSequence... lines) {
		instructions.addAll(lines)
	}

	/**
	 * Set the bnd property from a multi-line string using a
	 * {@link Provider<? extends CharSequence>}.
	 */
	public void setBnd(Provider<? extends CharSequence> lines) {
		instructions.empty()
		bnd(lines)
	}

	/**
	 * Add a multi-line string of instructions to the bnd property
	 * using a {@link Provider<? extends CharSequence>}.
	 */
	public void bnd(Provider<? extends CharSequence> lines) {
		instructions.add(lines)
	}

	/**
	 * Set the bnd property from a map.
	 */
	public void setBnd(Map<String, ?> map) {
		instructions.empty()
		bnd(map)
	}

	/**
	 * Add instructions to the bnd property from a map.
	 */
	public void bnd(Map<String, ?> map) {
		ListProperty<CharSequence> list = instructions
		map.forEach((key, value) -> {
			list.add("${key}=${value}")
		})
	}

	/**
	 * Add files to the classpath.
	 *
	 * <p>
	 * The arguments will be handled using
	 * ConfigurableFileCollection.from().
	 */
	public ConfigurableFileCollection classpath(Object... paths) {
		return builtBy(getClasspath().from(paths), paths)
	}

	/**
	 * Set the classpath property.
	 *
	 * <p>
	 * The argument will be handled using
	 * ConfigurableFileCollection.from().
	 */
	public void setClasspath(Object path) {
		getClasspath().setFrom(Collections.emptyList())
		getClasspath().setBuiltBy(Collections.emptyList())
		classpath(path)
	}

	/**
	 * Set the sourceSet.
	 */
	public void setSourceSet(SourceSet sourceSet) {
		allSource.setFrom(sourceSet.getAllSource().getSourceDirectories())
		jarLibraryElements(task, sourceSet.getCompileClasspathConfigurationName())
	}

	void buildBundle() {
		org.gradle.api.tasks.bundling.Jar jarTask = task
		File projectDir = unwrap(layout.getProjectDirectory())
		File buildDir = unwrap(layout.getBuildDirectory())
		File buildFile = this.buildFile
		FileCollection sourcepath = allSource.filter((File file) -> file.exists())
		// create Builder
		Properties gradleProperties = new PropertiesWrapper()
		gradleProperties.put("task", jarTask)
		gradleProperties.put("project", jarTask.getProject())
		try (Builder builder = new Builder(new Processor(gradleProperties, false))) {
			// load bnd properties
			File temporaryBndFile = File.createTempFile("bnd", ".bnd", jarTask.getTemporaryDir())
			try (Writer writer = IO.writer(temporaryBndFile)) {
				// write any task manifest entries into the tmp bnd file
				jarTask.manifest.effectiveManifest.attributes.inject(new UTF8Properties()) { properties, key, value ->
					if (!Objects.equals(key, "Manifest-Version")) {
						properties.setProperty(key, value.toString())
					}
					return properties
				}.replaceHere(projectDir).store(writer, null)
				// if the bnd file exists, add its contents to the tmp bnd file
				File bndfile = unwrap(getBndfile(), true)
				if (bndfile?.isFile()) {
					builder.loadProperties(bndfile).store(writer, null)
				} else {
					String bnd = unwrap(getBnd())
					if (!bnd.isEmpty()) {
						UTF8Properties props = new UTF8Properties()
						props.load(bnd, buildFile, builder)
						props.replaceHere(projectDir).store(writer, null)
					}
				}
			}
			builder.setProperties(temporaryBndFile, projectDir) // this will cause project.dir property to be set
			builder.setProperty("project.output", buildDir.getCanonicalPath())
			// If no bundle to be built, we have nothing to do
			if (builder.is(Constants.NOBUNDLES)) {
				return
			}
			// Reject sub-bundle projects
			if (!Objects.equals(builder.getSubBuilders(), singletonList(builder))) {
				throw new GradleException("Sub-bundles are not supported by this task")
			}
			File archiveFile = unwrap(jarTask.getArchiveFile())
			String archiveFileName = unwrap(jarTask.getArchiveFileName())
			String archiveBaseName = unwrap(jarTask.getArchiveBaseName())
			String archiveClassifier = unwrap(jarTask.getArchiveClassifier())
			String archiveVersion = unwrap(jarTask.getArchiveVersion(), true)

			// Include entire contents of Jar task generated jar (except the manifest)
			File archiveCopyFile = new File(jarTask.getTemporaryDir(), archiveFileName)
			IO.copy(archiveFile, archiveCopyFile)
			Jar bundleJar = new Jar(archiveFileName, archiveCopyFile)
			String reproducible = builder.getProperty(Constants.REPRODUCIBLE)
			bundleJar.setReproducible(Objects.nonNull(reproducible) ? Processor.isTrue(reproducible) : !jarTask.isPreserveFileTimestamps())
			bundleJar.updateModified(archiveFile.lastModified(), "time of Jar task generated jar")
			bundleJar.setManifest(new Manifest())
			builder.setJar(bundleJar)

			// set builder classpath
			FileCollection buildpath = getClasspath().filter((File file) -> {
				if (!file.exists()) {
					return false
				}
				if (file.isDirectory()) {
					return true
				}
				try (ZipFile zip = new ZipFile(file)) {
					zip.entries() // make sure it is a valid zip file and not a pom
				} catch (ZipException e) {
					return false
				}
				return true
			})
			builder.setProperty("project.buildpath", buildpath.getAsPath())
			builder.setClasspath(buildpath.getFiles() as File[])
			jarTask.getLogger().debug("builder classpath: {}", builder.getClasspath()*.getSource())
			// set builder sourcepath
			builder.setProperty("project.sourcepath", sourcepath.getAsPath())
			builder.setSourcepath(sourcepath.getFiles() as File[])
			jarTask.getLogger().debug("builder sourcepath: {}", builder.getSourcePath())
			// set bundle symbolic name from tasks's archiveBaseName property if necessary
			String bundleSymbolicName = builder.getProperty(Constants.BUNDLE_SYMBOLICNAME)
			if (isEmpty(bundleSymbolicName)) {
				bundleSymbolicName = archiveClassifier.isEmpty() ? archiveBaseName : "${archiveBaseName}-${archiveClassifier}"
				builder.setProperty(Constants.BUNDLE_SYMBOLICNAME, bundleSymbolicName)
			}

			// set bundle version from task's archiveVersion if necessary
			String bundleVersion = builder.getProperty(Constants.BUNDLE_VERSION)
			if (isEmpty(bundleVersion)) {
				builder.setProperty(Constants.BUNDLE_VERSION, MavenVersion.parseMavenString(archiveVersion).getOSGiVersion().toString())
			}

			jarTask.getLogger().debug("builder properties: {}", builder.getProperties())

			// Build bundle
			Jar builtJar = builder.build()
			if (!builder.isOk()) {
				// if we already have an error; fail now
				logReport(builder, jarTask.getLogger())
				failTask("Bundle ${archiveFileName} has errors", archiveFile)
			}

			// Write out the bundle
			builtJar.write(archiveFile)
			long now = System.currentTimeMillis()
			archiveFile.setLastModified(now)

			logReport(builder, jarTask.getLogger())
			if (!builder.isOk()) {
				failTask("Bundle ${archiveFileName} has errors", archiveFile)
			}
		}
	}

	private void failTask(String msg, File archiveFile) {
		IO.delete(archiveFile)
		throw new GradleException(msg)
	}

	private boolean isEmpty(String header) {
		return Objects.isNull(header) || header.trim().isEmpty() || Constants.EMPTY_HEADER.equals(header)
	}
}
