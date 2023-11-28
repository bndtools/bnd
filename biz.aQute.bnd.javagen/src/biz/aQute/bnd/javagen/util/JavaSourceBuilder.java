package biz.aQute.bnd.javagen.util;

import java.io.IOException;
import java.util.Collection;
import java.util.Formatter;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.Nullable;

import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Clazz.MethodDef;
import aQute.bnd.osgi.Descriptors.Descriptor;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.signatures.ArrayTypeSignature;
import aQute.bnd.signatures.BaseType;
import aQute.bnd.signatures.ClassTypeSignature;
import aQute.bnd.signatures.MethodSignature;
import aQute.bnd.signatures.ReferenceTypeSignature;
import aQute.bnd.signatures.Result;
import aQute.bnd.signatures.SimpleClassTypeSignature;
import aQute.bnd.signatures.TypeArgument;
import aQute.bnd.signatures.TypeParameter;
import aQute.bnd.signatures.TypeVariableSignature;
import aQute.bnd.signatures.VoidDescriptor;

/**
 * Utility class to build sources. It is simple and not complete. Please extend
 * it as you run in new use cases. This class does not attempt to abstract the
 * whole source code building in an AST and then print it. It is just a
 * convenience but it is still based on text. It is supposed to be used in
 * conjunction with the bnd Clazz and classfile library.
 */
public class JavaSourceBuilder {
	final static Pattern	PACKAGE_NAME_P	= Pattern.compile("(.+)\\.([^.]+)");
	final static Pattern	PRIMITIVES_P	= Pattern.compile("void|boolan|byte|char|short|int|long|float|double");
	final Set<String>		imports			= new HashSet<>();
	final Set<String>		shortImports	= new HashSet<>();
	final Formatter			app;

	int						indent			= 0;

	/**
	 * Default constructor. The output goes to Formatter.
	 */
	public JavaSourceBuilder() {
		app = new Formatter();
	}

	/**
	 * Format the output to an appendable.
	 *
	 * @param app the appendable
	 */
	public JavaSourceBuilder(Appendable app) {
		this.app = new Formatter(app);
	}

	/**
	 * Append the given object
	 *
	 * @param v the value to append
	 * @return this
	 */
	public JavaSourceBuilder append(Object v) {
		try {
			app.out()
				.append(Objects.toString(v));
			return this;
		} catch (IOException e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * Starts a body based on '{' and '}'
	 *
	 * @param body the code that will build the body
	 * @return this
	 */
	public JavaSourceBuilder body(Runnable body) {
		begin('{');
		if (body != null)
			body.run();
		end('}');
		return this;
	}

	/**
	 * Call a method
	 *
	 * @param m the method definition from Clazz
	 * @param expressions
	 * @return this
	 */
	public JavaSourceBuilder call(MethodDef m, String... expressions) {
		if (m == null)
			return this;

		format("%s(", m.getName());

		String del = "";
		int n = m.getDescriptor()
			.getPrototype().length;
		for (int i = 0; i < n; i++) {
			format("%s%s", del, argName(i, expressions));
			del = ",";
		}
		append(");").nl();

		return this;
	}

	/**
	 * Create a class declaration for the given className
	 *
	 * @param className the name of the class
	 * @return this
	 */
	public JavaSourceBuilder class_(String className) {
		return append("class ").append(className)
			.append(" ");
	}

	/**
	 * Create a class declaration for the given className
	 *
	 * @param className the name of the class
	 * @return this
	 */
	public JavaSourceBuilder class_(TypeRef className) {
		return class_(className.getShortName());
	}

	/**
	 * Extend the class with the given class name. If the first parameter is
	 * null, this will be skipped. There are multiple parameters because
	 * interfaces can extend multiple other interfaces.
	 *
	 * @param classNames the names of the class
	 * @return this
	 */
	public JavaSourceBuilder extends_(String... classNames) {
		if (classNames == null || classNames.length == 0 || classNames[0] == null)
			return this;

		format("extends ");
		list(classNames);
		append(" ");
		return this;
	}

	/**
	 * Extend the class with the given class name. If the first parameter is
	 * null, this will be skipped. There are multiple parameters because
	 * interfaces can extend multiple other interfaces.
	 *
	 * @param classNames the names of the class
	 * @return this
	 */
	public JavaSourceBuilder extends_(TypeRef... classNames) {
		if (classNames == null || classNames.length == 1 && classNames[0] == null)
			return this;

		return extends_(adjust(classNames));
	}

	/**
	 * Append 'final '
	 *
	 * @return this
	 */
	public JavaSourceBuilder final_() {
		return append("final ");
	}

	/**
	 * Generic format method.
	 *
	 * @param format the specification
	 * @param args the arguments
	 * @return this
	 */

	public JavaSourceBuilder format(String format, Object... args) {
		app.format(format, args);
		return this;
	}

	/**
	 * Add an implements of a set of interfaces
	 *
	 * @param interfaces the interfaces
	 * @return this
	 */
	public JavaSourceBuilder implements_(String... interfaces) {
		if (interfaces == null || interfaces.length == 0)
			return this;
		append("implements ");
		list(interfaces);
		append(" ");
		return this;
	}

	/**
	 * Add an implements of a set of interfaces
	 *
	 * @param interfaces the interfaces
	 * @return this
	 */
	public JavaSourceBuilder implements_(TypeRef... interfaces) {
		if (interfaces == null || interfaces.length == 0)
			return this;

		return implements_(Stream.of(interfaces)
			.map(this::adjust)
			.toArray(String[]::new));
	}

	/**
	 * Import a set of type references
	 *
	 * @param imported the set
	 * @return this
	 */
	public JavaSourceBuilder import_(Collection<TypeRef> imported) {
		for (TypeRef r : imported) {
			import_(r);
		}
		return this;
	}

	/**
	 * Import a type references. This will print the reference if it is not a
	 * primitive, not an array, and has not been imported already.
	 *
	 * @param s the reference
	 * @return this
	 */
	public JavaSourceBuilder import_(@Nullable
	String s) {
		if (s == null)
			return this;

		if (s.endsWith("[]")) {
			return import_(s.substring(0, s.length() - 2));
		}

		Matcher m = PACKAGE_NAME_P.matcher(s);
		if (m.matches()) {
			String shortName = m.group(2);
			if (shortImports.contains(shortName))
				return this;
		}

		if (!imports.add(s))
			return this;

		return format("import %s;", s).nl();
	}

	/**
	 * Import a type references. This will print the reference if it is not a
	 * primitive, not an array, and has not been imported already.
	 *
	 * @param s the reference
	 * @return this
	 */
	public JavaSourceBuilder import_(TypeRef s) {
		import_(s.getFQN());
		return this;
	}

	/**
	 * Append an interface if the given className is not null
	 *
	 * @param className the class name of the interface or null
	 * @return this
	 */
	public JavaSourceBuilder interface_(@Nullable
	String className) {

		if (className == null)
			return this;

		return append("interface ").append(className)
			.append(" ");
	}

	/**
	 * Append an interface if the given className is not null
	 *
	 * @param className the class name of the interface or null
	 * @return this
	 */
	public JavaSourceBuilder interface_(@Nullable
	TypeRef className) {

		if (className == null)
			return this;

		return class_(className.getShortName());
	}

	/**
	 * Create the prototype of a method. This is without the modifiers and body.
	 * The prototype will show the shortened names for imported classes and
	 * include all generic information.
	 *
	 * @param mdef the method definition from Clazz
	 * @param argNames the actual argument names, missing names are calculated
	 * @return this
	 */
	public JavaSourceBuilder method(MethodDef mdef, String... argNames) {
		if (mdef.isConstructor())
			return this;

		String name = mdef.getName();
		Clazz clazz = (Clazz) mdef.getClazz();

		String signature = mdef.getSignature();
		if (signature != null) {
			MethodSignature ms = MethodSignature.of(signature);
			typeParameters(ms.typeParameters);
			type(ms.resultType);
			append(" ");
			append(name);
			append("(");
			String del = "";
			for (int i = 0; i < ms.parameterTypes.length; i++) {
				format("%s", del);
				type(ms.parameterTypes[i]);
				format(" %s", argName(i, argNames));
				del = ", ";
			}
			append(") ");
			if (ms.throwTypes.length > 0) {
				append("throws ");
				listTyped(ms.throwTypes);
			} else {
				TypeRef[] trs = mdef.getThrows();
				if (trs.length > 0) {
					append(" throws ");
					list(trs);
				}
			}
		} else {
			Descriptor descriptor = mdef.getDescriptor();
			typeref(descriptor.getType());
			append(" ");
			append(name);
			append("(");

			String del = "";
			TypeRef[] types = descriptor.getPrototype();

			for (int i = 0; i < types.length; i++) {
				append(del);
				typeref(types[i]);
				format(" %s", argName(i, argNames));
				del = ", ";
			}
			append(") ");
			TypeRef[] trs = mdef.getThrows();
			if (trs.length > 0) {
				append("throws ");
				list(trs);
			}
		}
		return this;
	}

	/**
	 * Add a new line, handling indent
	 *
	 * @return this
	 */
	public JavaSourceBuilder nl() {
		append("\n");
		indent();
		return this;
	}

	/**
	 * Append count number of new lines if positive
	 *
	 * @param count the number of new lines
	 * @return this
	 */
	public JavaSourceBuilder nl(int count) {
		if (count <= 0)
			return this;
		while (count-- > 1) {
			append("\n");
		}
		return nl();
	}

	/**
	 * Add an override annotation
	 *
	 * @return this
	 */

	public JavaSourceBuilder override() {
		return append("@Override ");
	}

	/**
	 * Append a package statement
	 *
	 * @param packageRef the package name
	 * @return this
	 */
	public JavaSourceBuilder package_(PackageRef packageRef) {
		return package_(packageRef.toString());
	}

	/**
	 * Append a package statement
	 *
	 * @param packageRef the package name
	 * @return this
	 */
	public JavaSourceBuilder package_(String packageRef) {
		return format("package %s;", packageRef).nl();
	}

	/**
	 * Append private
	 *
	 * @return this
	 */
	public JavaSourceBuilder private_() {
		return append("private ");
	}

	/**
	 * Append protected
	 *
	 * @return this
	 */
	public JavaSourceBuilder protected_() {
		return append("protected ");
	}

	/**
	 * Append public
	 *
	 * @return this
	 */
	public JavaSourceBuilder public_() {
		return append("public ");
	}

	/**
	 * Append a return statement
	 *
	 * @return this
	 */
	public JavaSourceBuilder return_() {
		append("return ");
		return this;
	}

	/**
	 * Append static
	 *
	 * @return this
	 */
	public JavaSourceBuilder static_() {
		return append("static ");
	}

	/**
	 * Add a suppress warning annotation.
	 *
	 * @param warnings the warning string
	 * @return this
	 */
	public JavaSourceBuilder suppressWarnings(String... warnings) {
		if (warnings.length == 0)
			return this;

		append("@SuppressWarnings(");
		if (warnings.length == 1) {
			quoted(warnings[0]);
		} else {
			append("{ ");
			String del = "";
			for (String w : warnings) {
				append(del);
				quoted(w);
				del = ",";
			}
			append(" }");
		}
		append(") ");
		return this;
	}


	/**
	 * Append synchronized
	 *
	 * @return this
	 */
	public JavaSourceBuilder synchronized_() {
		return append("synchronized ");
	}

	/**
	 * toString
	 */
	@Override
	public String toString() {
		return app.toString();
	}

	/**
	 * Append a type parameter. Type parameters are part of a type declaration
	 * like {@code <T extends Foo>}.
	 *
	 * @param typeParameter the type parameter
	 * @return this
	 */
	public JavaSourceBuilder typeParamater(TypeParameter typeParameter) {
		format("%s", typeParameter.identifier);

		if (!isObject(typeParameter.classBound)) {
			format(" extends ");
			type(typeParameter.classBound);
		}
		if (typeParameter.interfaceBounds.length > 0) {
			for (ReferenceTypeSignature type : typeParameter.interfaceBounds) {
				append(" & ");
				type(type);
			}
		}
		return this;
	}

	/**
	 * Append a set of type parameter. Type parameters are part of a type
	 * declaration like {@code <T extends Foo>}.
	 *
	 * @param typeParameters the type parameters
	 * @return this
	 */
	public JavaSourceBuilder typeParameters(TypeParameter[] typeParameters) {
		if (typeParameters == null || typeParameters.length == 0) {
			return this;
		}
		format("<");
		String del = "";
		for (TypeParameter typeParameter : typeParameters) {
			format(del);
			typeParamater(typeParameter);
			del = ",";
		}
		format("> ");
		return this;
	}

	/**
	 * Append a type type name, using the short name if possible.
	 *
	 * @param type the type to add
	 * @return this
	 */
	public JavaSourceBuilder typeref(TypeRef type) {
		append(adjust(type));
		return this;
	}

	/*
	 * Append the current indent
	 */
	void indent() {
		for (int i = 0; i < indent; i++)
			append(" ");
	}

	/**
	 * Add a quoted string, escaping characters that need to be escaped.
	 *
	 * @param string the string to quote
	 * @return this
	 */
	JavaSourceBuilder quoted(String string) {
		StringBuilder sb = new StringBuilder(string);
		for (int i = 0; i < sb.length(); i++) {
			char c = sb.charAt(i);
			String repl = switch (c) {
				case '"' -> "\\\"";
				case '\n' -> "\\n";
				case '\t' -> "\\t";
				case '\r' -> "\\r";
				case '\b' -> "\\b";
				case '\f' -> "\\f";
				default -> null;
			};
			if (repl != null) {
				sb.delete(i, i + 1);
				sb.insert(i, repl);
			}
		}
		sb.insert(0, '"');
		sb.append('"');
		append(sb.toString());
		return this;
	}

	/*
	 * Adjust a type ref to a shorter name if it was imported.
	 * @param typeRef the type reference
	 * @return this
	 */
	private String adjust(String typeRef) {
		Matcher m = PACKAGE_NAME_P.matcher(typeRef);
		if (m.matches()) {
			String package_ = m.group(1);
			String simple = m.group(2);
			if (package_.equals("java.lang"))
				return simple;
			return imports.contains(typeRef) ? simple : typeRef;
		}
		return typeRef;
	}

	/*
	 * Adjust a type ref to a shorter name if it was imported.
	 * @param typeRef the type reference
	 * @return this
	 */
	private String adjust(TypeRef typeRef) {
		if (typeRef.isJava() && typeRef.getPackageRef()
			.toString()
			.equals("java.lang"))
			return typeRef.getShortName();
		return imports.contains(typeRef.getFQN()) ? typeRef.getShortName() : typeRef.toString();
	}

	private String[] adjust(TypeRef[] typeRef) {
		return Stream.of(typeRef)
			.map(this::adjust)
			.toArray(String[]::new);
	}

	private String argName(int i, String... argNames) {
		return i >= argNames.length ? "arg" + i : argNames[i];
	}

	private void begin(char start) {
		format(" %s\n", start);
		indent += 2;
		indent();
	}

	private void end(char end) {
		format("\n");
		indent -= 2;
		indent();
		format("}\n", end);
		indent();
	}

	private boolean isObject(Result type) {
		return ClassTypeSignature.OBJECT.equals(type);
	}

	private void list(String[] strings) {
		String del = "";
		for (Object r : strings) {
			append(del).append(r);
			del = ",";
		}
	}

	private void list(TypeRef[] prototype) {
		String del = "";
		for (TypeRef r : prototype) {
			append(del).typeref(r);
			del = ",";
		}
	}

	private void listTyped(Object[] parameterTypes) {
		String del = "";
		for (Object r : parameterTypes) {
			append(del);
			type(r);
			del = ",";
		}
	}

	private void type(Object type) {
		if (type instanceof BaseType ats) {
			String name = switch (ats) {
				case B -> "byte";
				case C -> "char";
				case D -> "double";
				case F -> "float";
				case I -> "int";
				case J -> "long";
				case S -> "short";
				case Z -> "boolean";
			};
			append(name);
			return;
		}
		if (type instanceof VoidDescriptor ats) {
			append("void");
			return;
		}
		if (type instanceof ArrayTypeSignature ats) {
			type(ats.component);
			append("[]");
			return;
		}
		if (type instanceof SimpleClassTypeSignature ats) {
			append(adjust(ats.identifier));
			if (ats.typeArguments.length > 0) {
				append("<");
				listTyped(ats.typeArguments);
				append(">");
			}
			return;
		}
		if (type instanceof ClassTypeSignature ats) {
			type(ats.classType);
			for (SimpleClassTypeSignature s : ats.innerTypes) {
				append(".");
				type(s);
			}
			return;
		}
		if (type instanceof TypeVariableSignature ats) {
			append(ats.identifier);

		}

		if (type instanceof TypeArgument ats) {
			switch (ats.wildcard) {
				case EXACT :
					type(ats.type);
					break;
				case WILD :
					append("?");
					break;
				case EXTENDS :
					append("? extends ").type(ats.type);
					break;
				case SUPER :
					append("? super ").type(ats.type);
					break;
			}
		}
	}
}
