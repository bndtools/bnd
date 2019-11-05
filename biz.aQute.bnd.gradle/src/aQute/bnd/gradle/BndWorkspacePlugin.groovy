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

import aQute.bnd.build.Workspace
import aQute.bnd.osgi.Constants

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.initialization.Settings
import org.gradle.api.tasks.Delete

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
      receiver.configure(receiver, configureWorkspaceProject())
    } else {
      throw new GradleException("The receiver ${receiver} is not a Settings or a Project")
    }
  }

  Closure configureSettings() {
    return { Settings settings ->
      /* Start with the declared build project name */
      String build = ''
      try {
        build = bnd_build
      } catch (MissingPropertyException mpe) {}
      String defaultProjectName = build

      /* If in a subproject, use the subproject name */
      for (File currentDir = startParameter.currentDir; currentDir != rootDir; currentDir = currentDir.parentFile) {
        defaultProjectName = currentDir.name
      }

      /* Build a set of project names we need to include from the specified tasks */
      Set<String> projectNames = new LinkedHashSet<>()
      for (Iterator<String> iter = startParameter.taskNames.iterator(); iter.hasNext();) {
        String taskName = iter.next()
        if (taskName == '--tests') {
          if (iter.hasNext()) {
            iter.next()
          }
          continue
        }
        String[] elements = taskName.split(':')
        switch (elements.length) {
          case 1:
            projectNames.add(defaultProjectName)
            break
          case 2:
            projectNames.add(elements[0].empty ? build : elements[0])
            break
          default:
            projectNames.add(elements[0].empty ? elements[1] : elements[0])
            break
        }
      }

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

      /* Add cnf project to the graph */
      String cnf = 'cnf'
      try {
        cnf = bnd_cnf.trim()
      } catch (MissingPropertyException mpe) {}
      projectNames.add(cnf)

      /* Add any projects which must always be included */
      try {
        projectNames.addAll(bnd_include.trim().split(/\s*,\s*/))
      } catch (MissingPropertyException mpe) {}

      /* Initialize the Bnd workspace */
      Workspace.setDriver(Constants.BNDDRIVER_GRADLE)
      Workspace.addGestalt(Constants.GESTALT_BATCH, null)
      Workspace workspace = new Workspace(rootDir, cnf).setOffline(startParameter.offline)
      if (gradle.ext.has('bndWorkspaceConfigure')) {
        gradle.bndWorkspaceConfigure(workspace)
      }

      /* Make sure all workspace plugins are loaded before preparing
       * projects.
       */
      workspace.getPlugins()
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
      gradle.rootProject {
        ext.bnd_cnf = cnf
        ext.bndWorkspace = workspace
        apply plugin: BndWorkspacePlugin.class
      }
    }
  }

  Closure configureWorkspaceProject() {
    return { Project p ->
      /* Initialize the Bnd workspace */
      if (!ext.has('bnd_cnf')) { // if not passed from settings
        ext.bnd_cnf = findProperty('bnd_cnf') ?: 'cnf'
      }
      if (!ext.has('bndWorkspace')) { // if not passed from settings
        Workspace.setDriver(Constants.BNDDRIVER_GRADLE)
        Workspace.addGestalt(Constants.GESTALT_BATCH, null)
        ext.bndWorkspace = new Workspace(projectDir, bnd_cnf).setOffline(gradle.startParameter.offline)
        if (gradle.ext.has('bndWorkspaceConfigure')) {
          gradle.bndWorkspaceConfigure(bndWorkspace)
        }
      }

      /* Configure cnf project */
      Project cnfProject = findProject(bnd_cnf)
      if (cnfProject != null) {
        ext.cnf = cnfProject
        cnfProject.tasks.register('cleanCache', Delete.class) { t ->
          t.description 'Clean the cache folder.'
          t.group 'build'
          t.delete 'cache'
        }
      }

      /* Configure the Bnd projects */
      subprojects {
        if (bndWorkspace.getProject(name) != null) {
          apply plugin: BndPlugin.class
        }
      }
    }
  }
}
