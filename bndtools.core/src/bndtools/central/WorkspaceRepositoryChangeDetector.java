package bndtools.central;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bndtools.utils.workspace.WorkspaceUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import aQute.bnd.build.Workspace;
import aQute.bnd.build.WorkspaceRepository;
import aQute.bnd.service.RepositoryListenerPlugin;

public class WorkspaceRepositoryChangeDetector implements Closeable, IResourceChangeListener {

	private final Workspace				workspace;
	private final IWorkspace			iworkspace;
	private final WorkspaceRepository	repository;
	private final IProject				cnfProject;

	private final AtomicBoolean			refresh	= new AtomicBoolean();

	class RootFolderVisitor implements IResourceDeltaVisitor {
		ProjectFolderVisitor projectFolder = new ProjectFolderVisitor();

		class ProjectFolderVisitor implements IResourceDeltaVisitor {

			@Override
			public boolean visit(IResourceDelta delta) throws CoreException {
				if (refresh.get())
					return false;

				switch (delta.getResource()
					.getType()) {
					case IResource.FILE : // project folder
						String fileExtension = delta.getResource()
							.getFileExtension();
						if ("bnd".equals(fileExtension)) {
							refresh.set(true);
						}
						return false;
					case IResource.FOLDER :
						if (cnfProject == null)
							return false;
						return delta.getResource()
							.getParent()
							.getType() == IResource.PROJECT && delta.getResource()
								.getParent()
								.equals(cnfProject)
							&& delta.getResource()
								.getName()
								.equals("ext");
					default :
						return false;
				}
			}

		}

		@Override
		public boolean visit(IResourceDelta delta) throws CoreException {
			if (refresh.get())
				return false;

			switch (delta.getResource()
				.getType()) {
				case IResource.ROOT :
					return true;

				case IResource.PROJECT : // project folder

					if (delta.getKind() == IResourceDelta.ADDED || delta.getKind() == IResourceDelta.REMOVED) {
						refresh.set(true);
						workspace.refreshProjects();
						return false;
					}
					for (IResourceDelta subDelta : delta
						.getAffectedChildren(IResourceDelta.ADDED | IResourceDelta.REMOVED | IResourceDelta.CHANGED))
						subDelta.accept(projectFolder);
					return false;

				default :
					return false;
			}
		}

	}

	public WorkspaceRepositoryChangeDetector(Workspace workspace) {
		this.workspace = workspace;
		this.repository = workspace.getWorkspaceRepository();
		this.iworkspace = ResourcesPlugin.getWorkspace();

		IProject cnf = null;

		try {
			cnf = WorkspaceUtils.findCnfProject(iworkspace.getRoot(), workspace);
		} catch (Exception ex) {}

		this.cnfProject = cnf;

		iworkspace.addResourceChangeListener(this);
		workspace.addClose(this);
	}

	@Override
	public void close() throws IOException {
		iworkspace.removeResourceChangeListener(this);
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		try {
			if (refresh.get())
				return;

			if (event.getType() == IResourceChangeEvent.POST_CHANGE) {

				RootFolderVisitor rootFolderVisitor = new RootFolderVisitor();
				event.getDelta()
					.accept(rootFolderVisitor);

				if (refresh.getAndSet(false)) {
					WorkspaceJob job = new WorkspaceJob("Refresh Workspace Repository") {
						@Override
						public IStatus runInWorkspace(IProgressMonitor monitor) {
							if (monitor == null)
								monitor = new NullProgressMonitor();
							List<RepositoryListenerPlugin> plugins = workspace
								.getPlugins(RepositoryListenerPlugin.class);
							monitor.beginTask("Refresh ", plugins.size());
							int n = 0;
							for (RepositoryListenerPlugin rlp : plugins)
								try {
									monitor.worked(n++);
									rlp.repositoryRefreshed(repository);
								} catch (Exception e) {
									e.printStackTrace();
								}
							monitor.done();
							return Status.OK_STATUS;
						}
					};
					job.schedule(500);
				}
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
