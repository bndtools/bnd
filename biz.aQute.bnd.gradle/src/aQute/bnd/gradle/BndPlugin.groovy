/**
 * BndPlugin for Gradle.
 *
 * <p>
 * The plugin name is {@code biz.aQute.bnd}.
 *
 * <p>
 * If the bndWorkspace property is set, it will be used for the bnd Workspace.
 *
 * <p>
 * If the bnd_defaultTask property is set, it will be used for the the default
 * task.
 */

package aQute.bnd.gradle

import static aQute.bnd.exporter.executable.ExecutableJarExporter.EXECUTABLE_JAR
import static aQute.bnd.exporter.runbundles.RunbundlesExporter.RUNBUNDLES
import static aQute.bnd.gradle.BndUtils.jarLibraryElements
import static aQute.bnd.gradle.BndUtils.logReport
import static aQute.bnd.gradle.BndUtils.unwrap
import static aQute.bnd.osgi.Processor.isTrue
import static aQute.bnd.osgi.Processor.removeDuplicateMarker
import static java.util.stream.Collectors.toList
import static org.gradle.api.tasks.PathSensitivity.RELATIVE

import aQute.bnd.build.Container
import aQute.bnd.build.Container.TYPE
import aQute.bnd.build.Workspace
import aQute.bnd.osgi.About
import aQute.bnd.osgi.Constants
import aQute.lib.strings.Strings

import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.ClasspathNormalizer
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.CompileOptions
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.process.CommandLineArgumentProvider

public class BndPlugin implements Plugin<Project> {
	public static final String PLUGINID = "biz.aQute.bnd"
	private Project project
	private Project workspace
	private ObjectFactory objects
	private aQute.bnd.build.Project bndProject

	/**
	 * Apply the {@code biz.aQute.bnd} plugin to the specified project.
	 */
	@Override
	public void apply(Project project) {
		this.project = project
		Project workspace = this.workspace = project.getParent()
		ProjectLayout layout = project.getLayout()
		ObjectFactory objects = this.objects = project.getObjects()
		TaskContainer tasks = project.getTasks()
		if (project.getPlugins().hasPlugin(BndBuilderPlugin.PLUGINID)) {
			throw new GradleException("Project already has \"${BndBuilderPlugin.PLUGINID}\" plugin applied.")
		}
		if (Objects.isNull(workspace)) {
			throw new GradleException("The \"${PLUGINID}\" plugin cannot be applied to the root project. Perhaps you meant to use the \"${BndBuilderPlugin.PLUGINID}\" plugin?")
		}
		Workspace bndWorkspace = BndWorkspacePlugin.getBndWorkspace(workspace)
		aQute.bnd.build.Project bndProject = this.bndProject = bndWorkspace.getProject(project.getName())
		if (Objects.isNull(bndProject)) {
			throw new GradleException("Unable to load bnd project ${project.getName()} from workspace ${workspace.getLayout().getProjectDirectory()}")
		}
		bndProject.prepare()
		if (!bndProject.isValid()) {
			checkErrors(project.getLogger())
			throw new GradleException("Project ${bndProject.getName()} is not a valid bnd project")
		}
		BndPluginExtension extension = project.getExtensions().create("bnd", BndPluginExtension.class, bndProject)
		project.getConvention().getPlugins().put("bnd", new BndPluginConvention(extension))

		layout.getBuildDirectory().set(bndProject.getTargetDir())
		project.getPlugins().apply("java")
		project.libsDirName = "."
		project.testResultsDirName = project.bnd("test-reports", "test-reports")

		if (project.hasProperty("bnd_defaultTask")) {
			project.setDefaultTasks(Strings.split(project.bnd_defaultTask))
		}

		/* Set up configurations */
		project.configurations {
			runtimeOnly.artifacts.clear()
			archives.artifacts.clear()
		}
		/* Set up deliverables */
		decontainer(bndProject.getDeliverables()).forEach((File deliverable) -> {
			project.artifacts {
				runtimeOnly(deliverable) {
					builtBy("jar")
				}
				archives(deliverable) {
					builtBy("jar")
				}
			}
		})
		FileCollection deliverables = project.getConfigurations().archives.getArtifacts().getFiles()

		/* Set up Bnd generate support */
		var generateInputs = bndProject.getGenerate().getInputs()
		var generate = null
		if (!generateInputs.isErr() && !generateInputs.unwrap().isEmpty()) {
			generate = tasks.register("generate", t -> {
				t.setDescription("Generate source code")
				t.setGroup("build")
				t.getInputs().files(generateInputs.unwrap()).withPathSensitivity(RELATIVE).withPropertyName("generateInputs")
				/* bnd can include from -dependson */
				t.getInputs().files(getBuildDependencies("jar")).withPropertyName("buildDependencies")
				/* Workspace and project configuration changes should trigger task */
				t.getInputs().files(bndConfiguration()).withPathSensitivity(RELATIVE).withPropertyName("bndConfiguration")
				t.getOutputs().dirs(bndProject.getGenerate().getOutputDirs()).withPropertyName("generateOutputs")
				t.doLast("generate", tt -> {
					try {
						bndProject.getGenerate().generate(false)
					} catch (Exception e) {
						throw new GradleException("Project ${bndProject.getName()} failed to generate", e)
					}
					checkErrors(tt.getLogger())
				})
			})
		}
		/* Set up source sets */
		project.sourceSets {
			/* bnd uses the same directory for java and resources. */
			main {
				ConfigurableFileCollection srcDirs = objects.fileCollection().from(bndProject.getSourcePath())
				File destinationDir = bndProject.getSrcOutput()
				String compileTaskName = getCompileJavaTaskName()
				getJava().srcDirs = srcDirs
				getResources().srcDirs = srcDirs
				getJava().outputDir = destinationDir
				getOutput().resourcesDir = destinationDir
				tasks.named(compileTaskName, t -> {
					t.destinationDir = destinationDir
					if (generate) {
						t.getInputs().files(generate).withPropertyName(generate.getName())
					}
					jarLibraryElements(t, getCompileClasspathConfigurationName())
				})
				getOutput().dir(destinationDir, "builtBy": compileTaskName)
			}
			test {
				ConfigurableFileCollection srcDirs = objects.fileCollection().from(bndProject.getTestSrc())
				File destinationDir = bndProject.getTestOutput()
				String compileTaskName = getCompileJavaTaskName()
				getJava().srcDirs = srcDirs
				getResources().srcDirs = srcDirs
				getJava().outputDir = destinationDir
				getOutput().resourcesDir = destinationDir
				tasks.named(compileTaskName, t -> {
					t.destinationDir = destinationDir
					jarLibraryElements(t, getCompileClasspathConfigurationName())
				})
				getOutput().dir(destinationDir, "builtBy": compileTaskName)
			}
		}
		/* Configure srcDirs for any additional languages */
		project.afterEvaluate {
			project.sourceSets {
				main {
					convention.getPlugins().forEach((lang, sourceDirSets) -> {
						var sourceDirSet = sourceDirSets[lang]
						if (sourceDirSet.hasProperty("srcDirs") && sourceDirSet.hasProperty("outputDir")){
							File destinationDir = getJava().getOutputDir()
							String compileTaskName = getCompileTaskName(lang)
							String resourceTaskName = getProcessResourcesTaskName()
							try {
								tasks.named(compileTaskName, t -> {
									t.destinationDir = destinationDir
									t.getInputs().files(tasks.named(resourceTaskName)).withPropertyName(resourceTaskName)
									if (generate) {
										t.getInputs().files(generate).withPropertyName(generate.getName())
									}
								})
								sourceDirSet.srcDirs = getJava().getSrcDirs()
								sourceDirSet.outputDir = destinationDir
								getOutput().dir(destinationDir, "builtBy": compileTaskName)
							} catch (UnknownDomainObjectException e) {
								// no such task
							}
						}
					})
				}
				test {
					convention.getPlugins().forEach((lang, sourceDirSets) -> {
						var sourceDirSet = sourceDirSets[lang]
						if (sourceDirSet.hasProperty("srcDirs") && sourceDirSet.hasProperty("outputDir")){
							File destinationDir = getJava().getOutputDir()
							String compileTaskName = getCompileTaskName(lang)
							String resourceTaskName = getProcessResourcesTaskName()
							try {
								tasks.named(compileTaskName, t -> {
									t.destinationDir = destinationDir
									t.getInputs().files(tasks.named(resourceTaskName)).withPropertyName(resourceTaskName)
								})
								sourceDirSet.srcDirs = getJava().getSrcDirs()
								sourceDirSet.outputDir = destinationDir
								getOutput().dir(destinationDir, "builtBy": compileTaskName)
							} catch (UnknownDomainObjectException e) {
								// no such task
							}
						}
					})
				}
			}
		}

		project.bnd.ext.allSrcDirs = objects.fileCollection().from(bndProject.getAllsourcepath())
		/* Set up dependencies */
		project.dependencies {
			implementation pathFiles(bndProject.getBuildpath())
			runtimeOnly objects.fileCollection().from(bndProject.getSrcOutput())
			testImplementation pathFiles(bndProject.getTestpath())
			testRuntimeOnly objects.fileCollection().from(bndProject.getTestOutput())
		}
		/* Set up compile tasks */
		ConfigurableFileCollection javacBootclasspath = objects.fileCollection().from(decontainer(bndProject.getBootclasspath()))
		Property<String> javacSource = objects.property(String.class).convention(project.bnd("javac.source"))
		if (javacSource.isPresent()) {
			project.sourceCompatibility = javacSource.get()
		} else {
			javacSource.convention(project.provider(() -> project.sourceCompatibility.toString()))
		}
		Property<String> javacTarget = objects.property(String.class).convention(project.bnd("javac.target"))
		if (javacTarget.isPresent()) {
			if (Objects.equals(javacTarget.get(), "jsr14")) {
				javacTarget.set("1.5")
				javacBootclasspath.setFrom(bndProject.getBundle("ee.j2se", "1.5", null, ["strategy":"lowest"]).getFile())
			}
			project.targetCompatibility = javacTarget.get()
		} else {
			javacTarget.convention(project.provider(() -> project.targetCompatibility.toString()))
		}
		String javac = project.bnd("javac")
		Property<String> javacProfile = objects.property(String.class)
		if (!project.bnd("javac.profile", "").isEmpty()) {
			javacProfile.convention(project.bnd("javac.profile"))
		}
		boolean javacDebug = project.bndis("javac.debug")
		boolean javacDeprecation = isTrue(project.bnd("javac.deprecation", "true"))
		String javacEncoding = project.bnd("javac.encoding", "UTF-8")
		tasks.withType(JavaCompile.class).configureEach(t -> {
			t.setSourceCompatibility(javacSource.get())
			t.setTargetCompatibility(javacTarget.get())
			Property<Boolean> supportsRelease = objects.property(Boolean.class)
			if (t.hasProperty("javaCompiler")) {
				// Gradle 6.7
				supportsRelease.convention(t.getJavaCompiler().map(javaCompiler -> Boolean.valueOf(javaCompiler.getMetadata().getLanguageVersion().canCompileOrRun(9))))
			}
			Property<Integer> javacRelease = objects.property(Integer.class).convention(project.provider(() -> {
				if (supportsRelease.getOrElse(Boolean.valueOf(JavaVersion.current().isJava9Compatible())).booleanValue()) {
					if (Objects.equals(javacSource.get(), javacTarget.get()) && javacBootclasspath.isEmpty() && !javacProfile.isPresent()) {
						return Integer.valueOf(JavaVersion.toVersion(javacSource.get()).getMajorVersion())
					}
				}
				return null
			}))
			CompileOptions options = t.getOptions()
			if (javacDebug) {
				options.getDebugOptions().setDebugLevel("source,lines,vars")
			}
			options.setVerbose(t.getLogger().isDebugEnabled())
			options.setListFiles(t.getLogger().isInfoEnabled())
			options.setDeprecation(javacDeprecation)
			options.setEncoding(javacEncoding)
			if (!Objects.equals(javac, "javac")) {
				options.setFork(true)
				options.getForkOptions().setExecutable(javac)
			}
			if (!javacBootclasspath.isEmpty()) {
				options.setFork(true)
				options.setBootstrapClasspath(javacBootclasspath)
			}
			options.getCompilerArgumentProviders().add(argProvider(javacProfile.map(profile -> Arrays.asList("-profile", profile))))
			if (options.hasProperty("release")) {
				// Gradle 6.6
				options.getRelease().set(javacRelease)
			} else {
				options.getCompilerArgumentProviders().add(argProvider(javacRelease.map(release -> Arrays.asList("--release", release.toString()))))
			}
			t.doFirst("checkErrors", tt -> {
				Logger logger = tt.getLogger()
				checkErrors(logger)
				if (logger.isInfoEnabled()) {
					logger.info("Compile to {}", tt.getDestinationDir())
					List<String> allCompilerArgs = tt.getOptions().getAllCompilerArgs()
					if (tt.getOptions().hasProperty("release") && tt.getOptions().getRelease().isPresent()) {
						// Gradle 6.6
						logger.info("--release {} {}", tt.getOptions().getRelease().get(), allCompilerArgs.join(" "))
					} else if (allCompilerArgs.contains("--release")) {
						logger.info("{}", allCompilerArgs.join(" "))
					} else {
						logger.info("-source {} -target {} {}", tt.getSourceCompatibility(), tt.getTargetCompatibility(), allCompilerArgs.join(" "))
					}
					logger.info("-classpath {}", tt.getClasspath().getAsPath())
					if (Objects.nonNull(tt.getOptions().getBootstrapClasspath())) {
						logger.info("-bootclasspath {}", tt.getOptions().getBootstrapClasspath().getAsPath())
					}
				}
			})
		})

		var jar = tasks.named("jar", t -> {
			t.setDescription("Jar this project's bundles.")
			t.getActions().clear() /* Replace the standard task actions */
			t.setEnabled(!bndProject.isNoBundles())
			/* use first deliverable as archiveFileName */
			t.archiveFileName = project.provider(() -> deliverables.find()?.getName() ?: bndProject.getName())
			/* Additional excludes for projectDir inputs */
			t.ext.projectDirInputsExcludes = Strings.splitAsStream(project.bndMerge(Constants.BUILDERIGNORE))
			.map(i -> i.concat("/"))
			.collect(toList())
			/* all other files in the project like bnd and resources */
			t.getInputs().files(layout.getProjectDirectory().getAsFileTree().matching(filterable -> {
				project.sourceSets.forEach(sourceSet -> {
					/* exclude sourceSet dirs */
					filterable.exclude(sourceSet.getAllSource().getSourceDirectories().collect {
						project.relativePath(it)
					})
					filterable.exclude(sourceSet.getOutput().collect {
						project.relativePath(it)
					})
				})
				filterable.exclude(project.relativePath(layout.getBuildDirectory())) /* exclude buildDirectory */
				filterable.exclude(t.projectDirInputsExcludes) /* user specified excludes */
			})).withPathSensitivity(RELATIVE).withPropertyName("projectFolder")
			/* bnd can include from -buildpath */
			t.getInputs().files(project.sourceSets.main.getCompileClasspath()).withNormalizer(ClasspathNormalizer.class).withPropertyName("buildpath")
			/* bnd can include from -dependson */
			t.getInputs().files(getBuildDependencies("jar")).withPropertyName("buildDependencies")
			/* Workspace and project configuration changes should trigger jar task */
			t.getInputs().files(bndConfiguration()).withPathSensitivity(RELATIVE).withPropertyName("bndConfiguration")
			t.getOutputs().files(deliverables).withPropertyName("artifacts")
			t.getOutputs().file(layout.getBuildDirectory().file(Constants.BUILDFILES)).withPropertyName("buildfiles")
			t.doLast("build", tt -> {
				File[] built
				try {
					built = bndProject.build()
					if (Objects.nonNull(built)) {
						long now = System.currentTimeMillis()
						for (File f : built) {
							f.setLastModified(now)
						}
					}
				} catch (Exception e) {
					throw new GradleException("Project ${bndProject.getName()} failed to build", e)
				}
				checkErrors(tt.getLogger())
				if (Objects.nonNull(built)) {
					tt.getLogger().info("Generated bundles: {}", built as Object)
				}
			})
		})

		var jarDependencies = tasks.register("jarDependencies", t -> {
			t.setDescription("Jar all projects this project depends on.")
			t.setGroup("build")
			t.dependsOn(getBuildDependencies("jar"))
		})

		var buildDependencies = tasks.register("buildDependencies", t -> {
			t.setDescription("Assembles and tests all projects this project depends on.")
			t.setGroup("build")
			t.dependsOn(getTestDependencies("buildNeeded"))
		})

		var buildNeeded = tasks.named("buildNeeded", t -> {
			t.dependsOn(buildDependencies)
		})

		var buildDependents = tasks.named("buildDependents", t -> {
			t.dependsOn(getDependents("buildDependents"))
		})

		var release = tasks.register("release", t -> {
			t.setDescription("Release this project to the release repository.")
			t.setGroup("release")
			t.setEnabled(!bndProject.isNoBundles() && !project.bnd(Constants.RELEASEREPO, "unset").isEmpty())
			t.getInputs().files(jar).withPropertyName(jar.getName())
			t.doLast("release", tt -> {
				try {
					bndProject.release()
				} catch (Exception e) {
					throw new GradleException("Project ${bndProject.getName()} failed to release", e)
				}
				checkErrors(tt.getLogger())
			})
		})

		var releaseDependencies = tasks.register("releaseDependencies", t -> {
			t.setDescription("Release all projects this project depends on.")
			t.setGroup("release")
			t.dependsOn(getBuildDependencies("releaseNeeded"))
		})

		var releaseNeeded = tasks.register("releaseNeeded", t -> {
			t.setDescription("Release this project and all projects it depends on.")
			t.setGroup("release")
			t.dependsOn(releaseDependencies, release)
		})

		var test = tasks.named("test", t -> {
			t.setEnabled(!project.bndis(Constants.NOJUNIT) && !project.bndis("no.junit"))
			/* tests can depend upon jars from -dependson */
			t.getInputs().files(getBuildDependencies("jar")).withPropertyName("buildDependencies")
			t.doFirst("checkErrors", tt -> checkErrors(tt.getLogger(), tt.ignoreFailures))
		})

		var testOSGi = tasks.register("testOSGi", TestOSGi.class, t -> {
			t.setDescription("Runs the OSGi JUnit tests by launching a framework and running the tests in the launched framework.")
			t.setGroup("verification")
			t.setEnabled(!project.bndis(Constants.NOJUNITOSGI) && !project.bndUnprocessed(Constants.TESTCASES, "").isEmpty())
			t.getInputs().files(jar).withPropertyName(jar.getName())
			t.bndrun = bndProject.getPropertiesFile()
		})

		var check = tasks.named("check", t -> {
			t.dependsOn(testOSGi)
		})

		var checkDependencies = tasks.register("checkDependencies", t -> {
			t.setDescription("Runs all checks on all projects this project depends on.")
			t.setGroup("verification")
			t.dependsOn(getTestDependencies("checkNeeded"))
		})

		var checkNeeded = tasks.register("checkNeeded", t -> {
			t.setDescription("Runs all checks on this project and all projects it depends on.")
			t.setGroup("verification")
			t.dependsOn(checkDependencies, check)
		})

		var clean = tasks.named("clean", t -> {
			t.setDescription("Cleans the build and compiler output directories of this project.")
			t.delete(layout.getBuildDirectory(), project.sourceSets.main.getOutput(), project.sourceSets.test.getOutput())
			if (generate) {
				t.delete(generate)
			}
		})

		var cleanDependencies = tasks.register("cleanDependencies", t -> {
			t.setDescription("Cleans all projects this project depends on.")
			t.setGroup("build")
			t.dependsOn(getTestDependencies("cleanNeeded"))
		})

		var cleanNeeded = tasks.register("cleanNeeded", t -> {
			t.setDescription("Cleans this project and all projects it depends on.")
			t.setGroup("build")
			t.dependsOn(cleanDependencies, clean)
		})

		var assemble = tasks.named("assemble")

		Set<File> bndruns = layout.getProjectDirectory().getAsFileTree().matching(filterable -> filterable.include("*.bndrun")).getFiles()

		var exportTasks = bndruns.stream().map((File runFile) -> {
			tasks.register("export.${runFile.getName() - '.bndrun'}", Export.class, t -> {
				t.setDescription("Export the ${runFile.getName()} file.")
				t.setGroup("export")
				t.dependsOn(assemble)
				t.bndrun = runFile
				t.exporter = EXECUTABLE_JAR
			})
		}).collect(toList())

		var export = tasks.register("export", t -> {
			t.setDescription("Export all the bndrun files.")
			t.setGroup("export")
			t.dependsOn(exportTasks)
		})

		var runbundlesTasks = bndruns.stream().map((File runFile) -> {
			tasks.register("runbundles.${runFile.getName() - '.bndrun'}", Export.class, t -> {
				t.setDescription("Create a distribution of the runbundles in the ${runFile.getName()} file.")
				t.setGroup("export")
				t.dependsOn(assemble)
				t.bndrun = runFile
				t.exporter = RUNBUNDLES
			})
		}).collect(toList())

		var runbundles = tasks.register("runbundles", t -> {
			t.setDescription("Create a distribution of the runbundles in each of the bndrun files.")
			t.setGroup("export")
			t.dependsOn(runbundlesTasks)
		})

		var resolveTasks = bndruns.stream().map((File runFile) -> {
			tasks.register("resolve.${runFile.getName() - '.bndrun'}", Resolve.class, t -> {
				t.setDescription("Resolve the runbundles required for ${runFile.getName()} file.")
				t.setGroup("export")
				t.dependsOn(assemble)
				t.bndrun = runFile
			})
		}).collect(toList())

		var resolve = tasks.register("resolve", t -> {
			t.setDescription("Resolve the runbundles required for each of the bndrun files.")
			t.setGroup("export")
			t.dependsOn(resolveTasks)
		})

		bndruns.forEach((File runFile) -> {
			tasks.register("run.${runFile.getName() - '.bndrun'}", Bndrun.class, t -> {
				t.setDescription("Run the bndrun file ${runFile.getName()}.")
				t.setGroup("export")
				t.dependsOn(assemble)
				t.bndrun = runFile
			})
		})

		bndruns.forEach((File runFile) -> {
			tasks.register("testrun.${runFile.getName() - '.bndrun'}", TestOSGi.class, t -> {
				t.setDescription("Runs the OSGi JUnit tests in the bndrun file ${runFile.getName()}.")
				t.setGroup("verification")
				t.dependsOn(assemble)
				t.bndrun = runFile
			})
		})

		var echo = tasks.register("echo", t -> {
			t.setDescription("Displays the bnd project information.")
			t.setGroup("help")
			var compileJava = tasks.getByName("compileJava")
			var compileTestJava = tasks.getByName("compileTestJava")
			t.doLast("echo", tt -> {
				println("""
------------------------------------------------------------
Project ${project.getName()} // Bnd version ${About.CURRENT}
------------------------------------------------------------

project.workspace:      ${workspace.getLayout().getProjectDirectory()}
project.name:           ${project.getName()}
project.dir:            ${layout.getProjectDirectory()}
target:                 ${unwrap(layout.getBuildDirectory())}
project.dependson:      ${bndProject.getDependson()}
project.sourcepath:     ${project.sourceSets.main.getAllSource().getSourceDirectories().getAsPath()}
project.output:         ${compileJava.getDestinationDir()}
project.buildpath:      ${compileJava.getClasspath().getAsPath()}
project.allsourcepath:  ${project.bnd.allSrcDirs.getAsPath()}
project.testsrc:        ${project.sourceSets.test.getAllSource().getSourceDirectories().getAsPath()}
project.testoutput:     ${compileTestJava.getDestinationDir()}
project.testpath:       ${compileTestJava.getClasspath().getAsPath()}
project.bootclasspath:  ${compileJava.getOptions().getBootstrapClasspath()?.getAsPath()?:""}
project.deliverables:   ${deliverables.getFiles()}
javac:                  ${compileJava.getOptions().getForkOptions().getExecutable()?:"javac"}
javac.source:           ${javacSource.getOrElse("")}
javac.target:           ${javacTarget.getOrElse("")}
javac.profile:          ${javacProfile.getOrElse("")}
""")
				checkErrors(tt.getLogger(), true)
			})
		})

		var bndproperties = tasks.register("bndproperties", t -> {
			t.setDescription("Displays the bnd properties.")
			t.setGroup("help")
			t.doLast("bndproperties", tt -> {
				println("""
------------------------------------------------------------
Project ${project.getName()}
------------------------------------------------------------
""")
				bndProject.getPropertyKeys(true)
				.stream()
				.sorted()
				.forEachOrdered(key -> println("${key}: ${project.bnd(key, '')}"))
				println()
				checkErrors(tt.getLogger(), true)
			})
		})

		// Depend upon an output dir to avoid parallel task execution.
		// This effectively claims the resource and prevents
		// tasks claiming the same resource from executing concurrently.
		// -noparallel: launchpad;task="test,echo"
		Project noparallel = workspace.findProperty("cnf") ?: workspace
		bndProject.getMergedParameters("-noparallel").forEach((key, attrs) -> {
			var category = removeDuplicateMarker(key)
			var resource = noparallel.getLayout().getBuildDirectory().dir("noparallel/${category}")
			var taskNames = attrs?.get("task")
			Strings.splitAsStream(taskNames).forEach(taskName ->
			tasks.named(taskName, t -> {
				t.getOutputs().dir(resource).withPropertyName(category)
			}))
		})
	}

	private Provider<List<TaskProvider<?>>> getTasks(Collection<? extends aQute.bnd.build.Project> projects, String taskName) {
		Project workspace = this.workspace
		return project.provider(() -> projects.stream()
		.map(p -> workspace.project(p.getName()).getTasks().named(taskName))
		.collect(toList()))
	}

	private ConfigurableFileCollection pathFiles(Collection<? extends Container> path) {
		return objects.fileCollection().from(decontainer(path))
		.builtBy(getTasks(path.stream().filter((Container c) -> c.getType() == TYPE.PROJECT)
		.map((Container c) -> c.getProject())
		.collect(toList()), "jar"))
	}

	private Provider<List<TaskProvider<?>>> getBuildDependencies(String taskName) {
		return getTasks(bndProject.getBuildDependencies(), taskName)
	}

	private Provider<List<TaskProvider<?>>> getTestDependencies(String taskName) {
		return getTasks(bndProject.getTestDependencies(), taskName)
	}

	private Provider<List<TaskProvider<?>>> getDependents(String taskName) {
		return getTasks(bndProject.getDependents(), taskName)
	}

	private List<File> decontainer(Collection<? extends Container> path) {
		return path.stream().map((Container c) -> c.getFile()).collect(toList())
	}

	private CommandLineArgumentProvider argProvider(Provider<? extends Iterable<String>> provider) {
		return () -> provider.getOrElse(Collections.emptyList())
	}

	private ConfigurableFileCollection bndConfiguration() {
		Workspace bndWorkspace = bndProject.getWorkspace()
		return objects.fileCollection().from(bndWorkspace.getPropertiesFile(), bndWorkspace.getIncluded(),
		bndProject.getPropertiesFile(), bndProject.getIncluded())
	}

	private void checkErrors(Logger logger, boolean ignoreFailures = false) {
		checkProjectErrors(bndProject, logger, ignoreFailures)
	}

	private void checkProjectErrors(aQute.bnd.build.Project p, Logger logger, boolean ignoreFailures = false) {
		p.getInfo(p.getWorkspace(), p.getWorkspace().getBase().getName().concat(" :"))
		boolean failed = !ignoreFailures && !p.isOk()
		int errorCount = p.getErrors().size()
		logReport(p, logger)
		p.clear()
		if (failed) {
			String str = " even though no errors were reported"
			if (errorCount == 1) {
				str = ", one error was reported"
			} else if (errorCount > 1) {
				str = ", ${errorCount} errors were reported"
			}
			throw new GradleException("${p.getName()} has errors${str}")
		}
	}
}
