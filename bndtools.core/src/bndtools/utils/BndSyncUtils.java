package bndtools.utils;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;

import aQute.bnd.build.Project;
import aQute.bnd.plugin.Activator;

public class BndSyncUtils {
	
	@SuppressWarnings("deprecation")
	public static void syncProject(final IJavaProject project) {
		try {
			ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					// Refresh local resources
					project.getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
					
					// Get Bnd project model and set the generated dir to derived
					Project model = Activator.getDefault().getCentral().getModel(project);
					if(model != null) {
						String targetPath = model.getProperty("target", "generated");
						IFolder targetFolder = project.getProject().getFolder(targetPath);
						if(targetFolder != null && targetFolder.exists())
							targetFolder.setDerived(true);
					}
				}
			}, null);
		} catch (CoreException e) {
			//Plugin.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error refreshing project", e));
		}
	}
}
