package aQute.bnd.compatibility;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

public class RuntimeSignatureBuilder {
	final Scope root;

	public RuntimeSignatureBuilder(Scope root) {
		this.root = root;
	}

	static public String identity(Class<?> c) {
		return Scope.classIdentity(c.getName());
	}

	static public String identity(Method m) {
		return Scope.methodIdentity(m.getName(), getDescriptor(m.getReturnType(), m.getParameterTypes()));
	}

	static public String identity(Constructor<?> m) {
		return Scope.constructorIdentity(getDescriptor(void.class, m.getParameterTypes()));
	}

	static public String identity(Field m) {
		return Scope.fieldIdentity(m.getName(), getDescriptor(m.getType(), null));
	}

	static public String getDescriptor(Class<?> base, Class<?>[] parameters) {
		StringBuilder sb = new StringBuilder();
		if (parameters != null) {
			sb.append("(");
			for (Class<?> parameter : parameters) {
				sb.append(getDescriptor(parameter));
			}
			sb.append(")");
		}
		sb.append(getDescriptor(base));
		return sb.toString();
	}

	public Scope add(Class<?> c) {
		Scope local = add(root, getEnclosingScope(c), c.getModifiers(), c.getTypeParameters(), Kind.CLASS, identity(c),
			c.getGenericSuperclass(), c.getGenericInterfaces(), null);

		for (Field f : c.getDeclaredFields()) {
			add(local, // declaring scope
				local, // enclosing
				f.getModifiers(), // access modifiers
				null, // fields have no type vars
				Kind.FIELD, // field
				identity(f), // the name of the field
				f.getGenericType(), // the type of the field
				null, // fields have no parameters
				null // fields have no exceptions
			);
		}

		for (Constructor<?> constr : c.getConstructors()) {
			add(local, // class scope
				local, // enclosing
				constr.getModifiers(), // access modifiers
				constr.getTypeParameters(), // Type vars
				Kind.CONSTRUCTOR, // constructor
				identity(constr), // <init>(type*)
				void.class, // Always void
				constr.getGenericParameterTypes(), // parameters types
				constr.getGenericExceptionTypes() // exception types
			);
		}

		for (Method m : c.getDeclaredMethods()) {
			if (m.getDeclaringClass() != Object.class) {
				add(local, // class scope
					local, // enclosing
					m.getModifiers(), // access modifiers
					m.getTypeParameters(), Kind.METHOD, // method
					identity(m), // <name>(type*)return
					m.getGenericReturnType(), // return type
					m.getGenericParameterTypes(), // parameter types
					m.getGenericExceptionTypes() // exception types
				);
			}
		}

		return local;
	}

	private Scope getEnclosingScope(Class<?> c) {
		Method m = c.getEnclosingMethod();
		if (m != null) {
			Scope s = getGlobalScope(m.getDeclaringClass());
			return s.getScope(identity(m));
		}
		// TODO
		// Constructor cnstr = c.getEnclosingConstructor();
		// if (m != null) {
		// Scope s = getGlobalScope(cnstr.getDeclaringClass());
		// return s.getScope(identity(cnstr));
		//
		// }
		Class<?> enclosingClass = c.getEnclosingClass();
		if (enclosingClass != null) {
			return getGlobalScope(enclosingClass);
		}

		return null;
	}

	private Scope getGlobalScope(Class<?> c) {
		if (c == null)
			return null;
		String id = identity(c);
		return root.getScope(id);
	}

	private Scope add(Scope declaring, Scope enclosing, int modifiers, TypeVariable<?>[] typeVariables, Kind kind,
		String id, Type mainType, Type[] parameterTypes, Type exceptionTypes[]) {

		Scope scope = declaring.getScope(id);
		assert scope.access == Access.UNKNOWN;
		scope.setAccess(Access.modifier(modifiers));
		scope.setKind(kind);
		scope.setGenericParameter(convert(typeVariables));
		scope.setBase(convert(scope, mainType));
		scope.setParameterTypes(convert(parameterTypes));
		scope.setExceptionTypes(convert(exceptionTypes));
		scope.setDeclaring(declaring);
		scope.setEnclosing(enclosing);
		return scope;
	}

	private GenericType convert(Scope source, Type t) {
		if (t instanceof ParameterizedType) {
			// C<P..>
			ParameterizedType pt = (ParameterizedType) t;
			/* Scope reference = */root.getScope(identity((Class<?>) pt.getRawType()));
			Type args[] = pt.getActualTypeArguments();
			GenericType[] arguments = new GenericType[args.length];
			int n = 0;
			for (Type arg : args)
				arguments[n++] = convert(source, arg);
			// return new GenericType(reference,null,arguments);

		} else if (t instanceof TypeVariable) {
			// TypeVariable tv = (TypeVariable) t;
			// return new GenericType(source,tv.getName(), null);
		} else if (t instanceof WildcardType) {
			// WildcardType wc = (WildcardType) t;
			// wc.
		} else if (t instanceof GenericArrayType) {

		}
		if (t instanceof Class<?>) {
			// raw = ((Class<?>) t).getName() + ";";
		} else
			throw new IllegalArgumentException(t.toString());

		return null;
	}

	private GenericParameter[] convert(TypeVariable<?> vars[]) {
		if (vars == null)
			return null;

		GenericParameter out[] = new GenericParameter[vars.length];
		for (int i = 0; i < vars.length; i++) {
			GenericType gss[] = convert(vars[i].getBounds());
			out[i] = new GenericParameter(vars[i].getName(), gss);
		}
		return out;
	}

	private GenericType[] convert(Type[] parameterTypes) {
		if (parameterTypes == null || parameterTypes.length == 0)
			return GenericType.EMPTY;

		GenericType tss[] = new GenericType[parameterTypes.length];
		for (int i = 0; i < parameterTypes.length; i++) {
			// tss[i] = new GenericType(parameterTypes[i]);
		}
		return tss;
	}

	private static String getDescriptor(Class<?> c) {
		StringBuilder sb = new StringBuilder();
		if (c.isPrimitive()) {
			if (c == boolean.class)
				sb.append("Z");
			else if (c == byte.class)
				sb.append("Z");
			else if (c == char.class)
				sb.append("C");
			else if (c == short.class)
				sb.append("S");
			else if (c == int.class)
				sb.append("I");
			else if (c == long.class)
				sb.append("J");
			else if (c == float.class)
				sb.append("F");
			else if (c == double.class)
				sb.append("D");
			else if (c == void.class)
				sb.append("V");
			else
				throw new IllegalArgumentException("unknown primitive type: " + c);
		} else if (c.isArray()) {
			sb.append("[");
			sb.append(getDescriptor(c));
		} else {
			sb.append("L");
			sb.append(c.getName()
				.replace('.', '/'));
			sb.append(";");
		}
		return sb.toString();
	}

}
