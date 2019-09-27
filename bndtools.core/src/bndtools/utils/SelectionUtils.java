package bndtools.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.osgi.util.function.Predicate;

public class SelectionUtils {

	public static <T> Collection<T> getSelectionMembers(ISelection selection, Class<T> clazz) throws Exception {
		return getSelectionMembers(selection, clazz, null);
	}

	public static <T> T adaptObject(Object obj, Class<T> clazz) {
		if (clazz.isInstance(obj)) {
			@SuppressWarnings("unchecked")
			T result = (T) obj;
			return result;
		}

		if (obj instanceof IAdaptable) {
			T result = ((IAdaptable) obj).getAdapter(clazz);
			return result;
		}

		return null;
	}

	public static <T> Collection<T> getSelectionMembers(ISelection selection, Class<T> clazz,
		Predicate<? super T> filter) throws Exception {
		if (selection.isEmpty() || !(selection instanceof IStructuredSelection)) {
			return Collections.emptyList();
		}

		IStructuredSelection structSel = (IStructuredSelection) selection;
		List<T> result = new ArrayList<>(structSel.size());
		Iterator<?> iter = structSel.iterator();
		while (iter.hasNext()) {
			Object element = iter.next();
			if (clazz.isInstance(element)) {
				@SuppressWarnings("unchecked")
				T casted = (T) element;
				if (filter == null || filter.test(casted)) {
					result.add(casted);
				}
			} else if (element instanceof IAdaptable) {
				T adapted = ((IAdaptable) element).getAdapter(clazz);
				if (adapted != null) {
					if (filter == null || filter.test(adapted)) {
						result.add(adapted);
					}
				}
			}
		}
		return result;
	}
}
