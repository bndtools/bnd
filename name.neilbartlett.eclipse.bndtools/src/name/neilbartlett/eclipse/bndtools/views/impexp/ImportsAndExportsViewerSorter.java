/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 *******************************************************************************/
package name.neilbartlett.eclipse.bndtools.views.impexp;

import name.neilbartlett.eclipse.bndtools.editor.model.HeaderClause;
import name.neilbartlett.eclipse.bndtools.views.impexp.ImportsExportsTreeContentProvider.ImportUsedByClass;
import name.neilbartlett.eclipse.bndtools.views.impexp.ImportsExportsTreeContentProvider.ImportUsedByPackage;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

public class ImportsAndExportsViewerSorter extends ViewerSorter {
	
	@Override
	public int category(Object element) {
		if(element instanceof String)
			return 0;
		if(element instanceof HeaderClause)
			return 1;
		if(element instanceof ImportUsedByPackage)
			return 2;
		if(element instanceof ImportUsedByClass)
			return 3;
		
		return -1;
	}
	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		int cat1 = category(e1);
		int cat2 = category(e2);
		
		if (cat1 != cat2) {
			return cat1 - cat2;
		}
		
		if(e1 instanceof String && e2 instanceof String) {
			String s1 = (String) e1;
			String s2 = (String) e2;
			
			// We want "Import Packages" before "Export Packages", so we reverse the normal order
			return s2.compareTo(s1);
		}
		
		@SuppressWarnings("unchecked")
		Comparable<Object> c1 = (Comparable<Object>) e1;
		return c1.compareTo(e2);
	}
}
