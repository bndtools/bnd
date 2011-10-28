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
import aQute.lib.osgi.*;

public class ClassInfo extends Clazz implements ClassDiff, Comparable<ClassInfo> {
	
	private Set<MethodInfo> methods = new TreeSet<MethodInfo>();
	private Set<FieldInfo> fields = new TreeSet<FieldInfo>();
	private Set<AnnotationInfo> annotations = new TreeSet<AnnotationInfo>();
	
	private Delta delta = Delta.UNCHANGED;
	
	private PackageInfo packageInfo;
	
	public ClassInfo(PackageInfo pi, String path, Resource resource) throws Exception {
		super(path, resource);
		this.packageInfo = pi;
		ClassCollector collector = new ClassCollector();
		super.parseClassFileWithCollector(collector);
	}

	public String getName() {
		return Clazz.pathToFqn(getPath());
	}
	
	public int compareTo(ClassInfo o) {
		if (o == null) {
			return -1;
		}
		return getName().compareTo(o.getName());
	}
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj.getClass() != this.getClass()) {
			return false;
		}
		return ((ClassInfo) obj).getName().equals(getName());
	}
	
	public int hashCode() {
		return getName().hashCode();
	}

	public Set<MethodInfo> getDeletedMethods() {
		Set<MethodInfo> ret = new TreeSet<MethodInfo>();
		for (MethodInfo mi : methods) {
			if (mi.getDelta() != Delta.REMOVED) {
				continue;
			}
			ret.add(mi);
		}
		return ret;
	}

	public Set<MethodInfo> getNewMethods() {
		Set<MethodInfo> ret = new TreeSet<MethodInfo>();
		for (MethodInfo mi : methods) {
			if (mi.getDelta() != Delta.ADDED) {
				continue;
			}
			ret.add(mi);
		}
		return ret;
	}
	
	public Set<MethodInfo> getMethods() {
		return methods;
	}

	public Set<MethodInfo> getChangedMethods() {
		Set<MethodInfo> ret = new TreeSet<MethodInfo>();
		for (MethodInfo mi : methods) {
			if (mi.getDelta() == Delta.UNCHANGED) {
				continue;
			}
			ret.add(mi);
		}
		return ret;
	}

	public Set<FieldInfo> getDeletedFields() {
		Set<FieldInfo> ret = new TreeSet<FieldInfo>();
		for (FieldInfo mi : fields) {
			if (mi.getDelta() != Delta.REMOVED) {
				continue;
			}
			ret.add(mi);
		}
		return ret;
	}

	public Set<FieldInfo> getNewFields() {
		Set<FieldInfo> ret = new TreeSet<FieldInfo>();
		for (FieldInfo mi : fields) {
			if (mi.getDelta() != Delta.ADDED) {
				continue;
			}
			ret.add(mi);
		}
		return ret;
	}
	
	public Set<FieldInfo> getFields() {
		return fields;
	}

	public Set<FieldInfo> getChangedFields() {
		Set<FieldInfo> ret = new TreeSet<FieldInfo>();
		for (FieldInfo mi : fields) {
			if (mi.getDelta() == Delta.UNCHANGED) {
				continue;
			}
			ret.add(mi);
		}
		return ret;
	}

	
	public Delta getDelta() {
		return delta;
	}

	public void setDelta(Delta delta) {
		this.delta = delta;
	}
	
	public void addMethod(MethodInfo mi) {
		methods.add(mi);
	}

	public void addField(FieldInfo fi) {
		fields.add(fi);
	}

	public PackageInfo getPackageInfo() {
		return packageInfo;
	}
	
	public String toString() {
		return getName();
	}

	private class ClassCollector extends ClassDataCollector {

		private MethodInfo currentMethodInfo;
		private FieldInfo currentFieldInfo;
		
		@Override
		public void classBegin(int access, String name) {
			//System.out.println("classBegin " + access + " name: " + name);
			super.classBegin(access, name);
		}

		@Override
		public boolean classStart(int access, String name) {
			return super.classStart(access, name);
		}

		@Override
		public void extendsClass(String name) {
			//System.out.println("extendsClass name: " + name);
			super.extendsClass(name);
		}

		@Override
		public void implementsInterfaces(String[] name) {
			//System.out.println("implementsInterfaces names: " + name);
			super.implementsInterfaces(name);
		}

		@Override
		public void addReference(String token) {
			//System.out.println("addReference token: " + token);
			super.addReference(token);
		}

		@Override
		public void annotation(Annotation annotation) {
			//System.out.println("addAnnotation annotation: " + annotation.getName());
			//annotations.add(new AnnotationInfo(annotation));	
		}

		@Override
		public void parameter(int p) {
			//System.out.println("parameter int: " + p);
			super.parameter(p);
		}

		@Override
		public void method(MethodDef defined) {

			//System.out.println((defined.isConstructor() ? "constructor " : "method ") + defined.access + " descriptor: " + defined.descriptor);			
			if ((defined.access & Modifier.PUBLIC) == Modifier.PUBLIC || (defined.access & Modifier.PROTECTED) == Modifier.PROTECTED) {
				MethodInfo methodInfo = new MethodInfo(defined, ClassInfo.this);
				addMethod(methodInfo);
			}
		}

		@Override
		public void field(FieldDef defined) {
			//System.out.println("field " + defined.access + " descriptor: " + defined.descriptor);			
			if ((defined.access & Modifier.PUBLIC) == Modifier.PUBLIC || (defined.access & Modifier.PROTECTED) == Modifier.PROTECTED) {
				FieldInfo methodInfo = new FieldInfo(defined, ClassInfo.this);
				addField(methodInfo);
			}
		}

		@Override
		public void reference(MethodDef referenced) {
			//System.out.println("method reference " + referenced.access + " descriptor: " + referenced.descriptor);			
			super.reference(referenced);
		}

		@Override
		public void reference(FieldDef referenced) {
			//System.out.println("field reference " + referenced.access + " descriptor: " + referenced.descriptor);			
			super.reference(referenced);
		}

		@Override
		public void classEnd() {
			//System.out.println("classEnd");
			super.classEnd();
		}

		@Override
		public void enclosingMethod(String cName, String mName,
				String mDescriptor) {
			//System.out.println("enclosingMethod cName: " + cName + " mName: " + mName + " mDescriptor : " + mDescriptor);
			super.enclosingMethod(cName, mName, mDescriptor);
		}

		@Override
		public void innerClass(String innerClass, String outerClass,
				String innerName, int innerClassAccessFlags) {
			//System.out.println("innerClass innerClass: " + innerClass + " innerName: " + innerName + " outerClass : " + outerClass);
			super.innerClass(innerClass, outerClass, innerName, innerClassAccessFlags);
		}

		@Override
		public void signature(String signature) {
			//System.out.println("signature " + signature);
			super.signature(signature);
		}

		@Override
		public void constant(Object object) {
			//System.out.println("constant " + object);
			super.constant(object);
		}
	}



	public Diff getContainer() {
		return packageInfo;
	}

	public Collection<? extends Diff> getContained() {
		List<Diff> contained = new ArrayList<Diff>();
		contained.addAll(fields);
		contained.addAll(methods);
		return contained;
	}

	public String explain() {
		return delta + " " + getName();
	}
}
