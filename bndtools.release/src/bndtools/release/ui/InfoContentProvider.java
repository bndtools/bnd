package bndtools.release.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import aQute.bnd.differ.Baseline;
import aQute.bnd.differ.Baseline.Info;
import aQute.bnd.service.diff.Delta;

/**
 * @see org.eclipse.jface.viewers.ITreeContentProvider
 */
public class InfoContentProvider implements ITreeContentProvider {

	private boolean showAll = false;

	@Override
	public Object[] getChildren(Object parent) {
		if (parent instanceof List) {
			return ((List<?>) parent).toArray();
		}
		if (parent instanceof Baseline) {
			if (isShowAll()) {
				return ((Baseline) parent).getPackageInfos()
					.toArray();
			}
			Set<Info> infos = ((Baseline) parent).getPackageInfos();
			List<Info> filteredDiffs = new ArrayList<>();
			for (Info info : infos) {
				if (info.packageDiff.getDelta() == Delta.IGNORED || (info.packageDiff.getDelta() == Delta.UNCHANGED
					&& info.olderVersion.equals(info.suggestedVersion))) {
					continue;
				}
				filteredDiffs.add(info);
			}
			return filteredDiffs.toArray(new Info[0]);

		}

		return new Object[0];
	}

	/*
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.
	 * Object)
	 */
	@Override
	public Object getParent(Object item) {
		return null;
	}

	/*
	 * @see
	 * org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.
	 * Object)
	 */
	@Override
	public boolean hasChildren(Object parent) {
		if (parent instanceof Baseline) {
			return ((Baseline) parent).getPackageInfos()
				.size() > 0;
		}

		return false;
	}

	/*
	 * @see
	 * org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.
	 * lang.Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	/*
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	@Override
	public void dispose() {}

	/*
	 * @see
	 * org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface
	 * .viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

	public boolean isShowAll() {
		return showAll;
	}

	public void setShowAll(boolean showAll) {
		this.showAll = showAll;
	}

}
