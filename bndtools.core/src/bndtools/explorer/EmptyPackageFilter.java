package bndtools.explorer;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;


/**
 * Filters out all empty package fragments.
 */
public class EmptyPackageFilter extends ViewerFilter {

	/*
	 * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean select(Viewer viewer, Object parent, Object element) {
		if (element instanceof IPackageFragment) {
			IPackageFragment pkg= (IPackageFragment)element;
			try {
				return pkg.hasChildren() || hasUnfilteredResources(viewer, pkg);
			} catch (JavaModelException e) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Tells whether the given package has unfiltered resources.
	 *
	 * @param viewer the viewer
	 * @param pkg the package
	 * @return <code>true</code> if the package has unfiltered resources
	 * @throws JavaModelException if this element does not exist or if an exception occurs while
	 *             accessing its corresponding resource
	 * @since 3.4.1
	 */
	static boolean hasUnfilteredResources(Viewer viewer, IPackageFragment pkg) throws JavaModelException {
		Object[] resources= pkg.getNonJavaResources();
		int length= resources.length;
		if (length == 0)
			return false;

		if (!(viewer instanceof StructuredViewer))
			return true;

		ViewerFilter[] filters= ((StructuredViewer)viewer).getFilters();
		resourceLoop: for (int i= 0; i < length; i++) {
			for (int j= 0; j < filters.length; j++) {
				if (!filters[j].select(viewer, pkg, resources[i]))
					continue resourceLoop;
			}
			return true;

		}
		return false;
	}


}
