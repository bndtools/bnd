package bndtools.command;

import static aQute.bnd.exceptions.FunctionWithException.asFunction;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ISetSelectionTarget;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import bndtools.Plugin;
import bndtools.explorer.BndtoolsExplorer;

public class OpenProjectHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IProject project = ResourcesPlugin.getWorkspace()
			.getRoot()
			.getProject(event.getParameter(ProjectNamesParameterValues.PROJECT_NAME));

		if (!project.isOpen()) {
			new WorkspaceJob("open project") {
				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
					project.open(monitor);
					return Status.OK_STATUS;
				}
			}.schedule();
		}

		try {
			selectProject(project);
		} catch (Exception e) {
			IEvaluationContext context = (IEvaluationContext) event.getApplicationContext();
			ErrorDialog.openError((Shell) context.getVariable("activeShell"), "Open Project",
				"Unable to select project", new Status(IStatus.ERROR, Plugin.PLUGIN_ID, e.getMessage(), e));
		}

		return null;
	}

	private static void selectProject(IProject project) throws Exception {
		IWorkbenchPage activePage = PlatformUI.getWorkbench()
			.getActiveWorkbenchWindow()
			.getActivePage();

		Optional<String> viewPartId = restoreViewPartId(activePage, BndtoolsExplorer.VIEW_ID);

		if (!viewPartId.isPresent()) {
			viewPartId = restoreViewPartId(activePage, "org.eclipse.jdt.ui.PackageExplorer");
		}

		if (!viewPartId.isPresent()) {
			viewPartId = Optional.of(activePage.showView(BndtoolsExplorer.VIEW_ID)
				.getSite()
				.getId());
		}

		IResource select = getNotNull(() -> project.findMember(Workspace.BUILDFILE),
			() -> project.findMember(Project.BNDFILE), () -> project);

		assert select != null;

		viewPartId.map(asFunction(partId -> activePage.showView(partId)))
			.map(ISetSelectionTarget.class::cast)
			.ifPresent(target -> target.selectReveal(new StructuredSelection(select)));
	}

	@SafeVarargs
	private static <T> T getNotNull(Supplier<T>... suppliers) {
		for (Supplier<T> s : suppliers) {
			T t = s.get();
			if (t != null)
				return t;
		}
		throw new IllegalArgumentException("Caller must provide a last supplier that is not null");
	}

	private static Optional<String> restoreViewPartId(IWorkbenchPage page, String viewId) {
		return Arrays.stream(page.getViewReferences())
			.filter(ref -> Objects.equals(ref.getId(), viewId))
			.map(ref -> ref.getPart(true))
			.map(part -> part.getSite()
				.getId())
			.findFirst();
	}
}
