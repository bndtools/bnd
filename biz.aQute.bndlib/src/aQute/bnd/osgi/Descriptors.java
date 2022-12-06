package aQute.bnd.osgi;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

import aQute.bnd.result.Result;
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

		boolean isNested();

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

			return Constants.METAPACKAGES.stream()
				.anyMatch(meta -> binaryName.startsWith(meta)
					&& ((binaryName.length() == meta.length()) || (binaryName.charAt(meta.length()) == '/')));
		}

		/**
		 * Check if the package name is a valid Java package name.
		 *
		 * @return true if the package name is valid; false otherwise.
		 */
		public boolean isValidPackageName() {
			final int len = fqn.length();
			boolean start = true;
			for (int i = 0; i < len;) {
				int cp = Character.codePointAt(fqn, i);
				if (start) {
					if (!Character.isJavaIdentifierStart(cp)) {
						return false;
					}
					start = false;
				} else {
					if (cp == '.') {
						start = true;
					} else if (!Character.isJavaIdentifierPart(cp)) {
						return false;
					}
				}
				i += Character.charCount(cp);
			}
			return !start;
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

		@Override
		public boolean isNested() {
			return binaryName.indexOf('$') >= 0;
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
			return component.getDottedOnly()
				.concat("[]");
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

		@Override
		public boolean isNested() {
			return component.isNested();
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

	public static String binaryClassToFQN(String path) {
		return binaryToFQN(path.substring(0, path.length() - 6)).replace('$', '.');
	}

	public static String fqnToBinary(String fqn) {
		return fqn.replace('.', '/');
	}

	/**
	 * Converts the given fully-qualified top-level class name into the binary
	 * class path. For example:
	 * <p>
	 * {@code my.pkg.And.Clazz} becomes:
	 * <p>
	 * {@code my/pkg/And$Clazz.class}
	 * <p>
	 * This method uses {@link Descriptors#determine(String)} to split the class
	 * and package names, which is imperfect.
	 *
	 * @param fqn the fully-qualified name to be converted.
	 * @return The binary name corresponding to the fully-qualified name.
	 */
	public static String fqnClassToBinary(String fqn) {
		Result<String[]> result = determine(fqn);
		String[] parts = result.orElseThrow(IllegalArgumentException::new);
		if (parts[0] == null) {
			return classToPath(parts[1]);
		}
		if (parts[1] == null) {
			return fqnToBinary(parts[0]) + ".class";
		}
		return fqnToBinary(parts[0]) + "/" + classToPath(parts[1]);
	}

	/**
	 * Converts the class name (without the package qualifier) into the
	 * corresponding binary name. For example:
	 * <p>
	 * {@code my.pkg.and.Clazz} becomes:
	 * <p>
	 * {@code my$pkg$and$Clazz.class} As you can see, this method is not smart
	 * about distinguishing between package and class nesting - it always
	 * converts the . into a $.
	 *
	 * @param className the name of the class to be converted.
	 * @return The binary name corresponding to the class name.
	 */
	public static String classToPath(String className) {
		return className.replace('.', '$') + ".class";
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
		return switch (fqn) {
			case "boolean" -> BOOLEAN;
			case "byte" -> BOOLEAN;
			case "char" -> CHAR;
			case "short" -> SHORT;
			case "int" -> INTEGER;
			case "long" -> LONG;
			case "float" -> FLOAT;
			case "double" -> DOUBLE;
			default -> getTypeRef(fqnToBinary(fqn));
		};
	}

	public TypeRef getTypeRefFromPath(String path) {
		assert path.endsWith(".class");
		return getTypeRef(path.substring(0, path.length() - 6));
	}

	public static String pathToFqn(String path) {
		assert path.endsWith(".class");

		StringBuilder sb = new StringBuilder();
		int j = path.length() - 6;
		for (int i = 0; i < j; i++) {
			char c = path.charAt(i);
			if (c == '/')
				sb.append('.');
			else
				sb.append(c);
		}
		return sb.toString();
	}

	public static boolean isBinaryClass(String resource) {
		return resource.endsWith(".class");
	}

	/**
	 * Java really screwed up in using different names for the binary path and
	 * the fqns. This calculates the simple name of a potentially nested class.
	 *
	 * @param resource ( segment '/')+ (name '$')* name '.class'
	 * @return the last name
	 */
	public static String binaryToSimple(String resource) {
		if (resource == null)
			return null;

		assert isBinaryClass(resource);

		int end = resource.length() - 6;
		int rover = end;
		while (rover >= 0) {
			char ch = resource.charAt(rover);
			if (ch == '$' || ch == '/') {
				return resource.substring(rover + 1, end);
			}
			rover--;
		}
		return resource.substring(0, end);
	}

	/**
	 * Heuristic for a class name. We assume a segment with
	 *
	 * @param fqn can be a class name, nested class, or simple name
	 * @return true if the last segment starts with an upper case
	 */
	public static boolean isClassName(String fqn) {
		if (fqn.isEmpty())
			return false;

		int n = fqn.lastIndexOf('.') + 1;
		if (n >= fqn.length())
			return false;

		char ch = fqn.charAt(n);

		return Character.isUpperCase(ch);
	}

	/**
	 * Return a 2 element array based on the fqn. The first element is the
	 * package name, the second is the class name. Each can be absent, but not
	 * both. The class name can be a nested class (will contain a '.' then)
	 * <p>
	 * Because there is an inherent ambiguity between packages and nested
	 * classes, this method uses a heuristic that works most of the time: the
	 * start of the class name is considered to be the first element that begins
	 * with a capital letter. Hence "simple.Sample.Sumple" => ["simple",
	 * "Sample.Sumple" ] and not [ "simple.Sample", "Sumple" ].
	 *
	 * @param fqn a Java identifier name, either a simple class name, a
	 *            qualified class name, or a package name
	 * @return a Result with 2 element array with [package, class]
	 */
	public static Result<String[]> determine(String fqn) {
		if (fqn == null || fqn.isEmpty())
			return Result.err("No qualified name given (either null or empty) %s", fqn);

		final int len = fqn.length();
		int cstart = -1;
		boolean start = true;
		for (int i = 0; i < len;) {
			int cp = Character.codePointAt(fqn, i);
			if (start) {
				if (!Character.isJavaIdentifierStart(cp)) {
					return Result.err("Could not match %s to a qualified Java Identifier :: package? classname", fqn);
				}
				if (Character.isUpperCase(cp)) {
					cstart = i;
					break;
				}
				start = false;
			} else {
				if (cp == '.') {
					start = true;
				} else if (!Character.isJavaIdentifierPart(cp)) {
					return Result.err(
						"Could not match %s to a qualified Java Identifier :: package? classname, char %s", fqn, i);
				}
			}
			i += Character.charCount(cp);
		}
		String[] result = new String[2];
		if (cstart == 0) {
			result[1] = fqn;
		} else if (cstart > 0) {
			result[0] = fqn.substring(0, cstart - 1);
			result[1] = fqn.substring(cstart);
		} else {
			result[0] = fqn;
		}
		return Result.ok(result);
	}

}
