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
package bndtools.model.importanalysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import aQute.lib.osgi.Clazz;
import aQute.lib.osgi.Descriptors.PackageRef;

public class ImportsExportsTreeContentProvider implements ITreeContentProvider {

	static final Object IMPORTS_PLACEHOLDER = new String("_1_imports_placeholder");
	static final Object EXPORTS_PLACEHOLDER = new String("_3_exports_placeholder");
	static final Object REQUIRED_PLACEHOLDER = new String("_2_requires_placeholder");

	private final Set<String> exportNames = new HashSet<String>();
	private ImportsExportsAnalysisResult importsAndExports = null;

	public Object[] getChildren(Object parentElement) {
		Collection<?> result;
		if(parentElement == IMPORTS_PLACEHOLDER)
			result = importsAndExports != null ? importsAndExports.imports : Collections.emptyList();
		else if(parentElement == EXPORTS_PLACEHOLDER)
			result = importsAndExports != null ? importsAndExports.exports : Collections.emptyList();
		else if(parentElement == REQUIRED_PLACEHOLDER)
		    result = importsAndExports != null ? importsAndExports.requiredBundles : Collections.emptyList();
		else if(parentElement instanceof ExportPackage) {
			ExportPackage exportPackage = (ExportPackage) parentElement;
			List<PackageRef> uses = exportPackage.getUses();
			List<ExportUsesPackage> temp = new ArrayList<ExportUsesPackage>(uses.size());
			for (PackageRef pkg : uses) {
				temp.add(new ExportUsesPackage(exportPackage, pkg.getFQN()));
			}
			result = temp;
		} else if(parentElement instanceof ImportPackage) {
			ImportPackage importPackage = (ImportPackage) parentElement;
			Collection<? extends String> usedByNames = importPackage.getUsedBy();
			List<ImportUsedByPackage> temp = new ArrayList<ImportUsedByPackage>(usedByNames.size());
			for (String name : usedByNames) {
				temp.add(new ImportUsedByPackage(importPackage, name));
			}
			result = temp;
		} else if(parentElement instanceof ImportUsedByPackage) {
			ImportUsedByPackage importUsedBy = (ImportUsedByPackage) parentElement;
			Collection<Clazz> importingClasses = importUsedBy.importPackage.getImportingClasses(importUsedBy.usedByName);
			if(importingClasses != null) {
				List<ImportUsedByClass> temp = new ArrayList<ImportUsedByClass>(importingClasses.size());
				for (Clazz clazz : importingClasses) {
					temp.add(new ImportUsedByClass(importUsedBy, clazz));
				}
				result = temp;
			} else {
				result = Collections.emptyList();
			}
		} else {
			result = Collections.emptyList();
		}
		return result.toArray(new Object[result.size()]);
	}

	public Object getParent(Object element) {
		if(element instanceof ImportUsedByPackage) {
			return ((ImportUsedByPackage) element).importPackage;
		}

		if(element instanceof ImportPackage)
			return IMPORTS_PLACEHOLDER;

		if(element instanceof ExportPackage)
			return EXPORTS_PLACEHOLDER;

		if(element instanceof RequiredBundle)
		    return REQUIRED_PLACEHOLDER;

		return null;
	}

	public boolean hasChildren(Object element) {
		if(element == IMPORTS_PLACEHOLDER)
			return importsAndExports.imports != null && !importsAndExports.imports.isEmpty();

		if(element == EXPORTS_PLACEHOLDER)
			return importsAndExports.imports != null && !importsAndExports.exports.isEmpty();

		if(element == REQUIRED_PLACEHOLDER)
		    return importsAndExports.requiredBundles != null && !importsAndExports.requiredBundles.isEmpty();

		if(element instanceof ExportPackage) {
			List<PackageRef> uses = ((ExportPackage) element).getUses();
			return uses != null && !uses.isEmpty();
		}

		if(element instanceof ImportPackage) {
			Collection<? extends String> usedBy = ((ImportPackage) element).getUsedBy();
			return usedBy != null && !usedBy.isEmpty();
		}

		if(element instanceof ImportUsedByPackage)
			return true;

		return false;
	}

	public Object[] getElements(Object inputElement) {
	    Object[] result;
	    if(importsAndExports.requiredBundles != null && !importsAndExports.requiredBundles.isEmpty()) {
	        result = new Object[] { IMPORTS_PLACEHOLDER, REQUIRED_PLACEHOLDER, EXPORTS_PLACEHOLDER };
	    } else {
	        result = new Object[] { IMPORTS_PLACEHOLDER, EXPORTS_PLACEHOLDER };
	    }
	    return result;
	}

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.importsAndExports = (ImportsExportsAnalysisResult) newInput;

		exportNames.clear();
		if(importsAndExports != null) {
			for (ExportPackage export : importsAndExports.exports) {
				exportNames.add(export.getName());
			}
		}
	}

	static class ImportUsedByPackage implements Comparable<ImportUsedByPackage> {

		final ImportPackage importPackage;
		final String usedByName;

		public ImportUsedByPackage(ImportPackage importPackage, String usedByName) {
			this.importPackage = importPackage;
			this.usedByName = usedByName;
		}

		public int compareTo(ImportUsedByPackage other) {
			return this.usedByName.compareTo(other.usedByName);
		}
	}

	public static class ImportUsedByClass implements Comparable<ImportUsedByClass> {

		final ImportUsedByPackage importUsedBy;
		final Clazz clazz;

		public ImportUsedByClass(ImportUsedByPackage importUsedBy, Clazz clazz) {
			this.importUsedBy = importUsedBy;
			this.clazz = clazz;
		}
		public int compareTo(ImportUsedByClass other) {
			return this.clazz.getFQN().compareTo(other.clazz.getFQN());
		}
		public Clazz getClazz() {
		    return clazz;
		}
	}

	public static class ExportUsesPackage implements Comparable<ExportUsesPackage> {

		final ExportPackage exportPackage;
		final String name;

		public ExportUsesPackage(ExportPackage exportPackage, String name) {
			this.exportPackage = exportPackage;
			this.name = name;
		}
		public int compareTo(ExportUsesPackage other) {
			return this.name.compareTo(other.name);
		}
	}
}
