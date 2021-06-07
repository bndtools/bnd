package bndtools.explorer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import aQute.bnd.build.Project;
import bndtools.Plugin;

/**
 * Only select bndtools projects
 */
public class BndProjects extends ViewerFilter {

	/*
	 * @see
	 * org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.
	 * Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean select(Viewer viewer, Object parent, Object element) {
		if (element instanceof IProject) {

			IProject p = (IProject) element;

			try {
				IProjectNature nature = p.getNature(Plugin.BNDTOOLS_NATURE);
				return nature != null || p.getName() == Project.BNDCNF;
			} catch (CoreException e) {}

		}
		return true;
	}

}
