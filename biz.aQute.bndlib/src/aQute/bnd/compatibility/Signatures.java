package aQute.bnd.compatibility;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.Map;

/**
 * This class can convert a Class, Field, Method or constructor to a generic
 * signature and it can normalize a signature. Both are methods. Normalized
 * signatures can be string compared and match even if the type variable names
 * differ.
 *
 * @version $Id$
 */
public class Signatures {

	/**
	 * Helper class to track an index in a string.
	 */
	static class Rover {
		final String	s;
		int				i;

		public Rover(String s) {
			this.s = s;
			i = 0;
		}

		char peek() {
			return s.charAt(i);
		}

		char take() {
			return s.charAt(i++);
		}

		char take(char c) {
			char x = s.charAt(i++);
			if (c != x)
				throw new IllegalStateException("get() expected " + c + " but got + " + x);
			return x;
		}

		public String upTo(String except) {
			int start = i;
			while (except.indexOf(peek()) < 0)
				take();
			return s.substring(start, i);
		}

		public boolean isEOF() {
			return i >= s.length();
		}

	}

	/**
	 * Calculate the generic signature of a Class,Method,Field, or Constructor.
	 *
	 * @throws Exception
	 */
	public String getSignature(Object c) throws Exception {
		if (c instanceof Class<?>)
			return getSignature((Class<?>) c);
		if (c instanceof Constructor<?>)
			return getSignature((Constructor<?>) c);
		if (c instanceof Method)
			return getSignature((Method) c);
		if (c instanceof Field)
			return getSignature((Field) c);

		throw new IllegalArgumentException(c.toString());
	}

	/**
	 * Calculate the generic signature of a Class. A Class consists of:
	 *
	 * <pre>
	 * class ::= declaration? reference reference*
	 * </pre>
	 *
	 * @throws Exception
	 */
	public String getSignature(Class<?> c) throws Exception {
		StringBuilder sb = new StringBuilder();
		declaration(sb, c);
		reference(sb, c.getGenericSuperclass());
		for (Type type : c.getGenericInterfaces()) {
			reference(sb, type);
		}
		return sb.toString();
	}

	/**
	 * Calculate the generic signature of a Method. A Method consists of:
	 *
	 * <pre>
	 * method ::= declaration? '(' reference* ')' reference
	 * </pre>
	 *
	 * @throws Exception
	 */
	public String getSignature(Method m) throws Exception {
		StringBuilder sb = new StringBuilder();
		declaration(sb, m);
		sb.append('(');
		for (Type type : m.getGenericParameterTypes()) {
			reference(sb, type);
		}
		sb.append(')');
		reference(sb, m.getGenericReturnType());
		return sb.toString();
	}

	/**
	 * Calculate the generic signature of a Constructor. A Constructor consists
	 * of:
	 *
	 * <pre>
	 *  constructor ::= declaration? '(' reference* ')V'
	 * </pre>
	 *
	 * @param c
	 * @throws Exception
	 */
	public String getSignature(Constructor<?> c) throws Exception {
		StringBuilder sb = new StringBuilder();
		declaration(sb, c);
		sb.append('(');
		for (Type type : c.getGenericParameterTypes()) {
			reference(sb, type);
		}
		sb.append(')');
		reference(sb, void.class);
		return sb.toString();
	}

	/**
	 * Calculate the generic signature of a Field. A Field consists of:
	 *
	 * <pre>
	 * constructor ::= reference
	 * </pre>
	 *
	 * @throws Exception
	 */
	public String getSignature(Field f) throws Exception {
		StringBuilder sb = new StringBuilder();
		Type t = f.getGenericType();
		reference(sb, t);
		return sb.toString();
	}

	/**
	 * Classes, Methods, or Constructors can have a declaration that provides
	 * nested a scope for type variables. A Method/Constructor inherits the type
	 * variables from its class and a class inherits its type variables from its
	 * outer class. The declaration consists of the following syntax:
	 *
	 * <pre>
	 * declarations ::= '<' declaration ( ',' declaration )* '>' declaration ::=
	 * identifier ':' declare declare ::= types | variable types ::= ( 'L' class
	 * ';' )? ( ':' 'L' interface ';' )* variable ::= 'T' id ';'
	 * </pre>
	 *
	 * @param sb
	 * @param gd
	 * @throws Exception
	 */
	private void declaration(StringBuilder sb, GenericDeclaration gd) throws Exception {
		TypeVariable<?>[] typeParameters = gd.getTypeParameters();
		if (typeParameters.length > 0) {
			sb.append('<');
			for (TypeVariable<?> tv : typeParameters) {
				sb.append(tv.getName());

				Type[] bounds = tv.getBounds();
				if (bounds.length > 0 && isInterface(bounds[0])) {
					sb.append(':');
				}
				for (Type bound : bounds) {
					sb.append(':');
					reference(sb, bound);
				}
			}
			sb.append('>');
		}
	}

	/**
	 * Verify that the type is an interface.
	 *
	 * @param type the type to check.
	 * @return true if this is a class that is an interface or a Parameterized
	 *         Type that is an interface
	 * @throws Exception
	 */
	private boolean isInterface(Type type) throws Exception {
		if (type instanceof Class)
			return ((Class<?>) type).isInterface();

		if (type instanceof ParameterizedType)
			return isInterface(((ParameterizedType) type).getRawType());

		return false;
	}

	/**
	 * This is the heart of the signature builder. A reference is used in a lot
	 * of places. It referes to another type.
	 *
	 * <pre>
	 *  reference ::= array | class
	 * | primitive | variable array ::= '[' reference class ::= 'L' body ( '.'
	 * body )* ';' body ::= id ( '<' ( wildcard | reference )* '>' )? variable
	 * ::= 'T' id ';' primitive ::= PRIMITIVE
	 * </pre>
	 *
	 * @param sb
	 * @param t
	 * @throws Exception
	 */
	private void reference(StringBuilder sb, Type t) throws Exception {
		if (t instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) t;
			sb.append('L');
			parameterizedType(sb, pt);
			sb.append(';');
			return;
		} else if (t instanceof GenericArrayType) {
			GenericArrayType gat = (GenericArrayType) t;
			sb.append('[');
			reference(sb, gat.getGenericComponentType());
		} else if (t instanceof WildcardType) {
			WildcardType wt = (WildcardType) t;
			Type[] lowerBounds = wt.getLowerBounds();
			Type[] upperBounds = wt.getUpperBounds();

			if (upperBounds.length == 1 && upperBounds[0] == Object.class)
				upperBounds = new Type[0];

			if (upperBounds.length != 0) {
				// extend
				for (Type upper : upperBounds) {
					sb.append('+');
					reference(sb, upper);
				}
			} else if (lowerBounds.length != 0) {
				// super, can only be one by the language
				for (Type lower : lowerBounds) {
					sb.append('-');
					reference(sb, lower);
				}
			} else
				sb.append('*');
		} else if (t instanceof TypeVariable) {
			TypeVariable<?> tv = (TypeVariable<?>) t;
			sb.append('T');
			sb.append(tv.getName());
			sb.append(';');
		} else if (t instanceof Class<?>) {
			Class<?> c = (Class<?>) t;
			if (c.isPrimitive()) {
				sb.append(primitive(c));
			} else {
				sb.append('L');
				String name = c.getName()
					.replace('.', '/');
				sb.append(name);
				sb.append(';');
			}
		}
	}

	/**
	 * Creates the signature for a Parameterized Type. A Parameterized Type has
	 * a raw class and a set of type variables.
	 *
	 * @param sb
	 * @param pt
	 * @throws Exception
	 */
	private void parameterizedType(StringBuilder sb, ParameterizedType pt) throws Exception {
		String name = ((Class<?>) pt.getRawType()).getName()
			.replace('.', '/');
		Type owner = pt.getOwnerType();
		if (owner != null) {
			if (owner instanceof ParameterizedType)
				parameterizedType(sb, (ParameterizedType) owner);
			else
				sb.append(((Class<?>) owner).getName()
					.replace('.', '/'));
			sb.append('.');
			int n = name.lastIndexOf('$');
			name = name.substring(n + 1);
		}
		sb.append(name);

		sb.append('<');
		for (Type parameterType : pt.getActualTypeArguments()) {
			reference(sb, parameterType);
		}
		sb.append('>');

	}

	/**
	 * Handle primitives, these need to be translated to a single char.
	 *
	 * @param type the primitive class
	 * @return the single char associated with the primitive
	 */
	private char primitive(Class<?> type) {
		if (type == byte.class)
			return 'B';
		else if (type == char.class)
			return 'C';
		else if (type == double.class)
			return 'D';
		else if (type == float.class)
			return 'F';
		else if (type == int.class)
			return 'I';
		else if (type == long.class)
			return 'J';
		else if (type == short.class)
			return 'S';
		else if (type == boolean.class)
			return 'Z';
		else if (type == void.class)
			return 'V';
		else
			throw new IllegalArgumentException("Unknown primitive type " + type);
	}

	/**
	 * Normalize a signature to make sure the name of the variables are always
	 * the same. We change the names of the type variables to _n, where n is an
	 * integer. n is incremented for every new name and already used names are
	 * replaced with the _n name.
	 *
	 * @return a normalized signature
	 */

	public String normalize(String signature) {
		StringBuilder sb = new StringBuilder();
		Map<String, String> map = new HashMap<>();
		Rover rover = new Rover(signature);
		declare(sb, map, rover);

		if (rover.peek() == '(') {
			// method or constructor
			sb.append(rover.take('('));
			while (rover.peek() != ')') {
				reference(sb, map, rover, true);
			}
			sb.append(rover.take(')'));
			reference(sb, map, rover, true); // return type
		} else {
			// field or class
			reference(sb, map, rover, true); // field type or super class
			while (!rover.isEOF()) {
				reference(sb, map, rover, true); // interfaces
			}
		}
		return sb.toString();
	}

	/**
	 * The heart of the routine. Handle a reference to a type. Can be an array,
	 * a class, a type variable, or a primitive.
	 *
	 * @param sb
	 * @param map
	 * @param rover
	 * @param primitivesAllowed
	 */
	private void reference(StringBuilder sb, Map<String, String> map, Rover rover, boolean primitivesAllowed) {

		char type = rover.take();
		sb.append(type);

		if (type == '[') {
			reference(sb, map, rover, true);
		} else if (type == 'L') {
			String fqnb = rover.upTo("<;.");
			sb.append(fqnb);
			body(sb, map, rover);
			while (rover.peek() == '.') {
				sb.append(rover.take('.'));
				sb.append(rover.upTo("<;."));
				body(sb, map, rover);
			}
			sb.append(rover.take(';'));
		} else if (type == 'T') {
			String name = rover.upTo(";");
			name = assign(map, name);
			sb.append(name);
			sb.append(rover.take(';'));
		} else {
			if (!primitivesAllowed)
				throw new IllegalStateException("Primitives are not allowed without an array");
		}
	}

	/**
	 * Because classes can be nested the body handles the part that can be
	 * nested, the reference handles the enclosing L ... ;
	 *
	 * @param sb
	 * @param map
	 * @param rover
	 */
	private void body(StringBuilder sb, Map<String, String> map, Rover rover) {
		if (rover.peek() == '<') {
			sb.append(rover.take('<'));
			while (rover.peek() != '>') {
				switch (rover.peek()) {
					case 'L' :
					case '[' :
						reference(sb, map, rover, false);
						break;

					case 'T' :
						String name;
						sb.append(rover.take('T')); // 'T'
						name = rover.upTo(";");
						sb.append(assign(map, name));
						sb.append(rover.take(';'));
						break;

					case '+' : // extends
					case '-' : // super
						sb.append(rover.take());
						reference(sb, map, rover, false);
						break;

					case '*' : // wildcard
						sb.append(rover.take());
						break;

				}
			}
			sb.append(rover.take('>'));
		}
	}

	/**
	 * Handle the declaration part.
	 *
	 * @param sb
	 * @param map
	 * @param rover
	 */
	private void declare(StringBuilder sb, Map<String, String> map, Rover rover) {
		char c = rover.peek();
		if (c == '<') {
			sb.append(rover.take('<'));

			while (rover.peek() != '>') {
				String name = rover.upTo(":");
				name = assign(map, name);
				sb.append(name);
				typeVar: while (rover.peek() == ':') {
					sb.append(rover.take(':'));
					switch (rover.peek()) {
						case ':' : // empty class cases
							continue typeVar;

						default :
							reference(sb, map, rover, false);
							break;
					}
				}
			}
			sb.append(rover.take('>'));
		}
	}

	/**
	 * Handles the assignment of type variables to index names so that we have a
	 * normalized name for each type var.
	 *
	 * @param map the map with variables.
	 * @param name The name of the variable
	 * @return the index name, like _1
	 */
	private String assign(Map<String, String> map, String name) {
		if (map.containsKey(name))
			return map.get(name);
		int n = map.size();
		map.put(name, "_" + n);
		return "_" + n;
	}
}
