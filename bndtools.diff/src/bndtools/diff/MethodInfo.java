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
import org.objectweb.asm.tree.MethodNode;

public class MethodInfo implements Comparable<MethodInfo> {

	public static final int CHANGE_NONE = 0;
	public static final int CHANGE_NEW = 10;
	public static final int CHANGE_REMOVED = 20;
	
	private final MethodNode methodNode;
	private int changeCode = CHANGE_NONE;
	private ClassInfo classInfo;
	
	public MethodInfo(MethodNode methodNode, ClassInfo classInfo) {
		this.methodNode = methodNode;
		this.classInfo = classInfo;
	}

	public int getChangeCode() {
		return changeCode;
	}

	public void setChangeCode(int changeCode) {
		this.changeCode = changeCode;
	}
	
	public String getName() {
		return methodNode.name;
	}
	
	public String getDesc() {
		return methodNode.desc;
	}

	public boolean isStatic() {
		return ((methodNode.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC); 
	}

	public int compareTo(MethodInfo o) {
		String from = getName() + getDesc();
		String to = o.getName() + o.getDesc();
		return from.compareTo(to);
	}

	public PackageInfo getPackageInfo() {
		return classInfo.getPackageInfo();
	}
	
	@Override
	public String toString() {
		return getName() + getDesc();
	}
}
