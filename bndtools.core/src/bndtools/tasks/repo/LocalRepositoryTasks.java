package bndtools.tasks.repo;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;
import org.eclipse.jdt.ui.wizards.JavaCapabilityConfigurationPage;
import org.osgi.framework.Bundle;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.deployer.FileRepo;
import aQute.lib.osgi.Constants;
import aQute.lib.osgi.Jar;
import aQute.libg.version.Version;
import bndtools.Plugin;
import bndtools.api.repository.RemoteRepository;
import bndtools.utils.BundleUtils;

public class LocalRepositoryTasks {
    private static final String PATH_REPO_FOLDER = "repo";
    private static final String PREF_INSTALLED_REPOS = "installedRepos";
    private static final String PREF_PREFIX_INSTALLED_REPO = "installedRepoVersion-";

    /**
     * Returns whether the workspace is configured for bnd (i.e. the cnf project exists).
     * @return
     */
    public static boolean isBndWorkspaceConfigured() {
        IProject cnfProject = getCnfProject();
        return cnfProject != null && cnfProject.exists();
    }

    /**
     * Configure the bnd workspace by creating the cnf project if needed, or
     * opening the existing cnf project if it is closed. Creates the repository
     * folder but does not copy any content into it. This method must be run in
     * the context of an IWorkspaceRunnable.
     *
     * @param monitor
     *            the progress monitor to use for reporting progress to the
     *            user. It is the caller's responsibility to call done() on the
     *            given monitor. Accepts null, indicating that no progress
     *            should be reported and that the operation cannot be cancelled.
     *            * @throws CoreException
     */
    public static void configureBndWorkspace(IProgressMonitor monitor) throws CoreException {
        SubMonitor progress = SubMonitor.convert(monitor);

        IProject cnfProject = getCnfProject();
        if(cnfProject == null || !cnfProject.exists()) {
            progress.setWorkRemaining(4);
            JavaCapabilityConfigurationPage.createProject(cnfProject, (URI) null, progress.newChild(1));
            configureJavaProject(JavaCore.create(cnfProject), null, progress.newChild(1));

            copyResourceToFile("template_build.bnd", cnfProject.getFile(Workspace.BUILDFILE), progress.newChild(1));
            copyResourceToFile("template_cnf_build.xml", cnfProject.getFile("build.xml"), progress.newChild(1));
        } else if(!cnfProject.isOpen()) {
            progress.setWorkRemaining(2);
            cnfProject.open(progress.newChild(1));
        }

        IFolder repoFolder = getLocalRepositoryFolder(cnfProject);
        if(!repoFolder.exists())
            repoFolder.create(true, true, progress.newChild(1));
        else
            progress.worked(1);
    }

    public static RepositoryPlugin getLocalRepository() {
        FileRepo repo = new FileRepo();

        Map<String, String> props = new HashMap<String, String>();
        props.put("location", getLocalRepositoryFolder(getCnfProject()).getLocation().toString());
        repo.setProperties(props);

        return repo;
    }

    public static IStatus installImplicitRepositoryContents(RepositoryPlugin localRepo, MultiStatus status, IProgressMonitor monitor) throws CoreException {
        SubMonitor progress = SubMonitor.convert(monitor);

        Map<String, Version> installedVersions = loadInstalledRepositoryVersions();
        Map<String, Version> updatedVersions = new HashMap<String, Version>(installedVersions);

        // Copy in the implicit repository contributions
        IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
        IConfigurationElement[] elements = extensionRegistry.getConfigurationElementsFor(Plugin.PLUGIN_ID, Plugin.EXTPOINT_REPO_CONTRIB);
        if (elements != null && elements.length > 0) {
            for(int i = 0; i < elements.length; i++) {
                progress.setWorkRemaining(elements.length - i);
                IConfigurationElement element = elements[i];

                String implicit = element.getAttribute("implicit");
                if(!"true".equalsIgnoreCase(implicit))
                    continue;

                String contributorBSN = element.getContributor().getName();
                Bundle contributorBundle = BundleUtils.findBundle(contributorBSN, null);
                if(contributorBundle != null) {
                    String contributorVersionStr = (String) contributorBundle.getHeaders().get(Constants.BUNDLE_VERSION);
                    if(contributorVersionStr == null)
                        contributorVersionStr = "0";

                    try {
                        Version installedVersion = updatedVersions.get(contributorBSN);
                        Version contributorVersion = new Version(contributorVersionStr);

                        if(installedVersion == null || contributorVersion.compareTo(installedVersion) > 0) {
                            initialiseAndInstallRepository(element, localRepo, status, progress.newChild(1));
                            updatedVersions.put(contributorBSN, contributorVersion);
                        }
                    } catch (IllegalArgumentException e) {
                        Plugin.logError(MessageFormat.format("Repository contributor {0} has invalid Bundle-Version.", contributorBSN), e);
                    }
                }
            }
        }

        try {
            saveInstalledRepositoryVersion(updatedVersions);
        } catch (BackingStoreException e) {
            status.add(new Status(IStatus.WARNING, Plugin.PLUGIN_ID, 0, "Failed to save installed repository version information.", e));
        }
        return status;
    }

    public static void installBundle(RepositoryPlugin localRepo, URL url) throws IOException, CoreException {
        URLConnection connection = url.openConnection();
        InputStream stream = connection.getInputStream();
        try {
            Jar jar = new Jar("", stream, System.currentTimeMillis());
            localRepo.put(jar);
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error adding bundle to repository.", e));
        } finally {
            stream.close();
        }
    }


    public static void refreshWorkspaceForRepository(IProgressMonitor monitor) throws CoreException {
        SubMonitor progress = SubMonitor.convert(monitor, 1);
        IWorkspaceRunnable refreshOp = new IWorkspaceRunnable() {
            public void run(IProgressMonitor monitor) throws CoreException {
                IFolder folder = getLocalRepositoryFolder(getCnfProject());
                folder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
            }
        };
        ResourcesPlugin.getWorkspace().run(refreshOp, progress.newChild(1));
    }

    static IProject getCnfProject() {
        IProject cnfProject = ResourcesPlugin.getWorkspace().getRoot().getProject(Project.BNDCNF);
        return cnfProject;
    }

    static void configureJavaProject(IJavaProject javaProject, String newProjectCompliance, IProgressMonitor monitor) throws CoreException {
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

    static void copyResourceToFile(String resourceName, IFile destinationFile, IProgressMonitor monitor) throws CoreException {
        SubMonitor progress = SubMonitor.convert(monitor, 1);
        if (!destinationFile.exists()) {
            InputStream templateStream = LocalRepositoryTasks.class.getResourceAsStream(resourceName);
            try {
                destinationFile.create(templateStream, true, progress.newChild(1));
            } finally {
                try {
                    templateStream.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        } else {
            progress.worked(1);
        }
    }

    static IFolder getLocalRepositoryFolder(IProject cnfProject) {
        return cnfProject.getFolder(PATH_REPO_FOLDER);
    }

    static void initialiseAndInstallRepository(IConfigurationElement remoteRepositoryElem, RepositoryPlugin localRepo, MultiStatus status, IProgressMonitor monitor) {
        SubMonitor progress = SubMonitor.convert(monitor, 3);

        String implicitStr = remoteRepositoryElem.getAttribute("implicit");
        String repoName = remoteRepositoryElem.getAttribute("name");
        if(repoName == null) repoName = "<unknown>";
        if("true".equalsIgnoreCase(implicitStr)) {
            try {
                RemoteRepository repo = (RemoteRepository) remoteRepositoryElem.createExecutableExtension("class");
                repo.initialise(progress.newChild(1));
                installRepository(repo, localRepo, status, progress.newChild(2));
            } catch (CoreException e) {
                String message = MessageFormat.format("Failed to initialise remote repository {0}.", repoName);
                if(status != null)
                    status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, message, e));
                Plugin.logError(message, e);
            }
        } else {
            progress.worked(1);
        }
    }

    static void installRepository(RemoteRepository remoteRepo, RepositoryPlugin localRepo, MultiStatus status, IProgressMonitor monitor) {
        Collection<String> bsns = remoteRepo.list(null);
        SubMonitor progress = SubMonitor.convert(monitor, bsns.size());
        for (String bsn : bsns) {
            List<URL> urls = remoteRepo.get(bsn, null);
            if(urls != null && !urls.isEmpty()) {
                for (URL url: urls) {
                    String errorMessage = MessageFormat.format("Error installing bundle URL {0} to local repository.", url.toString());
                    try {
                        installBundle(localRepo, url);
                    } catch (IOException e) {
                        if(status != null) status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, errorMessage, e));
                        Plugin.logError(errorMessage, e);
                    } catch (CoreException e) {
                        if(status != null) status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, errorMessage, e));
                        Plugin.logError(errorMessage, e);
                    }
                }
            }
            progress.worked(1);
        }
    }

    static Map<String, Version> loadInstalledRepositoryVersions() {
        Preferences node = getProjectPreferencesNode();

        Map<String, Version> result = new HashMap<String, Version>();

        String installStr = node.get(PREF_INSTALLED_REPOS, "");
        StringTokenizer tok = new StringTokenizer(installStr, ",");
        while(tok.hasMoreTokens()) {
            String id = tok.nextToken().trim();
            String versionStr = node.get(PREF_PREFIX_INSTALLED_REPO + id, null);
            if(versionStr != null) {
                try {
                    Version version = new Version(versionStr);
                    result.put(id, version);
                } catch (Exception e) {
                    Plugin.logError(MessageFormat.format("Invalid installed version for repository contributor ID {0}.", id), e);
                }
            }
        }

        return result;
    }

    static void saveInstalledRepositoryVersion(Map<String, Version> map) throws BackingStoreException {
        Preferences node = getProjectPreferencesNode();

        StringBuilder builder = new StringBuilder();
        for (Iterator<Entry<String, Version>> iter = map.entrySet().iterator(); iter.hasNext(); ) {
            Entry<String, Version> entry = iter.next();
            node.put(PREF_PREFIX_INSTALLED_REPO + entry.getKey(), entry.getValue().toString());
            builder.append(entry.getKey());
            if(iter.hasNext()) builder.append(",");
        }
        node.put(PREF_INSTALLED_REPOS, builder.toString());
        node.flush();
    }

    static Preferences getProjectPreferencesNode() {
        IProject cnfProject = getCnfProject();
        ProjectScope cnfProjectPrefs = new ProjectScope(cnfProject);
        return cnfProjectPrefs.getNode(Plugin.PLUGIN_ID);
    }
}

