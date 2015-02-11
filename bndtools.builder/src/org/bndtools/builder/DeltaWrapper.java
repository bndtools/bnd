package org.bndtools.builder;

import java.io.File;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import aQute.bnd.build.Project;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import bndtools.central.Central;

class DeltaWrapper {

    private final Project model;
    private final IResourceDelta delta;
    private final BuildLogger log;

    DeltaWrapper(Project model, IResourceDelta delta, BuildLogger log) {
        this.model = model;
        this.delta = delta;
        this.log = log;
    }

    boolean hasCnfChanged() throws Exception {
        if (delta == null) {
            log.basic("Full build because delta for cnf is null");
            return true;
        }

        if (havePropertiesChanged(model.getWorkspace())) {
            log.basic("cnf properties have changed");
            return true;
        }

        return false;
    }

    boolean hasProjectChanged() throws Exception {

        if (delta == null) {
            log.basic("Full build because delta is null");
            return true;
        }

        if (havePropertiesChanged(model)) {
            log.basic("Properties have changed (or or one of their includes %s)", model.getIncluded());
            model.refresh();
            return true;
        }

        if (haveSubBuildersChanged()) {
            return true;
        }

        if (has(model.getOutput())) {
            log.basic("The output directory has changed");
            return true;
        }

        for (Project p : model.getDependson()) {
            File f = new File(p.getTarget(), Project.BUILDFILES);
            if (has(f)) {
                log.basic("The upstream project %s has changed", p);
                return true;
            }
        }

        NavigableSet<String> files = getFiles();
        if (files.isEmpty()) {
            log.basic("No include resource files changed");
            return false;
        }

        // Add a check for resource directories
        // -resourcedependencies

        for (Builder pb : model.getSubBuilders()) {
            if (getInIncludeResource(pb, files)) {
                log.basic("The sub builder %s has changed files %s in its scope", pb, files);
                return true;
            }
        }

        return false;
    }

    boolean hasBuildfile() throws Exception {
        File f = new File(model.getTarget(), Project.BUILDFILES);
        return has(f);
    }

    private boolean haveSubBuildersChanged() throws Exception {
        for (Builder pb : model.getSubBuilders()) {
            if (havePropertiesChanged(pb)) {
                log.basic("Sub builder %s has changed properties", pb);
                return true;
            }
            File f = model.getOutputFile(pb.getBsn(), pb.getVersion());
            if (!f.isFile()) {
                log.basic("Sub builder %s has no output file %s", pb, f.getName());
                return true;
            }
        }
        return false;
    }

    private boolean havePropertiesChanged(Processor processor) throws Exception {

        if (has(processor.getPropertiesFile()))
            return true;

        List<File> included = processor.getIncluded();
        if (included == null)
            return false;

        for (File incl : included) {
            if (has(incl))
                return true;
        }

        return false;
    }

    private NavigableSet<String> getFiles() throws CoreException {
        final NavigableSet<String> files = new TreeSet<String>();
        delta.accept(new IResourceDeltaVisitor() {

            @Override
            public boolean visit(IResourceDelta delta) throws CoreException {
                if (IResourceDelta.MARKERS == delta.getFlags())
                    return false;

                if ((delta.getKind() & (IResourceDelta.ADDED | IResourceDelta.CHANGED | IResourceDelta.REMOVED)) == 0)
                    return false;

                IResource resource = delta.getResource();
                if (resource.getType() == IResource.ROOT || resource.getType() == IResource.PROJECT)
                    return true;

                if (resource.getType() == IResource.FOLDER) {
                    String path = resource.getProjectRelativePath().toString();
                    if (path.startsWith(model.getProperty(Constants.DEFAULT_PROP_SRC_DIR)) || path.startsWith(model.getProperty(Constants.DEFAULT_PROP_BIN_DIR)))
                        return false;

                    if (path.startsWith(model.getProperty(Constants.DEFAULT_PROP_TESTSRC_DIR)) || path.startsWith(model.getProperty(Constants.DEFAULT_PROP_TESTBIN_DIR)))
                        return false;

                    if (path.startsWith(model.getProperty(Constants.DEFAULT_PROP_TARGET_DIR)))
                        return false;

                    return true;
                }

                if (resource.getType() == IResource.FILE) {
                    File file = resource.getLocation().toFile();
                    files.add(file.getAbsolutePath());
                }
                return false;
            }
        });
        return files;
    }

    private boolean has(File f) throws Exception {
        if (f == null)
            return false;

        IPath path = Central.toPath(f);
        if (delta == null)
            return false;
        IPath relativePath = path.makeRelativeTo(delta.getFullPath());
        if (relativePath == null)
            return false;

        IResourceDelta delta = this.delta.findMember(relativePath);
        if (delta == null)
            return false;

        if (delta.getKind() == IResourceDelta.ADDED || delta.getKind() == IResourceDelta.CHANGED || delta.getKind() == IResourceDelta.REMOVED)
            return true;

        return false;
    }

    static Pattern IR_PATTERN = Pattern.compile("[{]?-?@?(?:[^=]+=)?\\s*([^}!]+).*");

    private boolean getInIncludeResource(Builder builder, NavigableSet<String> files) {
        Parameters includeResource = builder.getIncludeResource();
        for (Entry<String,Attrs> p : includeResource.entrySet()) {

            if (p.getValue().containsKey("literal"))
                continue;

            Matcher m = IR_PATTERN.matcher(p.getKey());
            if (m.matches()) {

                String path = builder.getFile(m.group(1)).getAbsolutePath();
                if (files.contains(path))
                    return true;

                String higher = files.higher(path);
                if (higher != null && higher.startsWith(path))
                    return true;

            }
        }
        return false;
    }

}
