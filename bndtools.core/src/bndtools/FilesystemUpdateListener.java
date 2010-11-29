package bndtools;

import java.io.File;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import aQute.bnd.service.BndListener;

public final class FilesystemUpdateListener extends BndListener {

		void createFolderAndParents(IFolder folder, boolean force) throws CoreException {
			IContainer parent = folder.getParent();
			if(parent == null) {
				throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Cannot create the workspace root", null));
			}

			if(!parent.exists()) {
				if(!(parent instanceof IFolder)) {
					throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Cannot create the parent, it is not a regular folder.", null));
				}
				createFolderAndParents((IFolder) parent, force);
			}

			folder.create(force, true, null);
		}
		@Override
        public void changed(final File file) {
		    RefreshFileJob job = new RefreshFileJob(file);
			if(job.isFileInWorkspace()) {
			    job.schedule();
			}
		}
	}