package aQute.bnd.classpath;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import bndtools.Plugin;

import aQute.bnd.build.Container;
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

    public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
        // We maintain the models in the activator because other
        // parts also use the model. Unfortunately, one can only
        // do this with a static method :-(
        Project model = central.getModel(project);
        if (model == null)
            throw new CoreException(new Status(IStatus.ERROR, ID.toString(), "Can not create model, likely the project does not contain a bnd.bnd file"));

        // Update the Java Model so the changes become visible.
        // Notice the unreferenced object.
        requestClasspathContainerUpdate(containerPath, project, new BndContainer(model, project, calculateEntries(model)));
    }

    public boolean canUpdateClasspathContainer(IPath containerPath, IJavaProject project) {
        return true;
    }

    public void requestClasspathContainerUpdate(IPath containerPath, IJavaProject project, IClasspathContainer containerSuggestion) throws CoreException {
        JavaCore.setClasspathContainer(containerPath, new IJavaProject[] { project }, new IClasspathContainer[] { containerSuggestion }, null);
    }

    public void modelChanged(Project model) throws Exception {
        IJavaProject project = central.getJavaProject(model);
        if (model == null || project == null) {
            System.out.println("Help! No IJavaProject for " + model);
        } else {
            requestClasspathContainerUpdate(ID, project, new BndContainer(model, project, calculateEntries(model)));
        }
    }

    public void workspaceChanged(Workspace ws) throws Exception {
        System.out.println("Workspace changed");
    }
    
	IClasspathEntry[] calculateEntries(Project project) {
		
		Collection<Container> buildpath;
		try {
			buildpath = project.getBuildpath();
		} catch (Exception e) {
			Plugin.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error getting project build path.", e));
			buildpath = Collections.emptyList();
		}
		Collection<Container> bootclasspath;
		try {
			bootclasspath = project.getBootclasspath();
		} catch (Exception e) {
			Plugin.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error getting project boot classpath.", e));
			bootclasspath = Collections.emptyList();
		}
		
		List<Container> entries = new ArrayList<Container>(buildpath.size() + bootclasspath.size());
		entries.addAll(buildpath);

		// The first file is always the project directory,
		// Eclipse already includes that for us.
		if (entries.size() > 0) {
			entries.remove(0);
		}
		
		entries.addAll(bootclasspath);

		ArrayList<IClasspathEntry> result = new ArrayList<IClasspathEntry>(entries.size());
		for (Container c : entries) {
			IClasspathEntry cpe;
			IPath sourceAttachment = null;

			if (c.getError() == null) {
				File file = c.getFile();
				assert file.isAbsolute();

				IPath p = Central.toPath(project, file);
				// JDT seems to ignore files when they
				// are outside the workspace
				if (p == null)
					p = Path.fromOSString(file.getAbsolutePath());
				try {
					Central.refresh(p);
				} catch (Throwable e) {

				}
				if (c.getType() == Container.TYPE.PROJECT) {
					File sourceDir = c.getProject().getSrc();
					if (sourceDir.isDirectory())
						sourceAttachment = Central.toPath(c
								.getProject(), sourceDir);
				}

				cpe = JavaCore.newLibraryEntry(p, sourceAttachment, null);
				result.add(cpe);
			}
		}
		return result.toArray(new IClasspathEntry[result.size()]);
	}
}