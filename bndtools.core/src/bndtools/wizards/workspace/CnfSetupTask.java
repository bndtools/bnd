package bndtools.wizards.workspace;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
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

import aQute.bnd.build.Project;
import bndtools.Central;
import bndtools.Plugin;
import bndtools.utils.BundleUtils;

public class CnfSetupTask extends WorkspaceModifyOperation {

    private final IConfigurationElement templateConfig;

    public CnfSetupTask(IConfigurationElement templateConfig) {
        this.templateConfig = templateConfig;
    }

    /**
     * Returns whether the workspace is configured for bnd (i.e. the cnf project exists).
     * @return
     */
    static boolean isBndWorkspaceConfigured() {
        IProject cnfProject = getCnfProject();
        return cnfProject != null && cnfProject.exists();
    }

    @Override
    protected void execute(IProgressMonitor monitor) throws CoreException {
        SubMonitor progress = SubMonitor.convert(monitor);

        IProject cnfProject = getCnfProject();
        if(cnfProject == null || !cnfProject.exists()) {
            progress.setWorkRemaining(3);
            JavaCapabilityConfigurationPage.createProject(cnfProject, (URI) null, progress.newChild(1, SubMonitor.SUPPRESS_NONE));
            configureJavaProject(JavaCore.create(cnfProject), null, progress.newChild(1, SubMonitor.SUPPRESS_NONE));

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
                Plugin.logError("Unable to refresh Bnd workspace", e);
            }

        } else if(!cnfProject.isOpen()) {
            progress.setWorkRemaining(1);
            cnfProject.open(progress.newChild(1));
        }

    }

    private static void copyBundleEntries(Bundle sourceBundle, String sourcePath, IPath sourcePrefix, IContainer destination, IProgressMonitor monitor) throws CoreException {
        List<String> subPaths = new LinkedList<String>();
        @SuppressWarnings("unchecked")
        Enumeration<String> entries = sourceBundle.getEntryPaths(sourcePath);
        if (entries != null) while (entries.hasMoreElements()) {
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
        IPath destinationPath = new Path(sourcePath).makeRelativeTo(sourcePrefix);
        IFile file = destination.getFile(destinationPath);

        try {
            if (!file.exists()) {
                file.create(entry.openStream(), false, monitor);
            } else {
                file.appendContents(entry.openStream(), false, false, monitor);
            }
        } catch (IOException e) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Failed to load data from template source bundle.", e));
        }
    }


    private static IProject getCnfProject() {
        IProject cnfProject = ResourcesPlugin.getWorkspace().getRoot().getProject(Project.BNDCNF);
        return cnfProject;
    }

    private static void configureJavaProject(IJavaProject javaProject, String newProjectCompliance, IProgressMonitor monitor) throws CoreException {
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
        outputFolder.setDerived(true);
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

