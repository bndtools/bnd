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
import static aQute.bnd.gradle.BndUtils.logReport
import static aQute.bnd.gradle.BndUtils.configureEachTask
import static aQute.bnd.gradle.BndUtils.configureTask
import static aQute.bnd.gradle.BndUtils.createTask
import static aQute.bnd.gradle.BndUtils.namedTask
import static aQute.bnd.osgi.Processor.isTrue

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
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.compile.JavaCompile

public class BndPlugin implements Plugin<Project> {
  public static final String PLUGINID = 'biz.aQute.bnd'
  private Project project
  private aQute.bnd.build.Project bndProject

  /**
   * Apply the {@code biz.aQute.bnd} plugin to the specified project.
   */
  @Override
  public void apply(Project p) {
    p.configure(p) { Project project ->
      this.project = project
      if (plugins.hasPlugin(BndBuilderPlugin.PLUGINID)) {
          throw new GradleException("Project already has '${BndBuilderPlugin.PLUGINID}' plugin applied.")
      }
      if (parent == null) {
        throw new GradleException("The '${PLUGINID}' plugin cannot be applied to the root project. Perhaps you meant to use the '${BndBuilderPlugin.PLUGINID}' plugin?")
      }
      if (!parent.hasProperty('bndWorkspace')) {
        parent.ext.bndWorkspace = new Workspace(parent.projectDir).setOffline(gradle.startParameter.offline)
      }
      this.bndProject = bndWorkspace.getProject(name)
      if (bndProject == null) {
        throw new GradleException("Unable to load bnd project ${name} from workspace ${parent.projectDir}")
      }
      bndProject.prepare()
      if (!bndProject.isValid()) {
        checkErrors(logger)
        throw new GradleException("Project ${bndProject.getName()} is not a valid bnd project")
      }
      extensions.create('bnd', BndProperties, bndProject)
      convention.plugins.bnd = new BndPluginConvention(this)

      buildDir = relativePath(bndProject.getTargetDir())
      plugins.apply 'java'
      libsDirName = '.'
      testResultsDirName = bnd('test-reports', 'test-reports')

      if (project.hasProperty('bnd_defaultTask')) {
        defaultTasks = bnd_defaultTask.trim().split(/\s*,\s*/)
      }

      /* Set up configurations */
      configurations {
        runtime.artifacts.clear()
        archives.artifacts.clear()
      }
      /* Set up deliverables */
      bndProject.getDeliverables()*.getFile().each { File deliverable ->
        artifacts {
          runtime(deliverable) {
             builtBy 'jar'
          }
          archives(deliverable) {
             builtBy 'jar'
          }
        }
      }
      /* Set up source sets */
      sourceSets {
        /* bnd uses the same directory for java and resources. */
        main {
          FileCollection srcDirs = files(bndProject.getSourcePath())
          File destinationDir = bndProject.getSrcOutput()
          java.srcDirs = srcDirs
          resources.srcDirs = srcDirs
          java.outputDir = destinationDir
          output.resourcesDir = destinationDir
          configureTask(project, compileJavaTaskName) { t ->
            t.destinationDir = destinationDir
          }
          output.dir(destinationDir, builtBy: compileJavaTaskName)
        }
        test {
          FileCollection srcDirs = files(bndProject.getTestSrc())
          File destinationDir = bndProject.getTestOutput()
          java.srcDirs = srcDirs
          resources.srcDirs = srcDirs
          java.outputDir = destinationDir
          output.resourcesDir = destinationDir
          configureTask(project, compileJavaTaskName) { t ->
            t.destinationDir = destinationDir
          }
          output.dir(destinationDir, builtBy: compileJavaTaskName)
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
                try {
                  configureTask(project, compileTaskName) { t ->
                    t.destinationDir = destinationDir
                  }
                  sourceDirSet.srcDirs = java.srcDirs
                  sourceDirSet.outputDir = destinationDir
                  output.dir(destinationDir, builtBy: compileTaskName)
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
                try {
                  configureTask(project, compileTaskName) { t ->
                    t.destinationDir = destinationDir
                  }
                  sourceDirSet.srcDirs = java.srcDirs
                  sourceDirSet.outputDir = destinationDir
                  output.dir(destinationDir, builtBy: compileTaskName)
                } catch (UnknownDomainObjectException e) {
                 // no such task
                }
              }
            }
          }
        }
      }

      bnd.ext.allSrcDirs = files(bndProject.getAllsourcepath())
      /* Set up dependencies */
      dependencies {
        compile pathFiles(bndProject.getBuildpath())
        runtime files(bndProject.getSrcOutput())
        testCompile pathFiles(bndProject.getTestpath())
        testRuntime files(bndProject.getTestOutput())
      }
      /* Set up compile tasks */
      sourceCompatibility = bnd('javac.source', sourceCompatibility)
      String javacTarget = bnd('javac.target', targetCompatibility)
      FileCollection javacBootclasspath = files(bndProject.getBootclasspath()*.getFile())
      if (javacTarget == 'jsr14') {
        javacTarget = '1.5'
        javacBootclasspath = files(bndProject.getBundle('ee.j2se', '1.5', null, ['strategy':'lowest']).getFile())
      }
      targetCompatibility = javacTarget
      String javac = bnd('javac')
      String javacProfile = bnd('javac.profile', '')
      boolean javacDebug = bndis('javac.debug')
      boolean javacDeprecation = isTrue(bnd('javac.deprecation', 'true'))
      String javacEncoding = bnd('javac.encoding', 'UTF-8')
      configureEachTask(project, JavaCompile.class) { t ->
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
            if (delegate.hasProperty('bootstrapClasspath')) { // gradle 4.3
              bootstrapClasspath = javacBootclasspath
            } else {
              bootClasspath = javacBootclasspath.asPath
            }
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
            if (t.options.hasProperty('bootstrapClasspath')) { // gradle 4.3
              if (t.options.bootstrapClasspath != null) {
                t.logger.info '-bootclasspath {}', t.options.bootstrapClasspath.asPath
              }
            } else {
              if (t.options.bootClasspath != null) {
                t.logger.info '-bootclasspath {}', t.options.bootClasspath
              }
            }
          }
        }
      }

      def jar = configureTask(project, 'jar') { t ->
        t.description 'Jar this project\'s bundles.'
        t.actions.clear() /* Replace the standard task actions */
        t.enabled !bndProject.isNoBundles()
        project.configurations.archives.artifacts.files.find {
          t.archiveName = it.name /* use first artifact as archiveName */
        }
        /* Additional excludes for projectDir inputs */
        t.ext.projectDirInputsExcludes = Strings.split(bndMerge(Constants.BUILDERIGNORE)).collect { it.concat('/') }
        /* all other files in the project like bnd and resources */
        t.inputs.files({
          project.fileTree(projectDir) { tree ->
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
        }).withPropertyName('projectFolder')
        /* bnd can include from -buildpath */
        t.inputs.files(project.sourceSets.main.compileClasspath).withPropertyName('buildpath')
        /* bnd can include from -dependson */
        t.inputs.files(buildDependencies('jar')).withPropertyName('buildDependencies')
        /* Workspace and project configuration changes should trigger jar task */
        t.inputs.files(bndProject.getWorkspace().getPropertiesFile(),
          bndProject.getWorkspace().getIncluded(),
          bndProject.getPropertiesFile(),
          bndProject.getIncluded()).withPropertyName('bndFiles')
        t.outputs.files({ project.configurations.archives.artifacts.files }).withPropertyName('artifacts')
        t.outputs.file(new File(project.buildDir, Constants.BUILDFILES)).withPropertyName('buildfiles')
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

      createTask(project, 'jarDependencies') { t ->
        t.description 'Jar all projects this project depends on.'
        t.dependsOn buildDependencies('jar')
        t.group 'build'
      }

      createTask(project, 'buildDependencies') { t ->
        t.description 'Assembles and tests all projects this project depends on.'
        t.dependsOn testDependencies('buildNeeded')
        t.group 'build'
      }

      configureTask(project, 'buildNeeded') { t ->
        t.dependsOn 'buildDependencies'
      }

      configureTask(project, 'buildDependents') { t ->
        t.dependsOn dependents('buildDependents')
      }

      createTask(project, 'release') { t ->
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

      createTask(project, 'releaseDependencies') { t ->
        t.description 'Release all projects this project depends on.'
        t.dependsOn buildDependencies('releaseNeeded')
        t.group 'release'
      }

      createTask(project, 'releaseNeeded') { t ->
        t.description 'Release this project and all projects it depends on.'
        t.dependsOn 'releaseDependencies', 'release'
        t.group 'release'
      }

      configureTask(project, 'test') { t ->
        t.enabled !bndis(Constants.NOJUNIT) && !bndis('no.junit')
        t.doFirst {
          checkErrors(t.logger, t.ignoreFailures)
        }
      }

      createTask(project, 'testOSGi', TestOSGi.class) { t ->
        t.description 'Runs the OSGi JUnit tests by launching a framework and running the tests in the launched framework.'
        t.group 'verification'
        t.enabled !bndis(Constants.NOJUNITOSGI) && !bndUnprocessed(Constants.TESTCASES, '').empty
        t.inputs.files jar
        t.bndrun = bndProject.getPropertiesFile()
      }

      configureTask(project, 'check') { t ->
        t.dependsOn 'testOSGi'
      }

      createTask(project, 'checkDependencies') { t ->
        t.description 'Runs all checks on all projects this project depends on.'
        t.dependsOn testDependencies('checkNeeded')
        t.group 'verification'
      }

      createTask(project, 'checkNeeded') { t ->
        t.description 'Runs all checks on this project and all projects it depends on.'
        t.dependsOn 'checkDependencies', 'check'
        t.group 'verification'
      }

      configureTask(project, 'clean') { t ->
        t.description 'Cleans the build and compiler output directories of this project.'
        t.delete project.buildDir, project.sourceSets.main.output, project.sourceSets.test.output
      }

      createTask(project, 'cleanDependencies') { t ->
        t.description 'Cleans all projects this project depends on.'
        t.dependsOn testDependencies('cleanNeeded')
        t.group 'build'
      }

      createTask(project, 'cleanNeeded') { t ->
        t.description 'Cleans this project and all projects it depends on.'
        t.dependsOn 'cleanDependencies', 'clean'
        t.group 'build'
      }

      def bndruns = fileTree(projectDir) {
          include '*.bndrun'
      }

      createTask(project, 'export') { t ->
        t.description 'Export all the bndrun files.'
        t.group 'export'
      }

      bndruns.forEach { runFile ->
        def subtask = createTask(project, "export.${runFile.name - '.bndrun'}", Export.class) { t ->
          t.description "Export the ${runFile.name} file."
          t.dependsOn 'assemble'
          t.group 'export'
          t.bndrun = runFile
          t.exporter = EXECUTABLE_JAR
        }
        configureTask(project, 'export') { t ->
          t.dependsOn subtask
        }
      }

      createTask(project, 'runbundles') { t ->
        t.description 'Create a distribution of the runbundles in each of the bndrun files.'
        t.group 'export'
      }

      bndruns.forEach { runFile ->
        def subtask = createTask(project, "runbundles.${runFile.name - '.bndrun'}", Export.class) { t ->
          t.description "Create a distribution of the runbundles in the ${runFile.name} file."
          t.dependsOn 'assemble'
          t.group 'export'
          t.bndrun = runFile
          t.exporter = RUNBUNDLES
        }
        configureTask(project, 'runbundles') { t ->
          t.dependsOn subtask
        }
      }

      createTask(project, 'resolve') { t ->
        t.description 'Resolve the runbundles required for each of the bndrun files.'
        t.group 'export'
      }

      bndruns.forEach { runFile ->
        def subtask = createTask(project, "resolve.${runFile.name - '.bndrun'}", Resolve.class) { t ->
          t.description "Resolve the runbundles required for ${runFile.name} file."
          t.dependsOn 'assemble'
          t.group 'export'
          t.bndrun = runFile
        }
        configureTask(project, 'resolve') { t ->
          t.dependsOn subtask
        }
      }

      bndruns.forEach { runFile ->
        createTask(project, "run.${runFile.name - '.bndrun'}", Bndrun.class) { t ->
          t.description "Run the bndrun file ${runFile.name}."
          t.dependsOn 'assemble'
          t.group 'export'
          t.bndrun = runFile
        }
      }

      bndruns.forEach { runFile ->
        createTask(project, "testrun.${runFile.name - '.bndrun'}", TestOSGi.class) { t ->
          t.description "Runs the OSGi JUnit tests in the bndrun file ${runFile.name}."
          t.dependsOn 'assemble'
          t.group 'verification'
          t.bndrun = runFile
        }
      }

      createTask(project, 'echo') { t ->
        t.description 'Displays the bnd project information.'
        t.group 'help'
        def compileJava = project.tasks.getByName('compileJava')
        def compileTestJava = project.tasks.getByName('compileTestJava')
        t.doLast {
          println """
------------------------------------------------------------
Project ${project.name} // Bnd version ${About.CURRENT}
------------------------------------------------------------

project.workspace:      ${project.parent.projectDir}
project.name:           ${project.name}
project.dir:            ${project.projectDir}
target:                 ${project.buildDir}
project.dependson:      ${bndProject.getDependson()*.getName()}
project.sourcepath:     ${project.sourceSets.main.java.sourceDirectories.asPath}
project.output:         ${compileJava.destinationDir}
project.buildpath:      ${compileJava.classpath.asPath}
project.allsourcepath:  ${bnd.allSrcDirs.asPath}
project.testsrc:        ${project.sourceSets.test.java.sourceDirectories.asPath}
project.testoutput:     ${compileTestJava.destinationDir}
project.testpath:       ${compileTestJava.classpath.asPath}
project.bootclasspath:  ${compileJava.options.hasProperty('bootstrapClasspath')?compileJava.options.bootstrapClasspath?.asPath?:'':compileJava.options.bootClasspath?:''}
project.deliverables:   ${project.configurations.archives.artifacts.files*.path}
javac:                  ${compileJava.options.forkOptions.executable?:'javac'}
javac.source:           ${project.sourceCompatibility}
javac.target:           ${project.targetCompatibility}
javac.profile:          ${javacProfile}
"""
          checkErrors(t.logger, true)
        }
      }

      createTask(project, 'bndproperties') { t ->
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
    }
  }

  private FileCollection pathFiles(Collection<Container> path) {
    return project.files(path*.getFile()) {
      builtBy path.findAll { Container c ->
        c.getType() == TYPE.PROJECT
      }.collect { Container c ->
        project.parent.absoluteProjectPath("${c.getProject().getName()}:jar")
      }
    }
  }

  private Closure buildDependencies(String taskName) {
    return {
      bndProject.getBuildDependencies().collect { dependency ->
        namedTask(project.parent.project(dependency.getName()), taskName)
      }
    }
  }

  private Closure testDependencies(String taskName) {
    return {
      bndProject.getTestDependencies().collect { dependency ->
        namedTask(project.parent.project(dependency.getName()), taskName)
      }
    }
  }

  private Closure dependents(String taskName) {
    return {
      bndProject.getDependents().collect { dependent ->
        namedTask(project.parent.project(dependent.getName()), taskName)
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
    p.getWarnings().clear()
    p.getErrors().clear()
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
