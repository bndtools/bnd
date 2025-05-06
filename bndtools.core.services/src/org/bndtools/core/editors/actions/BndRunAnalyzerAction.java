package org.bndtools.core.editors.actions;

import static bndtools.central.RepositoryUtils.listRepositories;
import static org.bndtools.core.editors.actions.BndRunAnalyzer.collectReferences;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.osgi.service.component.annotations.Component;

import aQute.bnd.build.Container;
import aQute.bnd.build.Workspace;
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.strings.Strings;
import bndtools.central.Central;

@Component
public class BndRunAnalyzerAction implements IObjectActionDelegate {

	private static final String	PLUGIN_ID		= "org.bndtools.core.editors.actions.BndRunAnalysis";
	private List<IFile>			selectedBndRuns	= new ArrayList<>();

	@Override
	public void run(IAction action) {

		if (selectedBndRuns.isEmpty()) {
			return;
		}

		try {

			runAsync();

		} catch (Exception e) {
			error(e, "Error analyzing .bndrun");
		}
	}

	private void runAsync() {
		Job job = new Job("Analyzing .bndrun") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {

				try {
					monitor.beginTask("Running Bndrun analysis", IProgressMonitor.UNKNOWN);

					// ‑‑ long‑running work
					String output = analyze();
					// -----------------------------------------------------------------------

					Display.getDefault()
						.asyncExec(() -> {
							Shell shell = Display.getDefault()
								.getActiveShell();
							OutputDialog dlg = new OutputDialog(shell, "Analyze Bndrun Result (Experimental)", output);
							dlg.open();
						});

					return Status.OK_STATUS;

				} catch (Exception e) {
					// Log & propagate the error so it shows red in the
					// Progress view
					return new Status(IStatus.ERROR, PLUGIN_ID, "Error analysing .bndrun", e);
				} finally {
					monitor.done();
				}
			}
		};

		job.setUser(true);
		job.schedule();
	}

	private String analyze() throws Exception {
		Workspace ws = Central.getWorkspace();

		List<File> bndRuns = selectedBndRuns.stream()
			.map(f -> f.getLocation()
				.toFile())
			.toList();
		List<Container> references = collectReferences(ws, bndRuns);
		List<RepositoryPlugin> repos = listRepositories(ws, true).stream()
			.toList();

		BndRunAnalyzer bndRunAnalysis = new BndRunAnalyzer();
		return bndRunAnalysis.analyze(repos, references)
			.print();
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		selectedBndRuns = new ArrayList<>();

		if (selection instanceof IStructuredSelection s) {
			List<?> list = s.toList();

			for (Object element : s) {

				if (element instanceof IFile file) {
					selectedBndRuns.add(file);
				}

			}

		}
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		// Not needed for simple actions
	}

	private static void error(Throwable e, String format, Object... args) {
		String message = Strings.format(format, args);
		Status status = new Status(IStatus.ERROR, PLUGIN_ID, 0, message, e);
		Display.getDefault()
			.asyncExec(() -> {
				ErrorDialog.openError(null, "Error", message, status);
			});
	}
}
