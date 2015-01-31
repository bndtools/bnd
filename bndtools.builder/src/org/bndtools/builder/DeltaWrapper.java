package org.bndtools.builder;

import java.io.File;
import java.util.Set;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Processor;

class DeltaWrapper {

    private final Project model;
    private final IResourceDelta delta;

    public DeltaWrapper(Project model, IResourceDelta delta) {
        this.model = model;
        this.delta = delta;
    }

    public boolean hasCnfChanged() {
        if (delta == null)
            return true;

        if (havePropertiesChanged(model.getWorkspace()))
            return true;

        File files[] = model.getFile(Workspace.EXT).listFiles();
        for (File f : files) {
            if (has(f))
                return true;
        }

        return false;
    }

    public boolean hasProjectChanged() throws Exception {

        if (delta == null || havePropertiesChanged(model) || haveSubBuildersChanged()) {
            model.refresh();
            return true;
        }

        if (has(model.getOutput()))
            return true;

        Set<File> files = getFiles();

        for (Builder pb : model.getSubBuilders()) {
            if (pb.isInScope(files))
                return true;

            File f = model.getOutputFile(pb.getBsn(), pb.getVersion());
            if (!f.isFile())
                return true;
        }

        return false;
    }

    private boolean haveSubBuildersChanged() throws Exception {
        for (Builder pb : model.getSubBuilders()) {
            if (havePropertiesChanged(pb)) {
                model.refresh();
                return true;
            }
        }
        return false;
    }

    private boolean havePropertiesChanged(Processor processor) {

        if (has(processor.getPropertiesFile()))
            return true;

        for (File incl : model.getIncluded()) {
            if (has(incl))
                return true;
        }

        return false;
    }

    private Set<File> getFiles() {
        // TODO Auto-generated method stub
        return null;
    }

    private boolean has(File f) {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * Report the full delta in the log
     */
    void reportDelta(final NotYetDoneBuilder blder) throws CoreException {
        if (delta == null) {
            blder.log(NotYetDoneBuilder.LOG_FULL, "No delta, is null");
            return;
        }

        delta.accept(new IResourceDeltaVisitor() {

            @Override
            public boolean visit(IResourceDelta delta) throws CoreException {
                blder.log(NotYetDoneBuilder.LOG_FULL, delta.toString() + " " + delta.getResource().getModificationStamp() + " " + delta.getKind());
                return true;
            }
        });
    }

    //    private static boolean isChangeDelta(IResourceDelta delta) {
    //        if (IResourceDelta.MARKERS == delta.getFlags())
    //            return false;
    //        if ((delta.getKind() & (IResourceDelta.ADDED | IResourceDelta.CHANGED | IResourceDelta.REMOVED)) == 0)
    //            return false;
    //        return true;
    //    }
    //
    //    private static File getFileForPath(IPath path) {
    //        File file;
    //        IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
    //        if (resource != null && resource.exists())
    //            file = resource.getLocation().toFile();
    //        else
    //            file = path.toFile();
    //        return file;
    //    }
    //
    //    private boolean isLocalBndFileChange() throws CoreException {
    //        IResourceDelta myDelta = getDelta(getProject());
    //        if (myDelta == null) {
    //            log(LOG_BASIC, "local project delta is null, assuming changes exist", model.getName());
    //            return true;
    //        }
    //
    //        final AtomicBoolean result = new AtomicBoolean(false);
    //        myDelta.accept(new IResourceDeltaVisitor() {
    //            @Override
    //            public boolean visit(IResourceDelta delta) throws CoreException {
    //                if (!isChangeDelta(delta))
    //                    return false;
    //
    //                IResource resource = delta.getResource();
    //
    //                if (resource.getType() == IResource.ROOT || resource.getType() == IResource.PROJECT)
    //                    return true;
    //
    //                if (resource.getType() == IResource.FOLDER)
    //                    return true;
    //
    //                String extension = ((IFile) resource).getFileExtension();
    //                if (resource.getType() == IResource.FILE && "bnd".equalsIgnoreCase(extension)) {
    //                    log(LOG_FULL, "detected change due to resource %s, kind=0x%x, flags=0x%x", resource.getFullPath(), delta.getKind(), delta.getFlags());
    //                    result.set(true);
    //                    return false;
    //                }
    //
    //                if (resource.getType() == IResource.FILE) {
    //                    // Check files included by the -include directive in bnd.bnd
    //                    List<File> includedFiles = model.getIncluded();
    //                    if (includedFiles == null) {
    //                        return false;
    //                    }
    //                    for (File includedFile : includedFiles) {
    //                        IPath location = resource.getLocation();
    //                        if (location != null && includedFile.equals(location.toFile())) {
    //                            result.set(true);
    //                            return false;
    //                        }
    //                    }
    //                }
    //
    //                return false;
    //            }
    //
    //        });
    //
    //        return result.get();
    //    }
    //
    //    private Project getDependencyTargetChange() throws Exception {
    //        IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace().getRoot();
    //        Collection<Project> dependson = model.getDependson();
    //        log(LOG_FULL, "project depends on: %s", dependson);
    //
    //        for (Project dep : dependson) {
    //            File targetDir = dep.getTarget();
    //            // Does not exist... was it deleted?
    //            if (targetDir == null || !(targetDir.isDirectory()))
    //                return dep;
    //
    //            IProject project = WorkspaceUtils.findOpenProject(wsroot, dep);
    //            if (project == null) {
    //                logger.logWarning(String.format("Dependency project '%s' from project '%s' is not in the Eclipse workspace.", dep.getName(), model.getName()), null);
    //                return null;
    //            }
    //
    //            IFile buildFile = project.getFolder(targetDir.getName()).getFile(Workspace.BUILDFILES);
    //            IPath buildFilePath = buildFile.getProjectRelativePath();
    //            IResourceDelta delta = getDelta(project);
    //
    //            if (delta == null) {
    //                // May have changed
    //                log(LOG_FULL, "null delta in dependency project %s", dep.getName());
    //                return dep;
    //            } else if (!isChangeDelta(delta)) {
    //                continue;
    //            } else {
    //                IResourceDelta buildFileDelta = delta.findMember(buildFilePath);
    //                if (buildFileDelta != null && isChangeDelta(buildFileDelta)) {
    //                    log(LOG_FULL, "detected change due to file %s, kind=0x%x, flags=0x%x", buildFile, delta.getKind(), delta.getFlags());
    //                    return dep;
    //                }
    //            }
    //            // this dependency project did not change, move on to next
    //        }
    //
    //        // no dependencies changed
    //        return null;
    //    }
    //

}
