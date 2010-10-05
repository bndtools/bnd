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
package bndtools.diff;

import java.util.Set;
import java.util.TreeSet;

import org.objectweb.asm.tree.ClassNode;

public class ClassInfo extends ClassNode implements Comparable<ClassInfo> {
	
	public static final int CHANGE_CODE_NONE = 0;
	public static final int CHANGE_CODE_NEW = 1;
	public static final int CHANGE_CODE_MODIFIED = 2;
	public static final int CHANGE_CODE_REMOVED = 3;

	private Set<MethodInfo> publicMethods = new TreeSet<MethodInfo>();
	private Set<FieldInfo> publicFields = new TreeSet<FieldInfo>();
	
	private int changeCode = CHANGE_CODE_NONE;
	
	private PackageInfo packageInfo;
	
	public ClassInfo(PackageInfo pi) {
		super();
		this.packageInfo = pi;
	}

	public int compareTo(ClassInfo o) {
		if (o == null) {
			return -1;
		}
		return name.compareTo(o.name);
	}
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj.getClass() != this.getClass()) {
			return false;
		}
		return ((ClassInfo) obj).name.equals(name);
	}
	
	public int hashCode() {
		return name.hashCode();
	}

	public Set<MethodInfo> getDeletedMethods() {
		Set<MethodInfo> ret = new TreeSet<MethodInfo>();
		for (MethodInfo mi : publicMethods) {
			if (mi.getChangeCode() != MethodInfo.CHANGE_REMOVED) {
				continue;
			}
			ret.add(mi);
		}
		return ret;
	}

	public Set<MethodInfo> getNewMethods() {
		Set<MethodInfo> ret = new TreeSet<MethodInfo>();
		for (MethodInfo mi : publicMethods) {
			if (mi.getChangeCode() != MethodInfo.CHANGE_NEW) {
				continue;
			}
			ret.add(mi);
		}
		return ret;
	}
	
	public Set<MethodInfo> getMethods() {
		return publicMethods;
	}

	public Set<MethodInfo> getChangedMethods() {
		Set<MethodInfo> ret = new TreeSet<MethodInfo>();
		for (MethodInfo mi : publicMethods) {
			if (mi.getChangeCode() == MethodInfo.CHANGE_NONE) {
				continue;
			}
			ret.add(mi);
		}
		return ret;
	}

	public Set<FieldInfo> getDeletedFields() {
		Set<FieldInfo> ret = new TreeSet<FieldInfo>();
		for (FieldInfo mi : publicFields) {
			if (mi.getChangeCode() != FieldInfo.CHANGE_REMOVED) {
				continue;
			}
			ret.add(mi);
		}
		return ret;
	}

	public Set<FieldInfo> getNewFields() {
		Set<FieldInfo> ret = new TreeSet<FieldInfo>();
		for (FieldInfo mi : publicFields) {
			if (mi.getChangeCode() != FieldInfo.CHANGE_NEW) {
				continue;
			}
			ret.add(mi);
		}
		return ret;
	}
	
	public Set<FieldInfo> getFields() {
		return publicFields;
	}

	public Set<FieldInfo> getChangedFields() {
		Set<FieldInfo> ret = new TreeSet<FieldInfo>();
		for (FieldInfo mi : publicFields) {
			if (mi.getChangeCode() == FieldInfo.CHANGE_NONE) {
				continue;
			}
			ret.add(mi);
		}
		return ret;
	}

	
	public int getChangeCode() {
		return changeCode;
	}

	public void setChangeCode(int changeCode) {
		this.changeCode = changeCode;
	}
	
	public String getName() {
		return super.name;
	}
	
	public void addPublicMethod(MethodInfo mi) {
		publicMethods.add(mi);
	}

	public void addPublicField(FieldInfo fi) {
		publicFields.add(fi);
	}

	public PackageInfo getPackageInfo() {
		return packageInfo;
	}
	
	public String toString() {
		return name;
	}
}
