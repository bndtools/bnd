package bndtools.builder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bndtools.core.utils.workspace.WorkspaceUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IJavaModelMarker;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.lib.io.IO;
import aQute.lib.osgi.Builder;
import bndtools.Central;
import bndtools.Plugin;
import bndtools.classpath.BndContainerInitializer;

public class NewBuilder extends IncrementalProjectBuilder {

    public static final String BUILDER_ID = Plugin.PLUGIN_ID + ".bndbuilder";
    public static final String MARKER_BND_PROBLEM = Plugin.PLUGIN_ID + ".bndproblem";

    private static final int LOG_FULL = 2;
    private static final int LOG_BASIC = 1;
    private static final int LOG_NONE = 0;

    private Project model;

    private List<String> classpathErrors;

    private List<String> buildLog;
    private int logLevel = LOG_FULL; // TODO: load this from some config??

    @Override
    protected IProject[] build(int kind, @SuppressWarnings("rawtypes") Map args, IProgressMonitor monitor) throws CoreException {
        SubMonitor progress = SubMonitor.convert(monitor, "Building bundles", 0);

        classpathErrors = new LinkedList<String>();
        buildLog = new ArrayList<String>(5);

        // Initialise workspace OBR index (should only happen once)
        boolean builtAny = false;

        try {
            IProject myProject = getProject();
            Project model = Workspace.getProject(myProject.getLocation().toFile());
            if (model == null)
                return null;
            this.model = model;

            model.clear(); // Clear errors and warnings

            // CASE 1: CNF changed
            if (isCnfChanged()) {
                log(LOG_BASIC, "cnf project changed");
                model.refresh();
                if (BndContainerInitializer.resetClasspaths(model, myProject, classpathErrors)) {
                    log(LOG_BASIC, "classpaths were changed");
                } else {
                    log(LOG_FULL, "classpaths did not need to change");
                }
                return calculateDependsOn();
            }

            // CASE 2: local Bnd file changed, or Eclipse asks for full build
            boolean localChange = false;
            if (kind == FULL_BUILD) {
                localChange = true;
                log(LOG_BASIC, "Eclipse requested full build");
            } else if (isLocalBndFileChange()) {
                localChange = true;
                log(LOG_BASIC, "local bnd files changed");
            }
            if (localChange) {
                model.refresh();
                if (BndContainerInitializer.resetClasspaths(model, myProject, classpathErrors)) {
                    log(LOG_BASIC, "classpaths were changed");
                    return calculateDependsOn();
                } else {
                    log(LOG_FULL, "classpaths were not changed");
                    rebuildIfLocalChanges();
                    return calculateDependsOn();
                }
            }
            // (NB: from now on the delta cannot be null, due to the check in isLocalBndFileChange)

            // CASE 3: JAR file in dependency project changed
            Project changedDependency = getDependencyTargetChange();
            if (changedDependency != null) {
                log(LOG_BASIC, "target files in dependency project %s changed", changedDependency.getName());
                model.propertiesChanged();
                if (BndContainerInitializer.resetClasspaths(model, myProject, classpathErrors)) {
                    log(LOG_BASIC, "classpaths were changed");
                    return calculateDependsOn();
                } else {
                    log(LOG_FULL, "classpaths were not changed");
                }
            }

            // CASE 4: local file changes
            builtAny = rebuildIfLocalChanges();

            return calculateDependsOn();
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Build Error!", e));
        } finally {
            if (!builtAny) {
                try {
                    Central.getWorkspaceObrProvider().initialise();
                } catch (Exception e) {
                    Plugin.logError("Error initialising workspace OBR provider", e);
                }
            }

            if (!buildLog.isEmpty()) {
                System.err.println(String.format("==> BUILD LOG for project %s follows (%d entries):", getProject(), buildLog.size()));
                for (String message : buildLog) {
                    StringBuilder builder = new StringBuilder().append("    ").append(message);
                    System.err.println(builder.toString());
                }
            }
        }
    }

    @Override
    protected void clean(IProgressMonitor monitor) throws CoreException {
        try {
            IProject myProject = getProject();
            Project model = Workspace.getProject(myProject.getLocation().toFile());
            if (model == null)
                return;

            // Delete everything in the target directory
            File target = model.getTarget();
            if (target.isDirectory() && target.getParentFile() != null) {
                IO.delete(target);
                target.mkdirs();
            }

            // Tell Eclipse what we did...
            IFolder targetFolder = myProject.getFolder(calculateTargetDirPath(model));
            targetFolder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Build Error!", e));
        }
    }

    boolean isCnfChanged() throws Exception {
        IProject cnfProject = WorkspaceUtils.findCnfProject();
        if (cnfProject == null) {
            Plugin.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Bnd configuration project (cnf) is not available in the Eclipse workspace.", null));
            return false;
        }

        IResourceDelta cnfDelta = getDelta(cnfProject);
        if (cnfDelta == null) {
            log(LOG_FULL, "no delta available for cnf project, ignoring");
            return false;
        }

        final AtomicBoolean result = new AtomicBoolean(false);
        cnfDelta.accept(new IResourceDeltaVisitor() {
            public boolean visit(IResourceDelta delta) throws CoreException {
                if (!isChangeDelta(delta))
                    return false;

                if (IResourceDelta.MARKERS == delta.getFlags())
                    return false;

                IResource resource = delta.getResource();
                if (resource.getType() == IResource.ROOT || resource.getType() == IResource.PROJECT)
                    return true;

                if (resource.getType() == IResource.FOLDER && resource.getName().equals("ext")) {
                    log(LOG_FULL, "detected change in cnf due to resource %s, kind=0x%x, flags=0x%x", resource.getFullPath(), delta.getKind(), delta.getFlags());
                    result.set(true);
                }

                if (resource.getType() == IResource.FILE) {
                    if (Workspace.BUILDFILE.equals(resource.getName())) {
                        result.set(true);
                        log(LOG_FULL, "detected change in cnf due to resource %s, kind=0x%x, flags=0x%x", resource.getFullPath(), delta.getKind(), delta.getFlags());
                    } else {
                        // TODO: check other file names included from build.bnd
                    }
                }

                return false;
            }
        });

        return result.get();
    }

    private boolean isLocalBndFileChange() throws CoreException {
        IResourceDelta myDelta = getDelta(getProject());
        if (myDelta == null) {
            log(LOG_BASIC, "local project delta is null, assuming changes exist", model.getName());
            return true;
        }

        final AtomicBoolean result = new AtomicBoolean(false);
        myDelta.accept(new IResourceDeltaVisitor() {
            public boolean visit(IResourceDelta delta) throws CoreException {
                if (!isChangeDelta(delta))
                    return false;

                IResource resource = delta.getResource();

                if (resource.getType() == IResource.ROOT || resource.getType() == IResource.PROJECT)
                    return true;

                if (resource.getType() == IResource.FOLDER)
                    return true;

                String extension = ((IFile) resource).getFileExtension();
                if (resource.getType() == IResource.FILE && "bnd".equalsIgnoreCase(extension)) {
                    log(LOG_FULL, "detected change due to resource %s, kind=0x%x, flags=0x%x", resource.getFullPath(), delta.getKind(), delta.getFlags());
                    result.set(true);
                    return false;
                }

                return false;
            }

        });

        return result.get();
    }

    private Project getDependencyTargetChange() throws Exception {
        IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace().getRoot();
        Collection<Project> dependson = model.getDependson();
        log(LOG_FULL, "project depends on: %s", dependson);

        for (Project dep : dependson) {
            File targetDir = dep.getTarget();
            if (targetDir != null && !(targetDir.isDirectory())) // Does not exist... deleted?
                return dep;

            IProject project = WorkspaceUtils.findOpenProject(wsroot, dep);
            if (project == null) {
                Plugin.log(new Status(IStatus.WARNING, Plugin.PLUGIN_ID, 0, String.format("Dependency project '%s' from project '%s' is not in the Eclipse workspace.", dep.getName(), model.getName()), null));
                return null;
            }

            IFile buildFile = project.getFolder(targetDir.getName()).getFile(Workspace.BUILDFILES);
            IPath buildFilePath = buildFile.getProjectRelativePath();
            IResourceDelta delta = getDelta(project);

            if (delta == null) {
                // May have changed
                log(LOG_FULL, "null delta in dependency project %s", dep.getName());
                return dep;
            } else if (!isChangeDelta(delta)) {
                continue;
            } else {
                IResourceDelta buildFileDelta = delta.findMember(buildFilePath);
                if (buildFileDelta != null && isChangeDelta(buildFileDelta)) {
                    log(LOG_FULL, "detected change due to file %s, kind=0x%x, flags=0x%x", buildFile, delta.getKind(), delta.getFlags());
                    return dep;
                }
            }
            // this dependency project did not change, move on to next
        }

        // no dependencies changed
        return null;
    }

    /**
     * @return Whether any files were built
     */
    private boolean rebuildIfLocalChanges() throws Exception {
        log(LOG_FULL, "calculating local changes...");

        final Set<File> removedFiles = new HashSet<File>();
        final Set<File> changedFiles = new HashSet<File>();

        final IPath targetDirPath = calculateTargetDirPath(model);

        boolean force = false;

        IResourceDelta delta = getDelta(getProject());
        if (delta != null) {
            delta.accept(new IResourceDeltaVisitor() {
                public boolean visit(IResourceDelta delta) throws CoreException {
                    if (!isChangeDelta(delta))
                        return false;

                    IResource resource = delta.getResource();
                    if (resource.getType() == IResource.ROOT || resource.getType() == IResource.PROJECT)
                        return true;

                    if (resource.getType() == IResource.FOLDER) {
                        IPath folderPath = resource.getProjectRelativePath();
                        // ignore ALL files in target dir
                        return !folderPath.equals(targetDirPath);
                    }

                    if (resource.getType() == IResource.FILE) {
                        File file = resource.getLocation().toFile();
                        if ((delta.getKind() & IResourceDelta.REMOVED) != 0) {
                            removedFiles.add(file);
                        } else {
                            changedFiles.add(file);
                        }
                    }

                    return false;
                }
            });
            log(LOG_FULL, "%d local files (outside target) removed: %s", removedFiles.size(), removedFiles);
            log(LOG_FULL, "%d local files (outside target) changed: %s", changedFiles.size(), changedFiles);
        } else {
            log(LOG_BASIC, "no info on local changes available");
        }

        // Process the sub-builders to determine whether a rebuild, force rebuild, or nothing is required.
        for (Builder builder : model.getSubBuilders()) {
            // If the builder's output JAR has been removed, this could be because the user
            // deleted it, so we should force build in order to regenerate it.
            File targetFile = new File(model.getTarget(), builder.getBsn() + ".jar");
            if (!targetFile.isFile()) {
                log(LOG_FULL, "output file %s of builder %s was removed, will force a rebuild", targetFile, builder.getBsn());
                force = true;
                break;
            }

            // Finally if any removed files are in scope for the bundle, we must force it to rebuild
            // because bnd will not notice the deletion
            if (!removedFiles.isEmpty() && builder.isInScope(removedFiles)) {
                log(LOG_FULL, "some removed files were in scope for builder %s, will force a rebuild", builder.getBsn());
                force = true;
                break;
            }
        }

        // Do it
        boolean builtAny = false;
        if (force) {
            builtAny = rebuild(true);
        } else if (!changedFiles.isEmpty()) {
            builtAny = rebuild(false);
        }
        return builtAny;
    }

    private static IPath calculateTargetDirPath(Project model) throws Exception {
        IPath basePath = Path.fromOSString(model.getBase().getAbsolutePath());
        final IPath targetDirPath = Path.fromOSString(model.getTarget().getAbsolutePath()).makeRelativeTo(basePath);
        return targetDirPath;
    }

    /**
     * @param force Whether to force bnd to build
     * @return Whether any files were built
     */
    private boolean rebuild(boolean force) throws Exception {
        clearBuildMarkers();

        // Abort build if compilation errors exist
        if (hasBlockingErrors()) {
            addBuildMarker(String.format("Will not build OSGi bundle(s) for project %s until compilation problems are fixed.", model.getName()), IMarker.SEVERITY_ERROR);
            log(LOG_BASIC, "SKIPPING due to Java problem markers");
            return false;
        } else if (!classpathErrors.isEmpty()) {
            addBuildMarker("Will not build OSGi bundle(s) for project %s until classpath resolution problems are fixed.", IMarker.SEVERITY_ERROR);
            log(LOG_BASIC, "SKIPPING due to classpath resolution problem markers");
            return false;
        }

        log(LOG_BASIC, "REBUILDING, force=%b", force);

        File[] built;

        // Build!
        model.clear();
        model.setTrace(true);
        if (force)
            built = model.buildLocal(false);
        else
            built = model.build();
        if (built == null) built = new File[0];

        // Log rebuilt files
        log(LOG_BASIC, "requested rebuild of %d files", built.length);
        if (logLevel >= LOG_FULL) {
            for (File builtFile : built) {
                log(LOG_FULL, "target file %s has an age of %d ms", builtFile, System.currentTimeMillis() - builtFile.lastModified());
            }
        }

        // Make sure Eclipse knows about the changed files (should already have been done?)
        IFolder targetFolder = getProject().getFolder(calculateTargetDirPath(model));
        targetFolder.refreshLocal(IResource.DEPTH_INFINITE, null);

        // Replace into the workspace OBR provider
        if (built.length > 0) {
            try {
                Central.getWorkspaceObrProvider().replaceProjectFiles(model, built);
            } catch (Exception e) {
                Plugin.logError("Error rebuilding workspace OBR index", e);
            }
        }

        // Report errors
        List<String> errors = new ArrayList<String>(model.getErrors());
        List<String> warnings = new ArrayList<String>(model.getWarnings());
        createBuildMarkers(errors, warnings);

        return built.length > 0;
    }

    private IProject[] calculateDependsOn() throws Exception {
        Collection<Project> dependsOn = model.getDependson();
        List<IProject> result = new ArrayList<IProject>(dependsOn.size() + 1);

        result.add(WorkspaceUtils.findCnfProject());

        IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace().getRoot();
        for (Project project : dependsOn) {
            IProject targetProj = WorkspaceUtils.findOpenProject(wsroot, project);
            if (targetProj == null)
                Plugin.log(new Status(IStatus.WARNING, Plugin.PLUGIN_ID, 0, "No open project in workspace for Bnd '-dependson' dependency: " + project.getName(), null));
            else
                result.add(targetProj);
        }

        log(LOG_FULL, "returning depends-on list: %s", result);
        return result.toArray(new IProject[result.size()]);
    }

    private boolean hasBlockingErrors() {
        try {
            if (containsError(getProject().findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE)))
                return true;
            return false;
        } catch (CoreException e) {
            Plugin.logError("Error looking for project problem markers", e);
            return false;
        }
    }

    private static boolean containsError(IMarker[] markers) {
        if (markers != null)
            for (IMarker marker : markers) {
                int severity = marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
                if (severity == IMarker.SEVERITY_ERROR)
                    return true;
            }
        return false;
    }

    private static boolean isChangeDelta(IResourceDelta delta) {
        if (IResourceDelta.MARKERS == delta.getFlags())
            return false;
        if ((delta.getKind() & (IResourceDelta.ADDED | IResourceDelta.CHANGED | IResourceDelta.REMOVED)) == 0)
            return false;
        return true;
    }

    private void createBuildMarkers(Collection<? extends String> errors, Collection<? extends String> warnings) throws CoreException {
        for (String error : errors) {
            addBuildMarker(error, IMarker.SEVERITY_ERROR);
        }
        for (String warning : warnings) {
            addBuildMarker(warning, IMarker.SEVERITY_WARNING);
        }
        for (String error : classpathErrors) {
            addClasspathMarker(error, IMarker.SEVERITY_ERROR);
        }
    }

    private void clearBuildMarkers() throws CoreException {
        IFile bndFile = getProject().getFile(Project.BNDFILE);

        if (bndFile.exists()) {
            bndFile.deleteMarkers(MARKER_BND_PROBLEM, true, IResource.DEPTH_INFINITE);
        }
    }

    private IResource getBuildMarkerTargetResource() {
        IProject project = getProject();
        IResource bndFile = project.getFile(Project.BNDFILE);
        if (bndFile == null || !bndFile.exists())
            return project;
        return bndFile;
    }

    private void addBuildMarker(String message, int severity) throws CoreException {
        IResource resource = getBuildMarkerTargetResource();

        IMarker marker = resource.createMarker(MARKER_BND_PROBLEM);
        marker.setAttribute(IMarker.SEVERITY, severity);
        marker.setAttribute(IMarker.MESSAGE, message);
        // marker.setAttribute(IMarker.LINE_NUMBER, 1);
    }

    private void addClasspathMarker(String message, int severity) throws CoreException {
        IResource resource = getBuildMarkerTargetResource();

        IMarker marker = resource.createMarker(BndContainerInitializer.MARKER_BND_CLASSPATH_PROBLEM);
        marker.setAttribute(IMarker.SEVERITY, severity);
        marker.setAttribute(IMarker.MESSAGE, message);
        // marker.setAttribute(IMarker.LINE_NUMBER, 1);
    }

    private void log(int level, String message, Object... args) {
        if (logLevel >= level)
            buildLog.add(String.format(message, args));
    }

}
