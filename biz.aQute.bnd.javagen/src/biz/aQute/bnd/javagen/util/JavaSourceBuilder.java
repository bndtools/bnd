package biz.aQute.bnd.javagen.util;

import java.util.Collection;
import java.util.Formatter;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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
import aQute.bnd.signatures.ThrowsSignature;
import aQute.bnd.signatures.TypeArgument;
import aQute.bnd.signatures.TypeParameter;
import aQute.bnd.signatures.TypeVariableSignature;

public class JavaSourceBuilder {
	final static Pattern	PACKAGE_NAME_P	= Pattern.compile("(.+)\\.([^.]+)");
	final static Pattern	PRIMITIVES_P	= Pattern.compile("void|boolan|byte|char|short|int|long|float|double");
	final Set<String>		imports			= new HashSet<>();
	final Set<String>		shortImports	= new HashSet<>();
	final Formatter			app;

	int						indent			= 0;

	public JavaSourceBuilder() {
		app = new Formatter();
	}

	public JavaSourceBuilder(Appendable app) {
		this.app = new Formatter(app);
	}

	public JavaSourceBuilder append(Object v) {
		app.format("%s", v);
		return this;
	}

	public JavaSourceBuilder body(Consumer<JavaSourceBuilder> body) {
		begin('{');
		if (body != null)
			body.accept(this);
		end('}');
		return this;
	}

	public JavaSourceBuilder interface_(String className) {
		return append("interface ").append(className)
			.append(" ");
	}

	public JavaSourceBuilder interface_(TypeRef facade) {
		return class_(facade.getShortName());
	}

	public JavaSourceBuilder class_(String className) {
		return append("class ").append(className)
			.append(" ");
	}

	public JavaSourceBuilder class_(TypeRef facade) {
		return class_(facade.getShortName());
	}

	public JavaSourceBuilder extends_(String... className) {
		if (className == null || className.length == 0)
			return this;

		format("extends ");
		list(className);
		append(" ");
		return this;
	}

	public JavaSourceBuilder extends_(TypeRef... typeRef) {
		if (typeRef == null)
			return this;

		return extends_(adjust(typeRef));
	}

	public JavaSourceBuilder final_() {
		return append("final ");
	}

	public JavaSourceBuilder implements_(String... interfaces) {
		if (interfaces.length == 0)
			return this;
		append("implements ");
		list(interfaces);
		append(" ");
		return this;
	}

	public JavaSourceBuilder implements_(TypeRef... implements_) {
		return implements_(Stream.of(implements_)
			.map(this::adjust)
			.toArray(String[]::new));
	}

	public JavaSourceBuilder import_(Collection<TypeRef> imported) {
		for (TypeRef r : imported) {
			import_(r);
		}
		return this;
	}

	public JavaSourceBuilder import_(String s) {
		if (s == null)
			return this;

		if (ref.isJava()) {
			return ref.getPackageRef()
				.getFQN()
				.equals("java.lang");
		}
		if (ref.isPrimitive())
			return true;

		if (ref.isArray())
			return true;

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

	public JavaSourceBuilder import_(TypeRef tr) {
		import_(tr.toString());
		return this;
	}

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

	JavaSourceBuilder quoted(String string) {
		StringBuilder sb = new StringBuilder(string);
		for ( int i=0; i<sb.length(); i++) {
			char c = sb.charAt(i);
			String repl = switch(c) {
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

	public JavaSourceBuilder typeref(TypeRef type) {
		append(adjust(type));
		return this;
	}

	public JavaSourceBuilder methodProlog(String name, Descriptor descriptor) {
		append(descriptor.getType());
		format(" %s(", name);
		String del = "";
		TypeRef[] prototype = descriptor.getPrototype();
		for (int arg = 0; arg < prototype.length; arg++) {
			format("%s", del);
			type(prototype[arg]);
			format("arg%d", arg);
			del = ",";
		}
		return this;
	}

	public JavaSourceBuilder nl() {
		append("\n");
		indent();
		return this;
	}

	public JavaSourceBuilder package_(PackageRef packageRef) {
		return package_(packageRef.toString());
	}

	public JavaSourceBuilder package_(String string) {
		return format("package %s;", string).nl();
	}

	public JavaSourceBuilder private_() {
		return append("private ");
	}

	public JavaSourceBuilder protected_() {
		return append("protected ");
	}

	public JavaSourceBuilder prototype(String name, MethodSignature ms) {
		typeParameters(ms.typeParameters);
		type(ms.resultType);
		format("%s(", name);
		String del = "";
		for (int arg = 0; arg < ms.parameterTypes.length; arg++) {
			format("%s", del);
			type(ms.parameterTypes[arg]);
			format("arg%d", arg);
			del = ",";
		}
		format(")");
		throwTypes(ms.throwTypes);
		return this;
	}

	public JavaSourceBuilder public_() {
		return append("public ");
	}

	public JavaSourceBuilder static_() {
		return append("static ");
	}

	public JavaSourceBuilder synchronized_() {
		return append("synchronized ");
	}

	public JavaSourceBuilder throwTypes(ThrowsSignature[] throwTypes) {
		if (throwTypes == null || throwTypes.length == 0) {
			return this;
		}
		format("throws ");
		String del = ",";
		for (ThrowsSignature throwsSignature : throwTypes) {
			format(del);
			type(throwsSignature);
		}

		return this;
	}

	@Override
	public String toString() {
		return app.toString();
	}

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

	public JavaSourceBuilder format(String format, Object... args) {
		app.format(format, args);
		return this;
	}

	void indent() {
		for (int i = 0; i < indent; i++)
			append(" ");
	}

	private String adjust(TypeRef typeRef) {
		if (typeRef.isJava() && typeRef.getPackageRef()
			.toString()
			.equals("java.lang"))
			return typeRef.getShortName();
		return imports.contains(typeRef.getFQN()) ? typeRef.getShortName() : typeRef.toString();
	}

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

	public JavaSourceBuilder return_() {
		append("return ");
		return this;
	}

	public JavaSourceBuilder nl(int count) {
		if (count <= 0)
			return this;
		while (count-- > 1) {
			append("\n");
		}
		return nl();
	}

	public JavaSourceBuilder call(MethodDef m, String... argNames) {
		if (m == null)
			return this;

		format("%s(", m.getName());

		String del = "";
		int n = m.getDescriptor()
			.getPrototype().length;
		for (int i = 0; i < n; i++) {
			format("%s%s", del, argName(i, argNames));
			del = ",";
		}
		append(");").nl();

		return this;
	}

	public JavaSourceBuilder override() {
		return append("@Override ");
	}
}
