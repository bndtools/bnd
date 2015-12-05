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
 *
 * <p>
 * If the bnd_preCompileRefresh property is set to 'true', the project
 * properties will be refreshed just before compiling the project.
 */

package aQute.bnd.gradle

import aQute.bnd.build.Workspace
import aQute.bnd.osgi.Constants
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger

public class BndPlugin implements Plugin<Project> {
  public static final String PLUGINID = 'biz.aQute.bnd'
  private Project project
  private bndProject
  private boolean preCompileRefresh

  /**
   * Apply the {@code biz.aQute.bnd} plugin to the specified project.
   */
  @Override
  public void apply(Project p) {
    p.configure(p) { project ->
      this.project = project
      if (plugins.hasPlugin(BndBuilderPlugin.PLUGINID)) {
          throw new GradleException("Project already has '${BndBuilderPlugin.PLUGINID}' plugin applied.")
      }
      if (!rootProject.hasProperty('bndWorkspace')) {
        rootProject.ext.bndWorkspace = new Workspace(rootDir)
        if (bndWorkspace == null) {
          throw new GradleException("Unable to load bnd workspace ${rootDir}")
        }
      }
      this.bndProject = bndWorkspace.getProject(name)
      if (bndProject == null) {
        throw new GradleException("Unable to load bnd project ${name} from workspace ${rootDir}")
      }
      bndProject.prepare();
      if (!bndProject.isValid()) {
        checkErrors(logger)
        throw new GradleException("Project ${bndProject.getName()} is not a valid bnd project")
      }
      this.preCompileRefresh = project.hasProperty('bnd_preCompileRefresh') ? parseBoolean(bnd_preCompileRefresh) : false
      extensions.create('bnd', BndProperties, bndProject)
      bnd.ext.project = bndProject
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
        dependson.description = 'bnd project dependencies.'
        runtime.artifacts.clear()
        archives.artifacts.clear()
      }
      /* Set up deliverables */
      bndProject.getDeliverables()*.getFile().each { deliverable ->
        artifacts {
          runtime(deliverable) {
             builtBy jar
          }
          archives(deliverable) {
             builtBy jar
          }
        }
      }
      /* Set up dependencies */
      dependencies {
        compile compilePath()
        runtime runtimePath()
        testCompile testCompilePath()
        testRuntime testRuntimePath()
      }
      /* Set up source sets */
      sourceSets {
        /* bnd uses the same directory for java and resources. */
        main {
          java.srcDirs = resources.srcDirs = files(bndProject.getSourcePath())
          output.classesDir = output.resourcesDir = bndProject.getSrcOutput()
        }
        test {
          java.srcDirs = resources.srcDirs = files(bndProject.getTestSrc())
          output.classesDir = output.resourcesDir = bndProject.getTestOutput()
        }
      }
      bnd.ext.allSrcDirs = files(bndProject.getAllsourcepath())
      /* Set up compile tasks */
      sourceCompatibility = bnd('javac.source', sourceCompatibility)
      def javacTarget = bnd('javac.target', targetCompatibility)
      def bootclasspath = files(bndProject.getBootclasspath()*.getFile())
      if (javacTarget == 'jsr14') {
        javacTarget = '1.5'
        bootclasspath = files(bndProject.getBundle('ee.j2se', '1.5', null, ['strategy':'lowest']).getFile())
      }
      targetCompatibility = javacTarget
      def javac = bnd('javac')
      def javacProfile = bnd('javac.profile', '')
      def javacDebug = parseBoolean(bnd('javac.debug'))
      def javacDeprecation = parseBoolean(bnd('javac.deprecation', 'true'))
      def javacEncoding = bnd('javac.encoding', 'UTF-8')
      def compileOptions = {
        if (javacDebug) {
          options.debugOptions.debugLevel = 'source,lines,vars'
        }
        options.verbose = logger.isDebugEnabled()
        options.listFiles = logger.isInfoEnabled()
        options.deprecation = javacDeprecation
        options.encoding = javacEncoding
        if (javac != 'javac') {
          options.fork = true
          options.forkOptions.executable = javac
        }
        if (!bootclasspath.empty) {
          options.fork = true
          options.bootClasspath = bootclasspath.asPath
        }
        if (!javacProfile.empty) {
          options.compilerArgs += ['-profile', javacProfile]
        }
      }

      compileJava {
        configure compileOptions
        if (logger.isInfoEnabled()) {
          doFirst {
            logger.info 'Compile {} to {}', sourceSets.main.java.srcDirs, destinationDir
            if (javacProfile.empty) {
              logger.info '-source {} -target {}', sourceCompatibility, targetCompatibility
            } else {
              logger.info '-source {} -target {} -profile {}', sourceCompatibility, targetCompatibility, javacProfile
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
        if (preCompileRefresh) {
          doFirst {
            logger.info 'Refreshing the bnd Project before compilation.'
            bndProject.refresh()
            bndProject.propertiesChanged()
            bndProject.clear()
            bndProject.prepare()
            classpath = compilePath()
          }
        }
      }

      compileTestJava {
        configure compileOptions
        doFirst {
            checkErrors(logger)
        }
        if (preCompileRefresh) {
          doFirst {
            logger.info 'Refreshing the bnd Project before compilation.'
            bndProject.refresh()
            bndProject.propertiesChanged()
            bndProject.clear()
            bndProject.prepare()
            classpath = files(testCompilePath(), runtimePath(), compilePath())
          }
        }
      }

      processResources {
        def srcDirs = sourceSets.main.resources.srcDirs
        def outputDir = sourceSets.main.output.resourcesDir
        inputs.files.each {
          def input = it.absolutePath
          srcDirs.each {
            input -= it
          }
          outputs.file new File(outputDir, input)
        }
      }

      processTestResources {
        def srcDirs = sourceSets.test.resources.srcDirs
        def outputDir = sourceSets.test.output.resourcesDir
        inputs.files.each {
          def input = it.absolutePath
          srcDirs.each {
            input -= it
          }
          outputs.file new File(outputDir, input)
        }
      }

      jar {
        description 'Assemble the project bundles.'
        deleteAllActions() /* Replace the standard task actions */
        enabled !bndProject.isNoBundles()
        if (enabled) {
          /* bnd can include any class on the buildpath */
          inputs.files compilePath().collect {
            it.file ? it : fileTree(it)
          }
          /* all other files in the project like bnd and resources */
          inputs.files fileTree(projectDir) {
            exclude sourceSets.main.java.srcDirs.collect { relativePath(it) }
            exclude sourceSets.test.java.srcDirs.collect { relativePath(it) }
            exclude sourceSets.test.output.files.collect { relativePath(it) }
            exclude relativePath(buildDir)
          }
          /* project dependencies' artifacts should trigger jar task */
          configurations.compile.dependencies.withType(ProjectDependency.class).each {
            inputs.files it.dependencyProject.configurations.archives.artifacts.files
          }
          outputs.files configurations.archives.artifacts.files, new File(buildDir, Constants.BUILDFILES)
          doLast {
            def built
            try {
              built = bndProject.build()
            } catch (Exception e) {
              throw new GradleException("Project ${bndProject.getName()} failed to build", e)
            }
            checkErrors(logger)
            if (built != null) {
              logger.info 'Generated bundles: {}', built
            }
          }
        }
      }

      task('release') {
        description 'Release the project to the release repository.'
        dependsOn assemble
        group 'release'
        enabled !bndProject.isNoBundles() && !bnd(Constants.RELEASEREPO, 'unset').empty
        if (enabled) {
          inputs.files configurations.archives.artifacts.files
          doLast {
            try {
              bndProject.release()
            } catch (Exception e) {
              throw new GradleException("Project ${bndProject.getName()} failed to release", e)
            }
            checkErrors(logger)
          }
        }
      }

      task('releaseNeeded') {
        description 'Release the project and all projects it depends on.'
        dependsOn release
        group 'release'
      }

      test {
        enabled !parseBoolean(bnd(Constants.NOJUNIT, 'false')) && !parseBoolean(bnd('no.junit', 'false'))
        doFirst {
          try {
            checkErrors(logger)
          } catch (Exception e) {
            if (!ignoreFailures) {
              throw e
            }
          }
        }
      }

      check {
        dependsOn assemble
        enabled !parseBoolean(bnd(Constants.NOJUNITOSGI, 'false')) && !bndUnprocessed(Constants.TESTCASES, '').empty
        ext.ignoreFailures = false
        if (enabled) {
          doLast {
            try {
              bndProject.test()
            } catch (Exception e) {
              throw new GradleException("Project ${bndProject.getName()} failed to test", e)
            }
            try {
              checkErrors(logger)
            } catch (Exception e) {
              if (!ignoreFailures) {
                throw e
              }
            }
          }
        }
      }

      task('checkNeeded') {
        description 'Runs all checks on the project and all projects it depends on.'
        dependsOn check
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
        dependsOn clean
        group 'build'
      }

      tasks.addRule('Pattern: export.<name>: Export the <name>.bndrun file to an executable jar.') { taskName ->
        if (taskName.startsWith('export.')) {
          def bndrun = taskName - 'export.'
          def runFile = file("${bndrun}.bndrun")
          if (runFile.isFile()) {
            task(taskName) {
              description "Export the ${bndrun}.bndrun file to an executable jar."
              dependsOn assemble
              group 'export'
              def executableJar = new File(distsDir, "executable/${bndrun}.jar")
              doFirst {
                project.mkdir(executableJar.parent)
              }
              doLast {
                logger.info 'Exporting {} to {}', runFile.absolutePath, executableJar.absolutePath
                try {
                  bndProject.export(relativePath(runFile), false, executableJar)
                } catch (Exception e) {
                  throw new GradleException("Export of ${runFile.absolutePath} to an executable jar failed", e)
                }
                checkErrors(logger)
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
          dependsOn tasks.getByPath("export.${it.name - '.bndrun'}")
        }
      }

      tasks.addRule('Pattern: runbundles.<name>: Create a distribution of the runbundles in <name>.bndrun file.') { taskName ->
        if (taskName.startsWith('runbundles.')) {
          def bndrun = taskName - 'runbundles.'
          def runFile = file("${bndrun}.bndrun")
          if (runFile.isFile()) {
            task(taskName) {
              description "Create a distribution of the runbundles in the ${bndrun}.bndrun file."
              dependsOn assemble
              group 'export'
              def runbundlesDir = new File(distsDir, "runbundles/${bndrun}")
              doFirst {
                project.delete(runbundlesDir)
                project.mkdir(runbundlesDir)
              }
              doLast {
                logger.info 'Creating a distribution of the runbundles in {} in directory {}', runFile.absolutePath, runbundlesDir.absolutePath
                try {
                    bndProject.exportRunbundles(relativePath(runFile), runbundlesDir)
                } catch (Exception e) {
                  throw new GradleException("Creating a distribution of the runbundles in ${runFile.absolutePath} failed", e)
                }
                checkErrors(logger)
              }
            }
          }
        }
      }

      task('runbundles') {
        description "Create a distribution of the runbundles in each of the bndrun files."
        group 'export'
        fileTree(projectDir) {
            include '*.bndrun'
        }.each {
          dependsOn tasks.getByPath("runbundles.${it.name - '.bndrun'}")
        }
      }

      task('echo') {
        description 'Displays the bnd project information.'
        group 'help'
        doLast {
          println '------------------------------------------------------------'
          println "Project ${project.name}"
          println '------------------------------------------------------------'
          println()
          println "project.workspace:      ${rootDir}"
          println "project.dir:            ${projectDir}"
          println "project.name:           ${project.name}"
          println "project.dependson:      ${bndProject.getDependson()*.getName()}"
          println "project.sourcepath:     ${files(sourceSets.main.java.srcDirs).asPath}"
          println "project.output:         ${compileJava.destinationDir}"
          println "project.buildpath:      ${compileJava.classpath.asPath}"
          println "project.allsourcepath:  ${bnd.allSrcDirs.asPath}"
          println "project.testsrc:        ${files(sourceSets.test.java.srcDirs).asPath}"
          println "project.testoutput:     ${compileTestJava.destinationDir}"
          println "project.testpath:       ${compileTestJava.classpath.asPath}"
          println "project.bootclasspath:  ${compileJava.options.bootClasspath}"
          println "project.deliverables:   ${configurations.archives.artifacts.files*.path}"
          println "javac:                  ${compileJava.options.forkOptions.executable}"
          println "javac.source:           ${sourceCompatibility}"
          println "javac.target:           ${targetCompatibility}"
          if (!javacProfile.empty) {
            println "javac.profile:          ${javacProfile}"
          }
          println "target:                 ${buildDir}"
          println()
          checkErrors(logger)
        }
      }

      task('bndproperties') {
        description 'Displays the bnd properties.'
        group 'help'
        doLast {
          println '------------------------------------------------------------'
          println "Project ${project.name}"
          println '------------------------------------------------------------'
          println()
          bndProject.getPropertyKeys(true).sort({
            s1, s2 -> s1.compareTo(s2)
          }).each {
            println "${it}: ${bnd(it, '')}"
          }
          println()
          checkErrors(logger)
        }
      }

      /* Set up dependencies */
      bndProject.getDependson()*.getName().each { dependency ->
        dependencies { handler ->
          compile handler.project('path': ":${dependency}", 'configuration': 'dependson')
        }
        compileJava.dependsOn(":${dependency}:assemble")
        checkNeeded.dependsOn(":${dependency}:checkNeeded")
        releaseNeeded.dependsOn(":${dependency}:releaseNeeded")
        cleanNeeded.dependsOn(":${dependency}:cleanNeeded")
      }
    }
  }

  private FileCollection compilePath() {
    return project.files(bndProject.getBuildpath()*.getFile() - bndProject.getSrcOutput())
  }

  private FileCollection testCompilePath() {
    return project.files(bndProject.getTestpath()*.getFile() - bndProject.getTestOutput())
  }

  private FileCollection runtimePath() {
    return project.files(bndProject.getSrcOutput())
  }

  private FileCollection testRuntimePath() {
    return project.files(bndProject.getTestOutput())
  }

  private void checkErrors(Logger logger) {
    bndProject.getInfo(bndProject.getWorkspace(), "${bndProject.getWorkspace().getBase().name} :")
    boolean failed = !bndProject.isOk()
    int errorCount = bndProject.getErrors().size()
    bndProject.getWarnings().each {
      logger.warn 'Warning: {}', it
    }
    bndProject.getWarnings().clear()
    bndProject.getErrors().each {
      logger.error 'Error  : {}', it
    }
    bndProject.getErrors().clear()
    if (failed) {
      def str = ' even though no errors were reported'
      if (errorCount == 1) {
        str = ', one error was reported'
      } else if (errorCount > 1) {
        str = ", ${errorCount} errors were reported"
      }
      throw new GradleException("Project ${bndProject.getName()} has errors${str}")
    }
  }

  private boolean parseBoolean(String value) {
    return 'on'.equalsIgnoreCase(value) || 'true'.equalsIgnoreCase(value)
  }
}

