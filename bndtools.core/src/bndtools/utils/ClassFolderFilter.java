package bndtools.utils;

import java.util.Locale;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

public class ClassFolderFilter extends ViewerFilter {

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (element instanceof IProject && !((IProject) element).isOpen()) {
			return false;
		}
		if (element instanceof IContainer) {
			try {
				IResource[] members = ((IContainer) element).members();
				for (IResource member : members) {
					if (member instanceof IFile && member.getName()
						.toLowerCase(Locale.ENGLISH)
						.endsWith(".class")) {
						return true;
					} else if (member instanceof IContainer) {
						boolean memberResult = select(viewer, element, member);
						if (memberResult)
							return true;
					}
				}
			} catch (CoreException e) {
				// Ignore
			}
		}
		return false;
	}

}
