package bndtools.release.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import aQute.bnd.differ.Baseline;
import aQute.bnd.osgi.Constants;
import aQute.bnd.service.diff.Delta;
import aQute.bnd.service.diff.Diff;
import aQute.bnd.service.diff.Tree;
import aQute.bnd.service.diff.Type;

public class TreeContentProvider implements ITreeContentProvider {

	private boolean showAll = false;

	@Override
	public Object[] getChildren(Object parent) {
		if (parent instanceof List) {
			return ((List<?>) parent).toArray();
		}
		if (parent instanceof Baseline) {
			Collection<? extends Diff> diffs = ((Baseline) parent).getDiff()
				.getChildren();
			List<Diff> filteredDiffs = new ArrayList<>();
			for (Diff diff : diffs) {
				switch (diff.getType()) {
					case API :
					case MANIFEST :
					case RESOURCES :
						if (getChildren(diff).length == 0)
							continue;
						break;
					default :
						break;
				}
				filteredDiffs.add(diff);
			}
			return filteredDiffs.toArray(new Diff[0]);
		}
		if (parent instanceof Tree) {
			return ((Tree) parent).getChildren();
		}

		if (parent instanceof Diff) {
			return getChildren((Diff) parent);
		}

		return new Object[0];
	}

	private Object[] getChildren(Diff parent) {
		Collection<? extends Diff> diffs = parent.getChildren();
		List<Diff> filteredDiffs = new ArrayList<>();
		for (Diff diff : diffs) {
			if (!showAll && (diff.getDelta() == Delta.IGNORED || diff.getDelta() == Delta.UNCHANGED)) {
				continue;
			}
			if (diff.getType() == Type.SHA) {
				continue;
			}
			if ("META-INF/MANIFEST.MF".equals(diff.getName())) { //$NON-NLS-1$
				continue;
			}
			if (diff.getType() == Type.HEADER && diff.getName()
				.startsWith(Constants.BUNDLE_VERSION)) {
				continue;
			}
			filteredDiffs.add(diff);
		}
		return filteredDiffs.toArray(new Diff[0]);
	}

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
			for (Diff diff : ((Baseline) parent).getDiff()
				.getChildren()) {
				switch (diff.getType()) {
					case API :
					case MANIFEST :
					case RESOURCES :
						if (getChildren(diff).length > 0)
							return true;
						break;
					default :
						break;
				}
			}
			return false;
		}

		if (parent instanceof Tree) {
			return ((Tree) parent).getChildren().length > 0;
		}

		if (parent instanceof Diff) {
			Diff diff = (Diff) parent;
			return getChildren(diff).length > 0;
		}

		return false;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@Override
	public void dispose() {}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

	public boolean isShowAll() {
		return showAll;
	}

	public void setShowAll(boolean showAll) {
		this.showAll = showAll;
	}
}
