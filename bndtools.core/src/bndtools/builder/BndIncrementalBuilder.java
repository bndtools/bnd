/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 *******************************************************************************/
package bndtools.builder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.lib.osgi.Builder;
import bndtools.Plugin;
import bndtools.RepositoryIndexerJob;
import bndtools.classpath.BndContainerInitializer;
import bndtools.utils.DeltaAccumulator;
import bndtools.utils.FileUtils;

public class BndIncrementalBuilder extends IncrementalProjectBuilder {

	public static final String BUILDER_ID = Plugin.PLUGIN_ID + ".bndbuilder";
	public static final String MARKER_BND_PROBLEM = Plugin.PLUGIN_ID + ".bndproblem";

	private static final String BND_SUFFIX = ".bnd";

	private static final long BUILT_NEVER = -1;
    private static final int RETRIES = 3;

	private final Map<String, Long> projectLastBuildTimes = new HashMap<String, Long>();
	private final Map<File, Container> bndsToDeliverables = new HashMap<File, Container>();

    @Override
    protected IProject[] build(int kind, @SuppressWarnings("rawtypes") Map args, IProgressMonitor monitor) throws CoreException {

        IProject project = getProject();
        IProject[] depends = new IProject[] { project.getWorkspace().getRoot().getProject(Project.BNDCNF) };

        ensureBndBndExists(project);
        if (hasBlockingErrors(project)) {
            try {
                clearBuildMarkers(project);
            } catch (Exception e) {
                Plugin.logError("Unable to clear build markers", e);
            }
            return depends;
        }

		if (getLastBuildTime(project) == -1 || kind == FULL_BUILD) {
			rebuildBndProject(project, monitor, 0);
		} else {
			IResourceDelta delta = getDelta(project);
			if(delta == null) {
				rebuildBndProject(project, monitor, 0);
			} else {
				incrementalRebuild(delta, project, monitor);
			}
		}
		setLastBuildTime(project, System.currentTimeMillis());
		RepositoryIndexerJob.runIfNeeded();

        return depends;
	}

    static void clearBuildMarkers(IProject project) throws CoreException {
        IFile bndFile = project.getFile(Project.BNDFILE);

        if (bndFile.exists()) {
            bndFile.deleteMarkers(MARKER_BND_PROBLEM, true, IResource.DEPTH_INFINITE);
        }
    }

    static void addBuildMarker(IProject project, String error) throws CoreException {
        IFile bndFile = project.getFile(Project.BNDFILE);

        IMarker marker = bndFile.createMarker(MARKER_BND_PROBLEM);
        marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
        marker.setAttribute(IMarker.MESSAGE, error);
        marker.setAttribute(IMarker.LINE_NUMBER, 1);
    }

    static boolean containsError(IMarker[] markers) {
        if (markers != null)
            for (IMarker marker : markers) {
                int severity = marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
                if (severity == IMarker.SEVERITY_ERROR)
                    return true;
            }
        return false;
    }

    static boolean hasBlockingErrors(IProject project) {
        try {
            return containsError(project.findMarkers(BndContainerInitializer.MARKER_BND_CLASSPATH_PROBLEM, true, 0))
                    || containsError(project.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE));
        } catch (CoreException e) {
            Plugin.logError("Error looking for Java problem markers", e);
            return false;
        }
    }

	private void setLastBuildTime(IProject project, long time) {
		projectLastBuildTimes.put(project.getName(), time);
	}
	private long getLastBuildTime(IProject project) {
		Long time = projectLastBuildTimes.get(project.getName());
		return time != null ? time.longValue() : BUILT_NEVER;
	}
	Collection<IPath> enumerateBndFiles(IProject project) throws CoreException {
		final Collection<IPath> paths = new LinkedList<IPath>();
		project.accept(new IResourceProxyVisitor() {
			public boolean visit(IResourceProxy proxy) throws CoreException {
				if(proxy.getType() == IResource.FOLDER || proxy.getType() == IResource.PROJECT)
					return true;

				String name = proxy.getName();
				if(name.toLowerCase().endsWith(BND_SUFFIX)) {
					IPath path = proxy.requestFullPath();
					paths.add(path);
				}
				return false;
			}
		}, 0);
		return paths;
	}
	void ensureBndBndExists(IProject project) throws CoreException {
		IFile bndFile = project.getFile(Project.BNDFILE);
		bndFile.refreshLocal(0, null);
		if(!bndFile.exists()) {
			bndFile.create(new ByteArrayInputStream(new byte[0]), 0, null);
		}
	}
	@Override protected void clean(IProgressMonitor monitor)
			throws CoreException {
		// Clear markers
		getProject().deleteMarkers(MARKER_BND_PROBLEM, true,
				IResource.DEPTH_INFINITE);

		// Delete target files
		Project model = Plugin.getDefault().getCentral().getModel(JavaCore.create(getProject()));
		try {
			model.clean();
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error cleaning project outputs.", e));
		}
	}

	void incrementalRebuild(IResourceDelta delta, IProject project, IProgressMonitor monitor) {
		SubMonitor progress = SubMonitor.convert(monitor);
		Project model = Plugin.getDefault().getCentral().getModel(JavaCore.create(project));
        if (model == null) {
            // Don't try to build... no bnd workspace configured
            Plugin.log(new Status(IStatus.WARNING, Plugin.PLUGIN_ID, 0, MessageFormat.format("Unable to run Bnd on project {0}: Bnd workspace not configured.",
                    project.getName()), null));
            return;
        }

		try {
            List<File> affectedFiles = new ArrayList<File>();
            final File targetDir = model.getTarget();
            final File output = model.getOutput();
            FileFilter generatedFilter = new FileFilter() {
                public boolean accept(File pathname) {
                    return !FileUtils.isAncestor(targetDir, pathname) && !FileUtils.isAncestor(output, pathname);
                }
			};
			DeltaAccumulator<File> visitor = DeltaAccumulator.fileAccumulator(IResourceDelta.ADDED | IResourceDelta.CHANGED | IResourceDelta.REMOVED, affectedFiles, generatedFilter);
			delta.accept(visitor);

			progress.setWorkRemaining(affectedFiles.size() + 10);

			boolean rebuild = false;
			List<File> deletedBnds = new LinkedList<File>();

			File srcDir = model.getSrc();

			// Check if any affected file is a bnd file
			for (File file : affectedFiles) {
                if (file.getName().toLowerCase().endsWith(BND_SUFFIX)) {
					rebuild = true;
					int deltaKind = visitor.queryDeltaKind(file);
					if((deltaKind & IResourceDelta.REMOVED) > 0) {
						deletedBnds.add(file);
					}
					break;
				}
				// Check if source file was changed instead of class file
				if (FileUtils.isAncestor(srcDir, file)) {
				    rebuild = true;
				    break;
				}
			}
			if(!rebuild && !affectedFiles.isEmpty()) {
				// Check if any of the affected files are members of bundles built by a sub builder
                Collection<? extends Builder> builders = model.getSubBuilders();
				for (Builder builder : builders) {
					File buildPropsFile = builder.getPropertiesFile();
					if(affectedFiles.contains(buildPropsFile)) {
						rebuild = true;
						break;
					} else if(builder.isInScope(affectedFiles)) {
						rebuild = true;

                        // Delete the bundle if any contained resource was
                        // deleted... to force rebuild
                        for (File file : affectedFiles) {
                            if ((IResourceDelta.REMOVED & visitor.queryDeltaKind(file)) > 0) {
                                String bsn = builder.getBsn();
                                File f = new File(model.getTarget(), bsn + ".jar");
                                try {
                                    if (f.isFile()) f.delete();
                                } catch (Exception e) {
                                    Plugin.logError("Error deleting file: " + f.getAbsolutePath(), e);
                                }
                            }
                        }

						break;
					}
					progress.worked(1);
				}
			}

			// Delete corresponding bundles for deleted Bnds
			for (File bndFile : deletedBnds) {
				Container container = bndsToDeliverables.get(bndFile);
				if(container != null) {
					IResource resource = FileUtils.toWorkspaceResource(container.getFile());
					resource.delete(false, null);
				}
			}

			if(rebuild)
				rebuildBndProject(project, monitor, 0);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		model.refresh();
	}

    void rebuildBndProject(IProject project, IProgressMonitor monitor, int count) throws CoreException {
        SubMonitor progress = SubMonitor.convert(monitor, 3);
        IJavaProject javaProject = JavaCore.create(project);

        Project model = Plugin.getDefault().getCentral().getModel(javaProject);
        if (model == null) {
            // Don't try to build... no bnd workspace configured
            Plugin.log(new Status(IStatus.WARNING, Plugin.PLUGIN_ID, 0,
                    MessageFormat.format("Unable to run Bnd on project {0}: Bnd workspace not configured.", project.getName()), null));
            return;
        }

		model.refresh();
		model.setChanged();

		clearBuildMarkers(project);

		// Build
		try {
		    final Set<File> deliverableJars = new HashSet<File>();
			bndsToDeliverables.clear();
            Collection<? extends Builder> builders = model.getSubBuilders();
			for (Builder builder : builders) {
				File subBndFile = builder.getPropertiesFile();
				String bsn = builder.getBsn();
				Container deliverable = model.getDeliverable(bsn, null);
				bndsToDeliverables.put(subBndFile, deliverable);
				deliverableJars.add(deliverable.getFile());
			}

            model.buildLocal(false);

            int retryCount = 0;
            while (retryCount++ < RETRIES && !model.getErrors().isEmpty()) {
                model.refresh();
                model.setChanged();

                model.buildLocal(false);
            }

			progress.worked(1);

			File targetDir = model.getTarget();
			IContainer target = ResourcesPlugin.getWorkspace().getRoot().getContainerForLocation(new Path(targetDir.getAbsolutePath()));
			target.refreshLocal(IResource.DEPTH_INFINITE, null);

			// Clear any JARs in the target directory that have not just been built by Bnd
			final File[] targetJars = model.getTarget().listFiles(new FileFilter() {
                public boolean accept(File pathname) {
                    return pathname.getName().endsWith(".jar");
                }
            });
			WorkspaceJob deleteJob = new WorkspaceJob("delete") {
                @Override
                public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
                    SubMonitor progress = SubMonitor.convert(monitor);
                    for (File targetJar : targetJars) {
                        if(!deliverableJars.contains(targetJar)) {
                            IFile wsFile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(targetJar.getAbsolutePath()));
                            if(wsFile != null && wsFile.exists()) {
                                wsFile.delete(true, progress.newChild(1));
                            }
                        }
                    }
                    return Status.OK_STATUS;
                }
			};
			deleteJob.schedule();
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error building project.", e));
		}

        List<String> errors = new ArrayList<String>(model.getErrors());
        /*
        if (!errors.isEmpty() && count < RETRIES) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            rebuildBndProject(project, progress.newChild(1, SubMonitor.SUPPRESS_NONE), count + 1);
        } else {
        */
            for (String error : errors) {
                addBuildMarker(project, error);
            }
            model.clear();
        // }
	}
}
