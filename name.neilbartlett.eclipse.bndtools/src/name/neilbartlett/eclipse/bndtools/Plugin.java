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
package name.neilbartlett.eclipse.bndtools;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import aQute.bnd.plugin.Activator;
import aQute.bnd.plugin.Central;
import aQute.bnd.service.BndListener;
import aQute.lib.osgi.Processor;


public class Plugin extends AbstractUIPlugin {
	
	public static final String PLUGIN_ID = "name.neilbartlett.eclipse.bndtools";
	public static final String BND_EDITOR_ID = "name.neilbartlett.eclipse.bndtools.bndEditor";
	public static final String EXTPOINT_REPO_CONTRIB = "repositoryContributor";

	public static final String ID_FRAMEWORKS_PREF_PAGE = "name.neilbartlett.eclipse.bndtools.prefsPages.osgiFrameworks";
	
	private static volatile Plugin plugin;
	private BundleContext bundleContext;
	private Activator bndActivator;
	private BndListener bndListener;

	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		this.bundleContext = context;
		
		bndActivator = new Activator();
		bndActivator.start(context);
		
		bndListener = new FilesystemUpdateListener();
		Central.getWorkspace().addBasicPlugin(bndListener);
		
//		StartupBuildJob buildJob = new StartupBuildJob("Build Bnd Projects...");
//		buildJob.setSystem(false);
//		buildJob.schedule();
	}
	


	public void stop(BundleContext context) throws Exception {
		Central.getWorkspace().removeBasicPlugin(bndListener);
		bndActivator.stop(context);
		this.bundleContext = null;
		plugin = null;
		super.stop(context);
	}

	public static Plugin getDefault() {
		return plugin;
	}
	
	public static void log(IStatus status) {
		Plugin instance = plugin;
		if(instance != null) {
			instance.getLog().log(status);
		} else {
			System.err.println(String.format("Unable to print to log for %s: bundle has been stopped.", Plugin.PLUGIN_ID));
		}
	}
	
	public BundleContext getBundleContext() {
		return bundleContext;
	}
	
    public void report(boolean warnings, boolean acknowledge , Processor reporter, final String title, final String extra ) {
        if (reporter.getErrors().size() > 0
                || (warnings && reporter.getWarnings().size() > 0)) {
            final StringBuffer sb = new StringBuffer();
            sb.append("\n");
            if (reporter.getErrors().size() > 0) {
                sb.append("[Errors]\n");
                for (String msg : reporter.getErrors()) {
                    sb.append(msg);
                    sb.append("\n");
                }
            }
            sb.append("\n");
            if (reporter.getWarnings().size() > 0) {
                sb.append("[Warnings]\n");
                for (String msg : reporter.getWarnings()) {
                    sb.append(msg);
                    sb.append("\n");
                }
            }
            final Status s = new Status(Status.ERROR, PLUGIN_ID, 0, sb.toString(), null);
            reporter.clear();
            
            async(new Runnable() {
                public void run() {
                    ErrorDialog.openError(null, title, title + "\n" + extra, s);
                }
            });

        } else {
            message(title+ " : ok");
        }
    }
    public void message(final String msg) {
        async(new Runnable() {
            public void run() {
                MessageDialog.openInformation(null, "Bnd", msg);
            }
        });
    }
    void async(Runnable run) {
        if (Display.getCurrent() == null) {
            Display.getDefault().asyncExec(run);
        } else
            run.run();
    }
    public void error(List<String> errors) {
        final StringBuffer sb = new StringBuffer();
        for (String msg : errors) {
            sb.append(msg);
            sb.append("\n");
        }

        async(new Runnable() {
            public void run() {
                Status s = new Status(Status.ERROR, PLUGIN_ID, 0, "", null);
                ErrorDialog.openError(null, "Errors during bundle generation",
                        sb.toString(), s);
            }
        });
    }
    static final AtomicBoolean busy = new AtomicBoolean(false);
    public void error(final String msg, final Throwable t) {
        Status s = new Status(Status.ERROR, PLUGIN_ID, 0, msg, t);
        getLog().log(s);
        async(new Runnable() {
            public void run() {
            	if(!busy.compareAndSet(false, true)) {
		            Status s = new Status(Status.ERROR, PLUGIN_ID, 0, "", null);
		            ErrorDialog.openError(null, "Errors during bundle generation",
		                    msg + " " + t.getMessage(), s);
		            
		            busy.set(false);
            	}
            }
        });
    }
    public void warning(List<String> errors) {
        final StringBuffer sb = new StringBuffer();
        for (String msg : errors) {
            sb.append(msg);
            sb.append("\n");
        }
        async(new Runnable() {
            public void run() {
                Status s = new Status(Status.WARNING, PLUGIN_ID, 0, "", null);
                ErrorDialog.openError(null,
                        "Warnings during bundle generation", sb.toString(), s);
            }
        });
    }

	public static void logError(String message, Throwable exception) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, 0, message, exception));
	}
}