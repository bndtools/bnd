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
import static aQute.bnd.osgi.Processor.isTrue

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
             builtBy jar
          }
          archives(deliverable) {
             builtBy jar
          }
        }
      }
      /* Set up source sets */
      sourceSets {
        /* bnd uses the same directory for java and resources. */
        main {
          FileCollection srcDirs = files(bndProject.getSourcePath())
          java.srcDirs = srcDirs
          resources.srcDirs = srcDirs
          File destinationDir = bndProject.getSrcOutput()
          Task compileTask = tasks.getByName(compileJavaTaskName)
          java.outputDir = destinationDir
          output.resourcesDir = destinationDir
          compileTask.destinationDir = destinationDir
          output.dir(destinationDir, builtBy: compileTask)
        }
        test {
          FileCollection srcDirs = files(bndProject.getTestSrc())
          java.srcDirs = srcDirs
          resources.srcDirs = srcDirs
          File destinationDir = bndProject.getTestOutput()
          Task compileTask = tasks.getByName(compileJavaTaskName)
          java.outputDir = destinationDir
          output.resourcesDir = destinationDir
          compileTask.destinationDir = destinationDir
          output.dir(destinationDir, builtBy: compileTask)
        }
      }
      /* Configure srcDirs for any additional languages */
      afterEvaluate {
        sourceSets {
          main {
            File destinationDir = tasks.getByName(compileJavaTaskName).destinationDir
            convention.plugins.each { lang, sourceSet ->
              Task compileTask = tasks.findByName(getCompileTaskName(lang))
              if (compileTask) {
                sourceSet[lang].srcDirs = java.srcDirs
                sourceSet[lang].outputDir = destinationDir
                compileTask.destinationDir = destinationDir
                output.dir(destinationDir, builtBy: compileTask)
              }
            }
          }
          test {
            File destinationDir = tasks.getByName(compileJavaTaskName).destinationDir
            convention.plugins.each { lang, sourceSet ->
              Task compileTask = tasks.findByName(getCompileTaskName(lang))
              if (compileTask) {
                sourceSet[lang].srcDirs = java.srcDirs
                sourceSet[lang].outputDir = destinationDir
                compileTask.destinationDir = destinationDir
                output.dir(destinationDir, builtBy: compileTask)
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
      tasks.withType(JavaCompile) {
        configure(options) {
          if (javacDebug) {
            debugOptions.debugLevel = 'source,lines,vars'
          }
          verbose = logger.isDebugEnabled()
          listFiles = logger.isInfoEnabled()
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
            if ((sourceCompatibility == targetCompatibility) && javacBootclasspath.empty && javacProfile.empty) {
              compilerArgs.addAll(['--release', JavaVersion.toVersion(sourceCompatibility).majorVersion])
            }
          }
        }
        if (logger.isInfoEnabled()) {
          doFirst {
            logger.info 'Compile to {}', destinationDir
            if (options.compilerArgs.contains('--release')) {
              logger.info '{}', options.compilerArgs.join(' ')
            } else {
              logger.info '-source {} -target {} {}', sourceCompatibility, targetCompatibility, options.compilerArgs.join(' ')
            }
            logger.info '-classpath {}', classpath.asPath
            if (options.hasProperty('bootstrapClasspath')) { // gradle 4.3
              if (options.bootstrapClasspath != null) {
                logger.info '-bootclasspath {}', options.bootstrapClasspath.asPath
              }
            } else {
              if (options.bootClasspath != null) {
                logger.info '-bootclasspath {}', options.bootClasspath
              }
            }
          }
        }
        doFirst {
            checkErrors(logger)
        }
      }

      processResources {
        outputs.files({
          FileCollection sourceDirectories = sourceSets.main.resources.sourceDirectories
          source*.absolutePath.collect { String file ->
            sourceDirectories.each {
              file -= it
            }
            new File(destinationDir, file)
          }
        }).withPropertyName('resources')
      }

      processTestResources {
        outputs.files({
          FileCollection sourceDirectories = sourceSets.test.resources.sourceDirectories
          source*.absolutePath.collect { String file ->
            sourceDirectories.each {
              file -= it
            }
            new File(destinationDir, file)
          }
        }).withPropertyName('testResources')
      }

      jar {
        description 'Assemble the project bundles.'
        actions.clear() /* Replace the standard task actions */
        enabled !bndProject.isNoBundles()
        configurations.archives.artifacts.files.find {
          archiveName = it.name /* use first artifact as archiveName */
        }
        ext.projectDirInputsExcludes = [] /* Additional excludes for projectDir inputs */
        /* all other files in the project like bnd and resources */
        inputs.files({
          fileTree(projectDir) { tree ->
            sourceSets.each { sourceSet -> /* exclude sourceSet dirs */
              tree.exclude sourceSet.allSource.sourceDirectories.collect {
                project.relativePath(it)
              }
              tree.exclude sourceSet.output.collect {
                project.relativePath(it)
              }
            }
            tree.exclude project.relativePath(buildDir) /* exclude buildDir */
            tree.exclude projectDirInputsExcludes /* user specified excludes */
          }
        }).withPropertyName('projectFolder')
        /* bnd can include from -buildpath */
        inputs.files({
          compileJava.classpath
        }).withPropertyName('buildpath')
        /* bnd can include from -dependson */
        inputs.files(buildDependencies(name, { tasks.getByPath(it) })).withPropertyName('buildDependencies')
        /* Workspace and project configuration changes should trigger jar task */
        inputs.files(bndProject.getWorkspace().getPropertiesFile(),
          bndProject.getWorkspace().getIncluded(),
          bndProject.getPropertiesFile(),
          bndProject.getIncluded()).withPropertyName('bndFiles')
        outputs.files({ configurations.archives.artifacts.files }).withPropertyName('artifacts')
        outputs.file(new File(buildDir, Constants.BUILDFILES)).withPropertyName('buildfiles')
        doLast {
          File[] built
          try {
            built = bndProject.build()
          } catch (Exception e) {
            throw new GradleException("Project ${bndProject.getName()} failed to build", e)
          }
          checkErrors(logger)
          if (built != null) {
            logger.info 'Generated bundles: {}', built as Object
          }
        }
      }

      buildNeeded {
        dependsOn testDependencies(name)
      }

      buildDependents {
        dependsOn dependents(name)
      }

      task('release') {
        description 'Release the project to the release repository.'
        group 'release'
        enabled !bndProject.isNoBundles() && !bnd(Constants.RELEASEREPO, 'unset').empty
        inputs.files jar
        doLast {
          try {
            bndProject.release()
          } catch (Exception e) {
            throw new GradleException("Project ${bndProject.getName()} failed to release", e)
          }
          checkErrors(logger)
        }
      }

      task('releaseNeeded') {
        description 'Release the project and all projects it depends on.'
        dependsOn buildDependencies(name), release
        group 'release'
      }

      test {
        enabled !bndis(Constants.NOJUNIT) && !bndis('no.junit')
        doFirst {
          checkErrors(logger, ignoreFailures)
        }
      }

      task('testOSGi', type: TestOSGi) {
        description 'Runs the OSGi JUnit tests by launching a framework and running the tests in the launched framework.'
        group 'verification'
        enabled !bndis(Constants.NOJUNITOSGI) && !bndUnprocessed(Constants.TESTCASES, '').empty
        inputs.files jar
        bndrun = bndProject.getPropertiesFile()
      }

      check {
        dependsOn testOSGi
      }

      task('checkNeeded') {
        description 'Runs all checks on the project and all projects it depends on.'
        dependsOn testDependencies(name), check
        group 'verification'
      }

      clean {
        description 'Cleans the build and compiler output directories of the project.'
        delete buildDir, sourceSets.main.output, sourceSets.test.output
      }

      task('cleanNeeded') {
        description 'Cleans the project and all projects it depends on.'
        dependsOn testDependencies(name), clean
        group 'build'
      }

      tasks.addRule('Pattern: export.<name>: Export the <name>.bndrun file.') { taskName ->
        if (taskName.startsWith('export.')) {
          String bndrunName = taskName - 'export.'
          File runFile = file("${bndrunName}.bndrun")
          if (runFile.isFile()) {
            task(taskName, type: Export) {
              description "Export the ${bndrunName}.bndrun file."
              dependsOn assemble
              group 'export'
              bndrun = runFile
              exporter = EXECUTABLE_JAR
            }
          }
        }
      }

      task('export') {
        description 'Export all the bndrun files.'
        group 'export'
        fileTree(projectDir) {
            include '*.bndrun'
        }.each {
          dependsOn tasks.getByName("export.${it.name - '.bndrun'}")
        }
      }

      tasks.addRule('Pattern: runbundles.<name>: Create a distribution of the runbundles in <name>.bndrun file.') { taskName ->
        if (taskName.startsWith('runbundles.')) {
          String bndrunName = taskName - 'runbundles.'
          File runFile = file("${bndrunName}.bndrun")
          if (runFile.isFile()) {
            task(taskName, type: Export) {
              description "Create a distribution of the runbundles in the ${bndrunName}.bndrun file."
              dependsOn assemble
              group 'export'
              bndrun = runFile
              exporter = RUNBUNDLES
            }
          }
        }
      }

      task('runbundles') {
        description 'Create a distribution of the runbundles in each of the bndrun files.'
        group 'export'
        fileTree(projectDir) {
            include '*.bndrun'
        }.each {
          dependsOn tasks.getByName("runbundles.${it.name - '.bndrun'}")
        }
      }

      tasks.addRule('Pattern: resolve.<name>: Resolve the required runbundles in the <name>.bndrun file.') { taskName ->
        if (taskName.startsWith('resolve.')) {
          String bndrunName = taskName - 'resolve.'
          File runFile = file("${bndrunName}.bndrun")
          if (runFile.isFile()) {
            task(taskName, type: Resolve) {
              description "Resolve the runbundles required for ${bndrunName}.bndrun file."
              dependsOn assemble
              group 'export'
              bndrun = runFile
            }
          }
        }
      }

      task('resolve') {
        description 'Resolve the runbundles required for each of the bndrun files.'
        group 'export'
        fileTree(projectDir) {
            include '*.bndrun'
        }.each {
          dependsOn tasks.getByName("resolve.${it.name - '.bndrun'}")
        }
      }

      tasks.addRule('Pattern: run.<name>: Run the bndrun file <name>.bndrun.') { taskName ->
        if (taskName.startsWith('run.')) {
          String bndrunName = taskName - 'run.'
          File runFile = file("${bndrunName}.bndrun")
          if (runFile.isFile()) {
            task(taskName, type: Bndrun) {
              description "Run the bndrun file ${bndrunName}.bndrun."
              dependsOn assemble
              group 'export'
              bndrun = runFile
            }
          }
        }
      }

      tasks.addRule('Pattern: testrun.<name>: Runs the OSGi JUnit tests in the bndrun file <name>.bndrun.') { taskName ->
        if (taskName.startsWith('testrun.')) {
          String bndrunName = taskName - 'testrun.'
          File runFile = file("${bndrunName}.bndrun")
          if (runFile.isFile()) {
            task(taskName, type: TestOSGi) {
              description "Runs the OSGi JUnit tests in the bndrun file ${bndrunName}.bndrun."
              dependsOn assemble
              group 'verification'
              bndrun = runFile
            }
          }
        }
      }

      task('echo') {
        description 'Displays the bnd project information.'
        group 'help'
        doLast {
          println """
------------------------------------------------------------
Project ${project.name} // Bnd version ${About.CURRENT}
------------------------------------------------------------

project.workspace:      ${parent.projectDir}
project.name:           ${project.name}
project.dir:            ${projectDir}
target:                 ${buildDir}
project.dependson:      ${bndProject.getDependson()*.getName()}
project.sourcepath:     ${sourceSets.main.java.sourceDirectories.asPath}
project.output:         ${compileJava.destinationDir}
project.buildpath:      ${compileJava.classpath.asPath}
project.allsourcepath:  ${bnd.allSrcDirs.asPath}
project.testsrc:        ${sourceSets.test.java.sourceDirectories.asPath}
project.testoutput:     ${compileTestJava.destinationDir}
project.testpath:       ${compileTestJava.classpath.asPath}
project.bootclasspath:  ${compileJava.options.hasProperty('bootstrapClasspath')?compileJava.options.bootstrapClasspath?.asPath?:'':compileJava.options.bootClasspath?:''}
project.deliverables:   ${configurations.archives.artifacts.files*.path}
javac:                  ${compileJava.options.forkOptions.executable?:'javac'}
javac.source:           ${compileJava.sourceCompatibility}
javac.target:           ${compileJava.targetCompatibility}
javac.profile:          ${javacProfile}
"""
          checkErrors(logger, true)
        }
      }

      task('bndproperties') {
        description 'Displays the bnd properties.'
        group 'help'
        doLast {
          println """
------------------------------------------------------------
Project ${project.name}
------------------------------------------------------------
"""
          bndProject.getPropertyKeys(true).sort().each {
            println "${it}: ${bnd(it, '')}"
          }
          println()
          checkErrors(logger, true)
        }
      }
    }
  }

  private FileCollection pathFiles(Collection<Container> path) {
    return project.files(path*.getFile()) {
      builtBy path.findAll { Container c ->
        c.getType() == TYPE.PROJECT
      }.collect { Container c ->
        "${project.parent.absoluteProjectPath(c.getProject().getName())}:jar"
      }
    }
  }

  private Closure buildDependencies(String taskName, Closure transformer = { it }) {
    return {
      bndProject.getBuildDependencies()*.getName().collect { String dependency ->
        transformer("${project.parent.absoluteProjectPath(dependency)}:${taskName}")
      }
    }
  }

  private Closure testDependencies(String taskName, Closure transformer = { it }) {
    return {
      bndProject.getTestDependencies()*.getName().collect { String dependency ->
        transformer("${project.parent.absoluteProjectPath(dependency)}:${taskName}")
      }
    }
  }

  private Closure dependents(String taskName, Closure transformer = { it }) {
    return {
      bndProject.getDependents()*.getName().collect { String dependency ->
        transformer("${project.parent.absoluteProjectPath(dependency)}:${taskName}")
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
