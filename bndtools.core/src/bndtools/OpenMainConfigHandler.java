package bndtools;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.FileEditorInput;

import bndtools.central.Central;

public class OpenMainConfigHandler extends AbstractHandler {
	private static final ILogger logger = Logger.getLogger(OpenMainConfigHandler.class);

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			IFile buildFile = Central.getWorkspaceBuildFile();
			if (buildFile == null)
				return null;

			FileEditorInput input = new FileEditorInput(buildFile);
			IWorkbenchPage page = HandlerUtil.getActiveWorkbenchWindowChecked(event)
				.getActivePage();
			page.openEditor(input, "bndtools.bndWorkspaceConfigEditor", true);
		} catch (PartInitException e) {
			ErrorDialog.openError(HandlerUtil.getActiveWorkbenchWindowChecked(event)
				.getShell(), "Error", "Unable to open editor", e.getStatus());
		} catch (Exception e) {
			logger.logError("Error retrieving bnd configuration file", e);
		}

		return null;
	}

}
