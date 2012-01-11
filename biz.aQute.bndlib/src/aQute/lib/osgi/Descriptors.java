package aQute.lib.osgi;

import java.util.*;

import aQute.libg.generics.*;

public class Descriptors {
	Map<String, TypeRef>	typeRefCache	= Create.map();
	Map<String, Descriptor>	descriptorCache	= Create.map();
	Map<String, PackageRef>	packageCache	= Create.map();

	final static TypeRef	VOID			= new ConcreteRef("V", "void");
	final static TypeRef	BOOLEAN			= new ConcreteRef("Z", "boolean");
	final static TypeRef	BYTE			= new ConcreteRef("B", "byte");
	final static TypeRef	CHAR			= new ConcreteRef("C", "char");
	final static TypeRef	SHORT			= new ConcreteRef("S", "short");
	final static TypeRef	INTEGER			= new ConcreteRef("I", "int");
	final static TypeRef	LONG			= new ConcreteRef("J", "long");
	final static TypeRef	DOUBLE			= new ConcreteRef("D", "double");
	final static TypeRef	FLOAT			= new ConcreteRef("F", "float");

	final static PackageRef	DEFAULT_PACKAGE	= new PackageRef();

	{
		packageCache.put("", DEFAULT_PACKAGE);
	}

	public interface TypeRef {
		String getBinary();

		String getFQN();

		String getPath();

		boolean isPrimitive();

		TypeRef getComponentTypeRef();

		TypeRef getClassRef();

		PackageRef getPackageRef();

		String getShortName();

		boolean isJava();

		boolean isObject();

		String getSourcePath();

		String getDottedOnly();

	}

	public static class PackageRef {
		final String	binaryName;
		final String	fqn;
		final boolean	java;

		private PackageRef(String binaryName) {
			this.binaryName = binaryName;
			this.fqn = binaryToFQN(binaryName);
			this.java = this.fqn.startsWith("java.");
		}

		private PackageRef() {
			this.binaryName = "";
			this.fqn=".";
			this.java = false;
		}

		public String getFQN() {
			return fqn;
		}

		public String getBinary() {
			return binaryName;
		}

		public String getPath() {
			return binaryName;
		}

		public boolean isJava() {
			return java;
		}

		public String toString() {
			return fqn;
		}
		
		boolean isDefaultPackage() {
			return this.fqn.equals(".");
		}
	}

	// We "intern" the
	private static class ConcreteRef implements TypeRef {
		final String		binaryName;
		final String		fqn;
		final boolean		primitive;
		final PackageRef	packageRef;

		ConcreteRef(PackageRef packageRef, String binaryName) {
			if ( packageRef.getFQN().length() < 2 )
				System.out.println("in default pack? " + binaryName);
			this.binaryName = binaryName;
			this.fqn = binaryToFQN(binaryName);
			this.primitive = false;
			this.packageRef = packageRef;
		}

		ConcreteRef(String binaryName, String fqn) {
			this.binaryName = binaryName;
			this.fqn = fqn;
			this.primitive = true;
			this.packageRef = DEFAULT_PACKAGE;
		}

		public String getBinary() {
			return binaryName;
		}

		public String getPath() {
			return binaryName + ".class";
		}

		public String getSourcePath() {
			return binaryName + ".java";
		}

		public String getFQN() {
			return fqn;
		}

		public String getDottedOnly() {
			return fqn.replace('$', '.');
		}

		public boolean isPrimitive() {
			return primitive;
		}

		public TypeRef getComponentTypeRef() {
			return null;
		}

		public TypeRef getClassRef() {
			return this;
		}

		public PackageRef getPackageRef() {
			return packageRef;
		}

		public String getShortName() {
			int n = binaryName.lastIndexOf('/');
			return binaryName.substring(n + 1);
		}

		public boolean isJava() {
			return packageRef.isJava();
		}

		public String toString() {
			return fqn;
		}

		public boolean isObject() {
			return fqn.equals("java.lang.Object");
		}

		public boolean equals(Object other) {
			assert other instanceof TypeRef;
			return this == other;
		}
		
	}

	private static class ArrayRef implements TypeRef {
		final TypeRef	component;

		ArrayRef(TypeRef component) {
			this.component = component;
		}

		public String getBinary() {
			return "[" + component.getBinary();
		}

		public String getFQN() {
			return component.getFQN() + "[]";
		}

		public String getPath() {
			return component.getPath();
		}

		public String getSourcePath() {
			return component.getSourcePath();
		}
		
		public boolean isPrimitive() {
			return false;
		}

		public TypeRef getComponentTypeRef() {
			return component;
		}

		public TypeRef getClassRef() {
			return component.getClassRef();
		}

		public boolean equals(Object other) {
			if (other == null || other.getClass() != getClass())
				return false;

			return component.equals(((ArrayRef) other).component);
		}

		public PackageRef getPackageRef() {
			return component.getPackageRef();
		}

		public String getShortName() {
			return component.getShortName() + "[]";
		}

		public boolean isJava() {
			return component.isJava();
		}

		public String toString() {
			return component.toString() + "[]";
		}

		public boolean isObject() {
			return false;
		}

		public String getDottedOnly() {
			return component.getDottedOnly();
		}

	}

	public TypeRef getTypeRef(String binaryClassName) {		
		
		TypeRef ref = typeRefCache.get(binaryClassName);
		if (ref != null)
			return ref;

		if (binaryClassName.startsWith("[")) {
			ref = getTypeRef(binaryClassName.substring(1));
			ref = new ArrayRef(ref);
		} else {
			if (binaryClassName.length() >= 1) {
				switch (binaryClassName.charAt(0)) {
				case 'V':
					return VOID;
				case 'B':
					return BYTE;
				case 'C':
					return CHAR;
				case 'I':
					return INTEGER;
				case 'S':
					return SHORT;
				case 'D':
					return DOUBLE;
				case 'F':
					return FLOAT;
				case 'J':
					return LONG;
				case 'Z':
					return BOOLEAN;
				case 'L':
					binaryClassName = binaryClassName.substring(1, binaryClassName.length() - 1);
					break;
				}
				// falls trough for other 1 letter class names
			}
			PackageRef pref;
			int n = binaryClassName.lastIndexOf('/');
			if (n < 0)
				pref = DEFAULT_PACKAGE;
			else
				pref = getPackageRef(binaryClassName.substring(0, n));

			ref = new ConcreteRef(pref, binaryClassName);
		}
		typeRefCache.put(binaryClassName, ref);
		return ref;
	}

	public PackageRef getPackageRef(String binaryPackName) {
		assert binaryPackName.indexOf('.') < 0;
		PackageRef ref = packageCache.get(binaryPackName);
		if (ref != null)
			return ref;

		ref = new PackageRef(binaryPackName);
		packageCache.put(binaryPackName, ref);
		return ref;
	}

	public Descriptor getDescriptor(String descriptor) {
		Descriptor d = descriptorCache.get(descriptor);
		if (d != null)
			return d;
		d = new Descriptor(descriptor);
		descriptorCache.put(descriptor, d);
		return d;
	}

	public class Descriptor {
		final TypeRef	type;
		final TypeRef[]	prototype;
		final String	descriptor;

		private Descriptor(String descriptor) {
			this.descriptor = descriptor;
			int index = 0;
			List<TypeRef> types = Create.list();
			if (descriptor.charAt(index) == '(') {
				index++;
				while (descriptor.charAt(index) != ')') {
					index = parse(types, descriptor, index);
				}
				index++; // skip )
				prototype = types.toArray(new TypeRef[types.size()]);
				types.clear();
			} else
				prototype = null;

			index = parse(types, descriptor, index);
			type = types.get(0);
		}

		int parse(List<TypeRef> types, String descriptor, int index) {
			char c;
			StringBuilder sb = new StringBuilder();
			while ((c = descriptor.charAt(index++)) == '[') {
				sb.append('[');
			}

			switch (c) {
			case 'L':
				while ((c = descriptor.charAt(index++)) != ';') {
					// TODO
					sb.append(c);
				}
				break;

			case 'V':
			case 'B':
			case 'C':
			case 'I':
			case 'S':
			case 'D':
			case 'F':
			case 'J':
			case 'Z':
				sb.append(c);
				break;

			default:
				throw new IllegalArgumentException("Invalid type in descriptor: " + c + " from "
						+ descriptor + "[" + index + "]");
			}
			types.add(getTypeRef(sb.toString()));
			return index;
		}

		public TypeRef getType() {
			return type;
		}

		public TypeRef[] getPrototype() {
			return prototype;
		}

		public boolean equals(Object other) {
			if (other == null || other.getClass() != getClass())
				return false;

			return Arrays.equals(prototype, ((Descriptor) other).prototype)
					&& type == ((Descriptor) other).type;
		}

		public int hashCode() {
			return prototype == null ? type.hashCode() : type.hashCode()
					^ Arrays.hashCode(prototype);
		}

		public String toString() {
			return descriptor;
		}
	}

	/**
	 * Return the short name of a FQN
	 */

	public static String getShortName(String fqn) {
		assert fqn.indexOf('/') < 0;
		
		int n = fqn.lastIndexOf('.');
		if (n >= 0) {
			return fqn.substring(n + 1);
		}
		return fqn;
	}

	public static String binaryToFQN(String binary) {
		StringBuilder sb = new StringBuilder();
		for ( int i=0, l=binary.length(); i<l; i++) {
			char c = binary.charAt(i);
			
			if ( c == '/')
				sb.append('.');
			else
				sb.append(c);
		}
		String result = sb.toString();
		assert result.length() > 0;
		return result;
	}

	public static String fqnToBinary(String binary) {
		return binary.replace('.', '/');
	}

	public static String getPackage(String binaryNameOrFqn) {
		int n = binaryNameOrFqn.lastIndexOf('/');
		if (n >= 0)
			return binaryNameOrFqn.substring(0, n).replace('/', '.');

		n = binaryNameOrFqn.lastIndexOf(".");
		if (n >= 0)
			return binaryNameOrFqn.substring(0, n);

		return ".";
	}

	public static String fqnToPath(String s) {
		return fqnToBinary(s) + ".class";
	}

	public TypeRef getTypeRefFromFQN(String fqn) {
		return getTypeRef(fqnToBinary(fqn));
	}
}
