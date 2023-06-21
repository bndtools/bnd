package bndtools.jface.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import aQute.lib.strings.Strings;
import aQute.libg.glob.Glob;

/**
 * A filter used in conjunction with <code>FilteredTree</code>. In order to
 * determine if a node should be filtered it uses the content and label provider
 * of the tree to do pattern matching on its children. This causes the entire
 * tree structure to be realized. Note that the label provider must implement
 * ILabelProvider.
 */
public class TreeFilter extends ViewerFilter {
	private Map<Object, Object>					cache			= new HashMap<>();
	private Map<Object, Boolean>				foundAnyCache	= new HashMap<>();

	private boolean								useCache		= false;
	private BiFunction<Viewer, Object, Boolean>	matcher;
	private static Object[]						EMPTY			= new Object[0];

	@Override
	public Object[] filter(Viewer viewer, Object parent, Object[] elements) {
		if (matcher == null) {
			return elements;
		}

		if (!useCache) {
			return super.filter(viewer, parent, elements);
		}

		Object[] filtered = (Object[]) cache.get(parent);
		if (filtered == null) {
			Boolean foundAny = foundAnyCache.get(parent);
			if (foundAny != null && !foundAny.booleanValue()) {
				filtered = EMPTY;
			} else {
				filtered = super.filter(viewer, parent, elements);
			}
			cache.put(parent, filtered);
		}
		return filtered;
	}

	/**
	 * Returns true if any of the elements makes it through the filter. This
	 * method uses caching if enabled; the computation is done in
	 * computeAnyVisible.
	 *
	 * @param viewer
	 * @param parent
	 * @param elements the elements (must not be an empty array)
	 * @return true if any of the elements makes it through the filter.
	 */
	private boolean isAnyVisible(Viewer viewer, Object parent, Object[] elements) {
		if (matcher == null) {
			return true;
		}

		if (!useCache) {
			return computeAnyVisible(viewer, elements);
		}

		Object[] filtered = (Object[]) cache.get(parent);
		if (filtered != null) {
			return filtered.length > 0;
		}
		Boolean foundAny = foundAnyCache.get(parent);
		if (foundAny == null) {
			foundAny = computeAnyVisible(viewer, elements) ? Boolean.TRUE : Boolean.FALSE;
			foundAnyCache.put(parent, foundAny);
		}
		return foundAny.booleanValue();
	}

	/**
	 * Returns true if any of the elements makes it through the filter.
	 *
	 * @param viewer the viewer
	 * @param elements the elements to test
	 * @return <code>true</code> if any of the elements makes it through the
	 *         filter
	 */
	private boolean computeAnyVisible(Viewer viewer, Object[] elements) {
		boolean elementFound = false;
		for (int i = 0; i < elements.length && !elementFound; i++) {
			Object element = elements[i];
			elementFound = isElementVisible(viewer, element);
		}
		return elementFound;
	}

	@Override
	public final boolean select(Viewer viewer, Object parentElement, Object element) {
		return isElementVisible(viewer, element);
	}

	/**
	 * The pattern string for which this filter should select elements in the
	 * viewer.
	 *
	 * @param patternString the pattern string.
	 */
	public void setPattern(String patternString) {
		clearCaches();
		if (Strings.nonNullOrEmpty(patternString)) {
			matcher = createMatcher(patternString);
		} else {
			matcher = null;
		}
	}

	/**
	 * Clears the caches used for optimizing this filter. Needs to be called
	 * whenever the tree content changes.
	 */
	/* package */ void clearCaches() {
		cache.clear();
		foundAnyCache.clear();
	}

	/**
	 * Answers whether the given element is a valid selection in the filtered
	 * tree. For example, if a tree has items that are categorized, the category
	 * itself may not be a valid selection since it is used merely to organize
	 * the elements.
	 *
	 * @param element the element to check
	 * @return true if this element is eligible for automatic selection
	 */
	public boolean isElementSelectable(Object element) {
		return element != null;
	}

	/**
	 * Answers whether the given element in the given viewer matches the filter
	 * pattern. This is a default implementation that will show a leaf element
	 * in the tree based on whether the provided filter text matches the text of
	 * the given element's text, or that of it's children (if the element has
	 * any). Subclasses may override this method.
	 *
	 * @param viewer the tree viewer in which the element resides
	 * @param element the element in the tree to check for a match
	 * @return true if the element matches the filter pattern
	 */
	public boolean isElementVisible(Viewer viewer, Object element) {
		return isParentMatch(viewer, element) || isLeafMatch(viewer, element);
	}

	/**
	 * Check if the parent (category) is a match to the filter text. The default
	 * behavior returns true if the element has at least one child element that
	 * is a match with the filter text. Subclasses may override this method.
	 *
	 * @param viewer the viewer that contains the element
	 * @param element the tree element to check
	 * @return true if the given element has children that matches the filter
	 *         text
	 */
	protected boolean isParentMatch(Viewer viewer, Object element) {
		if (viewer instanceof AbstractTreeViewer
			&& ((AbstractTreeViewer) viewer).getContentProvider() instanceof ITreeContentProvider) {
			Object[] children = ((ITreeContentProvider) ((AbstractTreeViewer) viewer).getContentProvider())
				.getChildren(element);

			return children != null && children.length > 0 && isAnyVisible(viewer, element, children);
		}
		return false;
	}

	/**
	 * Check if the current (leaf) element is a match with the filter text. The
	 * default behavior checks that the label of the element is a match.
	 * Subclasses should override this method.
	 *
	 * @param viewer the viewer that contains the element
	 * @param element the tree element to check
	 * @return true if the given element's label matches the filter text
	 */
	protected boolean isLeafMatch(Viewer viewer, Object element) {
		return matches(viewer, element);
	}

	public boolean matches(Viewer viewer, Object element) {
		return matcher == null || matcher.apply(viewer, element);
	}

	public String getLabel(Viewer viewer, Object element) {
		return ((ILabelProvider) ((ContentViewer) viewer).getLabelProvider()).getText(element);
	}

	/**
	 * Can be called by the filtered tree to turn on caching.
	 *
	 * @param useCache The useCache to set.
	 */
	void setUseCache(boolean useCache) {
		this.useCache = useCache;
	}

	protected BiFunction<Viewer, Object, Boolean> createMatcher(String pattern) {
		Glob glob = new Glob(pattern);
		return (viewer, element) -> {
			String label = getLabel(viewer, element);
			return glob.finds(label) >= 0;
		};
	}
}
