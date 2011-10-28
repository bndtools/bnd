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
import aQute.lib.osgi.Clazz.FieldDef;

public class FieldInfo implements FieldDiff, Comparable<FieldInfo> {

	private final FieldDef fieldNode;
	private Delta delta = Delta.UNCHANGED;
	private ClassInfo classInfo;
	
	public FieldInfo(FieldDef fieldNode, ClassInfo classInfo) {
		this.fieldNode = fieldNode;
		this.classInfo = classInfo;
	}

	public Delta getDelta() {
		return delta;
	}

	public void setDelta(Delta delta) {
		this.delta = delta;
	}
	
	public String getName() {
		return fieldNode.getPretty();
	}
	
	public String getDesc() {
		return fieldNode.descriptor;
	}

	public boolean isStatic() {
		return ((fieldNode.access & Modifier.STATIC) == Modifier.STATIC); 
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
