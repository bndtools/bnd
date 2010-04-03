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
package bndtools.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.part.ResourceTransfer;

public abstract class PackageDropAdapter<T> extends ViewerDropAdapter {
	
	public PackageDropAdapter(Viewer viewer) {
		super(viewer);
	}
	
	protected abstract T createNewEntry(String packageName);
	
	protected abstract void addRows(int index, Collection<T> rows);
	
	protected abstract int indexOf(Object object);
	
	@Override
	public boolean validateDrop(Object target, int operation, TransferData transferType) {
		return TextTransfer.getInstance().isSupportedType(transferType)
			|| ResourceTransfer.getInstance().isSupportedType(transferType);
	}
	@Override
	public void dragEnter(DropTargetEvent event) {
		super.dragEnter(event);
		event.detail = DND.DROP_COPY;
	}
	@Override
	public boolean performDrop(Object data) {
		int insertionIndex = -1;
		Object target = getCurrentTarget();
		if(target != null) {
			insertionIndex = indexOf(target);
			int loc = getCurrentLocation();
			if(loc == LOCATION_ON || loc == LOCATION_AFTER)
				insertionIndex++;
		}
		
		List<T> newEntries = new ArrayList<T>();
		if(data instanceof String) {
			String stringData = (String) data;
			StringTokenizer tok = new StringTokenizer(stringData, ",");
			while(tok.hasMoreTokens()) {
				String pkgName = tok.nextToken().trim();
				newEntries.add(createNewEntry(pkgName));
			}
		} else if(data instanceof IResource[]) {
			for (IResource resource : (IResource[]) data) {
				IJavaElement javaElement = JavaCore.create(resource);
				if(javaElement instanceof IPackageFragment) {
					newEntries.add(createNewEntry(javaElement.getElementName()));
				}
			}
		}
		addRows(insertionIndex, newEntries);
		return true;
	}
}
