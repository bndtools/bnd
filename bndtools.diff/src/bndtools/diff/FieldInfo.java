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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldNode;

public class FieldInfo implements Comparable<FieldInfo> {

	public static final int CHANGE_NONE = 0;
	public static final int CHANGE_NEW = 10;
	public static final int CHANGE_REMOVED = 20;
	
	private final FieldNode fieldNode;
	private int changeCode = CHANGE_NONE;
	private ClassInfo classInfo;
	
	public FieldInfo(FieldNode fieldNode, ClassInfo classInfo) {
		this.fieldNode = fieldNode;
		this.classInfo = classInfo;
	}

	public int getChangeCode() {
		return changeCode;
	}

	public void setChangeCode(int changeCode) {
		this.changeCode = changeCode;
	}
	
	public String getName() {
		return fieldNode.name;
	}
	
	public String getDesc() {
		return fieldNode.desc;
	}

	public boolean isStatic() {
		return ((fieldNode.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC); 
	}
	public int compareTo(FieldInfo o) {
		return getName().compareTo(o.getName());
	}
	
	public PackageInfo getPackageInfo() {
		return classInfo.getPackageInfo();
	}
	
	public String toString() {
		return getName();
	}
}
