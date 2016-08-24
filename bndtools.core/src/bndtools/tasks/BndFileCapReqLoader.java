package bndtools.tasks;

import java.io.File;
import java.io.IOException;

import org.bndtools.utils.workspace.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.JavaCore;

import aQute.bnd.build.Project;
import aQute.bnd.osgi.Builder;
import bndtools.central.Central;

public class BndFileCapReqLoader extends BndBuilderCapReqLoader {

    private Builder builder;

    public BndFileCapReqLoader(File bndFile) {
        super(bndFile);
    }

    @Override
    protected synchronized Builder getBuilder() throws Exception {
        if (builder == null) {
            Builder b;

            IFile[] wsfiles = FileUtils.getWorkspaceFiles(file);
            if (wsfiles == null || wsfiles.length == 0)
                throw new Exception("Unable to determine project owner for Bnd file: " + file.getAbsolutePath());

            IProject project = wsfiles[0].getProject();

            // Calculate the manifest
            Project bndProject = Central.getInstance().getModel(JavaCore.create(project));
            if (bndProject == null)
                return null;
            if (file.getName().equals(Project.BNDFILE)) {
                b = bndProject.getSubBuilders().iterator().next();
            } else {
                b = bndProject.getSubBuilder(file);
            }

            if (b == null) {
                b = new Builder();
                b.setProperties(file);
            }
            b.build();

            builder = b;
        }
        return builder;
    }

    @Override
    public synchronized void close() throws IOException {
        if (builder != null)
            builder.close();
        builder = null;
    }

}
