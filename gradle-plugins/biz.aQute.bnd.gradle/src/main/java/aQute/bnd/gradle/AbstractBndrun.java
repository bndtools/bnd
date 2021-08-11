package aQute.bnd.gradle;

import static aQute.bnd.gradle.BndUtils.builtBy;
import static aQute.bnd.gradle.BndUtils.logReport;
import static aQute.bnd.gradle.BndUtils.sourceSets;
import static aQute.bnd.gradle.BndUtils.unwrap;
import static org.gradle.api.tasks.PathSensitivity.RELATIVE;

import java.io.File;
import java.util.Collections;
import java.util.Objects;
import java.util.Properties;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Processor;
import aQute.bnd.repository.fileset.FileSetRepository;
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.io.IO;

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

	/**
	 * The bndrun file for the execution.
	 *
	 * @return The bndrun file for the execution.
	 */
	@InputFile
	@PathSensitive(RELATIVE)
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
		ObjectFactory objects = project.getObjects();
		bndrun = objects.fileProperty();
		DirectoryProperty temporaryDirProperty = objects.directoryProperty()
			.fileValue(getTemporaryDir());
		workingDirectory = objects.directoryProperty()
			.convention(temporaryDirProperty);
		bundles = objects.fileCollection();
		if (project.hasProperty("bndWorkspace")) {
			// bundles must not be used for Bnd workspace builds
			bundles.disallowChanges();
		} else {
			SourceSet mainSourceSet = sourceSets(project).getByName(SourceSet.MAIN_SOURCE_SET_NAME);
			Configuration archivesConfiguration = project.getConfigurations()
				.getByName("archives");
			bundles(mainSourceSet.getRuntimeClasspath());
			bundles(archivesConfiguration.getArtifacts()
				.getFiles());
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
		File bndrunFile = unwrap(getBndrun());
		File workingDirFile = unwrap(getWorkingDirectory());
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
			}
			run.setBase(workingDirFile);
			if (run.isStandalone()) {
				runWorkspace.setOffline(Objects.nonNull(workspace) ? workspace.isOffline()
					: getProject().getGradle()
						.getStartParameter()
						.isOffline());
				File cnf = new File(workingDirFile, Workspace.CNFDIR);
				IO.mkdirs(cnf);
				runWorkspace.setBuildDir(cnf);
				if (Objects.isNull(workspace)) {
					FileSetRepository fileSetRepository = new FileSetRepository(getName(), getBundles().getFiles());
					runWorkspace.addBasicPlugin(fileSetRepository);
					for (RepositoryPlugin repo : runWorkspace.getRepositories()) {
						repo.list(null);
					}
				}
			}
			run.getInfo(runWorkspace);
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
}
