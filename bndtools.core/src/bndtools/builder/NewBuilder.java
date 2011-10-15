package bndtools.builder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.resources.IFile;
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
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.lib.osgi.Builder;
import bndtools.Plugin;
import bndtools.classpath.BndContainerInitializer;

public class NewBuilder extends IncrementalProjectBuilder {

    public static final String BUILDER_ID = Plugin.PLUGIN_ID + ".bndbuilder";
    public static final String MARKER_BND_PROBLEM = Plugin.PLUGIN_ID + ".bndproblem";

    private Project model;

    @Override
    protected IProject[] build(int kind, @SuppressWarnings("rawtypes") Map args, IProgressMonitor monitor) throws CoreException {
        try {
            IProject myProject = getProject();
            Project model = Workspace.getProject(myProject.getLocation().toFile());
            if (model == null)
                return null;
            this.model = model;

            clearBuildMarkers();

            // CASE 1: CNF changed
            if (isCnfChanged()) {
                model.refresh();
                resetClasspaths();
                return calculateDependsOn();
            }

            // CASE 2: local Bnd file changed
            if (isLocalBndFileChange()) {
                model.refresh();
                if (resetClasspaths()) {
                    return calculateDependsOn();
                } else {
                    rebuild(false);
                    return calculateDependsOn();
                }
            }

            // CASE 3: JAR file in dependency project changed
            if (isDependencyTargetChange()) {
                if (resetClasspaths()) {
                    return calculateDependsOn();
                }
            }

            // CASE 4: local class file change
            rebuildIfLocalChanges();

            return calculateDependsOn();
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Build Error!", e));
        }
    }

    private boolean isRelevantLocalChange() {
        // TODO Auto-generated method stub
        return false;
    }

    private boolean isDependencyTargetChange() throws Exception {
        IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace().getRoot();
        Collection<Project> dependson = model.getDependson();

        for (Project dep : dependson) {
            File targetDir = dep.getTarget();
            if (targetDir != null && !(targetDir.isDirectory())) // Exists but is not a directory? Weird, ignore it.
                continue;

            IProject project = findOpenProject(wsroot, dep);
            if (project == null)
                Plugin.log(new Status(IStatus.WARNING, Plugin.PLUGIN_ID, 0, String.format("Dependency project '%s' from project '%s' is not in the Eclipse workspace.", dep.getName(), model.getName()), null));

            IFile buildFile = project.getFolder(targetDir.getName()).getFile(Workspace.BUILDFILES);
            IPath buildFilePath = buildFile.getProjectRelativePath();
            IResourceDelta delta = getDelta(project);
            if (!isChangeDelta(delta))
                continue;

            if (delta == null) {
                // May have changed
                return true;
            } else {
                IResourceDelta buildFileDelta = delta.findMember(buildFilePath);
                if (buildFileDelta != null && isChangeDelta(buildFileDelta))
                    return true;
            }
            // this dependency project did not change, move on to next
        }

        // no dependencies changed
        return false;
    }

    private static enum BlockingBuildErrors {
        buildpath, javac
    };

    private void rebuildIfLocalChanges() throws Exception {
        IResourceDelta delta = getDelta(getProject());
        if (delta == null) {
            // full rebuild
            rebuild(true);
        } else {
            forceRebuildForDeletions(delta);
            rebuild(false);
        }
    }

    private void forceRebuildForDeletions(IResourceDelta delta) throws Exception {
        // Find deleted files
        final Set<File> removedFiles = new HashSet<File>();
        delta.accept(new IResourceDeltaVisitor() {
            public boolean visit(IResourceDelta delta) throws CoreException {
                if (!isChangeDelta(delta))
                    return false;

                IResource resource = delta.getResource();
                if (resource.getType() == IResource.ROOT || resource.getType() == IResource.PROJECT || resource.getType() == IResource.FOLDER)
                    return true;

                if (resource.getType() == IResource.FILE) {
                    File file = resource.getLocation().toFile();
                    if ((delta.getKind() & IResourceDelta.REMOVED) != 0)
                        removedFiles.add(file);
                }

                return false;
            }
        });

        if (removedFiles.isEmpty())
            return;

        Collection<Builder> allBuilders = new ArrayList<Builder>(model.getSubBuilders());

        // Find builders that need to be built
        for (Iterator<Builder> iter = allBuilders.iterator(); iter.hasNext(); ) {
            Builder builder = iter.next();
            if (builder.isInScope(removedFiles)) {
                iter.remove();
                builder.build();
            }
        }
    }

    private static EnumSet<BlockingBuildErrors> getBlockingErrors(IProject project) {
        try {
            EnumSet<BlockingBuildErrors> result = EnumSet.noneOf(BlockingBuildErrors.class);
            IFile bndFile = project.getFile(Project.BNDFILE);
            if (containsError(bndFile.findMarkers(BndContainerInitializer.MARKER_BND_CLASSPATH_PROBLEM, true, 0)))
                result.add(BlockingBuildErrors.buildpath);
            if (containsError(project.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE)))
                result.add(BlockingBuildErrors.javac);
            return result;
        } catch (CoreException e) {
            Plugin.logError("Error looking for project problem markers", e);
            return EnumSet.noneOf(BlockingBuildErrors.class);
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

    // force is used if we detect class file deletions, which bnd would ignore
    private void rebuild(boolean force) throws Exception {
        System.out.println("REBUILDING " + model.getName() + ", force=" + force);

        model.setTrace(true);
        model.build();
        model.setTrace(false);
    }

    private boolean isLocalBndFileChange() throws CoreException {
        IResourceDelta myDelta = getDelta(getProject());

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
                    result.set(true);
                    return false;
                }

                return false;
            }

        });

        return result.get();
    }

    private static IProject findOpenProject(IWorkspaceRoot wsroot, Project model) {
        return findOpenProject(wsroot, model.getName());
    }

    private static IProject findOpenProject(IWorkspaceRoot wsroot, String name) {
        IProject project = wsroot.getProject(name);
        if (project == null || !project.exists() || !project.isOpen())
            return null;
        return project;
    }

    private IProject[] calculateDependsOn() throws Exception {
        Collection<Project> dependsOn = model.getDependson();
        List<IProject> result = new ArrayList<IProject>(dependsOn.size() + 1);

        result.add(findCnfProject());

        IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace().getRoot();
        for (Project project : dependsOn) {
            IProject targetProj = findOpenProject(wsroot, project);
            if (targetProj == null)
                Plugin.log(new Status(IStatus.WARNING, Plugin.PLUGIN_ID, 0, "No open project in workspace for Bnd '-dependson' dependency: " + project.getName(), null));
            else
                result.add(targetProj);
        }

        return result.toArray(new IProject[result.size()]);
    }

    private boolean resetClasspaths() throws CoreException {
        IJavaProject javaProject = JavaCore.create(getProject());

        IClasspathContainer container = JavaCore.getClasspathContainer(BndContainerInitializer.PATH_ID, javaProject);
        List<IClasspathEntry> currentClasspath = Arrays.asList(container.getClasspathEntries());

        List<IClasspathEntry> newClasspath = BndContainerInitializer.calculateProjectClasspath(model, javaProject, false);

        if (!newClasspath.equals(currentClasspath)) {
            BndContainerInitializer.setClasspathEntries(javaProject, newClasspath.toArray(new IClasspathEntry[newClasspath.size()]));
            return true;
        }
        return false;
    }

    boolean isCnfChanged() throws Exception {
        IProject cnfProject = findCnfProject();
        if (cnfProject == null) {
            Plugin.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Bnd configuration project (cnf) is not available in the Eclipse workspace.", null));
            return false;
        }

        IResourceDelta cnfDelta = getDelta(cnfProject);
        if (cnfDelta == null)
            return false;

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
                    result.set(true);
                }

                if (resource.getType() == IResource.FILE) {
                    if (Workspace.BUILDFILE.equals(resource.getName())) {
                        result.set(true);
                    } else {
                        // TODO: check other file names included from build.bnd
                    }
                }

                return false;
            }
        });

        return result.get();
    }

    private static IProject findCnfProject() throws Exception {
        return findOpenProject(ResourcesPlugin.getWorkspace().getRoot(), "cnf");
    }

    private static boolean isChangeDelta(IResourceDelta delta) {
        if (IResourceDelta.MARKERS == delta.getFlags())
            return false;
        if ((delta.getKind() & (IResourceDelta.ADDED | IResourceDelta.CHANGED | IResourceDelta.REMOVED)) == 0)
            return false;
        return true;
    }

    private void clearBuildMarkers() throws CoreException {
        IFile bndFile = getProject().getFile(Project.BNDFILE);

        if (bndFile.exists()) {
            bndFile.deleteMarkers(MARKER_BND_PROBLEM, true, IResource.DEPTH_INFINITE);
        }
    }



}
