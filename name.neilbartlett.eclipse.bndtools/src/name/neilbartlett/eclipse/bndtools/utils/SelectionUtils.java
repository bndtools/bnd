/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 ******************************************************************************/
package name.neilbartlett.eclipse.bndtools.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

public class SelectionUtils {
	public static <T> Collection<T> getSelectionMembers(ISelection selection, Class<T> clazz) {
		return getSelectionMembers(selection, clazz, null);
	}
	public static <T> Collection<T> getSelectionMembers(ISelection selection, Class<T> clazz, Predicate<? super T> filter) {
		if(selection.isEmpty() || !(selection instanceof IStructuredSelection)) {
			return Collections.emptyList();
		}
		
		IStructuredSelection structSel = (IStructuredSelection) selection;
		List<T> result = new ArrayList<T>(structSel.size());
		Iterator<?> iter = structSel.iterator();
		while(iter.hasNext()) {
			Object element = iter.next();
			if(clazz.isInstance(element)) {
				@SuppressWarnings("unchecked")
				T casted = (T) element;
				if(filter == null || filter.select(casted)) {
					result.add(casted);
				}
			} else if(element instanceof IAdaptable) {
				@SuppressWarnings("unchecked")
				T adapted = (T) ((IAdaptable) element).getAdapter(clazz);
				if(adapted != null) {
					if(filter == null || filter.select(adapted)) {
						result.add(adapted);
					}
				}
			}
		}
		return result;
	}
}
