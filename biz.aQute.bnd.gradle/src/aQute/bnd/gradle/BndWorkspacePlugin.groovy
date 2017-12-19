/**
 * BndWorkspacePlugin for Gradle.
 *
 * <p>
 * The plugin name is {@code biz.aQute.bnd.workspace}.
 * <p>
 * This plugin can be applied to the Settings object in
 * settings.gradle. It can also be applied to the root
 * project in build.gradle.
 *
 */

package aQute.bnd.gradle

import static aQute.bnd.gradle.BndUtils.logReport

import aQute.bnd.build.Workspace
import aQute.bnd.osgi.Constants
import biz.aQute.bnd.reporter.generator.WorkspaceReportGenerator

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.initialization.Settings
import org.gradle.api.tasks.Delete
import groovy.lang.MissingPropertyException

public class BndWorkspacePlugin implements Plugin<Object> {
  public static final String PLUGINID = 'biz.aQute.bnd.workspace'
  /**
   * Apply the {@code biz.aQute.bnd.workspace} plugin.
   */
  @Override
  public void apply(Object receiver) {
    if (receiver instanceof Settings) {
      Closure configure = configureSettings()
      configure.delegate = receiver
      configure(receiver)
    } else if (receiver instanceof Project) {
      receiver.configure(receiver, configureRootProject())
    } else {
      throw new GradleException("The receiver ${receiver} is not a Settings or a Project")
    }
  }

  Closure configureSettings() {
    return { Settings settings ->
      /* Add cnf project to the graph */
      String cnf = 'cnf'
      try {
        cnf = bnd_cnf
      } catch (MissingPropertyException mpe) {}
      include cnf

      /* Start with the declared build project name */
      String build = ''
      try {
        build = bnd_build
      } catch (MissingPropertyException mpe) {}
      String defaultProjectName = build

      /* If in a subproject, use the subproject name */
      for (File currentDir = startParameter.currentDir; currentDir != settings.rootDir; currentDir = currentDir.parentFile) {
        defaultProjectName = currentDir.name
      }

      /* Build a set of project names we need to include from the specified tasks */
      Set<String> projectNames = startParameter.taskNames.collect { String taskName ->
        String[] elements = taskName.split(':')
        switch (elements.length) {
          case 1:
            return defaultProjectName
          case 2:
            return elements[0].empty ? build : elements[0]
          default:
            return elements[0].empty ? elements[1] : elements[0]
        }
      }.toSet()

      /* Include the default project name if in a subproject or no tasks specified */
      if ((startParameter.currentDir != rootDir) || projectNames.empty) {
        projectNames.add(defaultProjectName)
      }

      /* If build used but empty, add all non-private folders of rootDir */
      if (projectNames.remove('')) {
        rootDir.eachDir {
          String projectName = it.name
          if (!projectName.startsWith('.')) {
            projectNames.add(projectName)
          }
        }
      }

      /* Initialize the Bnd workspace */
      Workspace.setDriver(Constants.BNDDRIVER_GRADLE)
      Workspace.addGestalt(Constants.GESTALT_BATCH, null)
      Workspace workspace = new Workspace(rootDir, cnf).setOffline(startParameter.offline)
      if (gradle.ext.has('bndWorkspaceConfigure')) {
        gradle.bndWorkspaceConfigure(workspace)
      }

      /* Prepare each project in the workspace to establish complete 
       * dependencies and dependents information.
       */
      workspace.getAllProjects().each { it.prepare() }

      /* Add each project and its dependents to the graph */
      Set<String> projectGraph = new LinkedHashSet<>()
      while (!projectNames.empty) {
        String projectName = projectNames.head()
        projectGraph.add(projectName)
        def p = workspace.getProject(projectName)
        if (p) {
          projectNames.addAll(p.getDependents()*.getName())
        }
        projectNames.removeAll(projectGraph)
      }

      /* Add each project and its dependencies to the graph */
      projectNames = projectGraph
      projectGraph = new LinkedHashSet<>()
      while (!projectNames.empty) {
        String projectName = projectNames.head()
        projectGraph.add(projectName)
        def p = workspace.getProject(projectName)
        if (p) {
          projectNames.addAll(p.getTestDependencies()*.getName())
        }
        projectNames.removeAll(projectGraph)
      }

      include projectGraph as String[]

      /* Apply workspace plugin to root project */
      gradle.projectsLoaded { gradle ->
        gradle.rootProject.apply plugin: BndWorkspacePlugin.class
      }
    }
  }

  Closure configureRootProject() {
    return { Project p ->
      if (p != rootProject) {
        throw new GradleException("The project ${p.name} is not the root project")
      }

      /* Initialize the Bnd workspace */
      ext.bnd_cnf = findProperty('bnd_cnf') ?: 'cnf'
      Workspace.setDriver(Constants.BNDDRIVER_GRADLE)
      Workspace.addGestalt(Constants.GESTALT_BATCH, null)
      ext.bndWorkspace = new Workspace(rootDir, bnd_cnf).setOffline(gradle.startParameter.offline)
      if (gradle.ext.has('bndWorkspaceConfigure')) {
        gradle.bndWorkspaceConfigure(bndWorkspace)
      }

      /* Configure cnf project */
      Project cnfProject = findProject(bnd_cnf)
      if (cnfProject != null) {
        ext.cnf = cnfProject
        cnfProject.task('cleanCache', type: Delete) {
          description 'Clean the cache folder.'
          group 'build'
          delete 'cache'
        }
      }

      /* Configure the Bnd projects */
      subprojects {
        if (bndWorkspace.getProject(name) != null) {
          apply plugin: BndPlugin.class
        }
      }
      
      task('listReport') {
        description 'Displays the list of documents that can be generated.'
        group 'help'
        doLast {
        	WorkspaceReportGenerator g;
        	try {
        		g = new WorkspaceReportGenerator(bndWorkspace) 
				g.getAvailableReports().each { r ->
	            	println "${r}"
          		}  
        	} catch (Exception e) {
	        	throw new GradleException("report failure", e)
        	} finally {
	        	bndWorkspace.getInfo(g)
    			boolean failed = !bndWorkspace.isOk()
    			int errorCount = bndWorkspace.getErrors().size()
    			logReport(bndWorkspace, logger)
    			bndWorkspace.getWarnings().clear()
   				bndWorkspace.getErrors().clear()
    			if (failed) {
      				String str = ' even though no errors were reported'
      				if (errorCount == 1) {
        				str = ', one error was reported'
      				} else if (errorCount > 1) {
        				str = ", ${errorCount} errors were reported"
      				}
      				throw new GradleException("${bndWorkspace} has errors${str}")
    			}
        		g.close()
        	}
        }
      }
      
      task('report') {
        description "Generates the documents that match the given glob expression. Use -Pglob='<glob expression>'"
        group 'export'
        doLast {
        	WorkspaceReportGenerator g;
        	try {
        		g = new WorkspaceReportGenerator(bndWorkspace) 
				g.generateReports("$glob"); 
        	} catch (MissingPropertyException e) {
	        	throw new GradleException("Missing glob expression, use -Pglob='<glob expression>'", e)
        	} catch (Exception e) {
	        	throw new GradleException("report failure", e)
        	} finally {
	        	bndWorkspace.getInfo(g)
    			boolean failed = !bndWorkspace.isOk()
    			int errorCount = bndWorkspace.getErrors().size()
    			logReport(bndWorkspace, logger)
    			bndWorkspace.getWarnings().clear()
   				bndWorkspace.getErrors().clear()
    			if (failed) {
      				String str = ' even though no errors were reported'
      				if (errorCount == 1) {
        				str = ', one error was reported'
      				} else if (errorCount > 1) {
        				str = ", ${errorCount} errors were reported"
      				}
      				throw new GradleException("${bndWorkspace} has errors${str}")
    			}
        		g.close()
        	}
        }
      }
    }
  }
}
