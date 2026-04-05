package aQute.bnd.gradle;

import static aQute.bnd.gradle.BndUtils.isGradleCompatible;
import static aQute.bnd.gradle.BndUtils.jarLibraryElements;
import static aQute.bnd.gradle.BndUtils.logReport;
import static aQute.bnd.gradle.BndUtils.sourceSets;
import static aQute.bnd.gradle.BndUtils.unwrap;
import static aQute.bnd.gradle.BndUtils.unwrapFile;
import static aQute.bnd.osgi.Processor.isTrue;
import static aQute.bnd.osgi.Processor.removeDuplicateMarker;
import static java.lang.invoke.MethodHandles.publicLookup;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.gradle.api.tasks.PathSensitivity.RELATIVE;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

import aQute.bnd.build.Container;
import aQute.bnd.build.Container.TYPE;
import aQute.bnd.build.Project.ReleaseParameter;
import aQute.bnd.build.Workspace;
import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.exporter.executable.ExecutableJarExporter;
import aQute.bnd.exporter.runbundles.RunbundlesExporter;
import aQute.bnd.osgi.About;
import aQute.bnd.osgi.Constants;
import aQute.bnd.stream.MapStream;
import aQute.bnd.unmodifiable.Maps;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.UnknownDomainObjectException;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.ArtifactHandler;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.logging.Logger;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.BasePluginExtension;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.HelpTasksPlugin;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.publish.plugins.PublishingPlugin;
import org.gradle.api.tasks.ClasspathNormalizer;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.AbstractTestTask;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.gradle.process.CommandLineArgumentProvider;
import org.slf4j.LoggerFactory;

/**
 * BndPlugin for Gradle.
 * <p>
 * The plugin name is {@code biz.aQute.bnd}.
 * <p>
 * If the bndWorkspace property is set, it will be used for the bnd Workspace.
 * <p>
 * If the bnd_defaultTask property is set, it will be used for the default
 * task.
 */
public class BndPlugin implements Plugin<Project> {
	private static final org.slf4j.Logger	logger		= LoggerFactory.getLogger(BndPlugin.class);
	/**
	 * Name of the plugin.
	 */
	public static final String				PLUGINID	= "biz.aQute.bnd";
	private Project							project;
	private Project							workspace;
	private ObjectFactory					objects;
	private aQute.bnd.build.Project			bndProject;

	/**
	 * Default public constructor.
	 */
	public BndPlugin() {}

	/**
	 * Apply the {@code biz.aQute.bnd} plugin to the specified project.
	 */
	@Override
	public void apply(Project project) {
		try {
			this.project = project;
			Project workspace = this.workspace = project.getParent();
			ProjectLayout layout = project.getLayout();
			ObjectFactory objects = this.objects = project.getObjects();
			TaskContainer tasks = project.getTasks();
			if (project.getPluginManager()
				.hasPlugin(BndBuilderPlugin.PLUGINID)) {
				throw new GradleException("Project already has \"" + BndBuilderPlugin.PLUGINID + "\" plugin applied.");
			}
			if (Objects.isNull(workspace)) {
				throw new GradleException("The \"" + PLUGINID
					+ "\" plugin cannot be applied to the root project. Perhaps you meant to use the \""
					+ BndBuilderPlugin.PLUGINID + "\" plugin?");
			}
			Workspace bndWorkspace = BndWorkspacePlugin.getBndWorkspace(workspace);
			aQute.bnd.build.Project bndProject = this.bndProject = bndWorkspace.getProject(project.getName());
			if (Objects.isNull(bndProject)) {
				throw new GradleException(String.format("Unable to load bnd project %s from workspace %s",
					project.getName(), workspace.getLayout()
						.getProjectDirectory()));
			}
			bndProject.prepare();
			if (!bndProject.isValid()) {
				checkErrors(project.getLogger());
				throw new GradleException(String.format("Project %s is not a valid bnd project", bndProject.getName()));
			}
			BndPluginExtension extension = project.getExtensions()
				.create(BndPluginExtension.NAME, BndPluginExtension.class, bndProject);

			layout.getBuildDirectory()
				.fileValue(bndProject.getTargetDir());
			project.getPluginManager()
				.apply("java");
			project.getExtensions()
				.getByType(BasePluginExtension.class)
				.getLibsDirectory()
				.value(layout.getBuildDirectory());
			project.getExtensions()
				.getByType(JavaPluginExtension.class)
				.getTestResultsDir()
				.value(layout.getBuildDirectory()
					.dir(bndProject.getProperty("test-reports", "test-reports")));
			String bnd_defaultTask = (String) project.findProperty("bnd_defaultTask");
			if (Objects.nonNull(bnd_defaultTask)) {
				project.setDefaultTasks(Strings.split(bnd_defaultTask));
			}

			/* Set up configurations */
			ConfigurationContainer configurations = project.getConfigurations();
			configurations.getByName(JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME)
				.getArtifacts()
				.clear();
			if (!isGradleCompatible("9.0")) { // only for pre 9.0
				//@SuppressWarnings("deprecation")
				configurations.getByName(Dependency.ARCHIVES_CONFIGURATION)
					.getArtifacts()
					.clear();
			}
			/* Set up deliverables */
			ArtifactHandler artifacts = project.getArtifacts();
			decontainer(bndProject.getDeliverables()).forEach(deliverable -> {
				artifacts.add(JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME, deliverable,
					cpa -> cpa.builtBy(JavaPlugin.JAR_TASK_NAME));
				if (!isGradleCompatible("9.0")) { // only for pre 9.0
					artifacts.add(Dependency.ARCHIVES_CONFIGURATION, deliverable,
						cpa -> cpa.builtBy(JavaPlugin.JAR_TASK_NAME));
				}
			});
			FileCollection deliverables = configurations.getByName(JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME)
				.getArtifacts()
				.getFiles();

			/* Set up Bnd generate support */
			Optional<TaskProvider<Task>> generate = bndProject.getGenerate()
				.getInputs()
				.value()
				.filter(files -> !files.isEmpty())
				.map(files -> tasks.register("generate", t -> {
					t.setDescription("Generate source code");
					t.setGroup(LifecycleBasePlugin.BUILD_GROUP);
					t.getInputs()
						.files(files)
						.withPathSensitivity(RELATIVE)
						.withPropertyName("generateInputs");
					/* bnd can include from -dependson */
					t.getInputs()
						.files(getBuildDependencies(JavaPlugin.JAR_TASK_NAME))
						.withPropertyName("buildDependencies");
					/*
					 * Workspace and project configuration changes should
					 * trigger task
					 */
					t.getInputs()
						.files(bndConfiguration())
						.withPathSensitivity(RELATIVE)
						.withPropertyName("bndConfiguration")
						.normalizeLineEndings();

					t.getOutputs()
						.dirs(bndProject.getGenerate()
							.getOutputDirs())
						.withPropertyName("generateOutputs");
					t.doLast("generate", new Action<>() {
						@Override
						public void execute(Task tt) {
							try {
								bndProject.getGenerate()
									.generate(false);
							} catch (Exception e) {
								throw new GradleException(String.format("Project %s failed to generate", bndProject.getName()), e);
							}
							checkErrors(tt.getLogger());
						}
					});
				}));
			Optional<Action<Task>> generateInputAction = generate.map(generateTask -> t -> {
				t.getInputs()
					.files(generateTask)
					.withPropertyName(generateTask.getName());
			});

			/* Set up source sets */
			SourceSetContainer sourceSets = sourceSets(project);
			/* bnd uses the same directory for java and resources. */
			sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME, sourceSet -> {
				ConfigurableFileCollection srcDirs;
				try {
					srcDirs = objects.fileCollection()
						.from(bndProject.getSourcePath());
				} catch (Exception e) {
					throw Exceptions.duck(e);
				}
				File destinationDir = bndProject.getSrcOutput();
				sourceSet.getJava()
					.setSrcDirs(srcDirs);
				sourceSet.getResources()
					.setSrcDirs(srcDirs);
				sourceSet.getJava()
					.getDestinationDirectory()
					.fileValue(destinationDir);
				sourceSet.getResources()
					.getDestinationDirectory()
					.fileValue(destinationDir);
				TaskProvider<AbstractCompile> compileTask = tasks.named(sourceSet.getCompileJavaTaskName(),
					AbstractCompile.class, t -> {
						t.getDestinationDirectory()
							.fileValue(destinationDir);
						FileCollection jarLibraryElements = jarLibraryElements(t, sourceSet.getCompileClasspathConfigurationName());
						t.setClasspath(jarLibraryElements.plus(t.getClasspath()));
					});
				generateInputAction.ifPresent(compileTask::configure);
				sourceSet.getOutput()
					.dir(Maps.of("builtBy", compileTask.getName()), destinationDir);
				TaskProvider<Task> processResourcesTask = tasks.named(sourceSet.getProcessResourcesTaskName());
				generateInputAction.ifPresent(processResourcesTask::configure);
			});
			sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME, sourceSet -> {
				ConfigurableFileCollection srcDirs = objects.fileCollection()
					.from(bndProject.getTestSrc());
				File destinationDir = bndProject.getTestOutput();
				sourceSet.getJava()
					.setSrcDirs(srcDirs);
				sourceSet.getResources()
					.setSrcDirs(srcDirs);
				sourceSet.getJava()
					.getDestinationDirectory()
					.fileValue(destinationDir);
				sourceSet.getOutput()
					.setResourcesDir(destinationDir);
				TaskProvider<AbstractCompile> compileTask = tasks.named(sourceSet.getCompileJavaTaskName(),
					AbstractCompile.class, t -> {
						t.getDestinationDirectory()
							.fileValue(destinationDir);
						FileCollection jarLibraryElements = jarLibraryElements(t, sourceSet.getCompileClasspathConfigurationName());
						t.setClasspath(jarLibraryElements.plus(t.getClasspath()));
					});
				sourceSet.getOutput()
					.dir(Maps.of("builtBy", compileTask.getName()), destinationDir);
			});
			/* Configure srcDirs for any additional languages */
			project.afterEvaluate(p -> {
				sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME, sourceSet -> {
					Map<String, SourceDirectorySet> sourceDirectorySets = new LinkedHashMap<>();
					ExtensionContainer extensions = sourceSet.getExtensions();
					extensions.getExtensionsSchema()
						.forEach(schema -> {
							String name = schema.getName();
							Object sds = extensions.getByName(name);
							if (sds instanceof SourceDirectorySet sourceDirectorySet) {
								sourceDirectorySets.put(name, sourceDirectorySet);
							}
						});
					Provider<Directory> destinationDir = sourceSet.getJava()
						.getClassesDirectory();
					TaskProvider<Task> processResourcesTask = tasks.named(sourceSet.getProcessResourcesTaskName());
					sourceDirectorySets.forEach((name, sourceDirectorySet) -> {
						try {
							TaskProvider<AbstractCompile> compileTask = tasks.named(sourceSet.getCompileTaskName(name),
								AbstractCompile.class, t -> {
									t.getDestinationDirectory()
										.value(destinationDir);
									t.getInputs()
										.files(processResourcesTask)
										.withPropertyName(processResourcesTask.getName());
								});
							generateInputAction.ifPresent(compileTask::configure);
							sourceDirectorySet.setSrcDirs(sourceSet.getJava()
								.getSrcDirs());
							sourceDirectorySet.getDestinationDirectory()
								.value(destinationDir);
							sourceSet.getOutput()
								.dir(Maps.of("builtBy", compileTask.getName()), destinationDir);
						} catch (UnknownDomainObjectException e) {
							// no such task
						}
					});
				});
				sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME, sourceSet -> {
					Map<String, SourceDirectorySet> sourceDirectorySets = new LinkedHashMap<>();
					ExtensionContainer extensions = sourceSet.getExtensions();
					extensions.getExtensionsSchema()
						.forEach(schema -> {
							String name = schema.getName();
							Object sds = extensions.getByName(name);
							if (sds instanceof SourceDirectorySet sourceDirectorySet) {
								sourceDirectorySets.put(name, sourceDirectorySet);
							}
						});
					Provider<Directory> destinationDir = sourceSet.getJava()
						.getClassesDirectory();
					TaskProvider<Task> processResourcesTask = tasks.named(sourceSet.getProcessResourcesTaskName());
					sourceDirectorySets.forEach((name, sourceDirectorySet) -> {
						try {
							TaskProvider<AbstractCompile> compileTask = tasks.named(sourceSet.getCompileTaskName(name),
								AbstractCompile.class, t -> {
									t.getDestinationDirectory()
										.value(destinationDir);
									t.getInputs()
										.files(processResourcesTask)
										.withPropertyName(processResourcesTask.getName());
								});
							sourceDirectorySet.setSrcDirs(sourceSet.getJava()
								.getSrcDirs());
							sourceDirectorySet.getDestinationDirectory()
								.value(destinationDir);
							sourceSet.getOutput()
								.dir(Maps.of("builtBy", compileTask.getName()), destinationDir);
						} catch (UnknownDomainObjectException e) {
							// no such task
						}
					});

				});
			});

			FileCollection allSrcDirs = objects.fileCollection()
				.from(bndProject.getAllsourcepath());
			extension.getExtensions()
				.getExtraProperties()
				.set("allSrcDirs", allSrcDirs);
			/* Set up dependencies */
			DependencyHandler dependencies = project.getDependencies();
			dependencies.add(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, pathFiles(bndProject.getBuildpath()));
			dependencies.add(JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME, objects.fileCollection()
				.from(bndProject.getSrcOutput()));
			dependencies.add(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, pathFiles(bndProject.getTestpath()));
			dependencies.add(JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME, objects.fileCollection()
				.from(bndProject.getTestOutput()));
			/* Set up compile tasks */
			ConfigurableFileCollection javacBootclasspath = objects.fileCollection()
				.from(decontainer(bndProject.getBootclasspath()));
			String javac = bndProject.getProperty("javac", "javac");
			Optional<String> javacSource = optional(bndProject.getProperty("javac.source"));
			Optional<String> javacTarget = optional(bndProject.getProperty("javac.target"));
			Optional<String> javacProfile = optional(bndProject.getProperty("javac.profile"));
			boolean javacDebug = bndProject.is("javac.debug");
			boolean javacDeprecation = isTrue(bndProject.getProperty("javac.deprecation", "true"));
			String javacEncoding = bndProject.getProperty("javac.encoding", "UTF-8");
			JavaPluginExtension javaPlugin = project.getExtensions()
				.getByType(JavaPluginExtension.class);
			javacSource.ifPresent(javaPlugin::setSourceCompatibility);
			javacTarget.ifPresent(javaPlugin::setTargetCompatibility);
			tasks.withType(JavaCompile.class)
				.configureEach(t -> {
					CompileOptions options = t.getOptions();
					javacSource.ifPresent(t::setSourceCompatibility);
					javacTarget.ifPresent(t::setTargetCompatibility);
					if (javacSource.isPresent() && javacTarget.isPresent()) {
						Property<Boolean> supportsRelease = objects.property(Boolean.class)
							.value(t.getJavaCompiler()
								.map(javaCompiler -> Boolean.valueOf(javaCompiler.getMetadata()
									.getLanguageVersion()
									.canCompileOrRun(9))));
						options.getRelease()
							.convention(project.provider(() -> {
								if (supportsRelease.getOrElse(Boolean.valueOf(JavaVersion.current()
									.isJava9Compatible()))
									.booleanValue()) {
									JavaVersion sourceVersion = JavaVersion.toVersion(javacSource.get());
									JavaVersion targetVersion = JavaVersion.toVersion(javacTarget.get());
									if (Objects.equals(sourceVersion, targetVersion) && javacBootclasspath.isEmpty()
										&& javacProfile.isEmpty()) {
										return Integer.valueOf(sourceVersion.getMajorVersion());
									}
								}
								return null;
							}));
					}
					if (javacDebug) {
						options.getDebugOptions()
							.setDebugLevel("source,lines,vars");
					}
					options.setVerbose(t.getLogger()
						.isDebugEnabled());
					options.setListFiles(t.getLogger()
						.isInfoEnabled());
					options.setDeprecation(javacDeprecation);
					options.setEncoding(javacEncoding);
					if (!Objects.equals(javac, "javac")) {
						options.setFork(true);
						options.getForkOptions()
							.setExecutable(javac);
					}
					if (!javacBootclasspath.isEmpty()) {
						options.setFork(true);
						options.setBootstrapClasspath(javacBootclasspath);
					}
					options.getCompilerArgumentProviders()
						.add(argProvider(javacProfile.map(profile -> Arrays.asList("-profile", profile))));
					t.doFirst("checkErrors", new Action<>() {
						@Override
						public void execute(Task tt) {
							Logger logger = tt.getLogger();
							checkErrors(logger);
							if (logger.isInfoEnabled()) {
								logger.info("Compile to {}", unwrapFile(t.getDestinationDirectory()));
								if (t.getOptions()
									.getRelease()
									.isPresent()) {
									logger.info("--release {} {}", unwrap(t.getOptions()
										.getRelease()), Strings.join(" ",
											t.getOptions()
												.getAllCompilerArgs()));
								} else {
									logger.info("-source {} -target {} {}", t.getSourceCompatibility(),
										t.getTargetCompatibility(), Strings.join(" ", t.getOptions()
											.getAllCompilerArgs()));
								}
								logger.info("-classpath {}", t.getClasspath()
									.getAsPath());
								if (Objects.nonNull(t.getOptions()
									.getBootstrapClasspath())) {
									logger.info("-bootclasspath {}", t.getOptions()
										.getBootstrapClasspath()
										.getAsPath());
								}
							}
						}
					});
				});

			TaskProvider<AbstractArchiveTask> jar = tasks.named(JavaPlugin.JAR_TASK_NAME, AbstractArchiveTask.class,
				t -> {
					t.setDescription("Jar this project's bundles.");
					t.getActions()
						.clear(); /* Replace the standard task actions */
					t.setEnabled(!bndProject.isNoBundles());
					/* use first deliverable as archiveFileName */
					t.getArchiveFileName()
						.set(project.provider(() -> StreamSupport.stream(deliverables.spliterator(), false)
							.filter(Objects::nonNull)
							.map(File::getName)
							.findFirst()
							.orElse(bndProject.getName())));
					/* Additional excludes for projectDir inputs */
					List<String> projectDirInputsExcludes = Strings
						.splitAsStream(bndProject.mergeProperties(Constants.BUILDERIGNORE))
						.map(i -> i.concat("/"))
						.collect(toList());
					/* all other files in the project like bnd and resources */
					t.getInputs()
						.files(objects.fileTree()
							.from(layout.getProjectDirectory())
							.matching(filterable -> {
								sourceSets.forEach(sourceSet -> {
									/* exclude sourceSet dirs */
									filterable.exclude(sourceSet.getAllSource()
										.getSourceDirectories()
										.getFiles()
										.stream()
										.map(project::relativePath)
										.collect(toList()));
									filterable.exclude(sourceSet.getOutput()
										.getFiles()
										.stream()
										.map(project::relativePath)
										.collect(toList()));
								});
								// exclude buildDirectory
								filterable.exclude(project.relativePath(layout.getBuildDirectory()));
								// user specified excludes
								filterable.exclude(projectDirInputsExcludes);
							}))
						.withPathSensitivity(RELATIVE)
						.withPropertyName("projectFolder");
					/* bnd can include from -buildpath */
					t.getInputs()
						.files(tasks.named(sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
								.getCompileJavaTaskName(), AbstractCompile.class)
							.map(AbstractCompile::getClasspath))
						.withNormalizer(ClasspathNormalizer.class)
						.withPropertyName("buildpath");
					/* bnd can include from -dependson */
					t.getInputs()
						.files(getBuildDependencies(JavaPlugin.JAR_TASK_NAME))
						.withPropertyName("buildDependencies");
					/*
					 * Workspace and project configuration changes should
					 * trigger jar task
					 */
					t.getInputs()
						.files(bndConfiguration())
						.withPathSensitivity(RELATIVE)
						.withPropertyName("bndConfiguration")
						.normalizeLineEndings();
					t.getOutputs()
						.files(deliverables)
						.withPropertyName("artifacts");
					t.getOutputs()
						.file(layout.getBuildDirectory()
							.file(Constants.BUILDFILES))
						.withPropertyName("buildfiles");
					t.doLast("build", new Action<>() {
						@Override
						public void execute(Task tt) {
							File[] built;
							try {
								built = bndProject.build();
								if (Objects.nonNull(built)) {
									long now = System.currentTimeMillis();
									for (File f : built) {
										f.setLastModified(now);
									}
								}
							} catch (Exception e) {
								throw new GradleException(
									String.format("Project %s failed to build", bndProject.getName()), e);
							}
							checkErrors(tt.getLogger());
							if (Objects.nonNull(built)) {
								tt.getLogger()
									.info("Generated bundles: {}", (Object) built);
							}
						}
					});
				});

			TaskProvider<Task> jarDependencies = tasks.register("jarDependencies", t -> {
				t.setDescription("Jar all projects this project depends on.");
				t.setGroup(LifecycleBasePlugin.BUILD_GROUP);
				t.dependsOn(getBuildDependencies(JavaPlugin.JAR_TASK_NAME));
			});

			TaskProvider<Task> buildDependencies = tasks.register("buildDependencies", t -> {
				t.setDescription("Assembles and tests all projects this project depends on.");
				t.setGroup(LifecycleBasePlugin.BUILD_GROUP);
				t.dependsOn(getTestDependencies(JavaBasePlugin.BUILD_NEEDED_TASK_NAME));
			});

			TaskProvider<Task> buildNeeded = tasks.named(JavaBasePlugin.BUILD_NEEDED_TASK_NAME, t -> {
				t.dependsOn(buildDependencies);
			});

			TaskProvider<Task> buildDependents = tasks.named(JavaBasePlugin.BUILD_DEPENDENTS_TASK_NAME, t -> {
				t.dependsOn(getDependents(JavaBasePlugin.BUILD_DEPENDENTS_TASK_NAME));
			});

			Provider<ReleaseCounterService> releaseCounter =
			    project.getGradle().getSharedServices().registerIfAbsent(
			        "bndReleaseCounter",
			        ReleaseCounterService.class,
						spec -> {
							spec.getMaxParallelUsages()
								.set(1);
						}
			    );

			project.getGradle()
				.getTaskGraph()
				.whenReady(graph -> {
					long count = graph.getAllTasks()
						.stream()
						.filter(t -> t.getName()
							.equals("release"))
						.filter(Task::getEnabled)
						.count();

					// helpful while you validate:
					//project.getLogger()
					//	.lifecycle("bnd: release tasks in execution graph = {}", count);

					releaseCounter.get()
						.setInitialCount((int) count);
				});

			TaskProvider<Task> release = tasks.register("release", t -> {
				t.setDescription("Release this project to the release repository.");
				t.setGroup(PublishingPlugin.PUBLISH_TASK_GROUP);

				boolean enabled = !bndProject.isNoBundles() && !bndProject.getProperty(Constants.RELEASEREPO, "unset")
					.isEmpty();
				t.setEnabled(enabled);
				t.getInputs()
					.files(jar)
					.withPropertyName(jar.getName());

			    // Declare the service usage for correctness w/ parallel execution + configuration cache
			    t.usesService(releaseCounter);


				t.doLast("release", new Action<>() {
					@Override
					public void execute(Task tt) {
						try {

							int count = releaseCounter.get()
								.getRemaining();
							boolean isLastBundle = releaseCounter.get()
								.isLastReleaseTask();

							if (!isLastBundle) {
								tt.getLogger()
									.lifecycle("bnd: Release bundle ({}) {}", count, bndProject.getName());
								bndProject.release();
							} else {
								// releasing last bundle in workspace (special
								// case for sonatype release)
								tt.getLogger()
									.lifecycle("bnd: Last release bundle ({}) {}", count, bndProject.getName());
								bndProject.release(new ReleaseParameter(null, false, true));
							}
						} catch (Exception e) {
							throw new GradleException(
								String.format("Project %s failed to release", bndProject.getName()), e);
						}
						checkErrors(tt.getLogger());
					}
				});
			});

			TaskProvider<Task> releaseDependencies = tasks.register("releaseDependencies", t -> {
				t.setDescription("Release all projects this project depends on.");
				t.setGroup(PublishingPlugin.PUBLISH_TASK_GROUP);
				t.dependsOn(getBuildDependencies("releaseNeeded"));
			});

			TaskProvider<Task> releaseNeeded = tasks.register("releaseNeeded", t -> {
				t.setDescription("Release this project and all projects it depends on.");
				t.setGroup(PublishingPlugin.PUBLISH_TASK_GROUP);
				t.dependsOn(releaseDependencies, release);
			});

			TaskProvider<AbstractTestTask> test = tasks.named(JavaPlugin.TEST_TASK_NAME, AbstractTestTask.class, t -> {
				t.setEnabled(!bndProject.is(Constants.NOJUNIT) && !bndProject.is("no.junit"));
				/* tests can depend upon jars from -dependson */
				t.getInputs()
					.files(getBuildDependencies(JavaPlugin.JAR_TASK_NAME))
					.withPropertyName("buildDependencies");
				t.doFirst("checkErrors", new Action<>() {
					@Override
					public void execute(Task tt) {
						checkErrors(tt.getLogger(), t.getIgnoreFailures());
					}
				});
			});

			TaskProvider<TestOSGi> testOSGi = tasks.register("testOSGi", TestOSGi.class, t -> {
				t.setDescription(
					"Runs the OSGi JUnit tests by launching a framework and running the tests in the launched framework.");
				t.setEnabled(
					!bndProject.is(Constants.NOJUNITOSGI) && !Optional.ofNullable(bndProject.getUnexpandedProperty(Constants.TESTCASES)).orElse("")
						.isEmpty());
				t.getInputs()
					.files(jar)
					.withPropertyName(jar.getName());
				t.getBndrun()
					.fileValue(bndProject.getPropertiesFile());
			});

			TaskProvider<Task> check = tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME, t -> {
				t.dependsOn(testOSGi);
			});

			TaskProvider<Task> checkDependencies = tasks.register("checkDependencies", t -> {
				t.setDescription("Runs all checks on all projects this project depends on.");
				t.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
				t.dependsOn(getTestDependencies("checkNeeded"));
			});

			TaskProvider<Task> checkNeeded = tasks.register("checkNeeded", t -> {
				t.setDescription("Runs all checks on this project and all projects it depends on.");
				t.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
				t.dependsOn(checkDependencies, check);
			});

			TaskProvider<Delete> clean = tasks.named(LifecycleBasePlugin.CLEAN_TASK_NAME, Delete.class, t -> {
				t.setDescription("Cleans the build and compiler output directories of this project.");
				t.delete(layout.getBuildDirectory(), sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
					.getOutput(),
					sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME)
						.getOutput());
				generate.ifPresent(t::delete);
			});

			TaskProvider<Task> cleanDependencies = tasks.register("cleanDependencies", t -> {
				t.setDescription("Cleans all projects this project depends on.");
				t.setGroup(LifecycleBasePlugin.BUILD_GROUP);
				t.dependsOn(getTestDependencies("cleanNeeded"));
			});

			TaskProvider<Task> cleanNeeded = tasks.register("cleanNeeded", t -> {
				t.setDescription("Cleans this project and all projects it depends on.");
				t.setGroup(LifecycleBasePlugin.BUILD_GROUP);
				t.dependsOn(cleanDependencies, clean);
			});

			TaskProvider<Task> assemble = tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME);

			Map<String, File> bndruns = layout.getProjectDirectory()
				.getAsFileTree()
				.matching(filterable -> filterable.include("*.bndrun"))
				.getFiles()
				.stream()
				.collect(toMap(runFile -> {
					String name = runFile.getName();
					return Optional.ofNullable(Strings.extension(name))
						.map(parts -> parts[0])
						.orElse(name);
				}, runFile -> runFile));

			List<TaskProvider<Export>> exportTasks = MapStream.of(bndruns)
				.mapToObj((name, runFile) -> tasks.register("export.".concat(name), Export.class, t -> {
					t.setDescription(String.format("Export the %s file.", runFile.getName()));
					t.dependsOn(assemble);
					t.getBndrun()
						.fileValue(runFile);
					t.getExporter()
						.set(ExecutableJarExporter.EXECUTABLE_JAR);
				}))
				.collect(toList());

			TaskProvider<Task> export = tasks.register("export", t -> {
				t.setDescription("Export all the bndrun files.");
				t.setGroup(PublishingPlugin.PUBLISH_TASK_GROUP);
				t.dependsOn(exportTasks);
			});

			List<TaskProvider<Export>> runbundlesTasks = MapStream.of(bndruns)
				.mapToObj((name, runFile) -> tasks.register("runbundles.".concat(name), Export.class, t -> {
					t.setDescription(
						String.format("Create a distribution of the runbundles in the %s file.", runFile.getName()));
					t.dependsOn(assemble);
					t.getBndrun()
						.fileValue(runFile);
					t.getExporter()
						.set(RunbundlesExporter.RUNBUNDLES);
				}))
				.collect(toList());

			TaskProvider<Task> runbundles = tasks.register("runbundles", t -> {
				t.setDescription("Create a distribution of the runbundles in each of the bndrun files.");
				t.setGroup(PublishingPlugin.PUBLISH_TASK_GROUP);
				t.dependsOn(runbundlesTasks);
			});

			List<TaskProvider<Resolve>> resolveTasks = MapStream.of(bndruns)
				.mapToObj((name, runFile) -> tasks.register("resolve.".concat(name), Resolve.class, t -> {
					t.setDescription(String.format("Resolve the runbundles required for %s file.", runFile.getName()));
					t.dependsOn(assemble);
					t.getBndrun()
						.fileValue(runFile);
				}))
				.collect(toList());

			TaskProvider<Task> resolve = tasks.register("resolve", t -> {
				t.setDescription("Resolve the runbundles required for each of the bndrun files.");
				t.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
				t.dependsOn(resolveTasks);
			});

			bndruns.forEach((name, runFile) -> {
				tasks.register("run.".concat(name), Bndrun.class, t -> {
					t.setDescription(String.format("Run the bndrun file %s.", runFile.getName()));
					t.dependsOn(assemble);
					t.getBndrun()
						.fileValue(runFile);
				});
			});

			bndruns.forEach((name, runFile) -> {
				tasks.register("testrun.".concat(name), TestOSGi.class, t -> {
					t.setDescription(
						String.format("Runs the OSGi JUnit tests in the bndrun file %s.", runFile.getName()));
					t.dependsOn(assemble);
					t.getBndrun()
						.fileValue(runFile);
				});
			});

			Collection<aQute.bnd.build.Project> dependson = bndProject.getDependson();
			TaskProvider<Task> echo = tasks.register("echo", t -> {
				t.setDescription("Displays the bnd project information.");
				t.setGroup(HelpTasksPlugin.HELP_GROUP);
				JavaCompile compileJava = unwrap(tasks.named(JavaPlugin.COMPILE_JAVA_TASK_NAME, JavaCompile.class));
				JavaCompile compileTestJava = unwrap(
					tasks.named(JavaPlugin.COMPILE_TEST_JAVA_TASK_NAME, JavaCompile.class));
				t.doLast("echo", new Action<>() {
					@Override
					public void execute(Task tt) {
						try (Formatter f = new Formatter()) {
							f.format("------------------------------------------------------------%n");
							f.format("Project %s // Bnd version %s%n", project.getName(), About.getBndVersion());
							f.format("------------------------------------------------------------%n");
							f.format("%n");
							f.format("project.workspace:      %s%n", workspace.getLayout()
								.getProjectDirectory());
							f.format("project.name:           %s%n", project.getName());
							f.format("project.dir:            %s%n", layout.getProjectDirectory());
							f.format("target:                 %s%n", unwrap(layout.getBuildDirectory()));
							f.format("project.dependson:      %s%n", dependson);
							f.format("project.sourcepath:     %s%n",
								sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
									.getAllSource()
									.getSourceDirectories()
									.getAsPath());
							f.format("project.output:         %s%n", unwrap(compileJava.getDestinationDirectory()));
							f.format("project.buildpath:      %s%n", compileJava.getClasspath()
								.getAsPath());
							f.format("project.allsourcepath:  %s%n", allSrcDirs.getAsPath());
							f.format("project.testsrc:        %s%n",
								sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME)
									.getAllSource()
									.getSourceDirectories()
									.getAsPath());
							f.format("project.testoutput:     %s%n", unwrap(compileTestJava.getDestinationDirectory()));
							f.format("project.testpath:       %s%n", compileTestJava.getClasspath()
								.getAsPath());
							if (Objects.nonNull(compileJava.getOptions()
								.getBootstrapClasspath())) {
								f.format("project.bootclasspath:  %s%n", compileJava.getOptions()
									.getBootstrapClasspath()
									.getAsPath());
							}
							f.format("project.deliverables:   %s%n", deliverables.getFiles());
							String executable = Optional.ofNullable(compileJava.getOptions()
								.getForkOptions()
								.getExecutable())
								.orElseGet(() -> compileJava.getJavaCompiler()
									.map(javaCompiler -> IO.absolutePath(unwrapFile(javaCompiler.getExecutablePath())))
									.getOrElse("javac"));
							f.format("javac:                  %s%n", executable);
							if (compileJava.getOptions()
								.getRelease()
								.isPresent()) {
								f.format("--release:              %s%n", unwrap(compileJava.getOptions()
									.getRelease()));
							} else {
								f.format("-source:                %s%n", compileJava.getSourceCompatibility());
								f.format("-target:                %s%n", compileJava.getTargetCompatibility());
							}
							if (javacProfile.isPresent()) {
								f.format("-profile:               %s%n", javacProfile.get());
							}
							System.out.print(f.toString());
						}
						checkErrors(tt.getLogger(), true);
					}
				});
			});

			TaskProvider<Task> bndproperties = tasks.register("bndproperties", t -> {
				t.setDescription("Displays the bnd properties.");
				t.setGroup(HelpTasksPlugin.HELP_GROUP);
				t.doLast("bndproperties", new Action<>() {
					@Override
					public void execute(Task tt) {
						try (Formatter f = new Formatter()) {
							f.format("------------------------------------------------------------%n");
							f.format("Project %s // Bnd version %s%n", project.getName(), About.getBndVersion());
							f.format("------------------------------------------------------------%n");
							f.format("%n");
							bndProject.getPropertyKeys(true)
								.stream()
								.sorted()
								.forEachOrdered(key -> f.format("%s: %s%n", key, bndProject.getProperty(key, "")));
							f.format("%n");
							System.out.print(f.toString());
						}
						checkErrors(tt.getLogger(), true);
					}
				});
			});

			// Depend upon an output dir to avoid parallel task execution.
			// This effectively claims the resource and prevents
			// tasks claiming the same resource from executing concurrently.
			// -noparallel: launchpad;task="test,echo"
			Project noparallel = workspace.hasProperty("cnf") ? (Project) workspace.property("cnf") : workspace;

			bndProject.getMergedParameters("-noparallel")
				.forEach((key, attrs) -> {
					String category = removeDuplicateMarker(key);
					Provider<Directory> resource = noparallel.getLayout()
						.getBuildDirectory()
						.dir("noparallel/".concat(category));
					String taskNames = Objects.nonNull(attrs) ? attrs.get("task") : null;
					Strings.splitAsStream(taskNames)
						.forEach(taskName -> tasks.named(taskName, t -> t.getOutputs()
							.dir(resource)
							.withPropertyName(category)));
				});
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	private Provider<List<TaskProvider<Task>>> getTasks(Collection<? extends aQute.bnd.build.Project> projects,
		String taskName) {
		return project.provider(() -> projects.stream()
			.map(p -> workspace.project(p.getName())
				.getTasks()
				.named(taskName))
			.collect(toList()));
	}

	private ConfigurableFileCollection pathFiles(Collection<? extends Container> path) {
		return objects.fileCollection()
			.from(decontainer(path))
			.builtBy(getTasks(path.stream()
				.filter(c -> c.getType() == TYPE.PROJECT)
				.map(Container::getProject)
				.collect(toList()), JavaPlugin.JAR_TASK_NAME));
	}

	private Provider<List<TaskProvider<Task>>> getBuildDependencies(String taskName) {
		try {
			return getTasks(bndProject.getBuildDependencies(), taskName);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	private Provider<List<TaskProvider<Task>>> getTestDependencies(String taskName) {
		try {
			return getTasks(bndProject.getTestDependencies(), taskName);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	private Provider<List<TaskProvider<Task>>> getDependents(String taskName) {
		try {
			return getTasks(bndProject.getDependents(), taskName);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	private List<File> decontainer(Collection<? extends Container> path) {
		return path.stream()
			.map(Container::getFile)
			.collect(toList());
	}

	private <ITERABLE extends Iterable<String>> CommandLineArgumentProvider argProvider(Optional<ITERABLE> provider) {
		return new CommandLineArgumentProvider() {
			@SuppressWarnings("unchecked")
			@Override
			public Iterable<String> asArguments() {
				return provider.orElse((ITERABLE) Collections.<String> emptyList());
			}
		};
	}

	private ConfigurableFileCollection bndConfiguration() {
		Workspace bndWorkspace = bndProject.getWorkspace();
		return objects.fileCollection()
			.from(bndWorkspace.getPropertiesFile(), bndWorkspace.getIncluded(), bndProject.getPropertiesFile(),
				bndProject.getIncluded());
	}

	private void checkErrors(Logger logger) {
		checkProjectErrors(bndProject, logger, false);
	}

	private void checkErrors(Logger logger, boolean ignoreFailures) {
		checkProjectErrors(bndProject, logger, ignoreFailures);
	}

	private void checkProjectErrors(aQute.bnd.build.Project p, Logger logger, boolean ignoreFailures) {
		p.getInfo(p.getWorkspace(), p.getWorkspace()
			.getBase()
			.getName()
			.concat(" :"));
		boolean failed = !ignoreFailures && !p.isOk();
		int errorCount = p.getErrors()
			.size();
		logReport(p, logger);
		p.clear();
		if (failed) {
			String str;
			if (errorCount == 1) {
				str = "%s has errors, one error was reported";
			} else if (errorCount > 1) {
				str = "%s has errors, %s errors were reported";
			} else {
				str = "%s has errors even though no errors were reported";
			}
			throw new GradleException(String.format(str, p.getName(), errorCount));
		}
	}

	private Optional<String> optional(String value) {
		return Optional.ofNullable(value).filter(Strings::notEmpty);
	}

	private static Object getter(Object target, String name) {
		try {
			String getterSuffix = Character.toUpperCase(name.charAt(0)) + name.substring(1);
			Class<?> targetClass = target.getClass();
			while (!Modifier.isPublic(targetClass.getModifiers())) {
				targetClass = targetClass.getSuperclass();
			}
			MethodHandle mh = publicLookup().unreflect(targetClass.getMethod("get" + getterSuffix));
			return mh.invoke(target);
		} catch (Error e) {
			throw e;
		} catch (Throwable e) {
			logger.debug("Could not find getter method for field {}", name, e);
		}
		return null;
	}
}
