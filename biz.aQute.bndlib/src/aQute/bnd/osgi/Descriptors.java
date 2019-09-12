package aQute.bnd.osgi;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

import aQute.bnd.signatures.ClassSignature;
import aQute.bnd.signatures.FieldSignature;
import aQute.bnd.signatures.MethodSignature;
import aQute.libg.generics.Create;

public class Descriptors {
	private final Map<String, TypeRef>			typeRefCache			= new HashMap<>();
	private final Map<String, Descriptor>		descriptorCache			= new HashMap<>();
	private final Map<String, PackageRef>		packageRefCache			= new HashMap<>();
	private final Map<String, ClassSignature>	classSignatureCache		= new HashMap<>();
	private final Map<String, MethodSignature>	methodSignatureCache	= new HashMap<>();
	private final Map<String, FieldSignature>	fieldSignatureCache		= new HashMap<>();

	// MUST BE BEFORE PRIMITIVES, THEY USE THE DEFAULT PACKAGE!!
	final static PackageRef						DEFAULT_PACKAGE			= new PackageRef();
	final static PackageRef						PRIMITIVE_PACKAGE		= new PackageRef();

	final static TypeRef						VOID					= new ConcreteRef("V", "void",
		PRIMITIVE_PACKAGE);
	final static TypeRef						BOOLEAN					= new ConcreteRef("Z", "boolean",
		PRIMITIVE_PACKAGE);
	final static TypeRef						BYTE					= new ConcreteRef("B", "byte",
		PRIMITIVE_PACKAGE);
	final static TypeRef						CHAR					= new ConcreteRef("C", "char",
		PRIMITIVE_PACKAGE);
	final static TypeRef						SHORT					= new ConcreteRef("S", "short",
		PRIMITIVE_PACKAGE);
	final static TypeRef						INTEGER					= new ConcreteRef("I", "int",
		PRIMITIVE_PACKAGE);
	final static TypeRef						LONG					= new ConcreteRef("J", "long",
		PRIMITIVE_PACKAGE);
	final static TypeRef						DOUBLE					= new ConcreteRef("D", "double",
		PRIMITIVE_PACKAGE);
	final static TypeRef						FLOAT					= new ConcreteRef("F", "float",
		PRIMITIVE_PACKAGE);

	@Deprecated
	public enum SignatureType {
		TYPEVAR,
		METHOD,
		FIELD;
	}

	@Deprecated
	public class Signature {
		public Map<String, Signature>	typevariables	= new HashMap<>();
		public Signature				type;
		public List<Signature>			parameters;
	}

	public Descriptors() {
		packageRefCache.put(DEFAULT_PACKAGE.getBinary(), DEFAULT_PACKAGE);
	}

	@ProviderType
	public interface TypeRef extends Comparable<TypeRef> {
		String getBinary();

		String getShorterName();

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

		boolean isArray();

	}

	public static class PackageRef implements Comparable<PackageRef> {
		final String	binaryName;
		final String	fqn;
		final boolean	java;

		PackageRef(String binaryName) {
			this.binaryName = requireNonNull(binaryName);
			this.fqn = binaryToFQN(binaryName);
			this.java = this.fqn.startsWith("java.");
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

		@Override
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
		 */
		public boolean isMetaData() {
			if (isDefaultPackage())
				return true;

			return Arrays.stream(Constants.METAPACKAGES)
				.anyMatch(meta -> binaryName.startsWith(meta)
					&& ((binaryName.length() == meta.length()) || (binaryName.charAt(meta.length()) == '/')));
		}

	}

	// We "intern" the
	private static class ConcreteRef implements TypeRef {
		final String		binaryName;
		final String		fqn;
		final boolean		primitive;
		final PackageRef	packageRef;

		ConcreteRef(PackageRef packageRef, String binaryName) {
			this.binaryName = requireNonNull(binaryName);
			this.fqn = binaryToFQN(binaryName);
			this.primitive = false;
			this.packageRef = requireNonNull(packageRef);
		}

		ConcreteRef(String binaryName, String fqn, PackageRef packageRef) {
			this.binaryName = binaryName;
			this.fqn = fqn;
			this.primitive = true;
			this.packageRef = packageRef;
		}

		@Override
		public String getBinary() {
			return binaryName;
		}

		@Override
		public String getPath() {
			return binaryName.concat(".class");
		}

		@Override
		public String getSourcePath() {
			return binaryName.concat(".java");
		}

		@Override
		public String getFQN() {
			return fqn;
		}

		@Override
		public String getDottedOnly() {
			return fqn.replace('$', '.');
		}

		@Override
		public boolean isPrimitive() {
			return primitive;
		}

		@Override
		public TypeRef getComponentTypeRef() {
			return null;
		}

		@Override
		public TypeRef getClassRef() {
			return this;
		}

		@Override
		public PackageRef getPackageRef() {
			return packageRef;
		}

		@Override
		public String getShortName() {
			int n = binaryName.lastIndexOf('/');
			return binaryName.substring(n + 1);
		}

		@Override
		public String getShorterName() {
			String name = getShortName();
			int n = name.lastIndexOf('$');
			if (n <= 0)
				return name;

			return name.substring(n + 1);
		}

		@Override
		public boolean isJava() {
			return packageRef.isJava();
		}

		/**
		 * Returning {@link #getFQN()} is relied upon by other classes.
		 */
		@Override
		public String toString() {
			return fqn;
		}

		@Override
		public boolean isObject() {
			return fqn.equals("java.lang.Object");
		}

		@Override
		public boolean equals(Object other) {
			assert other instanceof TypeRef;
			return this == other;
		}

		@Override
		public int compareTo(TypeRef other) {
			if (this == other)
				return 0;
			return fqn.compareTo(other.getFQN());
		}

		@Override
		public int hashCode() {
			return super.hashCode();
		}

		@Override
		public boolean isArray() {
			return false;
		}

	}

	private static class ArrayRef implements TypeRef {
		final TypeRef component;

		ArrayRef(TypeRef component) {
			this.component = requireNonNull(component);
		}

		@Override
		public String getBinary() {
			return "[".concat(component.getBinary());
		}

		@Override
		public String getFQN() {
			return component.getFQN()
				.concat("[]");
		}

		@Override
		public String getPath() {
			return component.getPath();
		}

		@Override
		public String getSourcePath() {
			return component.getSourcePath();
		}

		@Override
		public boolean isPrimitive() {
			return false;
		}

		@Override
		public TypeRef getComponentTypeRef() {
			return component;
		}

		@Override
		public TypeRef getClassRef() {
			return component.getClassRef();
		}

		@Override
		public boolean equals(Object other) {
			if (other == null || other.getClass() != getClass())
				return false;

			return component.equals(((ArrayRef) other).component);
		}

		@Override
		public PackageRef getPackageRef() {
			return component.getPackageRef();
		}

		@Override
		public String getShortName() {
			return component.getShortName()
				.concat("[]");
		}

		@Override
		public boolean isJava() {
			return component.isJava();
		}

		@Override
		public String toString() {
			return component.toString()
				.concat("[]");
		}

		@Override
		public boolean isObject() {
			return false;
		}

		@Override
		public String getDottedOnly() {
			return component.getDottedOnly();
		}

		@Override
		public int compareTo(TypeRef other) {
			if (this == other)
				return 0;

			return getFQN().compareTo(other.getFQN());
		}

		@Override
		public int hashCode() {
			return super.hashCode();
		}

		@Override
		public String getShorterName() {
			String name = getShortName();
			int n = name.lastIndexOf('$');
			if (n <= 0)
				return name;

			return name.substring(n + 1);
		}

		@Override
		public boolean isArray() {
			return true;
		}

	}

	public TypeRef getTypeRef(String binaryClassName) {
		assert !binaryClassName.endsWith(".class");
		int last = binaryClassName.length() - 1;
		if ((last > 0) && (binaryClassName.charAt(0) == 'L') && (binaryClassName.charAt(last) == ';')) {
			binaryClassName = binaryClassName.substring(1, last);
			last -= 2;
		}

		binaryClassName = binaryClassName.replace('.', '$');

		if ((last >= 0) && (binaryClassName.charAt(0) == '[')) {
			// We handle arrays here since computeIfAbsent does not like
			// recursive calls starting in Java 9
			TypeRef ref = typeRefCache.get(binaryClassName);
			if (ref == null) {
				ref = new ArrayRef(getTypeRef(binaryClassName.substring(1)));
				typeRefCache.put(binaryClassName, ref);
			}
			return ref;
		}

		return typeRefCache.computeIfAbsent(binaryClassName, this::createTypeRef);
	}

	private TypeRef createTypeRef(String binaryClassName) {
		if (binaryClassName.length() == 1) {
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
			}
			// falls through for other 1 letter class names
		}
		int n = binaryClassName.lastIndexOf('/');
		PackageRef pref = (n < 0) ? DEFAULT_PACKAGE : getPackageRef(binaryClassName.substring(0, n));
		return new ConcreteRef(pref, binaryClassName);
	}

	public TypeRef getPackageInfo(PackageRef packageRef) {
		String bin = packageRef.getBinary()
			.concat("/package-info");
		return getTypeRef(bin);
	}

	public PackageRef getPackageRef(String binaryPackName) {
		binaryPackName = fqnToBinary(binaryPackName);
		//
		// Check here if a package is actually a nested class
		// com.example.Foo.Bar should have package com.example,
		// not com.example.Foo.
		//

		return packageRefCache.computeIfAbsent(binaryPackName, PackageRef::new);
	}

	public Descriptor getDescriptor(String descriptor) {
		return descriptorCache.computeIfAbsent(descriptor, Descriptor::new);
	}

	public ClassSignature getClassSignature(String signature) {
		return classSignatureCache.computeIfAbsent(signature.replace('$', '.'), ClassSignature::of);
	}

	public MethodSignature getMethodSignature(String signature) {
		return methodSignatureCache.computeIfAbsent(signature.replace('$', '.'), MethodSignature::of);
	}

	public FieldSignature getFieldSignature(String signature) {
		return fieldSignatureCache.computeIfAbsent(signature.replace('$', '.'), FieldSignature::of);
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
				prototype = types.toArray(new TypeRef[0]);
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
					throw new IllegalArgumentException(
						"Invalid type in descriptor: " + c + " from " + descriptor + "[" + index + "]");
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
			final int prime = 31;
			int result = prime + type.hashCode();
			result = prime * result + ((prototype == null) ? 0 : Arrays.hashCode(prototype));
			return result;
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
		assert !binary.isEmpty();
		return binary.replace('/', '.');
	}

	public static String fqnToBinary(String binary) {
		return binary.replace('.', '/');
	}

	public static String getPackage(String binaryNameOrFqn) {
		int n = binaryNameOrFqn.lastIndexOf('/');
		if (n >= 0)
			return binaryToFQN(binaryNameOrFqn.substring(0, n));

		n = binaryNameOrFqn.lastIndexOf('.');
		if (n >= 0)
			return binaryNameOrFqn.substring(0, n);

		return DEFAULT_PACKAGE.getFQN();
	}

	public static String fqnToPath(String s) {
		return fqnToBinary(s).concat(".class");
	}

	public TypeRef getTypeRefFromFQN(String fqn) {
		switch (fqn) {
			case "boolean" :
				return BOOLEAN;
			case "byte" :
				return BOOLEAN;
			case "char" :
				return CHAR;
			case "short" :
				return SHORT;
			case "int" :
				return INTEGER;
			case "long" :
				return LONG;
			case "float" :
				return FLOAT;
			case "double" :
				return DOUBLE;
			default :
				return getTypeRef(fqnToBinary(fqn));
		}
	}

	public TypeRef getTypeRefFromPath(String path) {
		assert path.endsWith(".class");
		return getTypeRef(path.substring(0, path.length() - 6));
	}

}
