/*******************************************************************************
 * Copyright (c) 2010 Per Kr. Soreide.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Per Kr. Soreide - initial API and implementation
 *******************************************************************************/
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

	public Object[] getChildren(Object parent) {
		if (parent instanceof List) {
			return ((List<?>) parent).toArray();
		}
		if (parent instanceof Baseline) {
			if (isShowAll()) {
				return ((Baseline) parent).getPackageInfos().toArray();
			}
			Set<Info> infos = ((Baseline) parent).getPackageInfos();
			List<Info> filteredDiffs = new ArrayList<Info>();
			for (Info info : infos) {
				if (info.packageDiff.getDelta() == Delta.IGNORED || (info.packageDiff.getDelta() == Delta.UNCHANGED && info.olderVersion.equals(info.suggestedVersion))) {
					continue;
				}
				filteredDiffs.add(info);
			}
			return filteredDiffs.toArray(new Info[filteredDiffs.size()]);

		}

		return new Object[0];
	}
	/*
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	public Object getParent(Object item) {
		return null;
	}

	/*
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	public boolean hasChildren(Object parent) {
		if (parent instanceof Baseline) {
			return ((Baseline) parent).getPackageInfos().size() > 0;
		}

		return false;
	}
	/*
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}
	/*
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	public void dispose() {
	}
	/*
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	public boolean isShowAll() {
		return showAll;
	}
	public void setShowAll(boolean showAll) {
		this.showAll = showAll;
	}

}
