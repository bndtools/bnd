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
import org.gradle.api.file.ProjectLayout
import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.ClasspathNormalizer
import org.gradle.api.tasks.compile.JavaCompile

public class BndPlugin implements Plugin<Project> {
  public static final String PLUGINID = 'biz.aQute.bnd'
  private Project project
  private Project workspace
  private ProjectLayout layout
  private ObjectFactory objects
  private aQute.bnd.build.Project bndProject
  private BndPluginExtension extension

  /**
   * Apply the {@code biz.aQute.bnd} plugin to the specified project.
   */
  @Override
  public void apply(Project p) {
    extension = p.getExtensions()
      .create("bnd", BndPluginExtension.class);

    p.configure(p) { Project project ->
      this.project = project
      this.layout = project.layout
      this.objects = project.objects
      this.workspace = project.parent
      if (plugins.hasPlugin(BndBuilderPlugin.PLUGINID)) {
          throw new GradleException("Project already has '${BndBuilderPlugin.PLUGINID}' plugin applied.")
      }
      if (workspace == null) {
        throw new GradleException("The '${PLUGINID}' plugin cannot be applied to the root project. Perhaps you meant to use the '${BndBuilderPlugin.PLUGINID}' plugin?")
      }
      if (!workspace.hasProperty('bndWorkspace')) {
        workspace.ext.bndWorkspace = new Workspace(unwrap(workspace.layout.projectDirectory)).setOffline(gradle.startParameter.offline)
        if (gradle.ext.has('bndWorkspaceConfigure')) {
          gradle.bndWorkspaceConfigure(bndWorkspace)
        }
      }
      this.bndProject = bndWorkspace.getProject(name)
      if (bndProject == null) {
        throw new GradleException("Unable to load bnd project ${name} from workspace ${workspace.layout.projectDirectory}")
      }
      bndProject.prepare()
      if (!bndProject.isValid()) {
        checkErrors(logger)
        throw new GradleException("Project ${bndProject.getName()} is not a valid bnd project")
      }
      extensions.create('bnd', BndProperties, bndProject)
      convention.plugins.bnd = new BndPluginConvention(this)

      layout.buildDirectory.set(bndProject.getTargetDir())
      plugins.apply 'java'
      libsDirName = '.'
      testResultsDirName = bnd('test-reports', 'test-reports')

      if (project.hasProperty('bnd_defaultTask')) {
        defaultTasks = bnd_defaultTask.trim().split(/\s*,\s*/)
      }

      /* Set up configurations */
      configurations {
        runtimeOnly.artifacts.clear()
        archives.artifacts.clear()
      }
      /* Set up deliverables */
      bndProject.getDeliverables()*.getFile().each { File deliverable ->
        artifacts {
          runtimeOnly(deliverable) {
             builtBy 'jar'
          }
          archives(deliverable) {
             builtBy 'jar'
          }
        }
      }
      /* Set up Bnd generate support */
      def generateInputs = bndProject.getGenerate().getInputs()
      def generate = null
      if (!generateInputs.isErr() && !generateInputs.unwrap().isEmpty()) {
        generate = tasks.register('generate') { t ->
          t.description 'Generate source code'
          t.group 'build'
          t.inputs.files(generateInputs.unwrap()).withPathSensitivity(RELATIVE).withPropertyName('generateInputs')
          /* bnd can include from -dependson */
          t.inputs.files(getBuildDependencies('jar')).withPropertyName('buildDependencies')
          /* Workspace and project configuration changes should trigger task */
          t.inputs.files(bndProject.getWorkspace().getPropertiesFile(),
            bndProject.getWorkspace().getIncluded(),
            bndProject.getPropertiesFile(),
            bndProject.getIncluded()).withPathSensitivity(RELATIVE).withPropertyName('bndFiles')
          t.outputs.dirs(bndProject.getGenerate().getOutputDirs())
          t.doLast {
            try {
              bndProject.getGenerate().generate(false)
            } catch (Exception e) {
              throw new GradleException("Project ${bndProject.getName()} failed to generate", e)
            }
            checkErrors(t.logger)
          }
        }
      }
      /* Set up source sets */
      sourceSets {
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
              t.inputs.files(generate)
            }
          }
          output.dir(destinationDir, 'builtBy': compileJavaTaskName)
          jarLibraryElements(project, compileClasspathConfigurationName)
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
          }
          output.dir(destinationDir, 'builtBy': compileJavaTaskName)
          jarLibraryElements(project, compileClasspathConfigurationName)
        }
      }
      /* Configure srcDirs for any additional languages */
      afterEvaluate {
        sourceSets {
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
                    t.inputs.files(tasks.named(resourceTaskName))
                    if (generate) {
                      t.inputs.files(generate)
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
                    t.inputs.files(tasks.named(resourceTaskName))
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

      bnd.ext.allSrcDirs = objects.fileCollection().from(bndProject.getAllsourcepath())
      /* Set up dependencies */
      dependencies {
        implementation pathFiles(bndProject.getBuildpath())
        runtimeOnly objects.fileCollection().from(bndProject.getSrcOutput())
        testImplementation pathFiles(bndProject.getTestpath())
        testRuntimeOnly objects.fileCollection().from(bndProject.getTestOutput())
      }
      /* Set up compile tasks */
      sourceCompatibility = bnd('javac.source', sourceCompatibility)
      String javacTarget = bnd('javac.target', targetCompatibility)
      ConfigurableFileCollection javacBootclasspath = objects.fileCollection().from(bndProject.getBootclasspath()*.getFile())
      if (javacTarget == 'jsr14') {
        javacTarget = '1.5'
        javacBootclasspath = objects.fileCollection().from(bndProject.getBundle('ee.j2se', '1.5', null, ['strategy':'lowest']).getFile())
      }
      targetCompatibility = javacTarget
      String javac = bnd('javac')
      String javacProfile = bnd('javac.profile', '')
      boolean javacDebug = bndis('javac.debug')
      boolean javacDeprecation = isTrue(bnd('javac.deprecation', 'true'))
      String javacEncoding = bnd('javac.encoding', 'UTF-8')
      tasks.withType(JavaCompile.class).configureEach { t ->
        configure(t.options) {
          if (javacDebug) {
            debugOptions.debugLevel = 'source,lines,vars'
          }
          verbose = t.logger.isDebugEnabled()
          listFiles = t.logger.isInfoEnabled()
          deprecation = javacDeprecation
          encoding = javacEncoding
          if (javac != 'javac') {
            fork = true
            forkOptions.executable = javac
          }
          if (!javacBootclasspath.empty) {
            fork = true
            bootstrapClasspath = javacBootclasspath
          }
          if (!javacProfile.empty) {
            compilerArgs.addAll(['-profile', javacProfile])
          }
          if (JavaVersion.current().isJava9Compatible()) {
            if ((project.sourceCompatibility == project.targetCompatibility) && javacBootclasspath.empty && javacProfile.empty) {
              compilerArgs.addAll(['--release', JavaVersion.toVersion(project.sourceCompatibility).majorVersion])
            }
          }
        }
        t.doFirst {
          checkErrors(t.logger)
          if (t.logger.isInfoEnabled()) {
            t.logger.info 'Compile to {}', t.destinationDir
            if (t.options.compilerArgs.contains('--release')) {
              t.logger.info '{}', t.options.compilerArgs.join(' ')
            } else {
              t.logger.info '-source {} -target {} {}', project.sourceCompatibility, project.targetCompatibility, options.compilerArgs.join(' ')
            }
            t.logger.info '-classpath {}', t.classpath.asPath
            if (t.options.bootstrapClasspath != null) {
              t.logger.info '-bootclasspath {}', t.options.bootstrapClasspath.asPath
            }
          }
        }
      }

      def jar = tasks.named('jar') { t ->
        t.description 'Jar this project\'s bundles.'
        t.actions.clear() /* Replace the standard task actions */
        t.enabled !bndProject.isNoBundles()
        project.configurations.archives.artifacts.files.find {
          t.archiveFileName = it.name /* use first artifact as archiveFileName */
        }
        /* Additional excludes for projectDir inputs */
        t.ext.projectDirInputsExcludes = Strings.split(bndMerge(Constants.BUILDERIGNORE)).collect { it.concat('/') }
        /* all other files in the project like bnd and resources */
        t.inputs.files({
          project.fileTree(layout.projectDirectory) { tree ->
            project.sourceSets.each { sourceSet -> /* exclude sourceSet dirs */
              tree.exclude sourceSet.allSource.sourceDirectories.collect {
                project.relativePath(it)
              }
              tree.exclude sourceSet.output.collect {
                project.relativePath(it)
              }
            }
            tree.exclude project.relativePath(buildDir) /* exclude buildDir */
            tree.exclude t.projectDirInputsExcludes /* user specified excludes */
          }
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
        t.outputs.files({ project.configurations.archives.artifacts.files }).withPropertyName('artifacts')
        t.outputs.file(layout.buildDirectory.file(Constants.BUILDFILES)).withPropertyName('buildfiles')
        t.doLast {
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
          checkErrors(t.logger)
          if (built != null) {
            t.logger.info 'Generated bundles: {}', built as Object
          }
        }
      }

      def jarDependencies = tasks.register('jarDependencies') { t ->
        t.description 'Jar all projects this project depends on.'
        t.dependsOn getBuildDependencies('jar')
        t.group 'build'
      }

      def buildDependencies = tasks.register('buildDependencies') { t ->
        t.description 'Assembles and tests all projects this project depends on.'
        t.dependsOn getTestDependencies('buildNeeded')
        t.group 'build'
      }

      def buildNeeded = tasks.named('buildNeeded') { t ->
        t.dependsOn buildDependencies
      }

      def buildDependents = tasks.named('buildDependents') { t ->
        t.dependsOn getDependents('buildDependents')
      }

      def release = tasks.register('release') { t ->
        t.description 'Release this project to the release repository.'
        t.group 'release'
        t.enabled !bndProject.isNoBundles() && !bnd(Constants.RELEASEREPO, 'unset').empty
        t.inputs.files jar
        t.doLast {
          try {
            bndProject.release()
          } catch (Exception e) {
            throw new GradleException("Project ${bndProject.getName()} failed to release", e)
          }
          checkErrors(t.logger)
        }
      }

      def releaseDependencies = tasks.register('releaseDependencies') { t ->
        t.description 'Release all projects this project depends on.'
        t.dependsOn getBuildDependencies('releaseNeeded')
        t.group 'release'
      }

      def releaseNeeded = tasks.register('releaseNeeded') { t ->
        t.description 'Release this project and all projects it depends on.'
        t.dependsOn releaseDependencies, release
        t.group 'release'
      }

      def test = tasks.named('test') { t ->
        t.enabled !bndis(Constants.NOJUNIT) && !bndis('no.junit')
        /* tests can depend upon jars from -dependson */
        t.inputs.files(getBuildDependencies('jar')).withPropertyName('buildDependencies')
        t.doFirst {
          checkErrors(t.logger, t.ignoreFailures)
        }
      }

      def testOSGi = tasks.register('testOSGi', TestOSGi.class) { t ->
        t.description 'Runs the OSGi JUnit tests by launching a framework and running the tests in the launched framework.'
        t.group 'verification'
        t.enabled !bndis(Constants.NOJUNITOSGI) && !bndUnprocessed(Constants.TESTCASES, '').empty
        t.inputs.files jar
        t.bndrun = bndProject.getPropertiesFile()
      }

      def check = tasks.named('check') { t ->
        t.dependsOn testOSGi
      }

      def checkDependencies = tasks.register('checkDependencies') { t ->
        t.description 'Runs all checks on all projects this project depends on.'
        t.dependsOn getTestDependencies('checkNeeded')
        t.group 'verification'
      }

      def checkNeeded = tasks.register('checkNeeded') { t ->
        t.description 'Runs all checks on this project and all projects it depends on.'
        t.dependsOn checkDependencies, check
        t.group 'verification'
      }

      def clean = tasks.named('clean') { t ->
        t.description 'Cleans the build and compiler output directories of this project.'
        t.delete layout.buildDirectory, project.sourceSets.main.output, project.sourceSets.test.output
        if (generate) {
          t.delete generate
        }
      }

      def cleanDependencies = tasks.register('cleanDependencies') { t ->
        t.description 'Cleans all projects this project depends on.'
        t.dependsOn getTestDependencies('cleanNeeded')
        t.group 'build'
      }

      def cleanNeeded = tasks.register('cleanNeeded') { t ->
        t.description 'Cleans this project and all projects it depends on.'
        t.dependsOn cleanDependencies, clean
        t.group 'build'
      }

      def bndruns = project.fileTree(layout.projectDirectory) {
          include '*.bndrun'
      }

      def exportTasks = bndruns.collect { runFile ->
        tasks.register("export.${runFile.name - '.bndrun'}", Export.class) { t ->
          t.description "Export the ${runFile.name} file."
          t.dependsOn 'assemble'
          t.group 'export'
          t.enabled runFile.name.matches(extension.getExportFilter());
          t.bndrun = runFile
          t.exporter = EXECUTABLE_JAR
        }
      }

      def export = tasks.register('export') { t ->
        t.description 'Export all the bndrun files.'
        t.group 'export'
        t.dependsOn exportTasks
      }

      def runbundlesTasks = bndruns.collect { runFile ->
        tasks.register("runbundles.${runFile.name - '.bndrun'}", Export.class) { t ->
          t.description "Create a distribution of the runbundles in the ${runFile.name} file."
          t.dependsOn 'assemble'
          t.group 'export'
          t.bndrun = runFile
          t.exporter = RUNBUNDLES
        }
      }

      def runbundles = tasks.register('runbundles') { t ->
        t.description 'Create a distribution of the runbundles in each of the bndrun files.'
        t.group 'export'
        t.dependsOn runbundlesTasks
      }

      def resolveTasks = bndruns.collect { runFile ->
        tasks.register("resolve.${runFile.name - '.bndrun'}", Resolve.class) { t ->
          t.description "Resolve the runbundles required for ${runFile.name} file."
          t.dependsOn 'assemble'
          t.group 'export'
          t.enabled runFile.name.matches(extension.getResolveFilter());
          t.bndrun = runFile
        }
      }

      def resolve = tasks.register('resolve') { t ->
        t.description 'Resolve the runbundles required for each of the bndrun files.'
        t.group 'export'
        t.dependsOn resolveTasks
      }

      bndruns.forEach { runFile ->
        tasks.register("run.${runFile.name - '.bndrun'}", Bndrun.class) { t ->
          t.description "Run the bndrun file ${runFile.name}."
          t.dependsOn 'assemble'
          t.group 'export'
          t.bndrun = runFile
        }
      }

      bndruns.forEach { runFile ->
        tasks.register("testrun.${runFile.name - '.bndrun'}", TestOSGi.class) { t ->
          t.description "Runs the OSGi JUnit tests in the bndrun file ${runFile.name}."
          t.dependsOn 'assemble'
          t.group 'verification'
          t.enabled runFile.name.matches(extension.getTestrunFilter());
          t.bndrun = runFile
        }
      }

      def echo = tasks.register('echo') { t ->
        t.description 'Displays the bnd project information.'
        t.group 'help'
        def compileJava = tasks.getByName('compileJava')
        def compileTestJava = tasks.getByName('compileTestJava')
        t.doLast {
          println """
------------------------------------------------------------
Project ${project.name} // Bnd version ${About.CURRENT}
------------------------------------------------------------

project.workspace:      ${workspace.layout.projectDirectory}
project.name:           ${project.name}
project.dir:            ${layout.projectDirectory}
target:                 ${unwrap(layout.buildDirectory)}
project.dependson:      ${bndProject.getDependson()*.getName()}
project.sourcepath:     ${project.sourceSets.main.java.sourceDirectories.asPath}
project.output:         ${compileJava.destinationDir}
project.buildpath:      ${compileJava.classpath.asPath}
project.allsourcepath:  ${bnd.allSrcDirs.asPath}
project.testsrc:        ${project.sourceSets.test.java.sourceDirectories.asPath}
project.testoutput:     ${compileTestJava.destinationDir}
project.testpath:       ${compileTestJava.classpath.asPath}
project.bootclasspath:  ${compileJava.options.bootstrapClasspath?.asPath?:''}
project.deliverables:   ${project.configurations.archives.artifacts.files*.path}
javac:                  ${compileJava.options.forkOptions.executable?:'javac'}
javac.source:           ${project.sourceCompatibility}
javac.target:           ${project.targetCompatibility}
javac.profile:          ${javacProfile}
"""
          checkErrors(t.logger, true)
        }
      }

      def bndproperties = tasks.register('bndproperties') { t ->
        t.description 'Displays the bnd properties.'
        t.group 'help'
        t.doLast {
          println """
------------------------------------------------------------
Project ${project.name}
------------------------------------------------------------
"""
          bndProject.getPropertyKeys(true).sort().each {
            println "${it}: ${bnd(it, '')}"
          }
          println()
          checkErrors(t.logger, true)
        }
      }

      // Depend upon an output dir to avoid parallel task execution.
      // This effectively claims the resource and prevents
      // tasks claiming the same resource from executing concurrently.
      // -noparallel: launchpad;task="test,echo"
      def noparallel = bndProject.getMergedParameters('-noparallel')
      noparallel.each { key, attrs ->
        def taskNames = attrs?.'task'
        if (taskNames) {
          def category = removeDuplicateMarker(key)
          def resource = workspace.project('cnf').layout.buildDirectory.dir("noparallel/${category}")
          taskNames.trim().tokenize(',').each { taskName ->
            tasks.named(taskName.trim()) { t ->
              t.outputs.dir resource
            }
          }
        }
      }
    }
  }

  private ConfigurableFileCollection pathFiles(Collection<Container> path) {
    return objects.fileCollection().from(path*.getFile())
      .builtBy(path.findAll { Container c ->
        c.getType() == TYPE.PROJECT
      }.collect { Container c ->
        workspace.absoluteProjectPath("${c.getProject().getName()}:jar")
      })
  }

  private Closure getBuildDependencies(String taskName) {
    return {
      bndProject.getBuildDependencies().collect { dependency ->
        workspace.project(dependency.getName()).tasks.named(taskName)
      }
    }
  }

  private Closure getTestDependencies(String taskName) {
    return {
      bndProject.getTestDependencies().collect { dependency ->
        workspace.project(dependency.getName()).tasks.named(taskName)
      }
    }
  }

  private Closure getDependents(String taskName) {
    return {
      bndProject.getDependents().collect { dependent ->
        workspace.project(dependent.getName()).tasks.named(taskName)
      }
    }
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
