package bndtools.refactor;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.DeleteParticipant;

import aQute.bnd.build.Workspace;

public class CnfDeleteParticipant extends DeleteParticipant {

	private IProject project;

	@Override
	protected boolean initialize(Object element) {
		if (element instanceof IProject candidate && candidate.isOpen() && candidate.getLocation() != null
			&& Workspace.CNFDIR.equals(candidate.getLocation().lastSegment())) {
			project = candidate;
			return true;
		}
		return false;
	}

	@Override
	public String getName() {
		return "Bndtools cnf Delete Participant";
	}

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor monitor, CheckConditionsContext context) {
		try {
			project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		} catch (CoreException e) {
			return RefactoringStatus.createFatalErrorStatus(e.getMessage());
		}
		return new RefactoringStatus();
	}

	@Override
	public Change createChange(IProgressMonitor monitor) {
		return null;
	}
}