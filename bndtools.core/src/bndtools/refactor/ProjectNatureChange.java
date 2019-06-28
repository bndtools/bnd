package bndtools.refactor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public abstract class ProjectNatureChange extends Change {

	protected final IProject project;

	public ProjectNatureChange(IProject project) {
		this.project = project;
	}

	@Override
	public void initializeValidationData(IProgressMonitor pm) {
		// TODO
	}

	@Override
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		// TODO
		return status;
	}

	@Override
	public final Change perform(IProgressMonitor pm) throws CoreException {
		final IProjectDescription desc = project.getDescription();
		Set<String> natures = new HashSet<>(Arrays.asList(desc.getNatureIds()));
		boolean changed = modifyNatures(natures);
		if (changed) {
			desc.setNatureIds(natures.toArray(new String[0]));
			project.setDescription(desc, pm);
		}

		return createInverse();
	}

	protected abstract boolean modifyNatures(Set<String> natures);

	protected abstract Change createInverse();

	@Override
	public Object getModifiedElement() {
		return project;
	}

}
