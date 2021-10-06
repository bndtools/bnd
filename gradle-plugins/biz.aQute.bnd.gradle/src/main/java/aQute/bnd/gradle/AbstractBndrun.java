package aQute.bnd.gradle;

import static aQute.bnd.exceptions.FunctionWithException.asFunctionOrElse;
import static aQute.bnd.gradle.BndUtils.builtBy;
import static aQute.bnd.gradle.BndUtils.logReport;
import static aQute.bnd.gradle.BndUtils.sourceSets;
import static aQute.bnd.gradle.BndUtils.unwrapFile;
import static org.gradle.api.tasks.PathSensitivity.RELATIVE;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.work.NormalizeLineEndings;

import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.EE;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Processor;
import aQute.bnd.repository.fileset.FileSetRepository;
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;

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
 * </ul>
 */
public abstract class AbstractBndrun<WORKER extends aQute.bnd.build.Project, RUN extends WORKER> extends DefaultTask {
	private final RegularFileProperty			bndrun;
	private final ConfigurableFileCollection	bundles;
	private boolean								ignoreFailures	= false;
	private final DirectoryProperty				workingDirectory;
	private final String						projectName;
	private final Provider<String>				targetVersion;
	private final FileCollection				artifacts;

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
	@InputFiles
	@PathSensitive(RELATIVE)
	public ConfigurableFileCollection getBundles() {
		return bundles;
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
	 * Create a Bndrun task.
	 */
	@SuppressWarnings("deprecation")
	public AbstractBndrun() {
		super();
		Project project = getProject();
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
				.map(r -> r.toString())
				.getOrElse(t.getTargetCompatibility()));
		Configuration archivesConfiguration = project.getConfigurations()
			.getByName(Dependency.ARCHIVES_CONFIGURATION);
		artifacts = archivesConfiguration.getArtifacts()
			.getFiles();
		if (project.hasProperty("bndWorkspace")) {
			// bundles must not be used for Bnd workspace builds
			bundles.disallowChanges();
		} else {
			bundles(mainSourceSet.getRuntimeClasspath());
			bundles(artifacts);
			// We add this in case someone actually looks for this convention
			getConvention().getPlugins()
				.put("bundles", new FileSetRepositoryConvention(this));
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
	 * Setup the Run object and call worker on it.
	 *
	 * @throws Exception If the run action has an exception.
	 */
	@TaskAction
	public void bndrunAction() throws Exception {
		Workspace workspace = (Workspace) getProject().findProperty("bndWorkspace");
		File bndrunFile = unwrapFile(getBndrun());
		File workingDirFile = unwrapFile(getWorkingDirectory());
		if (Objects.nonNull(workspace) && getProject().getPlugins()
			.hasPlugin(BndPlugin.PLUGINID)) {
			BndPluginExtension extension = getProject().getExtensions()
				.getByType(BndPluginExtension.class);
			if (Objects.equals(bndrunFile, extension.getProject()
				.getPropertiesFile())) {
				@SuppressWarnings("unchecked")
				WORKER worker = (WORKER) extension.getProject();
				worker(worker);
				return;
			}
		}
		try (RUN run = createRun(workspace, bndrunFile)) {
			Workspace runWorkspace = run.getWorkspace();
			IO.mkdirs(workingDirFile);
			if (Objects.isNull(workspace)) {
				Properties gradleProperties = new BeanProperties(runWorkspace.getProperties());
				gradleProperties.put("task", this);
				gradleProperties.put("project", getProject());
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
				runWorkspace.setOffline(Objects.nonNull(workspace) ? workspace.isOffline()
					: getProject().getGradle()
						.getStartParameter()
						.isOffline());
				if (Objects.isNull(workspace)) {
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
	 * @throws Exception If the create action has an exception.
	 */
	protected RUN createRun(Workspace workspace, File bndrunFile) throws Exception {
		@SuppressWarnings("unchecked")
		RUN run = (RUN) Run.createRun(workspace, bndrunFile);
		return run;
	}

	/**
	 * Execute the Run object.
	 *
	 * @param run The Run object.
	 * @throws Exception If the worker action has an exception.
	 */
	abstract protected void worker(WORKER run) throws Exception;

	/**
	 * Set -runee from the build environment if not already set in the Run
	 * object.
	 *
	 * @param run The Run object.
	 */
	protected void inferRunEE(RUN run) {
		String runee = run.getProperty(Constants.RUNEE);
		if (Objects.isNull(runee)) {
			runee = Optional.ofNullable(targetVersion.getOrElse(System.getProperty("java.specification.version")))
				.flatMap(EE::highestFromTargetVersion)
				.orElse(EE.JavaSE_1_8) // Fallback to Java 8
				.getEEName();
			run.setProperty(Constants.RUNEE, runee);
			getLogger().info("Bnd inferred {}: {}", Constants.RUNEE, run.getProperty(Constants.RUNEE));
		}
	}

	/**
	 * Set -runrequires from the build environment if not already set in the Run
	 * object.
	 *
	 * @param run The Run object.
	 */
	protected void inferRunRequires(RUN run) {
		String runrequires = run.getProperty(Constants.RUNREQUIRES);
		if (Objects.isNull(runrequires) && !artifacts.isEmpty()) {
			runrequires = artifacts.getFiles()
				.stream()
				.filter(File::isFile)
				.map(file -> Optional.of(file)
					.map(asFunctionOrElse(Domain::domain, null))
					.map(Domain::getBundleSymbolicName)
					.map(Map.Entry::getKey)
					.orElse(projectName))
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
