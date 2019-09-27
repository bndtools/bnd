package bndtools.model.obr;

import java.util.Comparator;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

public class SorterComparatorAdapter implements Comparator<Object> {

	private final Viewer			viewer;
	private final ViewerComparator	sorter;

	public SorterComparatorAdapter(Viewer viewer, ViewerComparator sorter) {
		this.viewer = viewer;
		this.sorter = sorter;
	}

	@Override
	public int compare(Object o1, Object o2) {
		int diff;

		diff = sorter.category(o1) - sorter.category(o2);
		if (diff != 0)
			return diff;

		diff = sorter.compare(viewer, o1, o2);

		return diff;
	}

}
