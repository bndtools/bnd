package bndtools.jareditor.internal;

import java.util.Optional;

import org.bndtools.api.BndtoolsConstants;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

public class TemporaryProjectFilter extends ViewerFilter {

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (element instanceof IJavaProject) {
			element = ((IJavaProject) element).getProject();
		}

		return !Optional.ofNullable(element)
			.filter(IProject.class::isInstance)
			.map(IProject.class::cast)
			.filter(p -> BndtoolsConstants.BNDTOOLS_JAREDITOR_TEMP_PROJECT_NAME.equals(p
				.getName()))
			.isPresent();
	}

}