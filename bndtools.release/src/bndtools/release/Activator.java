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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
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
import org.osgi.util.tracker.ServiceTracker;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.service.RepositoryPlugin;
import bndtools.release.api.IReleaseParticipant;
import bndtools.release.api.ReleaseUtils;
import bndtools.release.nl.Messages;
import bndtools.release.ui.BundleTreeImages;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "bndtools.release"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	private static ServiceTracker workspaceTracker;
	/**
	 * The constructor
	 */
	public Activator() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		workspaceTracker = new ServiceTracker(context, Workspace.class.getName(), null);
		workspaceTracker.open();
	}

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
		Status status = new Status(msgType, getDefault().getBundle().getSymbolicName(), msgType, msg + "\n", null); //$NON-NLS-1$
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
                MessageDialog.openInformation(null, Messages.releaseDialogTitle1, msg);
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

		loadBundleImages(reg, BundleTreeImages.BUNDLE_PATH, BundleTreeImages.DELTA, "*.gif"); //$NON-NLS-1$
		loadBundleImages(reg, BundleTreeImages.BUNDLE_PATH, BundleTreeImages.IMPORT_EXPORT, "*.gif"); //$NON-NLS-1$
		loadBundleImages(reg, BundleTreeImages.BUNDLE_PATH, BundleTreeImages.MODIFIERS, "*.gif"); //$NON-NLS-1$
		loadBundleImages(reg, BundleTreeImages.BUNDLE_PATH, BundleTreeImages.TYPES, "*.gif"); //$NON-NLS-1$
	}

	private static void loadBundleImages(ImageRegistry reg, String rootPath, String parent, String filePattern) {
        @SuppressWarnings("unchecked")
        Enumeration<URL> en = plugin.getBundle().findEntries(rootPath + "/" + parent, filePattern, false);
        if (en == null) {
            return;
        }
        while (en.hasMoreElements()) {
            URL url = en.nextElement();
            String name = getResourceName(url);
            ImageDescriptor id = ImageDescriptor.createFromURL(url);
            reg.put(parent + "_" + name, id); //$NON-NLS-1$
        }
    }

    private static String getResourceName(URL url) {
        int idx = url.getPath().lastIndexOf('/');
        String name = url.getPath().substring(idx + 1);
        return name.substring(0, name.lastIndexOf('.'));
    }

	public static void refreshProject(Project project) throws Exception {
		Workspace ws = getWorkspace();
		if (ws == null) {
			return;
		}
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IProject p  = root.getProject(project.getName());
		refreshProject(p);
	}

	public static File getLocalRepoLocation(RepositoryPlugin repository) {
		try {
			Method m = repository.getClass().getMethod("getRoot"); //$NON-NLS-1$
			if (m.getReturnType() == File.class) {
				return (File) m.invoke(repository);
			}
		} catch (Exception e) {
			/* ignore */
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
		IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(PLUGIN_ID, "bndtoolsReleaseParticipant"); //$NON-NLS-1$
		if (elements.length == 0) {
			return Collections.emptyList();
		}
		List<IReleaseParticipant> participants = new ArrayList<IReleaseParticipant>();
	    for (IConfigurationElement element : elements) {
            try {
            	IReleaseParticipant participant = (IReleaseParticipant) element.createExecutableExtension("class"); //$NON-NLS-1$
            	String strRanking = element.getAttribute("ranking");  //$NON-NLS-1$
            	int ranking = 0;
            	if (strRanking != null && strRanking.length() > 0) {
            		ranking = Integer.valueOf(strRanking);
            	}
            	participant.setRanking(ranking);
            	participants.add(participant);
            } catch (CoreException e) {
                logError(Messages.errorExecutingStartupParticipant, e);
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
			System.err.println(String.format(Messages.loggingError, Activator.PLUGIN_ID));
		}
	}

	public static void error(List<String> errors) {
        final StringBuffer sb = new StringBuffer();
        for (String msg : errors) {
            sb.append(msg);
            sb.append("\n"); //$NON-NLS-1$
        }

        async(new Runnable() {
            public void run() {
                Status s = new Status(Status.ERROR, PLUGIN_ID, 0, "", null); //$NON-NLS-1$
                ErrorDialog.openError(null, Messages.errorDialogTitle,
                        sb.toString(), s);
            }
        });
    }
}
