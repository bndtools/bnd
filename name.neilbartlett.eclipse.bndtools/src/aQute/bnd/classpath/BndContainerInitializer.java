package aQute.bnd.classpath;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.plugin.Activator;
import aQute.bnd.plugin.Central;

/**
 * A bnd container reads the bnd.bnd file in the project directory and use the
 * information in there to establish the classpath. The classpath is defined by
 * the -build-env instruction. This instruction contains a list of bsn's that
 * are searched in the available repositories and returned as File objects.
 * 
 * This initializer establishes the link between the container object and the
 * BndModel. The container object is just a delegator because for some unknown
 * reasons, you can only update the container (refresh the contents) when you
 * give it a new object ;-(
 * 
 * Because this plugin uses the Bnd Builder in different places, the Bnd Model
 * is centralized and available from the Activator.
 */
public class BndContainerInitializer extends ClasspathContainerInitializer
        implements ModelListener {

    public final static Path ID      = new Path("aQute.bnd.classpath.container");

    final Central            central = Activator.getDefault().getCentral();

    public BndContainerInitializer() {
        central.addModelListener(this);
    }

    /**
     * Called when a new project is found. This class is instantiated once and
     * then used for any project that has a bnd container associated. We create
     * a link between the project and the Bnd Model. A delegating container
     * object is created to link the project to the container so it can get its
     * classpath entries. Note that the container object is not stored or
     * remembered because we create a new one for every update (otherwise the
     * update is not visible for some reason)
     */
    public void initialize(IPath containerPath, IJavaProject project) throws CoreException {

        // We maintain the models in the actitvator because other
        // parts also use the model. Unfortunately, one can only
        // do this with a static method :-(
        Project model = central.getModel(project);
        if (model == null)
            throw new CoreException(new Status(IStatus.ERROR, ID.toString(), "Can not create model, likely the project does not contain a bnd.bnd file"));

        // Update the Java Model so the changes become visible.
        // Notice the unreferenced object.
        requestClasspathContainerUpdate(containerPath, project, new BndContainer(model, project));
    }

    /**
     * We can always update.
     */
    public boolean canUpdateClasspathContainer(IPath containerPath,
            IJavaProject project) {
        return true;
    }

    /**
     * Update the container. The containerSuggestion should always be a new
     * BndContainer ...
     */
    public void requestClasspathContainerUpdate(IPath containerPath,
            IJavaProject project, IClasspathContainer containerSuggestion)
            throws CoreException {

        JavaCore.setClasspathContainer(containerPath,
                new IJavaProject[] { project },
                new IClasspathContainer[] { containerSuggestion }, null);
    }

    public void modelChanged(Project model) throws Exception {
        IJavaProject project = central.getJavaProject(model);
        if (model == null || project == null) {
            System.out.println("Help! No IJavaProject for " + model);
        } else
            requestClasspathContainerUpdate(ID, project, new BndContainer(model, project));
    }

    public void workspaceChanged(Workspace ws) throws Exception {
        System.out.println("Workspace changed");
    }

}
