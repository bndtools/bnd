package bndtools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
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

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.osgi.Jar;
import bndtools.api.repository.RemoteRepository;
import bndtools.types.Pair;
import bndtools.utils.BundleUtils;
import bndtools.utils.CollectionUtils;
import bndtools.utils.ProgressReportingInputStream;

public class LocalRepositoryTasks {
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
            progress.setWorkRemaining(3);
            JavaCapabilityConfigurationPage.createProject(cnfProject, (URI) null, progress.newChild(1));
            configureJavaProject(JavaCore.create(cnfProject), null, progress.newChild(1));

            copyResourceToFile("template_build.bnd", cnfProject.getFile(Workspace.BUILDFILE), progress.newChild(1));
            copyResourceToFile("template_cnf_build.xml", cnfProject.getFile("build.xml"), progress.newChild(1));
        } else if(!cnfProject.isOpen()) {
            progress.setWorkRemaining(1);
            cnfProject.open(progress.newChild(1));
        }
    }

    /*
    public static RepositoryPlugin getLocalRepository() throws CoreException {
        List<RepositoryPlugin> repos;
        try {
            Workspace workspace = Central.getWorkspace();
            repos = workspace.getRepositories();
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error querying repositories from workspace.", e));
        }

        if (repos == null || repos.isEmpty())
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "No repositories configured in the workspace.", null));

        for (RepositoryPlugin repo : repos) {
            if (repo.canWrite())
                return repo;
        }
        throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "No writeable repositories configured in the workspace.", null));
    }
    */

    static List<Pair<Bundle, String>> getInitialRepositories(MultiStatus status) {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IConfigurationElement[] elements = registry.getConfigurationElementsFor(Plugin.PLUGIN_ID, Plugin.EXTPOINT_INITIAL_REPO);

        List<Pair<Bundle, String>> result;
        if (elements == null || elements.length == 0)
            result = Collections.emptyList();
        else {
            result = new ArrayList<Pair<Bundle,String>>();
            for (IConfigurationElement element : elements) {
                String bsn = element.getContributor().getName();
                Bundle bundle = BundleUtils.findBundle(Plugin.getDefault().getBundleContext(), bsn, null);
                if (bundle != null) {
                    String path = element.getAttribute("path");
                    result.add(Pair.newInstance(bundle, path));
                }
            }
        }
        return result;
    }

    /*
    static List<ImplicitRepositoryWrapper> getImplicitRepositories(MultiStatus status) {
        IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
        IConfigurationElement[] elements = extensionRegistry.getConfigurationElementsFor(Plugin.PLUGIN_ID, Plugin.EXTPOINT_REPO_CONTRIB);

        List<ImplicitRepositoryWrapper> result;
        if(elements == null || elements.length == 0) {
            result = Collections.emptyList();
        } else {
            result = new ArrayList<ImplicitRepositoryWrapper>(elements.length);
            for (IConfigurationElement element : elements) {
                String implicit = element.getAttribute("implicit");
                if("true".equalsIgnoreCase(implicit)) {
                    ImplicitRepositoryWrapper wrapper = new ImplicitRepositoryWrapper();

                    String className = element.getAttribute("class");
                    wrapper.contributorBSN = element.getContributor().getName();
                    wrapper.path = element.getAttribute("path");
                    try {
                        wrapper.repository = (RemoteRepository) element.createExecutableExtension("class");
                        Bundle contributorBundle = BundleUtils.findBundle(Plugin.getDefault().getBundleContext(), wrapper.contributorBSN, null);
                        wrapper.contributorVersion = new Version(0);
                        if(contributorBundle != null) {
                            String versionStr = (String) contributorBundle.getHeaders().get(Constants.BUNDLE_VERSION);
                            if(versionStr != null) {
                                try {
                                    wrapper.contributorVersion = new Version(versionStr);
                                } catch (IllegalArgumentException e) {
                                    Plugin.logError("Invalid bundle version in repository contributor bundle: " + wrapper.contributorBSN, e);
                                }
                            }
                        } else {
                            Plugin.logError("Unable to find bundle for repository contributor ID: " + wrapper.contributorBSN, null);
                        }
                        result.add(wrapper);
                    } catch (CoreException e) {
                        String message = MessageFormat.format("Error instantiating repositoryContributor element from bundle {0}, class name {1}.", wrapper.contributorBSN, className);
                        if (status != null)
                            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, message, e));

                        Plugin.logError(message, e);
                    }
                }
            }
        }
        return result;
    }
    */

    public static void installInitialRepositories(MultiStatus status, IProgressMonitor monitor) {
        IProject cnf = getCnfProject();
        SubMonitor progress = SubMonitor.convert(monitor);

        List<Pair<Bundle,String>> repositories = getInitialRepositories(status);
        for (Pair<Bundle, String> repository : repositories) {
            Bundle bundle = repository.getFirst();
            String pathStr = repository.getSecond();
            if (!pathStr.endsWith("/"))
                pathStr += "/";
            IPath path = new Path(pathStr);
            try {
                recurseInstallEntries(bundle, path, cnf, true, progress.newChild(1, SubMonitor.SUPPRESS_NONE));
            } catch (CoreException e) {
                status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, String.format("Error installing initial repository from bundle %s, path %s.", repository.getFirst().getSymbolicName(), pathStr), e));
            } catch (IOException e) {
                status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, String.format("Error installing initial repository from bundle %s, path %s.", repository.getFirst().getSymbolicName(), pathStr), e));
            }
        }
    }

    static void recurseInstallEntries(Bundle bundle, IPath path, IContainer root, boolean overwrite, IProgressMonitor monitor) throws IOException, CoreException {
        SubMonitor progress = SubMonitor.convert(monitor);

        if (path.hasTrailingSeparator()) {
            @SuppressWarnings("unchecked")
            Enumeration<String> childEnum = bundle.getEntryPaths(path.toString());
            List<String> children = CollectionUtils.asList(childEnum);
            progress.setWorkRemaining(1 + children.size());

            IFolder newFolder = root.getFolder(path);
            if (!newFolder.exists())
                newFolder.create(false, true, progress.newChild(1, SubMonitor.SUPPRESS_NONE));

            for (String childPath : children)
                recurseInstallEntries(bundle, new Path(childPath), root, overwrite, progress.newChild(1, SubMonitor.SUPPRESS_NONE));

        } else {
            progress.setWorkRemaining(1);
            InputStream input = bundle.getEntry(path.toString()).openStream();
            try {
                IFile newFile = root.getFile(path);
                if (newFile.exists() && overwrite)
                    newFile.setContents(input, false, true, progress.newChild(1, SubMonitor.SUPPRESS_NONE));
                else
                    newFile.create(input, false, progress.newChild(1, SubMonitor.SUPPRESS_NONE));
            } finally {
                input.close();
            }
        }
    }

    /*
    public static IStatus installImplicitRepositoryContents(boolean skipContent, MultiStatus status, IProgressMonitor monitor, RepositoryPlugin repository) throws CoreException {
        SubMonitor progress = SubMonitor.convert(monitor);

        Map<String, Version> installedVersions = loadInstalledRepositoryVersions();
        Map<String, Version> updatedVersions = new HashMap<String, Version>(installedVersions);
        boolean updated = false;

        // Copy in the implicit repository contributions
        List<ImplicitRepositoryWrapper> implicitRepos = getImplicitRepositories(status);
        if (status.getSeverity() >= IStatus.ERROR)
            return status;

        int count = implicitRepos.size();
        for (ImplicitRepositoryWrapper wrapper : implicitRepos) {
            progress.setWorkRemaining(count--);

            Version installedVersion = updatedVersions.get(wrapper.contributorBSN);
            if (installedVersion == null || wrapper.contributorVersion.compareTo(installedVersion) > 0) {
                if (!skipContent)
                    initialiseAndInstallRepository(wrapper.repository, repository, status, progress.newChild(1));
                updatedVersions.put(wrapper.contributorBSN, wrapper.contributorVersion);
                updated = true;
            }
        }

        // Save the updated versions if necessary
        try {
            if(updated) saveInstalledRepositoryVersion(updatedVersions);
        } catch (BackingStoreException e) {
            status.add(new Status(IStatus.WARNING, Plugin.PLUGIN_ID, 0, "Failed to save installed repository version information.", e));
        }
        return status;
    }
    */

    /**
     * Install a bundle from the supplied URL into the local bundle repository.
     *
     * @param localRepo The local repository
     * @param url The source URL
     * @param size The size of the remote file if known, otherwise -1
     * @param monitor A progress monitor for reporting progress of the copy, or {@code null} if progress reporting is not required.
     * @throws IOException
     * @throws CoreException
     */
    public static void installBundle(RepositoryPlugin localRepo, URL url, int size, IProgressMonitor monitor) throws IOException, CoreException {
        URLConnection connection = url.openConnection();
        if(monitor == null) monitor = new NullProgressMonitor();

        monitor.beginTask("Installing " + url.toExternalForm(), size > 0 ? size : IProgressMonitor.UNKNOWN);
        ProgressReportingInputStream stream = new ProgressReportingInputStream(connection.getInputStream(), monitor);

        try {
            Jar jar = new Jar("", stream, System.currentTimeMillis());
            jar.setDoNotTouchManifest();
            File newFile = localRepo.put(jar);

            RefreshFileJob refreshJob = new RefreshFileJob(newFile);
            if(refreshJob.isFileInWorkspace())
                refreshJob.schedule();
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
                getCnfProject().getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
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

    static void initialiseAndInstallRepository(RemoteRepository remoteRepo, RepositoryPlugin localRepo, MultiStatus status, IProgressMonitor monitor) {
        SubMonitor progress = SubMonitor.convert(monitor, 3);
        try {
            remoteRepo.initialise(progress.newChild(1));
            installRepository(remoteRepo, localRepo, status, progress.newChild(2));
        } catch (CoreException e) {
            String message = MessageFormat.format("Failed to initialise remote repository {0}.", remoteRepo.getName());
            if(status != null)
                status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, message, e));
            Plugin.logError(message, e);
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
                        installBundle(localRepo, url, -1, null);
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

    /*
    public static boolean isRepositoryUpToDate() {
        Map<String, Version> installedVersions = loadInstalledRepositoryVersions();

        List<ImplicitRepositoryWrapper> repos = getImplicitRepositories(null);
        for (ImplicitRepositoryWrapper repo : repos) {
            Version version = installedVersions.get(repo.contributorBSN);
            if(version == null)
                return false;
            else if (repo.contributorVersion != null && repo.contributorVersion.compareTo(version) > 0)
                return false;
        }

        return true;
    }
    */

    /*
    public static Map<String, Version> loadInstalledRepositoryVersions() {
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
    */

    /*
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
    */

}

