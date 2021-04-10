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
import static org.gradle.api.tasks.PathSensitivity.RELATIVE

import aQute.lib.strings.Strings
import aQute.bnd.build.Container
import aQute.bnd.build.Container.TYPE
import aQute.bnd.build.Workspace
import aQute.bnd.osgi.About
import aQute.bnd.osgi.Constants

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
import org.gradle.api.tasks.compile.JavaCompile

public class BndPlugin implements Plugin<Project> {
  public static final String PLUGINID = 'biz.aQute.bnd'
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
    Project workspace = this.workspace = project.parent
    ProjectLayout layout = project.layout
    ObjectFactory objects = this.objects = project.objects
    TaskContainer tasks = project.tasks
    if (project.plugins.hasPlugin(BndBuilderPlugin.PLUGINID)) {
        throw new GradleException("Project already has '${BndBuilderPlugin.PLUGINID}' plugin applied.")
    }
    if (workspace == null) {
      throw new GradleException("The '${PLUGINID}' plugin cannot be applied to the root project. Perhaps you meant to use the '${BndBuilderPlugin.PLUGINID}' plugin?")
    }
    Workspace bndWorkspace = BndWorkspacePlugin.getBndWorkspace(workspace)
    aQute.bnd.build.Project bndProject = this.bndProject = bndWorkspace.getProject(project.name)
    if (bndProject == null) {
      throw new GradleException("Unable to load bnd project ${name} from workspace ${workspace.layout.projectDirectory}")
    }
    bndProject.prepare()
    if (!bndProject.isValid()) {
      checkErrors(project.logger)
      throw new GradleException("Project ${bndProject.getName()} is not a valid bnd project")
    }
    project.extensions.create('bnd', BndProperties, bndProject)
    project.convention.plugins.bnd = new BndPluginConvention(project)

    layout.buildDirectory.set(bndProject.getTargetDir())
    project.plugins.apply('java')
    project.libsDirName = '.'
    project.testResultsDirName = project.bnd('test-reports', 'test-reports')

    if (project.hasProperty('bnd_defaultTask')) {
      project.defaultTasks = project.bnd_defaultTask.trim().split(/\s*,\s*/)
    }

    /* Set up configurations */
    project.configurations {
      runtimeOnly.artifacts.clear()
      archives.artifacts.clear()
    }
    /* Set up deliverables */
    bndProject.getDeliverables()*.getFile().each { File deliverable ->
      project.artifacts {
        runtimeOnly(deliverable) {
           builtBy('jar')
        }
        archives(deliverable) {
           builtBy('jar')
        }
      }
    }
    FileCollection deliverables = project.configurations.archives.artifacts.files

    /* Set up Bnd generate support */
    def generateInputs = bndProject.getGenerate().getInputs()
    def generate = null
    if (!generateInputs.isErr() && !generateInputs.unwrap().isEmpty()) {
      generate = tasks.register('generate') { t ->
        t.description = 'Generate source code'
        t.group = 'build'
        t.inputs.files(generateInputs.unwrap()).withPathSensitivity(RELATIVE).withPropertyName('generateInputs')
        /* bnd can include from -dependson */
        t.inputs.files(getBuildDependencies('jar')).withPropertyName('buildDependencies')
        /* Workspace and project configuration changes should trigger task */
        t.inputs.files(bndProject.getWorkspace().getPropertiesFile(),
          bndProject.getWorkspace().getIncluded(),
          bndProject.getPropertiesFile(),
          bndProject.getIncluded()).withPathSensitivity(RELATIVE).withPropertyName('bndFiles')
        t.outputs.dirs(bndProject.getGenerate().getOutputDirs()).withPropertyName('generateOutputs')
        t.doLast('generate') { tt ->
          try {
            bndProject.getGenerate().generate(false)
          } catch (Exception e) {
            throw new GradleException("Project ${bndProject.getName()} failed to generate", e)
          }
          checkErrors(tt.logger)
        }
      }
    }
    /* Set up source sets */
    project.sourceSets {
      /* bnd uses the same directory for java and resources. */
      main {
        ConfigurableFileCollection srcDirs = objects.fileCollection().from(bndProject.getSourcePath())
        File destinationDir = bndProject.getSrcOutput()
        java.srcDirs = srcDirs
        resources.srcDirs = srcDirs
        java.outputDir = destinationDir
        output.resourcesDir = destinationDir
        tasks.named(compileJavaTaskName) { t ->
          t.destinationDir = destinationDir
          if (generate) {
            t.inputs.files(generate).withPropertyName(generate.name)
          }
          jarLibraryElements(t, compileClasspathConfigurationName)
        }
        output.dir(destinationDir, 'builtBy': compileJavaTaskName)
      }
      test {
        ConfigurableFileCollection srcDirs = objects.fileCollection().from(bndProject.getTestSrc())
        File destinationDir = bndProject.getTestOutput()
        java.srcDirs = srcDirs
        resources.srcDirs = srcDirs
        java.outputDir = destinationDir
        output.resourcesDir = destinationDir
        tasks.named(compileJavaTaskName) { t ->
          t.destinationDir = destinationDir
          jarLibraryElements(t, compileClasspathConfigurationName)
        }
        output.dir(destinationDir, 'builtBy': compileJavaTaskName)
      }
    }
    /* Configure srcDirs for any additional languages */
    project.afterEvaluate {
      project.sourceSets {
        main {
          convention.plugins.each { lang, sourceDirSets ->
            def sourceDirSet = sourceDirSets[lang]
            if (sourceDirSet.hasProperty('srcDirs') && sourceDirSet.hasProperty('outputDir')){
              File destinationDir = java.outputDir
              String compileTaskName = getCompileTaskName(lang)
              String resourceTaskName = getProcessResourcesTaskName()
              try {
                tasks.named(compileTaskName) { t ->
                  t.destinationDir = destinationDir
                  t.inputs.files(tasks.named(resourceTaskName)).withPropertyName(resourceTaskName)
                  if (generate) {
                    t.inputs.files(generate).withPropertyName(generate.name)
                  }
                }
                sourceDirSet.srcDirs = java.srcDirs
                sourceDirSet.outputDir = destinationDir
                output.dir(destinationDir, 'builtBy': compileTaskName)
              } catch (UnknownDomainObjectException e) {
               // no such task
              }
            }
          }
        }
        test {
          convention.plugins.each { lang, sourceDirSets ->
            def sourceDirSet = sourceDirSets[lang]
            if (sourceDirSet.hasProperty('srcDirs') && sourceDirSet.hasProperty('outputDir')){
              File destinationDir = java.outputDir
              String compileTaskName = getCompileTaskName(lang)
              String resourceTaskName = getProcessResourcesTaskName()
              try {
                tasks.named(compileTaskName) { t ->
                  t.destinationDir = destinationDir
                  t.inputs.files(tasks.named(resourceTaskName)).withPropertyName(resourceTaskName)
                }
                sourceDirSet.srcDirs = java.srcDirs
                sourceDirSet.outputDir = destinationDir
                output.dir(destinationDir, 'builtBy': compileTaskName)
              } catch (UnknownDomainObjectException e) {
               // no such task
              }
            }
          }
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
    ConfigurableFileCollection javacBootclasspath = objects.fileCollection().from(bndProject.getBootclasspath()*.getFile())
    Property<String> javacSource = objects.property(String.class).convention(project.bnd('javac.source'))
    if (javacSource.isPresent()) {
      project.sourceCompatibility = javacSource.get()
    } else {
      javacSource.convention(provider({ -> project.sourceCompatibility.toString() }))
    }
    Property<String> javacTarget = objects.property(String.class).convention(project.bnd('javac.target'))
    if (javacTarget.isPresent()) {
      if (javacTarget.get() == 'jsr14') {
        javacTarget.set('1.5')
        javacBootclasspath.setFrom(bndProject.getBundle('ee.j2se', '1.5', null, ['strategy':'lowest']).getFile())
      }
      project.targetCompatibility = javacTarget.get()
    } else {
      javacTarget.convention(provider({ -> project.targetCompatibility.toString() }))
    }
    String javac = project.bnd('javac')
    Property<String> javacProfile = objects.property(String.class)
    if (!project.bnd('javac.profile', '').empty) {
      javacProfile.convention(project.bnd('javac.profile'))
    }
    Property<String> javacRelease = objects.property(String.class)
    if (JavaVersion.current().isJava9Compatible()) {
      javacRelease.convention(project.provider({ -> 
        if ((javacSource.get() == javacTarget.get()) && javacBootclasspath.empty && !javacProfile.isPresent()) {
          return JavaVersion.toVersion(javacSource.get()).majorVersion
        }
        return null
      }))
    }
    boolean javacDebug = project.bndis('javac.debug')
    boolean javacDeprecation = isTrue(project.bnd('javac.deprecation', 'true'))
    String javacEncoding = project.bnd('javac.encoding', 'UTF-8')
    tasks.withType(JavaCompile.class).configureEach { t ->
      t.sourceCompatibility = javacSource.get()
      t.targetCompatibility = javacTarget.get()
      def options = t.options
      if (javacDebug) {
        options.debugOptions.debugLevel = 'source,lines,vars'
      }
      options.verbose = t.logger.isDebugEnabled()
      options.listFiles = t.logger.isInfoEnabled()
      options.deprecation = javacDeprecation
      options.encoding = javacEncoding
      if (javac != 'javac') {
        options.fork = true
        options.forkOptions.executable = javac
      }
      if (!javacBootclasspath.empty) {
        options.fork = true
        options.bootstrapClasspath = javacBootclasspath
      }
      if (javacProfile.isPresent()) {
        options.compilerArgs.addAll(['-profile', javacProfile.get()])
      }
      if (javacRelease.isPresent()) {
        options.compilerArgs.addAll(['--release', javacRelease.get()])
      }
      t.doFirst('checkErrors') { tt ->
        checkErrors(tt.logger)
        if (tt.logger.isInfoEnabled()) {
          tt.logger.info('Compile to {}', tt.destinationDir)
          if (tt.options.compilerArgs.contains('--release')) {
            tt.logger.info('{}', tt.options.compilerArgs.join(' '))
          } else {
            tt.logger.info('-source {} -target {} {}', tt.sourceCompatibility, tt.targetCompatibility, tt.options.compilerArgs.join(' '))
          }
          tt.logger.info('-classpath {}', tt.classpath.asPath)
          if (tt.options.bootstrapClasspath != null) {
            tt.logger.info('-bootclasspath {}', tt.options.bootstrapClasspath.asPath)
          }
        }
      }
    }

    def jar = tasks.named('jar') { t ->
      t.description = 'Jar this project\'s bundles.'
      t.actions.clear() /* Replace the standard task actions */
      t.enabled = !bndProject.isNoBundles()
      /* use first deliverable as archiveFileName */
      t.archiveFileName = project.provider({ -> deliverables.find()?.name ?: bndProject.getName() })
      /* Additional excludes for projectDir inputs */
      t.ext.projectDirInputsExcludes = Strings.split(t.project.bndMerge(Constants.BUILDERIGNORE)).collect { it.concat('/') }
      /* all other files in the project like bnd and resources */
      t.inputs.files(project.fileTree(layout.projectDirectory) { tree ->
        project.sourceSets.each { sourceSet -> /* exclude sourceSet dirs */
          tree.exclude(sourceSet.allSource.sourceDirectories.collect {
            project.relativePath(it)
          })
          tree.exclude(sourceSet.output.collect {
            project.relativePath(it)
          })
        }
        tree.exclude(project.relativePath(layout.buildDirectory)) /* exclude buildDirectory */
        tree.exclude(t.projectDirInputsExcludes) /* user specified excludes */
      }).withPathSensitivity(RELATIVE).withPropertyName('projectFolder')
      /* bnd can include from -buildpath */
      t.inputs.files(project.sourceSets.main.compileClasspath).withNormalizer(ClasspathNormalizer).withPropertyName('buildpath')
      /* bnd can include from -dependson */
      t.inputs.files(getBuildDependencies('jar')).withPropertyName('buildDependencies')
      /* Workspace and project configuration changes should trigger jar task */
      t.inputs.files(bndProject.getWorkspace().getPropertiesFile(),
        bndProject.getWorkspace().getIncluded(),
        bndProject.getPropertiesFile(),
        bndProject.getIncluded()).withPathSensitivity(RELATIVE).withPropertyName('bndFiles')
      t.outputs.files(deliverables).withPropertyName('artifacts')
      t.outputs.file(layout.buildDirectory.file(Constants.BUILDFILES)).withPropertyName('buildfiles')
      t.doLast('build') { tt ->
        File[] built
        try {
          built = bndProject.build()
          long now = System.currentTimeMillis()
          built?.each {
            it.setLastModified(now)
          }
        } catch (Exception e) {
          throw new GradleException("Project ${bndProject.getName()} failed to build", e)
        }
        checkErrors(tt.logger)
        if (built != null) {
          tt.logger.info('Generated bundles: {}', built as Object)
        }
      }
    }

    def jarDependencies = tasks.register('jarDependencies') { t ->
      t.description = 'Jar all projects this project depends on.'
      t.group = 'build'
      t.dependsOn(getBuildDependencies('jar'))
    }

    def buildDependencies = tasks.register('buildDependencies') { t ->
      t.description = 'Assembles and tests all projects this project depends on.'
      t.group = 'build'
      t.dependsOn(getTestDependencies('buildNeeded'))
    }

    def buildNeeded = tasks.named('buildNeeded') { t ->
      t.dependsOn(buildDependencies)
    }

    def buildDependents = tasks.named('buildDependents') { t ->
      t.dependsOn(getDependents('buildDependents'))
    }

    def release = tasks.register('release') { t ->
      t.description = 'Release this project to the release repository.'
      t.group = 'release'
      t.enabled = !bndProject.isNoBundles() && !project.bnd(Constants.RELEASEREPO, 'unset').empty
      t.inputs.files(jar).withPropertyName(jar.name)
      t.doLast('release') { tt ->
        try {
          bndProject.release()
        } catch (Exception e) {
          throw new GradleException("Project ${bndProject.getName()} failed to release", e)
        }
        checkErrors(tt.logger)
      }
    }

    def releaseDependencies = tasks.register('releaseDependencies') { t ->
      t.description = 'Release all projects this project depends on.'
      t.group = 'release'
      t.dependsOn(getBuildDependencies('releaseNeeded'))
    }

    def releaseNeeded = tasks.register('releaseNeeded') { t ->
      t.description = 'Release this project and all projects it depends on.'
      t.group = 'release'
      t.dependsOn(releaseDependencies, release)
    }

    def test = tasks.named('test') { t ->
      t.enabled = !project.bndis(Constants.NOJUNIT) && !project.bndis('no.junit')
      /* tests can depend upon jars from -dependson */
      t.inputs.files(getBuildDependencies('jar')).withPropertyName('buildDependencies')
      t.doFirst('checkErrors') { tt ->
        checkErrors(tt.logger, tt.ignoreFailures)
      }
    }

    def testOSGi = tasks.register('testOSGi', TestOSGi.class) { t ->
      t.description = 'Runs the OSGi JUnit tests by launching a framework and running the tests in the launched framework.'
      t.group = 'verification'
      t.enabled = !project.bndis(Constants.NOJUNITOSGI) && !project.bndUnprocessed(Constants.TESTCASES, '').empty
      t.inputs.files(jar).withPropertyName(jar.name)
      t.bndrun = bndProject.getPropertiesFile()
    }

    def check = tasks.named('check') { t ->
      t.dependsOn(testOSGi)
    }

    def checkDependencies = tasks.register('checkDependencies') { t ->
      t.description = 'Runs all checks on all projects this project depends on.'
      t.group = 'verification'
      t.dependsOn(getTestDependencies('checkNeeded'))
    }

    def checkNeeded = tasks.register('checkNeeded') { t ->
      t.description = 'Runs all checks on this project and all projects it depends on.'
      t.group = 'verification'
      t.dependsOn(checkDependencies, check)
    }

    def clean = tasks.named('clean') { t ->
      t.description = 'Cleans the build and compiler output directories of this project.'
      t.delete(layout.buildDirectory, project.sourceSets.main.output, project.sourceSets.test.output)
      if (generate) {
        t.delete(generate)
      }
    }

    def cleanDependencies = tasks.register('cleanDependencies') { t ->
      t.description = 'Cleans all projects this project depends on.'
      t.group = 'build'
      t.dependsOn(getTestDependencies('cleanNeeded'))
    }

    def cleanNeeded = tasks.register('cleanNeeded') { t ->
      t.description = 'Cleans this project and all projects it depends on.'
      t.group = 'build'
      t.dependsOn(cleanDependencies, clean)
    }

    def assemble = tasks.named('assemble')

    def bndruns = project.fileTree(layout.projectDirectory) {
        include('*.bndrun')
    }

    def exportTasks = bndruns.collect { File runFile ->
      tasks.register("export.${runFile.name - '.bndrun'}", Export.class) { t ->
        t.description = "Export the ${runFile.name} file."
        t.group = 'export'
        t.dependsOn(assemble)
        t.bndrun = runFile
        t.exporter = EXECUTABLE_JAR
      }
    }

    def export = tasks.register('export') { t ->
      t.description = 'Export all the bndrun files.'
      t.group = 'export'
      t.dependsOn(exportTasks)
    }

    def runbundlesTasks = bndruns.collect { File runFile ->
      tasks.register("runbundles.${runFile.name - '.bndrun'}", Export.class) { t ->
        t.description = "Create a distribution of the runbundles in the ${runFile.name} file."
        t.group = 'export'
        t.dependsOn(assemble)
        t.bndrun = runFile
        t.exporter = RUNBUNDLES
      }
    }

    def runbundles = tasks.register('runbundles') { t ->
      t.description = 'Create a distribution of the runbundles in each of the bndrun files.'
      t.group = 'export'
      t.dependsOn(runbundlesTasks)
    }

    def resolveTasks = bndruns.collect { File runFile ->
      tasks.register("resolve.${runFile.name - '.bndrun'}", Resolve.class) { t ->
        t.description = "Resolve the runbundles required for ${runFile.name} file."
        t.group = 'export'
        t.dependsOn(assemble)
        t.bndrun = runFile
      }
    }

    def resolve = tasks.register('resolve') { t ->
      t.description = 'Resolve the runbundles required for each of the bndrun files.'
      t.group = 'export'
      t.dependsOn(resolveTasks)
    }

    bndruns.forEach { File runFile ->
      tasks.register("run.${runFile.name - '.bndrun'}", Bndrun.class) { t ->
        t.description = "Run the bndrun file ${runFile.name}."
        t.group = 'export'
        t.dependsOn(assemble)
        t.bndrun = runFile
      }
    }

    bndruns.forEach { File runFile ->
      tasks.register("testrun.${runFile.name - '.bndrun'}", TestOSGi.class) { t ->
        t.description = "Runs the OSGi JUnit tests in the bndrun file ${runFile.name}."
        t.group = 'verification'
        t.dependsOn(assemble)
        t.bndrun = runFile
      }
    }

    def echo = tasks.register('echo') { t ->
      t.description = 'Displays the bnd project information.'
      t.group = 'help'
      def compileJava = tasks.getByName('compileJava')
      def compileTestJava = tasks.getByName('compileTestJava')
      t.doLast('echo') { tt ->
        println("""
------------------------------------------------------------
Project ${project.name} // Bnd version ${About.CURRENT}
------------------------------------------------------------

project.workspace:      ${workspace.layout.projectDirectory}
project.name:           ${project.name}
project.dir:            ${layout.projectDirectory}
target:                 ${unwrap(layout.buildDirectory)}
project.dependson:      ${bndProject.getDependson()*.getName()}
project.sourcepath:     ${project.sourceSets.main.allSource.sourceDirectories.asPath}
project.output:         ${compileJava.destinationDir}
project.buildpath:      ${compileJava.classpath.asPath}
project.allsourcepath:  ${project.bnd.allSrcDirs.asPath}
project.testsrc:        ${project.sourceSets.test.allSource.sourceDirectories.asPath}
project.testoutput:     ${compileTestJava.destinationDir}
project.testpath:       ${compileTestJava.classpath.asPath}
project.bootclasspath:  ${compileJava.options.bootstrapClasspath?.asPath?:''}
project.deliverables:   ${deliverables*.path}
javac:                  ${compileJava.options.forkOptions.executable?:'javac'}
javac.source:           ${javacSource.getOrElse('')}
javac.target:           ${javacTarget.getOrElse('')}
javac.profile:          ${javacProfile.getOrElse('')}
""")
        checkErrors(tt.logger, true)
      }
    }

    def bndproperties = tasks.register('bndproperties') { t ->
      t.description = 'Displays the bnd properties.'
      t.group = 'help'
      t.doLast('bndproperties') { tt ->
        println("""
------------------------------------------------------------
Project ${project.name}
------------------------------------------------------------
""")
        bndProject.getPropertyKeys(true).sort().each {
          println("${it}: ${tt.project.bnd(it, '')}")
        }
        println()
        checkErrors(tt.logger, true)
      }
    }

    // Depend upon an output dir to avoid parallel task execution.
    // This effectively claims the resource and prevents
    // tasks claiming the same resource from executing concurrently.
    // -noparallel: launchpad;task="test,echo"
    Project noparallel = workspace.findProperty('cnf') ?: workspace
    bndProject.getMergedParameters('-noparallel').each { key, attrs ->
      def taskNames = attrs?.'task'
      if (taskNames) {
        def category = removeDuplicateMarker(key)
        def resource = noparallel.layout.buildDirectory.dir("noparallel/${category}")
        taskNames.trim().tokenize(',').each { taskName ->
          tasks.named(taskName.trim()) { t ->
            t.outputs.dir(resource).withPropertyName(category)
          }
        }
      }
    }
  }

  private Provider<List<TaskProvider<?>>> getTasks(Collection<aQute.bnd.build.Project> projects, String taskName) {
    Project workspace = this.workspace
    return project.provider({ ->
      projects.collect { aQute.bnd.build.Project p ->
        workspace.project(p.getName()).tasks.named(taskName)
      }
    })
  }

  private ConfigurableFileCollection pathFiles(Collection<Container> path) {
    return objects.fileCollection().from(path*.getFile())
      .builtBy(getTasks(path.findAll { Container c ->
          c.getType() == TYPE.PROJECT
        }.collect { Container c ->
          c.getProject()
        }, 'jar'))
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

  private void checkErrors(Logger logger, boolean ignoreFailures = false) {
    checkProjectErrors(bndProject, logger, ignoreFailures)
  }

  private void checkProjectErrors(aQute.bnd.build.Project p, Logger logger, boolean ignoreFailures = false) {
    p.getInfo(p.getWorkspace(), "${p.getWorkspace().getBase().name} :")
    boolean failed = !ignoreFailures && !p.isOk()
    int errorCount = p.getErrors().size()
    logReport(p, logger)
    p.clear()
    if (failed) {
      String str = ' even though no errors were reported'
      if (errorCount == 1) {
        str = ', one error was reported'
      } else if (errorCount > 1) {
        str = ", ${errorCount} errors were reported"
      }
      throw new GradleException("${p.getName()} has errors${str}")
    }
  }
}
