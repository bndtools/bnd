package bndtools.launch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bndtools.api.RunMode;
import org.eclipse.core.runtime.CoreException;
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
import aQute.bnd.build.ProjectLauncher.NotificationType;
import aQute.bnd.osgi.Jar;
import aQute.bnd.service.result.Result;
import bndtools.Plugin;

public class OSGiRunLaunchDelegate extends AbstractOSGiLaunchDelegate {
	private ProjectLauncher			bndLauncher	= null;

	private Display					display;

	private PopupDialog				dialog;

	private Text					textArea;

	@Override
	protected void initialiseBndLauncher(ILaunchConfiguration configuration, Project model) throws Exception {
		synchronized (model) {
			Result<ProjectLauncher, String> resolvingProjectLauncher = Result.of(model.getProjectLauncher(),
				"Failed to get projectlauncher");

			bndLauncher = resolvingProjectLauncher
				.orElseThrow(
					(e) -> new IllegalStateException(String.format("Failed to obtain launcher for project %s (%s): %s",
						model.getName(), model.getPropertiesFile(), e)));
		}
		configureLauncher(configuration);

		bndLauncher.registerForNotifications((type, notification) -> {
			if (type == NotificationType.ERROR) {
				display.syncExec(() -> {
					dialog.open();
					textArea.append(notification + "\n\n");
					dialog.getShell()
						.redraw();
				});
			}
		});
		bndLauncher.prepare();
	}

	@Override
	protected RunMode getRunMode() {
		return RunMode.LAUNCH;
	}

	@Override
	protected IStatus getLauncherStatus() {
		List<String> launcherErrors = bndLauncher.getErrors();
		List<String> projectErrors = bndLauncher.getProject()
			.getErrors();
		List<String> errors = new ArrayList<>(projectErrors.size() + launcherErrors.size());
		errors.addAll(launcherErrors);
		errors.addAll(projectErrors);

		List<String> launcherWarnings = bndLauncher.getWarnings();
		List<String> projectWarnings = bndLauncher.getProject()
			.getWarnings();
		List<String> warnings = new ArrayList<>(launcherWarnings.size() + projectWarnings.size());
		warnings.addAll(launcherWarnings);
		warnings.addAll(projectWarnings);

		String frameworkPath = validateClasspath(bndLauncher.getRunpath());
		if (frameworkPath == null)
			errors.add("No OSGi framework has been added to the run path.");

		return createStatus("Problem(s) preparing the runtime environment.", errors, warnings);
	}

	private static String validateClasspath(Collection<String> classpath) {
		for (String fileName : classpath) {
			try (Jar jar = new Jar(new File(fileName))) {
				boolean frameworkExists = jar.exists("META-INF/services/" + FrameworkFactory.class.getName());
				if (frameworkExists)
					return fileName;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@Override
	public void launch(final ILaunchConfiguration configuration, String mode, final ILaunch launch,
		IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, 2);

		display = Workbench.getInstance()
			.getDisplay();
		display.syncExec(() -> dialog = new PopupDialog(new Shell(display), PopupDialog.INFOPOPUPRESIZE_SHELLSTYLE,
			false, true, true, true, false, "Errors in running OSGi Framework", "") {
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

				GridDataFactory.fillDefaults()
					.grab(true, false)
					.align(SWT.END, SWT.FILL)
					.applyTo(link);
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
		});

		// Register a listener for termination of the launched process
		DebugPlugin.getDefault()
			.addDebugEventListener(new TerminationListener(launch, () -> display.asyncExec(() -> {
				if (dialog != null && dialog.getShell() != null) {
					dialog.getShell()
						.dispose();
				}
			})));

		super.launch(configuration, mode, launch, progress.newChild(1, SubMonitor.SUPPRESS_NONE));
	}

	@Override
	protected ProjectLauncher getProjectLauncher() throws CoreException {
		if (bndLauncher == null)
			throw new CoreException(
				new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Bnd launcher was not initialised.", null));
		return bndLauncher;
	}
}
