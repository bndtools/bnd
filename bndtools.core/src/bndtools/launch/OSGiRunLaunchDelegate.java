package bndtools.launch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.internal.Workbench;
import org.osgi.framework.launch.FrameworkFactory;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.build.ProjectLauncher.NotificationListener;
import aQute.bnd.build.ProjectLauncher.NotificationType;
import aQute.bnd.osgi.Jar;
import bndtools.Plugin;
import bndtools.central.Central;
import bndtools.launch.util.LaunchUtils;

public class OSGiRunLaunchDelegate extends AbstractOSGiLaunchDelegate {
    private static final ILogger logger = Logger.getLogger(OSGiRunLaunchDelegate.class);

    private ProjectLauncher bndLauncher = null;

    private Display display;

    private PopupDialog dialog;

    private Text textArea;

    @Override
    protected void initialiseBndLauncher(ILaunchConfiguration configuration, Project model) throws Exception {
        synchronized (model) {
            bndLauncher = model.getProjectLauncher();
            if (bndLauncher == null)
                throw new IllegalStateException(String.format("Failed to obtain launcher for project %s (%s)", model.getName(), model.getPropertiesFile()));
        }

        configureLauncher(configuration);

        bndLauncher.registerForNotifications(new NotificationListener() {
            @Override
            public void notify(NotificationType type, final String notification) {

                if (type == NotificationType.ERROR) {
                    display.syncExec(new Runnable() {
                        @Override
                        public void run() {
                            dialog.open();
                            textArea.append(notification + "\n\n");
                            dialog.getShell().redraw();
                        }
                    });
                }
            }
        });
        bndLauncher.prepare();
    }

    @Override
    protected IStatus getLauncherStatus() {
        List<String> launcherErrors = bndLauncher.getErrors();
        List<String> projectErrors = bndLauncher.getProject().getErrors();
        List<String> errors = new ArrayList<String>(projectErrors.size() + launcherErrors.size());
        errors.addAll(launcherErrors);
        errors.addAll(projectErrors);

        List<String> launcherWarnings = bndLauncher.getWarnings();
        List<String> projectWarnings = bndLauncher.getProject().getWarnings();
        List<String> warnings = new ArrayList<String>(launcherWarnings.size() + projectWarnings.size());
        warnings.addAll(launcherWarnings);
        warnings.addAll(projectWarnings);

        String frameworkPath = validateClasspath(bndLauncher.getClasspath());
        if (frameworkPath == null)
            errors.add("No OSGi framework has been added to the run path.");

        return createStatus("Problem(s) preparing the runtime environment.", errors, warnings);
    }

    private static String validateClasspath(Collection<String> classpath) {
        for (String fileName : classpath) {
            Jar jar = null;
            try {
                jar = new Jar(new File(fileName));
                boolean frameworkExists = jar.exists("META-INF/services/" + FrameworkFactory.class.getName());
                if (frameworkExists)
                    return fileName;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (jar != null) {
                    jar.close();
                }
            }
        }
        return null;
    }

    @Override
    public void launch(final ILaunchConfiguration configuration, String mode, final ILaunch launch, IProgressMonitor monitor) throws CoreException {
        SubMonitor progress = SubMonitor.convert(monitor, 2);

        try {
            boolean dynamic = configuration.getAttribute(LaunchConstants.ATTR_DYNAMIC_BUNDLES, LaunchConstants.DEFAULT_DYNAMIC_BUNDLES);
            if (dynamic)
                registerLaunchPropertiesRegenerator(model, launch);
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error obtaining OSGi project launcher.", e));
        }

        display = Workbench.getInstance().getDisplay();
        display.syncExec(new Runnable() {
            @Override
            public void run() {
                dialog = new PopupDialog(new Shell(display), PopupDialog.INFOPOPUPRESIZE_SHELLSTYLE, false, true, true, true, false, "Errors in running OSGi Framework", "") {
                    @Override
                    protected Control createDialogArea(Composite parent) {
                        textArea = new Text(parent, SWT.LEAD | SWT.READ_ONLY | SWT.WRAP);
                        return textArea;
                    }

                    @Override
                    protected void fillDialogMenu(IMenuManager dialogMenu) {
                        super.fillDialogMenu(dialogMenu);

                        Action dismissAction = new Action("Close") {
                            @Override
                            public void run() {
                                close();
                            }
                        };

                        dialogMenu.add(dismissAction);
                    }

                    @Override
                    protected Control createInfoTextArea(Composite parent) {
                        Link link = new Link(parent, SWT.NONE);
                        link.setText("<a>Dismiss\u2026</a> ");
                        link.addSelectionListener(new SelectionAdapter() {
                            @Override
                            public void widgetSelected(SelectionEvent e) {
                                close();
                            }
                        });

                        GridDataFactory.fillDefaults().grab(true, false).align(SWT.END, SWT.FILL).applyTo(link);
                        return link;
                    }

                    @Override
                    protected Point getDefaultSize() {
                        Point p = getShell().getSize();
                        p.x = Math.max(400, p.x / 2);
                        p.y = Math.max(200, p.y / 2);
                        return p;
                    }

                    @Override
                    protected Point getInitialLocation(Point initialSize) {
                        Rectangle r = getShell().getBounds();
                        return new Point(r.x + r.width - initialSize.x, r.y + r.height - initialSize.y);
                    }

                    @Override
                    public boolean close() {
                        if (textArea != null) {
                            textArea.setText("");
                        }
                        return super.close();
                    }
                };
            }
        });

        super.launch(configuration, mode, launch, progress.newChild(1, SubMonitor.SUPPRESS_NONE));
    }

    /**
     * This was first always overriding -runkeep. Now it can only override it if -runkeep is set to false. However, I
     * think this option should go away in bndtools. Anyway, removed the actual clearing since this was already done in
     * the launcher.
     */
    private void configureLauncher(ILaunchConfiguration configuration) throws CoreException {
        if (bndLauncher.isKeep() == false) {
            boolean clean = configuration.getAttribute(LaunchConstants.ATTR_CLEAN, LaunchConstants.DEFAULT_CLEAN);

            bndLauncher.setKeep(!clean);
        }
        enableTraceOptionIfSetOnConfiguration(configuration, bndLauncher);
    }

    /**
     * Registers a resource listener with the project model file to update the launcher when the model or any of the
     * run-bundles changes. The resource listener is automatically unregistered when the launched process terminates.
     *
     * @param project
     * @param launch
     * @throws CoreException
     */
    private void registerLaunchPropertiesRegenerator(final Project project, final ILaunch launch) throws CoreException {
        final IResource targetResource = LaunchUtils.getTargetResource(launch.getLaunchConfiguration());
        if (targetResource == null)
            return;

        final IPath bndbndPath;
        try {
            bndbndPath = Central.toPath(project.getPropertiesFile());
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error querying bnd.bnd file location", e));
        }

        try {
            Central.toPath(project.getTarget());
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error querying project output folder", e));
        }
        final IResourceChangeListener resourceListener = new IResourceChangeListener() {
            @Override
            public void resourceChanged(IResourceChangeEvent event) {
                try {
                    final AtomicBoolean update = new AtomicBoolean(false);

                    // Was the properties file (bnd.bnd or *.bndrun) included in
                    // the delta?
                    IResourceDelta propsDelta = event.getDelta().findMember(bndbndPath);
                    if (propsDelta == null && targetResource.getType() == IResource.FILE)
                        propsDelta = event.getDelta().findMember(targetResource.getFullPath());
                    if (propsDelta != null) {
                        if (propsDelta.getKind() == IResourceDelta.CHANGED) {
                            update.set(true);
                        }
                    }

                    // Check for bundles included in the launcher's runbundles
                    // list
                    if (!update.get()) {
                        final Set<String> runBundleSet = new HashSet<String>();
                        for (String bundlePath : bndLauncher.getRunBundles()) {
                            runBundleSet.add(new org.eclipse.core.runtime.Path(bundlePath).toPortableString());
                        }
                        event.getDelta().accept(new IResourceDeltaVisitor() {
                            @Override
                            public boolean visit(IResourceDelta delta) throws CoreException {
                                // Short circuit if we have already found a
                                // match
                                if (update.get())
                                    return false;

                                IResource resource = delta.getResource();
                                if (resource.getType() == IResource.FILE) {
                                    IPath location = resource.getLocation();
                                    boolean isRunBundle = location != null ? runBundleSet.contains(location.toPortableString()) : false;
                                    update.compareAndSet(false, isRunBundle);
                                    return false;
                                }

                                // Recurse into containers
                                return true;
                            }
                        });
                    }

                    if (update.get()) {
                        project.forceRefresh();
                        project.setChanged();
                        bndLauncher.update();
                    }
                } catch (Exception e) {
                    logger.logError("Error updating launch properties file.", e);
                }
            }
        };
        ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceListener);

        // Register a listener for termination of the launched process
        Runnable onTerminate = new Runnable() {
            @Override
            public void run() {
                ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceListener);
                display.asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        if (dialog != null && dialog.getShell() != null) {
                            dialog.getShell().dispose();
                        }
                    }
                });
            }
        };
        DebugPlugin.getDefault().addDebugEventListener(new TerminationListener(launch, onTerminate));
    }

    @Override
    protected ProjectLauncher getProjectLauncher() throws CoreException {
        if (bndLauncher == null)
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Bnd launcher was not initialised.", null));
        return bndLauncher;
    }

}