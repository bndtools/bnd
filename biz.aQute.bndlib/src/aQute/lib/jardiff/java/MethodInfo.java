/*******************************************************************************
 * Copyright (c) 2011 Per Kr. Soreide.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Per Kr. Soreide - initial API and implementation
 *******************************************************************************/
package aQute.lib.jardiff.java;

import java.lang.reflect.*;
import java.util.*;

import aQute.lib.jardiff.*;
import aQute.lib.osgi.Clazz.MethodDef;

public class MethodInfo implements MethodDiff, Comparable<MethodInfo> {
	
	private final MethodDef methodNode;
	private Delta delta = Delta.UNCHANGED;
	private ClassInfo classInfo;
	
	public MethodInfo(MethodDef methodNode, ClassInfo classInfo) {
		this.methodNode = methodNode;
		this.classInfo = classInfo;
	}

	public Delta getDelta() {
		return delta;
	}

	public void setDelta(Delta delta) {
		this.delta = delta;
	}
	
	public String getName() {
		return methodNode.name;
	}
	
	public String getDesc() {
		return methodNode.descriptor;
	}

	public boolean isStatic() {
		return ((methodNode.access & Modifier.STATIC) == Modifier.STATIC); 
	}

	public int compareTo(MethodInfo o) {
		String from = getName() + getDesc();
		String to = o.getName() + o.getDesc();
		return from.compareTo(to);
	}

	public PackageInfo getPackageInfo() {
		return classInfo.getPackageInfo();
	}
	
	public String toString() {
		return getName() + getDesc();
	}
	
	public Diff getContainer() {
		return classInfo;
	}

	public Collection<? extends Diff> getContained() {
		return Collections.emptyList();
	}

	public String explain() {
		return delta + " " + getName() + " " + getDesc();
	}
}
