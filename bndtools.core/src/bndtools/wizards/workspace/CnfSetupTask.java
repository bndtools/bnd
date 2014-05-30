package bndtools.wizards.workspace;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.api.ProjectLayout;
import org.bndtools.api.ProjectPaths;
import org.bndtools.headless.build.manager.api.HeadlessBuildManager;
import org.bndtools.utils.javaproject.JavaProjectUtils;
import org.bndtools.utils.osgi.BundleUtils;
import org.bndtools.versioncontrol.ignores.manager.api.VersionControlIgnoresManager;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;
import org.eclipse.jdt.ui.wizards.JavaCapabilityConfigurationPage;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.osgi.framework.Bundle;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Processor;
import bndtools.Plugin;
import bndtools.central.Central;
import bndtools.preferences.BndPreferences;
import bndtools.wizards.workspace.CnfInfo.Existence;

public class CnfSetupTask extends WorkspaceModifyOperation {
    private static final ILogger logger = Logger.getLogger(CnfSetupTask.class);

    private final IConfigurationElement templateConfig;
    private final CnfSetupOperation operation;

    public CnfSetupTask(CnfSetupOperation operation, IConfigurationElement templateConfig) {
        this.operation = operation;
        this.templateConfig = templateConfig;
    }

    /**
     * Returns whether the workspace is configured for bnd (i.e. the cnf project exists).
     *
     * @return the cnf info
     */
    static CnfInfo getWorkspaceCnfInfo() {
        CnfInfo result;

        IProject cnf = ResourcesPlugin.getWorkspace().getRoot().getProject(Workspace.CNFDIR);
        if (cnf.exists()) {
            IPath location = cnf.getLocation();
            if (cnf.isOpen())
                result = new CnfInfo(Existence.ImportedOpen, location);
            else
                result = new CnfInfo(Existence.ImportedClosed, location);
        } else {
            IPath location = ResourcesPlugin.getWorkspace().getRoot().getLocation().append(Workspace.CNFDIR);
            File dir = location.toFile();

            if (dir.isDirectory())
                result = new CnfInfo(Existence.Exists, location);
            else
                result = new CnfInfo(Existence.None, location);
        }
        return result;
    }

    @Override
    protected void execute(IProgressMonitor monitor) throws CoreException {
        SubMonitor progress = SubMonitor.convert(monitor);

        switch (operation.getType()) {
        case Import :
            progress.setWorkRemaining(2);
            importCnf(progress.newChild(1, SubMonitor.SUPPRESS_NONE));
            rebuildWorkspace(progress.newChild(1, SubMonitor.SUPPRESS_NONE));
            break;
        case Open :
            progress.setWorkRemaining(2);
            openProject(progress.newChild(1, SubMonitor.SUPPRESS_NONE));
            rebuildWorkspace(progress.newChild(1, SubMonitor.SUPPRESS_NONE));
            break;
        case Create :
            progress.setWorkRemaining(3);
            createOrReplaceCnf(progress.newChild(1, SubMonitor.SUPPRESS_NONE));
            rebuildWorkspace(progress.newChild(1, SubMonitor.SUPPRESS_NONE));
            break;
        case Nothing :
        default :
            break;
        }
    }

    private static void openProject(IProgressMonitor monitor) throws CoreException {
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(Workspace.CNFDIR);
        project.open(monitor);
    }

    protected void importCnf(IProgressMonitor monitor) throws CoreException {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IPath location = operation.getLocation();
        IContainer container = workspace.getRoot().getContainerForLocation(location);
        if (container == null) {
            IProjectDescription projDesc = workspace.loadProjectDescription(location.append(IProjectDescription.DESCRIPTION_FILE_NAME));
            IProject project = workspace.getRoot().getProject(Workspace.CNFDIR);
            project.create(projDesc, monitor);
            project.open(monitor);
        } else if (container.getType() == IResource.PROJECT) {
            IProject project = (IProject) container;
            if (project.exists()) {
                project.open(monitor);
            } else {
                project.create(monitor);
            }
        } else {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Incorrect path (not a project): " + location, null));
        }
    }

    protected void createOrReplaceCnf(IProgressMonitor monitor) throws CoreException {
        SubMonitor progress = SubMonitor.convert(monitor);
        progress.setWorkRemaining(3);

        IProject cnfProject = ResourcesPlugin.getWorkspace().getRoot().getProject(Workspace.CNFDIR);
        URI location = operation.getLocation() != null ? operation.getLocation().toFile().toURI() : null;
        JavaCapabilityConfigurationPage.createProject(cnfProject, location, progress.newChild(1, SubMonitor.SUPPRESS_NONE));
        IJavaProject cnfJavaProject = JavaCore.create(cnfProject);

        configureJavaProject(cnfJavaProject, progress.newChild(1, SubMonitor.SUPPRESS_NONE));

        String bsn = templateConfig.getContributor().getName();
        Bundle bundle = BundleUtils.findBundle(Plugin.getDefault().getBundleContext(), bsn, null);
        String paths = templateConfig.getAttribute("paths");
        if (paths == null)
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Template is missing 'paths' property.", null));

        StringTokenizer tokenizer = new StringTokenizer(paths, ",");
        progress.setWorkRemaining(tokenizer.countTokens());

        while (tokenizer.hasMoreTokens()) {
            String path = tokenizer.nextToken().trim();
            if (!path.endsWith("/"))
                path = path + "/";

            copyBundleEntries(bundle, path, new Path(path), cnfProject, progress.newChild(1, SubMonitor.SUPPRESS_NONE));
        }

        try {
            Central.getWorkspace().refresh();
        } catch (Exception e) {
            logger.logError("Unable to refresh Bnd workspace", e);
        }

        /* Version control ignores */
        VersionControlIgnoresManager versionControlIgnoresManager = Plugin.getDefault().getVersionControlIgnoresManager();
        Set<String> enabledIgnorePlugins = new BndPreferences().getVersionControlIgnoresPluginsEnabled(versionControlIgnoresManager, cnfJavaProject, null);
        Map<String,String> sourceOutputLocations = JavaProjectUtils.getSourceOutputLocations(cnfJavaProject);
        versionControlIgnoresManager.createProjectIgnores(enabledIgnorePlugins, cnfJavaProject.getProject().getLocation().toFile(), sourceOutputLocations, ProjectPaths.get(ProjectLayout.BND).getTargetDir());
        String templateIgnores = null;
        try {
            templateIgnores = templateConfig.getAttribute("ignores");
        } catch (Exception e) {
            logger.logError("Could not retrieve the 'ignores' property from the cnf template " + bsn, e);
        }
        if (templateIgnores != null && !templateIgnores.isEmpty()) {
            versionControlIgnoresManager.addIgnores(enabledIgnorePlugins, cnfJavaProject.getProject().getLocation().toFile(), templateIgnores);
        }

        /* Headless build files */
        String nobuild = templateConfig.getAttribute("nobuild");
        if (!Processor.isTrue(nobuild)) {
            HeadlessBuildManager headlessBuildManager = Plugin.getDefault().getHeadlessBuildManager();
            Set<String> enabledPlugins = new BndPreferences().getHeadlessBuildPluginsEnabled(headlessBuildManager, null);
            headlessBuildManager.setup(enabledPlugins, true, cnfJavaProject.getProject().getLocation().toFile(), true, enabledIgnorePlugins);
        }
        /* refresh the project; files were created outside of Eclipse API */
        cnfProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
    }

    static void rebuildWorkspace(IProgressMonitor monitor) throws CoreException {
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
    }

    private static void copyBundleEntries(Bundle sourceBundle, String sourcePath, IPath sourcePrefix, IContainer destination, IProgressMonitor monitor) throws CoreException {
        List<String> subPaths = new LinkedList<String>();
        Enumeration<String> entries = sourceBundle.getEntryPaths(sourcePath);
        if (entries != null)
            while (entries.hasMoreElements()) {
                subPaths.add(entries.nextElement());
            }
        int work = subPaths.size();
        SubMonitor progress = SubMonitor.convert(monitor, work);

        for (String subPath : subPaths) {
            if (subPath.endsWith("/")) {
                IPath destinationPath = new Path(subPath).makeRelativeTo(sourcePrefix);
                IFolder folder = destination.getFolder(destinationPath);
                if (!folder.exists())
                    folder.create(true, true, null);
                copyBundleEntries(sourceBundle, subPath, sourcePrefix, destination, progress.newChild(1, SubMonitor.SUPPRESS_NONE));
                progress.setWorkRemaining(--work);
            } else {
                copyBundleEntry(sourceBundle, subPath, sourcePrefix, destination, progress.newChild(1, SubMonitor.SUPPRESS_NONE));
                progress.setWorkRemaining(--work);
            }
        }
    }

    private static void copyBundleEntry(Bundle sourceBundle, String sourcePath, IPath sourcePrefix, IContainer destination, IProgressMonitor monitor) throws CoreException {
        URL entry = sourceBundle.getEntry(sourcePath);
        if (entry == null) {
            return;
        }
        IPath destinationPath = new Path(sourcePath).makeRelativeTo(sourcePrefix);
        IFile file = destination.getFile(destinationPath);

        try {
            if (!file.exists()) {
                file.create(entry.openStream(), false, monitor);
            } else {
                file.setContents(entry.openStream(), false, true, monitor);
            }
        } catch (IOException e) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Failed to load data from template source bundle.", e));
        }
    }

    private static void configureJavaProject(IJavaProject javaProject, IProgressMonitor monitor) throws CoreException {
        SubMonitor progress = SubMonitor.convert(monitor, 5);
        IProject project = javaProject.getProject();
        BuildPathsBlock.addJavaNature(project, progress.newChild(1));

        // Create the source folder
        IFolder srcFolder = project.getFolder("src");
        if (!srcFolder.exists()) {
            srcFolder.create(true, true, progress.newChild(1));
        }
        progress.setWorkRemaining(3);

        // Create the output location
        IFolder outputFolder = project.getFolder("bin");
        if (!outputFolder.exists())
            outputFolder.create(true, true, progress.newChild(1));
        outputFolder.setDerived(true, null);
        progress.setWorkRemaining(2);

        // Set the output location
        javaProject.setOutputLocation(outputFolder.getFullPath(), progress.newChild(1));

        // Create classpath entries
        IClasspathEntry[] classpath = new IClasspathEntry[2];
        classpath[0] = JavaCore.newSourceEntry(srcFolder.getFullPath());
        classpath[1] = JavaCore.newContainerEntry(new Path("org.eclipse.jdt.launching.JRE_CONTAINER"));

        javaProject.setRawClasspath(classpath, progress.newChild(1));
    }

}
