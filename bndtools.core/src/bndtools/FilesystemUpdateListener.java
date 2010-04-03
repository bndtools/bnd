package bndtools;

import java.io.File;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import aQute.bnd.service.BndListener;

final class FilesystemUpdateListener extends BndListener {
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
		public void changed(final File file) {
			System.err.println("--> Changed file: " + file.toString());
			IPath changedPath = new Path(file.toString());
			
			final IWorkspace workspace = ResourcesPlugin.getWorkspace();
			final IWorkspaceRoot workspaceRoot = workspace.getRoot();
			IPath workspacePath = workspaceRoot.getLocation();
			
			if(workspacePath.isPrefixOf(changedPath)) {
				final IPath relativeChangedPath = changedPath.removeFirstSegments(workspacePath.segmentCount());
				WorkspaceJob job = new WorkspaceJob("update") {
					public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
						IResource resource;
						if(file.isDirectory()) {
							resource = workspaceRoot.getFolder(relativeChangedPath);
						} else {
							resource = workspaceRoot.getFile(relativeChangedPath);
						}
						resource.refreshLocal(0, monitor);
						resource.setDerived(true);
						return Status.OK_STATUS;
					}
				};
				job.schedule();
				
//				try {
//					final IWorkspaceRunnable operation = new IWorkspaceRunnable() {
//						public void run(IProgressMonitor monitor) throws CoreException {
//							try {
//								if(file.isDirectory()) {
//									// Folders can only be created or deleted
//									IFolder folder = workspaceRoot.getFolder(relativeChangedPath);
//									if(file.exists()) {
//										createFolderAndParents(folder, true);
//									} else {
//										folder.delete(true, null);
//									}
//								} else {
//									final IFile changedFile = workspaceRoot.getFile(relativeChangedPath);
//									if(file.exists()) {
//										byte[] bytes = FileUtils.readFully(new FileInputStream(file));
//										if(changedFile.exists()) {
//											changedFile.setContents(new ByteArrayInputStream(bytes), true, false, null);
//										} else {
//											changedFile.create(new ByteArrayInputStream(bytes), true, null);
//										}
//									} else {
//										changedFile.delete(true, null);
//									}
//								}
//							} catch (IOException e) {
//								throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error reading content of changed file", e));
//							} finally {
//								
//							}
//						}
//					};
//						PlatformUI.getWorkbench().getProgressService().run(true, false, new IRunnableWithProgress() {
//							public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
//								try {
//								workspace.run(operation, null);
//								} catch (CoreException e) {
//									throw new InvocationTargetException(e);
//								}
//							}
//						});
//					} catch (InvocationTargetException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//				} catch (CoreException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} finally {
//					
//				}
			}
		}
	}