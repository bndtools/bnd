package aQute.bnd.gradle;

import static aQute.bnd.exceptions.FunctionWithException.asFunctionOrElse;
import static aQute.bnd.gradle.BndUtils.builtBy;
import static aQute.bnd.gradle.BndUtils.logReport;
import static aQute.bnd.gradle.BndUtils.sourceSets;
import static aQute.bnd.gradle.BndUtils.unwrap;
import static aQute.bnd.gradle.BndUtils.unwrapFile;
import static aQute.bnd.gradle.BndUtils.unwrapOptional;
import static org.gradle.api.tasks.PathSensitivity.RELATIVE;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.EE;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Processor;
import aQute.bnd.repository.fileset.FileSetRepository;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.unmodifiable.Maps;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.work.DisableCachingByDefault;
import org.gradle.work.NormalizeLineEndings;

/**
 * Abstract Bndrun task type for Gradle.
 * <p>
 * This abstract task type is the super type of the bndrun based task types.
 * <p>
 * Properties:
 * <ul>
 * <li>bndrun - This is the bndrun file to be run. This property must be
 * set.</li>
 * <li>bundles - The bundles to added to a FileSetRepository for non-Bnd
 * Workspace builds. The default is "sourceSets.main.runtimeClasspath" plus
 * "configurations.archives.artifacts.files". This must not be used for Bnd
 * Workspace builds.</li>
 * <li>ignoreFailures - If true the task will not fail if the execution fails.
 * The default is false.</li>
 * <li>workingDirectory - This is the directory for the execution. The default
 * for workingDirectory is temporaryDir.</li>
 * <li>properties - Properties that are available for evaluation of the bnd
 * instructions for non-Bnd Workspace builds. The default is the properties of
 * the task and project objects. This must not be used for Bnd Workspace
 * builds.</li>
 * </ul>
 */
@DisableCachingByDefault(because = "Abstract base class; not used directly")
public abstract class AbstractBndrun extends DefaultTask {
	private final RegularFileProperty			bndrun;
	private final ConfigurableFileCollection	bundles;
	private boolean								ignoreFailures	= false;
	private final DirectoryProperty				workingDirectory;
	private final String						projectName;
	private final Provider<String>				targetVersion;
	private final FileCollection				artifacts;
	private final MapProperty<String, Object>	properties;
	private final Property<Boolean>				offline;
	private final Property<Project>				bndProject;
	private final Property<Workspace>			bndWorkspace;

	/**
	 * The bndrun file for the execution.
	 *
	 * @return The bndrun file for the execution.
	 */
	@InputFile
	@PathSensitive(RELATIVE)
	@NormalizeLineEndings
	public RegularFileProperty getBndrun() {
		return bndrun;
	}

	/**
	 * The bundles to be added to a FileSetRepository for non-Bnd Workspace
	 * builds.
	 * <p>
	 * This must not be used for Bnd Workspace builds.
	 *
	 * @return The bundles to be added to a FileSetRepository for non-Bnd
	 *         Workspace builds.
	 */
	@Internal
	public ConfigurableFileCollection getBundles() {
		return bundles;
	}

	/**
	 * Wrapper of the {@link #getBundles()} method to allow <code>@Classpath</code> normalization and sorting.
	 * <p>
	 * This method is only relevant for Gradle task input fingerprinting and should not be used.
	 *
	 * @return The sorted bundles.
	 */
	@Classpath
	Provider<List<File>> getBundlesSorted() {
		return getBundles().getElements().map(
			c -> c.stream()
				.map(FileSystemLocation::getAsFile)
				.sorted(IO.fileComparator(File::getAbsolutePath))
				.collect(Collectors.toList())
		);
	}

	/**
	 * Whether execution failures should be ignored.
	 *
	 * @return <code>true</code> if execution failures will not fail the task.
	 *         Otherwise, an execution failure will fail the task. The default
	 *         is <code>false</code>.
	 */
	@Input
	public boolean isIgnoreFailures() {
		return ignoreFailures;
	}

	/**
	 * Whether execution failures should be ignored.
	 * <p>
	 * Alias for {@link #isIgnoreFailures()}.
	 *
	 * @return <code>true</code> if execution failures will not fail the task.
	 *         Otherwise, an execution failure will fail the task. The default
	 *         is <code>false</code>.
	 */
	@Internal
	public boolean getIgnoreFailures() {
		return isIgnoreFailures();
	}

	/**
	 * Set whether execution failures should be ignored.
	 *
	 * @param ignoreFailures If <code>true</code>, then execution failures will
	 *            not fail the task. Otherwise, an execution failure will fail
	 *            the task. The default is <code>false</code>.
	 */
	public void setIgnoreFailures(boolean ignoreFailures) {
		this.ignoreFailures = ignoreFailures;
	}

	/**
	 * The working directory for the execution.
	 *
	 * @return The working directory for the execution.
	 */
	@Internal
	public DirectoryProperty getWorkingDirectory() {
		return workingDirectory;
	}

	/**
	 * Properties that are available for evaluation of the bnd instructions for
	 * non-Bnd Workspace builds.
	 * <p>
	 * This must not be used for Bnd Workspace builds.
	 * <p>
	 * If this property is not set, the properties of the following are
	 * available:
	 * <dl>
	 * <dt>{@code task}</dt>
	 * <dd>This Task object.</dd>
	 * <dt>{@code project}</dt>
	 * <dd>The Project object for this task.</dd>
	 * </dl>
	 * If the {@code task} property is not set, the properties of this Task
	 * object will automatically be available.
	 * <p>
	 * Note: The defaults for this property use the Project object which makes
	 * the task ineligible for the Gradle configuration cache. If you want to
	 * use this task with the Gradle configuration cache, you must set this
	 * property to ensure it does not use the Project object. Of course, this
	 * then means you cannot use <code>${project.xxx}</code> style expressions
	 * in the bnd instructions unless you set those values in this property.
	 *
	 * @return Properties available for evaluation of the bnd instructions.
	 */
	@Input
	public MapProperty<String, Object> getProperties() {
		return properties;
	}

	@Internal
	String getProjectName() {
		return projectName;
	}

	@Internal
	Provider<String> getTargetVersion() {
		return targetVersion;
	}

	@Internal
	FileCollection getArtifacts() {
		return artifacts;
	}

	@Internal
	Provider<Boolean> getOffline() {
		return offline;
	}

	@Internal
	Provider<Workspace> getBndWorkspace() {
		return bndWorkspace;
	}

	@Internal
	Provider<Project> getBndProject() {
		return bndProject;
	}

	/**
	 * Create a Bndrun task.
	 */
	public AbstractBndrun() {
		super();
		org.gradle.api.Project project = getProject();
		projectName = project.getName();
		ObjectFactory objects = project.getObjects();
		bndrun = objects.fileProperty();
		DirectoryProperty temporaryDirProperty = objects.directoryProperty()
			.fileValue(getTemporaryDir());
		workingDirectory = objects.directoryProperty()
			.convention(temporaryDirProperty);
		bundles = objects.fileCollection();
		SourceSet mainSourceSet = sourceSets(project).getByName(SourceSet.MAIN_SOURCE_SET_NAME);
		targetVersion = project.getTasks()
			.named(mainSourceSet.getCompileJavaTaskName(), JavaCompile.class)
			.map(t -> t.getOptions()
				.getRelease()
				.map(Object::toString)
				.getOrElse(t.getTargetCompatibility()));
		Configuration archivesConfiguration = project.getConfigurations()
			.getByName(Dependency.ARCHIVES_CONFIGURATION);
		artifacts = archivesConfiguration.getArtifacts()
			.getFiles();
		properties = objects.mapProperty(String.class, Object.class);
		offline = objects.property(Boolean.class)
			.convention(Boolean.valueOf(project.getGradle()
				.getStartParameter()
				.isOffline()));
		bndWorkspace = objects.property(Workspace.class)
			.value((Workspace) project.findProperty("bndWorkspace"));
		bndProject = objects.property(Project.class);

		if (bndWorkspace.isPresent()) {
			// bundles and properties must not be used for Bnd workspace builds
			bundles.disallowChanges();
			properties.disallowChanges();
			if (project.getPluginManager()
				.hasPlugin(BndPlugin.PLUGINID)) {
				BndPluginExtension extension = project.getExtensions()
					.getByType(BndPluginExtension.class);
				bndProject.value(extension.getProject());
			}
			offline.value(bndWorkspace.map(ws -> Boolean.valueOf(ws.isOffline())));
		} else {
			bundles(mainSourceSet.getRuntimeClasspath());
			bundles(artifacts);
			properties.convention(Maps.of("project", "__convention__"));
		}
	}

	/**
	 * Add files to use when locating bundles.
	 *
	 * @param paths The arguments will be handled using
	 *            ConfigurableFileCollection.from().
	 * @return The bundles to be added to a FileSetRepository for non-Bnd
	 *         Workspace builds.
	 */
	public ConfigurableFileCollection bundles(Object... paths) {
		return builtBy(getBundles().from(paths), paths);
	}

	/**
	 * Set the files to use when locating bundles.
	 * <p>
	 * The argument will be handled using ConfigurableFileCollection.from().
	 *
	 * @param path The argument will be handled using
	 *            ConfigurableFileCollection.from().
	 */
	public void setBundles(Object path) {
		getBundles().setFrom(Collections.emptyList());
		getBundles().setBuiltBy(Collections.emptyList());
		bundles(path);
	}

	/**
	 * Set up the Run object and call worker on it.
	 *
	 * @throws Exception If the run action has an exception.
	 */
	@TaskAction
	public void bndrunAction() throws Exception {
		File bndrunFile = unwrapFile(getBndrun());
		Optional<Project> project = unwrapOptional(getBndProject());
		if (project.map(Processor::getPropertiesFile)
			.filter(bndrunFile::equals)
			.isPresent()) {
			worker(project.get());
			return;
		}
		File workingDirFile = unwrapFile(getWorkingDirectory());
		Optional<Workspace> workspace = unwrapOptional(getBndWorkspace());
		try (biz.aQute.resolve.Bndrun run = createBndrun(workspace.orElse(null), bndrunFile)) {
			Workspace runWorkspace = run.getWorkspace();
			IO.mkdirs(workingDirFile);
			if (workspace.isEmpty()) {
				Properties gradleProperties = new BeanProperties(runWorkspace.getProperties());
				gradleProperties.putAll(unwrap(getProperties()));
				gradleProperties.computeIfPresent("project", (k, v) -> "__convention__".equals(v) ? getProject() : v);
				gradleProperties.putIfAbsent("task", this);
				run.setParent(new Processor(runWorkspace, gradleProperties, false));
				run.clear();
				run.forceRefresh(); // setBase must be called after forceRefresh
			}
			run.setBase(workingDirFile);
			if (run.isStandalone()) {
				runWorkspace.setBase(workingDirFile);
				File cnf = new File(workingDirFile, Workspace.CNFDIR);
				IO.mkdirs(cnf);
				runWorkspace.setBuildDir(cnf);
				runWorkspace.setOffline(unwrap(getOffline()).booleanValue());
				if (workspace.isEmpty()) {
					FileSetRepository fileSetRepository = new FileSetRepository(getName(), getBundles().getFiles());
					runWorkspace.addBasicPlugin(fileSetRepository);
					for (RepositoryPlugin repo : runWorkspace.getRepositories()) {
						repo.list(null);
					}
				}
			}
			run.getInfo(runWorkspace);
			inferRunEE(run);
			inferRunRequires(run);
			logReport(run, getLogger());
			if (!run.isOk()) {
				throw new GradleException(String.format("%s workspace errors", run.getPropertiesFile()));
			}

			worker(run);
		}
	}

	/**
	 * Create the RUN object.
	 *
	 * @param workspace The workspace for the RUN.
	 * @param bndrunFile The bndrun file for the RUN.
	 * @return The RUN object.
	 * @throws Exception If the creation action has an exception.
	 */
	protected biz.aQute.resolve.Bndrun createBndrun(Workspace workspace, File bndrunFile) throws Exception {
		return biz.aQute.resolve.Bndrun.createBndrun(workspace, bndrunFile);
	}

	/**
	 * Execute the Project object.
	 *
	 * @param run The Project object.
	 * @throws Exception If the worker action has an exception.
	 */
	abstract protected void worker(Project run) throws Exception;

	/**
	 * Set -runee from the build environment if not already set in the Processor
	 * object.
	 *
	 * @param run The Processor object.
	 */
	protected void inferRunEE(Processor run) {
		String runee = run.getProperty(Constants.RUNEE);
		if (Objects.isNull(runee)) {
			runee = Optional.ofNullable(getTargetVersion().getOrElse(System.getProperty("java.specification.version")))
				.flatMap(EE::highestFromTargetVersion)
				.orElse(EE.JavaSE_1_8) // Fallback to Java 8
				.getEEName();
			run.setProperty(Constants.RUNEE, runee);
			getLogger().info("Bnd inferred {}: {}", Constants.RUNEE, run.getProperty(Constants.RUNEE));
		}
	}

	/**
	 * Set -runrequires from the build environment if not already set in the
	 * Processor object.
	 *
	 * @param run The Processor object.
	 */
	protected void inferRunRequires(Processor run) {
		String runrequires = run.getProperty(Constants.RUNREQUIRES);
		if (Objects.isNull(runrequires) && !getArtifacts().isEmpty()) {
			runrequires = getArtifacts().getFiles()
				.stream()
				.filter(File::isFile)
				.map(file -> Optional.of(file)
					.map(asFunctionOrElse(Domain::domain, null))
					.map(Domain::getBundleSymbolicName)
					.map(Map.Entry::getKey)
					.orElseGet(this::getProjectName))
				.distinct()
				.map(bsn -> String.format("osgi.identity;filter:='(osgi.identity=%s)'", bsn))
				.collect(Strings.joining());
			if (!runrequires.isEmpty()) {
				run.setProperty(Constants.RUNREQUIRES, runrequires);
				getLogger().info("Bnd inferred {}: {}", Constants.RUNREQUIRES, run.getProperty(Constants.RUNREQUIRES));
			}
		}
	}
}
