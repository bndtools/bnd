package org.bndtools.core.resolve.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.bndtools.core.ui.resource.RequirementWithResourceLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.osgi.resource.Requirement;

import aQute.libg.glob.Glob;

public class UnresolvedRequirementsContentProvider implements ITreeContentProvider {

	private String wildcardFilter = null;
	private RequirementWithResourceLabelProvider	labelProvider;

	public UnresolvedRequirementsContentProvider(RequirementWithResourceLabelProvider labelProvider) {
		this.labelProvider = labelProvider;
	}

	@Override
	public Object[] getElements(Object inputElement) {

		List<Object[]> arrays = new LinkedList<>();

		if (inputElement instanceof Object[])
			arrays.add((Object[]) inputElement);

		if (inputElement instanceof Collection<?>) {
			Collection<?> coll = (Collection<?>) inputElement;
			arrays.add(coll.toArray());
		}

		return filter(flatten(arrays));
	}

	private Object[] flatten(List<Object[]> arrays) {
		// Iterate over once to count the lengths
		int length = 0;
		for (Object[] array : arrays) {
			length += array.length;
		}
		Object[] result = new Object[length];

		// Iterate again to flatten out the arrays
		int position = 0;
		for (Object[] array : arrays) {
			System.arraycopy(array, 0, result, position, array.length);
			position += array.length;
		}
		return result;
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// TODO Auto-generated method stub

	}

	@Override
	public Object[] getChildren(Object parentElement) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getParent(Object element) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		// TODO Auto-generated method stub
		return false;
	}

	private Object[] filter(Object[] array) {
		List<Object> filteredResults = new ArrayList<>();
		if (wildcardFilter == null || wildcardFilter.equals("*") || wildcardFilter.equals("")) {
			return array;
		} else {
			String[] split = wildcardFilter.split("\\s+");
			Glob globs[] = new Glob[split.length];
			for (int i = 0; i < split.length; i++) {
				globs[i] = new Glob(split[i].toLowerCase());
			}

			Arrays.stream(array)
				.forEach(obj -> {

					if (obj instanceof Requirement rw) {

						for (Glob g : globs) {
							if (g.matcher(labelProvider.getLabel(rw)
								.toString()
								.toLowerCase())
								.find()) {
								filteredResults.add(obj);
								return;
							}
						}
					}

				});

		}

		return filteredResults.toArray();
	}

	public void setFilter(String filter) {
		this.wildcardFilter = filter;
	}

}
