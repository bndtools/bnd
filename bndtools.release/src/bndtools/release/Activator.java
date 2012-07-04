/*******************************************************************************
 * Copyright (c) 2010 Per Kr. Soreide.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Per Kr. Soreide - initial API and implementation
 *******************************************************************************/
package bndtools.release;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.service.RepositoryPlugin;
import bndtools.release.api.IReleaseParticipant;
import bndtools.release.api.ReleaseUtils;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "bndtools.release";

	// The shared instance
	private static Activator plugin;
		
	
	private static ServiceTracker workspaceTracker;
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		workspaceTracker = new ServiceTracker(context, Workspace.class.getName(), null);
		workspaceTracker.open();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		workspaceTracker.close();
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	public static void log(String msg, int msgType) {
		ILog log = getDefault().getLog();
		Status status = new Status(msgType, getDefault().getBundle().getSymbolicName(), msgType, msg + "\n", null);
		log.log(status);
		
	}

    static void async(Runnable run) {
        if (Display.getCurrent() == null) {
            Display.getDefault().asyncExec(run);
        } else
            run.run();
    }
    public static void message(final String msg) {
        async(new Runnable() {
            public void run() {
                MessageDialog.openInformation(null, "Release Bundle", msg);
            }
        });
    }

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
	
	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		
		// Images
		ImageDescriptor id = getImageDescriptor("icons/tree16/bundle_obj.gif");
		reg.put("bundle", id);
		
		id = getImageDescriptor("icons/tree16/class_obj.gif");
		reg.put("class", id);
		
		id = getImageDescriptor("icons/tree16/field_public_obj.gif");
		reg.put("field", id);
		
		id = getImageDescriptor("icons/tree16/methpub_obj.gif");
		reg.put("method", id);
		
		id = getImageDescriptor("icons/tree16/package_obj.gif");
		reg.put("package", id);

		
		// Overlays
		id = getImageDescriptor("icons/ovr16/addition_red_ovr.gif");
		reg.put("major_add", id);
		
		id = getImageDescriptor("icons/ovr16/change_red_ovr.gif");
		reg.put("major_modify", id);
		
		id = getImageDescriptor("icons/ovr16/deletion_red_ovr.gif");
		reg.put("major_remove", id);
		
		id = getImageDescriptor("icons/ovr16/addition_blue_ovr.gif");
		reg.put("minor_add", id);
		
		id = getImageDescriptor("icons/ovr16/change_blue_ovr.gif");
		reg.put("minor_modify", id);
		
		id = getImageDescriptor("icons/ovr16/deletion_blue_ovr.gif");
		reg.put("minor_remove", id);
		
		id = getImageDescriptor("icons/ovr16/change_green_ovr.gif");
		reg.put("micro_modify", id);
		
		id = getImageDescriptor("icons/ovr16/deletion_green_ovr.gif");
		reg.put("micro_remove", id);

		id = getImageDescriptor("icons/ovr16/export_ovr.gif");
		reg.put("export", id);
		
		id = getImageDescriptor("icons/ovr16/import_ovr.gif");
		reg.put("import", id);

		id = getImageDescriptor("icons/ovr16/import_export_ovr.gif");
		reg.put("import_export", id);

		id = getImageDescriptor("icons/ovr16/static_ovr.gif");
		reg.put("static", id);

		// Images with overlay
		OverlayImageDescriptor oid = new OverlayImageDescriptor(reg, "package", "import");
		oid.setXValue(8);
		oid.setYValue(0);
		reg.put("package_import", oid);

		oid = new OverlayImageDescriptor(reg, "package", "export");
		oid.setXValue(8);
		oid.setYValue(0);
		reg.put("package_export", oid);

		oid = new OverlayImageDescriptor(reg, "package", "import_export");
		oid.setXValue(8);
		oid.setYValue(0);
		reg.put("package_import_export", oid);

		oid = new OverlayImageDescriptor(reg, "field", "static");
		oid.setXValue(0);
		oid.setYValue(0);
		reg.put("static_field", oid);

		oid = new OverlayImageDescriptor(reg, "method", "static");
		oid.setXValue(0);
		oid.setYValue(0);
		reg.put("static_method", oid);
		
		oid = new OverlayImageDescriptor(reg, "package_import", "major_add");
		reg.put("package_import_major_add", oid);

		oid = new OverlayImageDescriptor(reg, "package_import", "minor_remove");
		reg.put("package_import_minor_remove", oid);
		
		oid = new OverlayImageDescriptor(reg, "package_import", "minor_modify");
		reg.put("package_import_minor_modify", oid);

		oid = new OverlayImageDescriptor(reg, "package_import", "micro_remove");
		reg.put("package_import_micro_remove", oid);

		oid = new OverlayImageDescriptor(reg, "package_export", "major_add");
		reg.put("package_export_major_add", oid);

		oid = new OverlayImageDescriptor(reg, "package_export", "major_modify");
		reg.put("package_export_major_modify", oid);
		
		oid = new OverlayImageDescriptor(reg, "package_export", "major_remove");
		reg.put("package_export_major_remove", oid);

		oid = new OverlayImageDescriptor(reg, "package_export", "minor_add");
		reg.put("package_export_minor_add", oid);

		oid = new OverlayImageDescriptor(reg, "package_export", "minor_modify");
		reg.put("package_export_minor_modify", oid);
		
		oid = new OverlayImageDescriptor(reg, "package_export", "minor_remove");
		reg.put("package_export_minor_remove", oid);

		oid = new OverlayImageDescriptor(reg, "package_export", "micro_modify");
		reg.put("package_export_micro_modify", oid);
		

		oid = new OverlayImageDescriptor(reg, "package_import_export", "major_add");
		reg.put("package_import_export_major_add", oid);

		oid = new OverlayImageDescriptor(reg, "package_import_export", "major_modify");
		reg.put("package_import_export_major_modify", oid);
		
		oid = new OverlayImageDescriptor(reg, "package_import_export", "major_remove");
		reg.put("package_import_export_major_remove", oid);

		oid = new OverlayImageDescriptor(reg, "package_import_export", "minor_add");
		reg.put("package_import_export_minor_add", oid);

		oid = new OverlayImageDescriptor(reg, "package_import_export", "minor_modify");
		reg.put("package_import_export_minor_modify", oid);
		
		oid = new OverlayImageDescriptor(reg, "package_import_export", "minor_remove");
		reg.put("package_import_export_minor_remove", oid);

		oid = new OverlayImageDescriptor(reg, "package_import_export", "micro_modify");
		reg.put("package_import_export_micro_modify", oid);

		
		
		
		oid = new OverlayImageDescriptor(reg, "class", "major_add");
		reg.put("class_major_add", oid);

		oid = new OverlayImageDescriptor(reg, "class", "major_modify");
		reg.put("class_major_modify", oid);
		
		oid = new OverlayImageDescriptor(reg, "class", "major_remove");
		reg.put("class_major_remove", oid);

		oid = new OverlayImageDescriptor(reg, "class", "minor_add");
		reg.put("class_minor_add", oid);

		oid = new OverlayImageDescriptor(reg, "class", "minor_modify");
		reg.put("class_minor_modify", oid);
		
		oid = new OverlayImageDescriptor(reg, "class", "minor_remove");
		reg.put("class_minor_remove", oid);

		oid = new OverlayImageDescriptor(reg, "field", "major_add");
		reg.put("field_major_add", oid);

		oid = new OverlayImageDescriptor(reg, "field", "major_modify");
		reg.put("field_major_modify", oid);
		
		oid = new OverlayImageDescriptor(reg, "field", "major_remove");
		reg.put("field_major_remove", oid);

		oid = new OverlayImageDescriptor(reg, "field", "minor_add");
		reg.put("field_minor_add", oid);

		oid = new OverlayImageDescriptor(reg, "field", "minor_modify");
		reg.put("field_minor_modify", oid);
		
		oid = new OverlayImageDescriptor(reg, "field", "minor_remove");
		reg.put("field_minor_remove", oid);

		oid = new OverlayImageDescriptor(reg, "static_field", "major_add");
		reg.put("static_field_major_add", oid);

		oid = new OverlayImageDescriptor(reg, "static_field", "major_modify");
		reg.put("static_field_major_modify", oid);
		
		oid = new OverlayImageDescriptor(reg, "static_field", "major_remove");
		reg.put("static_field_major_remove", oid);

		oid = new OverlayImageDescriptor(reg, "static_field", "minor_add");
		reg.put("static_field_minor_add", oid);

		oid = new OverlayImageDescriptor(reg, "static_field", "minor_modify");
		reg.put("static_field_minor_modify", oid);
		
		oid = new OverlayImageDescriptor(reg, "static_field", "minor_remove");
		reg.put("static_field_minor_remove", oid);

		oid = new OverlayImageDescriptor(reg, "method", "major_add");
		reg.put("method_major_add", oid);

		oid = new OverlayImageDescriptor(reg, "method", "major_modify");
		reg.put("method_major_modify", oid);
		
		oid = new OverlayImageDescriptor(reg, "method", "major_remove");
		reg.put("method_major_remove", oid);

		oid = new OverlayImageDescriptor(reg, "method", "minor_add");
		reg.put("method_minor_add", oid);

		oid = new OverlayImageDescriptor(reg, "method", "minor_modify");
		reg.put("method_minor_modify", oid);
		
		oid = new OverlayImageDescriptor(reg, "method", "minor_remove");
		reg.put("method_minor_remove", oid);

		oid = new OverlayImageDescriptor(reg, "static_method", "major_add");
		reg.put("static_method_major_add", oid);

		oid = new OverlayImageDescriptor(reg, "static_method", "major_modify");
		reg.put("static_method_major_modify", oid);
		
		oid = new OverlayImageDescriptor(reg, "static_method", "major_remove");
		reg.put("static_method_major_remove", oid);

		oid = new OverlayImageDescriptor(reg, "static_method", "minor_add");
		reg.put("static_method_minor_add", oid);

		oid = new OverlayImageDescriptor(reg, "static_method", "minor_modify");
		reg.put("static_method_minor_modify", oid);
		
		oid = new OverlayImageDescriptor(reg, "static_method", "minor_remove");
		reg.put("static_method_minor_remove", oid);
	}
	
    @SuppressWarnings("unchecked")
	public static <T> T getService(Class<T> clazz) {
    	 if (clazz == Workspace.class) {
    		 try {
				return (T) Workspace.getWorkspace(ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
    	 }
    	 
    	 ServiceReference sr = getDefault().getBundle().getBundleContext().getServiceReference(clazz.getName());
    	 if (sr == null) {
    		 return null;
    	 }
    	 return (T) getDefault().getBundle().getBundleContext().getService(sr);
     }

	public static void refreshProject(Project project) throws Exception {
		Workspace ws = Activator.getService(Workspace.class);
		if (ws == null) {
			return;
		}
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IProject p  = root.getProject(project.getName());
		project.refresh();
		refreshProject(p);
	}
	
	public static File getLocalRepoLocation(RepositoryPlugin repository) {
		try {
			Method m = repository.getClass().getMethod("getRoot");
			if (m.getReturnType() == File.class) {
				return (File) m.invoke(repository);
			}
		} catch (Exception e) {
		}
 		return null;
	}

	public static void refreshProject(IProject project) throws Exception {
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
	}

	public static Workspace getWorkspace() {
		return (Workspace) workspaceTracker.getService();
	}
	
	public static List<RepositoryPlugin> getRepositories() {
		
		Workspace ws = (Workspace)workspaceTracker.getService();
		if (ws == null) {
			return Collections.emptyList();
		}

		return ws.getPlugins(RepositoryPlugin.class);
	}

	public static RepositoryPlugin getRepositoryPlugin(String name) {
		List<RepositoryPlugin> plugins = getRepositories();
		for (RepositoryPlugin plugin : plugins) {
			if (name.equals(plugin.getName())) {
				return plugin;
			}
		}
		return null;
	}
    
	public static void refreshFile(File f) throws Exception {
		if (f == null) {
			return;
		}
        IResource r = ReleaseUtils.toWorkspaceResource(f);
        if (r != null) {
            r.refreshLocal(IResource.DEPTH_INFINITE, null);
        }
    }

	public static IProject getProject(File f) {
		IResource res = ReleaseUtils.toResource(f);
		return res.getProject();
	}

	public static IPath getPath(File f) {
		IResource res = ReleaseUtils.toResource(f);
		return res.getProjectRelativePath();
	}

	public static List<IReleaseParticipant> getReleaseParticipants() {
		IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(PLUGIN_ID, "bndtoolsReleaseParticipant");
		if (elements.length == 0) {
			return Collections.emptyList();
		}
		List<IReleaseParticipant> participants = new ArrayList<IReleaseParticipant>();
	    for (IConfigurationElement element : elements) {
            try {
            	IReleaseParticipant participant = (IReleaseParticipant) element.createExecutableExtension("class");
            	String strRanking = element.getAttribute("ranking");
            	int ranking = 0;
            	if (strRanking != null && strRanking.length() > 0) {
            		ranking = Integer.valueOf(strRanking); 
            	}
            	participant.setRanking(ranking);
            	participants.add(participant);
            } catch (CoreException e) {
                logError("Error executing startup participant", e);
            }
        }
	    return participants;
	}
	
	public static void logError(String message, Throwable exception) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, 0, message, exception));
	}
	public static void log(IStatus status) {
		Activator instance = plugin;
		if(instance != null) {
			instance.getLog().log(status);
		} else {
			System.err.println(String.format("Unable to print to log for %s: bundle has been stopped.", Activator.PLUGIN_ID));
		}
	}
    public static void error(List<String> errors) {
        final StringBuffer sb = new StringBuffer();
        for (String msg : errors) {
            sb.append(msg);
            sb.append("\n");
        }

        async(new Runnable() {
            public void run() {
                Status s = new Status(Status.ERROR, PLUGIN_ID, 0, "", null);
                ErrorDialog.openError(null, "Errors during release",
                        sb.toString(), s);
            }
        });
    }

    
}
