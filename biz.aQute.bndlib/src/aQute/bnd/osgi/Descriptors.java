package aQute.bnd.osgi;

import java.util.*;

import aQute.libg.generics.*;

public class Descriptors {
	Map<String,TypeRef>		typeRefCache		= Create.map();
	Map<String,Descriptor>	descriptorCache		= Create.map();
	Map<String,PackageRef>	packageCache		= Create.map();

	// MUST BE BEFORE PRIMITIVES, THEY USE THE DEFAULT PACKAGE!!
	final static PackageRef	DEFAULT_PACKAGE		= new PackageRef();
	final static PackageRef	PRIMITIVE_PACKAGE	= new PackageRef();

	final static TypeRef	VOID				= new ConcreteRef("V", "void", PRIMITIVE_PACKAGE);
	final static TypeRef	BOOLEAN				= new ConcreteRef("Z", "boolean", PRIMITIVE_PACKAGE);
	final static TypeRef	BYTE				= new ConcreteRef("B", "byte", PRIMITIVE_PACKAGE);
	final static TypeRef	CHAR				= new ConcreteRef("C", "char", PRIMITIVE_PACKAGE);
	final static TypeRef	SHORT				= new ConcreteRef("S", "short", PRIMITIVE_PACKAGE);
	final static TypeRef	INTEGER				= new ConcreteRef("I", "int", PRIMITIVE_PACKAGE);
	final static TypeRef	LONG				= new ConcreteRef("J", "long", PRIMITIVE_PACKAGE);
	final static TypeRef	DOUBLE				= new ConcreteRef("D", "double", PRIMITIVE_PACKAGE);
	final static TypeRef	FLOAT				= new ConcreteRef("F", "float", PRIMITIVE_PACKAGE);

	{
		packageCache.put("", DEFAULT_PACKAGE);
	}

	public interface TypeRef extends Comparable<TypeRef> {
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

	public static class PackageRef implements Comparable<PackageRef> {
		final String	binaryName;
		final String	fqn;
		final boolean	java;

		PackageRef(String binaryName) {
			this.binaryName = fqnToBinary(binaryName);
			this.fqn = binaryToFQN(binaryName);
			this.java = this.fqn.startsWith("java."); // &&
														// !this.fqn.equals("java.sql)"

			// For some reason I excluded java.sql but the classloader will
			// delegate anyway. So lost the understanding why I did it??
		}

		PackageRef() {
			this.binaryName = "";
			this.fqn = ".";
			this.java = false;
		}

		public PackageRef getDuplicate() {
			return new PackageRef(binaryName + Constants.DUPLICATE_MARKER);
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

		@Override
		public String toString() {
			return fqn;
		}

		boolean isDefaultPackage() {
			return this.fqn.equals(".");
		}

		boolean isPrimitivePackage() {
			return this == PRIMITIVE_PACKAGE;
		}

		public int compareTo(PackageRef other) {
			return fqn.compareTo(other.fqn);
		}

		@Override
		public boolean equals(Object o) {
			assert o instanceof PackageRef;
			return o == this;
		}

		@Override
		public int hashCode() {
			return super.hashCode();
		}

		/**
		 * Decide if the package is a metadata package.
		 * 
		 * @param pack
		 * @return
		 */
		public boolean isMetaData() {
			if (isDefaultPackage())
				return true;

			for (int i = 0; i < Constants.METAPACKAGES.length; i++) {
				if (fqn.startsWith(Constants.METAPACKAGES[i]))
					return true;
			}
			return false;
		}

	}

	// We "intern" the
	private static class ConcreteRef implements TypeRef {
		final String		binaryName;
		final String		fqn;
		final boolean		primitive;
		final PackageRef	packageRef;

		ConcreteRef(PackageRef packageRef, String binaryName) {
			if (packageRef.getFQN().length() < 2)
				System.err.println("in default pack? " + binaryName);
			this.binaryName = binaryName;
			this.fqn = binaryToFQN(binaryName);
			this.primitive = false;
			this.packageRef = packageRef;
		}

		ConcreteRef(String binaryName, String fqn, PackageRef pref) {
			this.binaryName = binaryName;
			this.fqn = fqn;
			this.primitive = true;
			this.packageRef = pref;
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

		@Override
		public String toString() {
			return fqn;
		}

		public boolean isObject() {
			return fqn.equals("java.lang.Object");
		}

		@Override
		public boolean equals(Object other) {
			assert other instanceof TypeRef;
			return this == other;
		}

		public int compareTo(TypeRef other) {
			if (this == other)
				return 0;
			return fqn.compareTo(other.getFQN());
		}

		@Override
		public int hashCode() {
			return super.hashCode();
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

		@Override
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

		@Override
		public String toString() {
			return component.toString() + "[]";
		}

		public boolean isObject() {
			return false;
		}

		public String getDottedOnly() {
			return component.getDottedOnly();
		}

		public int compareTo(TypeRef other) {
			if (this == other)
				return 0;

			return getFQN().compareTo(other.getFQN());
		}

		@Override
		public int hashCode() {
			return super.hashCode();
		}

	}

	public TypeRef getTypeRef(String binaryClassName) {
		assert !binaryClassName.endsWith(".class");

		TypeRef ref = typeRefCache.get(binaryClassName);
		if (ref != null)
			return ref;

		if (binaryClassName.startsWith("[")) {
			ref = getTypeRef(binaryClassName.substring(1));
			ref = new ArrayRef(ref);
		} else {
			if (binaryClassName.length() >= 1) {
				switch (binaryClassName.charAt(0)) {
					case 'V' :
						return VOID;
					case 'B' :
						return BYTE;
					case 'C' :
						return CHAR;
					case 'I' :
						return INTEGER;
					case 'S' :
						return SHORT;
					case 'D' :
						return DOUBLE;
					case 'F' :
						return FLOAT;
					case 'J' :
						return LONG;
					case 'Z' :
						return BOOLEAN;
					case 'L' :
						binaryClassName = binaryClassName.substring(1, binaryClassName.length() - 1);
						break;
				}
				// falls trough for other 1 letter class names
			}
			ref = typeRefCache.get(binaryClassName);
			if (ref != null)
				return ref;

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
		if (binaryPackName.indexOf('.') >= 0) {
			binaryPackName = binaryPackName.replace('.', '/');
		}
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

		Descriptor(String descriptor) {
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
				case 'L' :
					while ((c = descriptor.charAt(index++)) != ';') {
						// TODO
						sb.append(c);
					}
					break;

				case 'V' :
				case 'B' :
				case 'C' :
				case 'I' :
				case 'S' :
				case 'D' :
				case 'F' :
				case 'J' :
				case 'Z' :
					sb.append(c);
					break;

				default :
					throw new IllegalArgumentException("Invalid type in descriptor: " + c + " from " + descriptor + "["
							+ index + "]");
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

		@Override
		public boolean equals(Object other) {
			if (other == null || other.getClass() != getClass())
				return false;

			return Arrays.equals(prototype, ((Descriptor) other).prototype) && type == ((Descriptor) other).type;
		}

		@Override
		public int hashCode() {
			return prototype == null ? type.hashCode() : type.hashCode() ^ Arrays.hashCode(prototype);
		}

		@Override
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
		for (int i = 0, l = binary.length(); i < l; i++) {
			char c = binary.charAt(i);

			if (c == '/')
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
		if (fqn.equals("boolean"))
			return BOOLEAN;

		if (fqn.equals("byte"))
			return BOOLEAN;

		if (fqn.equals("char"))
			return CHAR;

		if (fqn.equals("short"))
			return SHORT;

		if (fqn.equals("int"))
			return INTEGER;

		if (fqn.equals("long"))
			return LONG;

		if (fqn.equals("float"))
			return FLOAT;

		if (fqn.equals("double"))
			return DOUBLE;

		return getTypeRef(fqnToBinary(fqn));
	}

	public TypeRef getTypeRefFromPath(String path) {
		assert path.endsWith(".class");
		return getTypeRef(path.substring(0, path.length() - 6));
	}
}
