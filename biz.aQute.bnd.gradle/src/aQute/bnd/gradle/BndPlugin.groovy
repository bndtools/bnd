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

import static aQute.bnd.gradle.BndUtils.logReport
import static aQute.bnd.osgi.Processor.isTrue

import aQute.bnd.build.Container
import aQute.bnd.build.Container.TYPE
import aQute.bnd.build.Run
import aQute.bnd.build.Workspace
import aQute.bnd.osgi.Constants
import biz.aQute.resolve.Bndrun
import biz.aQute.resolve.ResolveProcess

import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.compile.JavaCompile

import org.osgi.service.resolver.ResolutionException

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
      if (!rootProject.hasProperty('bndWorkspace')) {
        rootProject.ext.bndWorkspace = new Workspace(rootDir).setOffline(rootProject.gradle.startParameter.offline)
      }
      this.bndProject = bndWorkspace.getProject(name)
      if (bndProject == null) {
        throw new GradleException("Unable to load bnd project ${name} from workspace ${rootDir}")
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
          java.srcDirs = resources.srcDirs = files(bndProject.getSourcePath())
          java.outputDir = output.resourcesDir = bndProject.getSrcOutput()
        }
        test {
          java.srcDirs = resources.srcDirs = files(bndProject.getTestSrc())
          java.outputDir = output.resourcesDir = bndProject.getTestOutput()
        }
      }
      /* Configure srcDirs for any additional languages */
      afterEvaluate {
        sourceSets {
          main.convention?.plugins.each { lang, object ->
            main[lang]?.srcDirs = main.java.srcDirs
            main[lang]?.outputDir = main.java.outputDir
            test[lang]?.srcDirs = test.java.srcDirs
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
            if ((sourceCompatibility == targetCompatibility) && !bootClasspath && javacProfile.empty) {
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
            if (options.bootClasspath != null) {
              logger.info '-bootclasspath {}', options.bootClasspath
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
        deleteAllActions() /* Replace the standard task actions */
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

      task('testOSGi') {
        description 'Runs the OSGi JUnit tests by launching a framework and running the tests in the launched framework.'
        group 'verification'
        enabled !bndis(Constants.NOJUNITOSGI) && !bndUnprocessed(Constants.TESTCASES, '').empty
        ext.ignoreFailures = false
        inputs.files jar
        outputs.dir({ new File(testResultsDir, name) }).withPropertyName('testResults')
        doLast {
          try {
            bndProject.test(new File(testResultsDir, name), null)
          } catch (Exception e) {
            throw new GradleException("Project ${bndProject.getName()} failed to test", e)
          }
          checkErrors(logger, ignoreFailures)
        }
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
        deleteAllActions() /* Replace the standard task actions */
        doLast {
          bndProject.clean()
        }
      }

      task('cleanNeeded') {
        description 'Cleans the project and all projects it depends on.'
        dependsOn testDependencies(name), clean
        group 'build'
      }

      tasks.addRule('Pattern: export.<name>: Export the <name>.bndrun file to an executable jar.') { taskName ->
        if (taskName.startsWith('export.')) {
          String bndrun = taskName - 'export.'
          File runFile = file("${bndrun}.bndrun")
          if (runFile.isFile()) {
            task(taskName) {
              description "Export the ${bndrun}.bndrun file to an executable jar."
              dependsOn assemble
              group 'export'
              ext.destinationDir = new File(distsDir, 'executable')
              outputs.file({ new File(destinationDir, "${bndrun}.jar") }).withPropertyName('bndrunJar')
              doFirst {
                project.mkdir(destinationDir)
              }
              doLast {
                File executableJar = new File(destinationDir, "${bndrun}.jar")
                Run.createRun(bndProject.getWorkspace(), runFile).withCloseable { run ->
                  logger.info 'Exporting {} to {}', run.getPropertiesFile(), executableJar.absolutePath
                  if (run.isStandalone()) {
                    run.getWorkspace().setOffline(bndProject.getWorkspace().isOffline())
                  }
                  try {
                    run.export(null, false, executableJar)
                  } catch (Exception e) {
                    throw new GradleException("Export of ${run.getPropertiesFile()} to an executable jar failed", e)
                  }
                  checkErrors(logger)
                }
              }
            }
          }
        }
      }

      task('export') {
        description 'Export all the bndrun files to runnable jars.'
        group 'export'
        fileTree(projectDir) {
            include '*.bndrun'
        }.each {
          dependsOn tasks.getByName("export.${it.name - '.bndrun'}")
        }
      }

      tasks.addRule('Pattern: runbundles.<name>: Create a distribution of the runbundles in <name>.bndrun file.') { taskName ->
        if (taskName.startsWith('runbundles.')) {
          String bndrun = taskName - 'runbundles.'
          File runFile = file("${bndrun}.bndrun")
          if (runFile.isFile()) {
            task(taskName) {
              description "Create a distribution of the runbundles in the ${bndrun}.bndrun file."
              dependsOn assemble
              group 'export'
              ext.destinationDir = new File(distsDir, "runbundles/${bndrun}")
              outputs.dir({ destinationDir }).withPropertyName('destinationDir')
              doFirst {
                project.delete(destinationDir)
                project.mkdir(destinationDir)
              }
              doLast {
                Run.createRun(bndProject.getWorkspace(), runFile).withCloseable { run ->
                  logger.info 'Creating a distribution of the runbundles from {} in directory {}', run.getPropertiesFile(), destinationDir.absolutePath
                  if (run.isStandalone()) {
                    run.getWorkspace().setOffline(bndProject.getWorkspace().isOffline())
                  }
                  try {
                      run.exportRunbundles(null, destinationDir)
                  } catch (Exception e) {
                    throw new GradleException("Creating a distribution of the runbundles in ${run.getPropertiesFile()} failed", e)
                  }
                  checkErrors(logger)
                }
              }
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
          String bndrun = taskName - 'resolve.'
          File runFile = file("${bndrun}.bndrun")
          if (runFile.isFile()) {
            task(taskName) {
              description "Resolve the runbundles required for ${bndrun}.bndrun file."
              dependsOn assemble
              group 'export'
              ext.failOnChanges = false
              outputs.file(runFile).withPropertyName('runFile')
              doLast {
                Bndrun.createBndrun(bndProject.getWorkspace(), runFile).withCloseable { run ->
                  logger.info 'Resolving runbundles required for {}', run.getPropertiesFile()
                  if (run.isStandalone()) {
                    run.getWorkspace().setOffline(bndProject.getWorkspace().isOffline())
                  }
                  try {
                    def result = run.resolve(failOnChanges, true)
                    logger.info '{}: {}', Constants.RUNBUNDLES, result
                  } catch (ResolutionException e) {
                    logger.error 'Unresolved requirements: {}', ResolveProcess.format(e.getUnresolvedRequirements())
                    throw new GradleException("${run.getPropertiesFile()} resolution failure", e)
                  } finally {
                    checkProjectErrors(run, logger)
                  }
                }
              }
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
          String bndrun = taskName - 'run.'
          File runFile = file("${bndrun}.bndrun")
          if (runFile.isFile()) {
            task(taskName) {
              description "Run the bndrun file ${bndrun}.bndrun."
              dependsOn assemble
              group 'export'
              doLast {
                Bndrun.createBndrun(bndProject.getWorkspace(), runFile).withCloseable { run ->
                  run.setBase(temporaryDir)
                  logger.lifecycle 'Running {} with vm args: {}', run.getPropertiesFile(), run.mergeProperties(Constants.RUNVM)
                  if (run.isStandalone()) {
                    run.getWorkspace().setOffline(bndProject.getWorkspace().isOffline())
                  }
                  run.run()
                  checkProjectErrors(run, logger)
                }
              }
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
Project ${project.name}
------------------------------------------------------------

project.workspace:      ${rootDir}
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
project.bootclasspath:  ${compileJava.options.bootClasspath}
project.deliverables:   ${configurations.archives.artifacts.files*.path}
javac:                  ${compileJava.options.forkOptions.executable}
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
        ":${c.getProject().getName()}:jar"
      }
    }
  }

  private Closure buildDependencies(String taskName, Closure transformer = { it }) {
    return {
      bndProject.getBuildDependencies()*.getName().collect { String dependency ->
        transformer(":${dependency}:${taskName}")
      }
    }
  }

  private Closure testDependencies(String taskName, Closure transformer = { it }) {
    return {
      bndProject.getTestDependencies()*.getName().collect { String dependency ->
        transformer(":${dependency}:${taskName}")
      }
    }
  }

  private Closure dependents(String taskName, Closure transformer = { it }) {
    return {
      bndProject.getDependents()*.getName().collect { String dependency ->
        transformer(":${dependency}:${taskName}")
      }
    }
  }

  private void checkErrors(Logger logger, boolean ignoreFailures = false) {
    checkProjectErrors(bndProject, logger, ignoreFailures)
  }

  private void checkProjectErrors(aQute.bnd.build.Project project, Logger logger, boolean ignoreFailures = false) {
    project.getInfo(project.getWorkspace(), "${project.getWorkspace().getBase().name} :")
    boolean failed = !ignoreFailures && !project.isOk()
    int errorCount = project.getErrors().size()
    logReport(project, logger)
    project.getWarnings().clear()
    project.getErrors().clear()
    if (failed) {
      String str = ' even though no errors were reported'
      if (errorCount == 1) {
        str = ', one error was reported'
      } else if (errorCount > 1) {
        str = ", ${errorCount} errors were reported"
      }
      throw new GradleException("${project.getName()} has errors${str}")
    }
  }
}
