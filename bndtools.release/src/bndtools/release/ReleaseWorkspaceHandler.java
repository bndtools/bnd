package bndtools.release;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PlatformUI;

public class ReleaseWorkspaceHandler extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {

			if (!PlatformUI.getWorkbench().saveAllEditors(true)) {
				return null;
			}

			WorkspaceAnalyserJob job = new WorkspaceAnalyserJob();
			job.schedule();
			
		} catch (Exception e) {
			throw new ExecutionException(e.getMessage(), e);
		}
		
		return null;
	}
}
