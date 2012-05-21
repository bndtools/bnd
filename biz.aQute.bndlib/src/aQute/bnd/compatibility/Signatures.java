/*
 * Copyright (c) OSGi Alliance (2009, 2010). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package aQute.bnd.compatibility;

import java.lang.reflect.*;
import java.util.*;

/**
 * This class is compiled against 1.5 or later to provide access to the generic
 * signatures. It can convert a Class, Field, Method or constructor to a generic
 * signature and it can normalize a signature. Both are methods. Normalized
 * signatures can be string compared and match even if the type variable names
 * differ.
 * 
 * @version $Id$
 */
public class Signatures {
	
	
	/**
	 * Check if the environment has generics, i.e. later than 
	 * Java 5 VM.
	 * 
	 * @return true if generics are supported
	 * @throws Exception
	 */
	public boolean hasGenerics() throws Exception {
		try {
			call( Signatures.class, "getGenericSuperClass");
			return true;
		} catch( NoSuchMethodException mnfe ) {
			return false;
		}
	}
	
	
	
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
				throw new IllegalStateException("get() expected " + c
						+ " but got + " + x);
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
	 * @param f
	 * @return
	 * @throws Exception 
	 */
	public String getSignature(Object c) throws Exception {
		if( c instanceof Class<?>)
			return getSignature((Class<?>)c);
		if( c instanceof Constructor<?>)
			return getSignature((Constructor<?>)c);
		if( c instanceof Method)
			return getSignature((Method)c);
		if( c instanceof Field)
			return getSignature((Field)c);
		
		throw new IllegalArgumentException(c.toString());
	}

	/**
	 * Calculate the generic signature of a Class. A Class consists of:
	 * 
	 * <pre>
	 * 	  class        ::= declaration? reference reference*
	 * </pre>
	 * 
	 * 
	 * @param f
	 * @return
	 * @throws Exception 
	 */
	public String getSignature(Class< ? > c) throws Exception {
		StringBuilder sb = new StringBuilder();
		declaration(sb, c);
		reference(sb, call(c, "getGenericSuperclass"));
		for (Object type : (Object[]) call(c,"getGenericInterfaces")) {
			reference(sb, type);
		}
		return sb.toString();
	}

	/**
	 * Calculate the generic signature of a Method. A Method consists of:
	 * 
	 * <pre>
	 *    method ::= declaration? '(' reference* ')' reference
	 * </pre>
	 * 
	 * @param c
	 * @return
	 * @throws Exception 
	 */
	public String getSignature(Method m) throws Exception {
		StringBuilder sb = new StringBuilder();
		declaration(sb, m);
		sb.append('(');
		for (Object type : (Object[]) call(m,"getGenericParameterTypes")) {
			reference(sb, type);
		}
		sb.append(')');
		reference(sb, call(m,"getGenericReturnType"));
		return sb.toString();
	}

	/**
	 * Calculate the generic signature of a Constructor. A Constructor consists
	 * of:
	 * 
	 * <pre>
	 *    constructor ::= declaration? '(' reference* ')V'
	 * </pre>
	 * 
	 * @param c
	 * @return
	 * @throws Exception 
	 */
	public String getSignature(Constructor< ? > c) throws Exception {
		StringBuilder sb = new StringBuilder();
		declaration(sb, c);
		sb.append('(');
		for (Object type : (Object[]) call(c,"getGenericParameterTypes")) {
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
	 *    constructor ::= reference
	 * </pre>
	 * 
	 * @param c
	 * @return
	 * @throws Exception 
	 */
	public String getSignature(Field f) throws Exception {
		StringBuilder sb = new StringBuilder();
		Object t = call(f,"getGenericType");
		reference(sb, t);
		return sb.toString();
	}

/**
	 * Classes, Methods, or Constructors can have a declaration that provides
	 * nested a scope for type variables. A Method/Constructor inherits
	 * the type variables from its class and a class inherits its type variables
	 * from its outer class. The declaration consists of the following
	 * syntax:
	 * <pre>
	 *    declarations ::= '<' declaration ( ',' declaration )* '>'
	 *    declaration  ::= identifier ':' declare
	 *    declare      ::= types | variable 
	 *    types        ::= ( 'L' class ';' )? ( ':' 'L' interface ';' )*
	 *    variable     ::= 'T' id ';'
	 * </pre>
	 * 
	 * @param sb
	 * @param gd
 * @throws Exception 
	 */
	private void declaration(StringBuilder sb, Object gd) throws Exception {
		Object[] typeParameters = (Object[]) call(gd,"getTypeParameters");
		if (typeParameters.length > 0) {
			sb.append('<');
			for (Object tv : typeParameters) {
				sb.append( call(tv,"getName"));

				Object[] bounds = (Object[]) call(tv,"getBounds");
				if (bounds.length > 0 && isInterface(bounds[0])) {
					sb.append(':');
				}
				for (int i = 0; i < bounds.length; i++) {
					sb.append(':');
					reference(sb, bounds[i]);
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
	private boolean isInterface(Object type) throws Exception {
		if (type instanceof Class)
			return (((Class< ? >) type).isInterface());

		if ( isInstance(type.getClass(), "java.lang.reflect.ParameterizedType"))
			return isInterface(call(type,"getRawType"));

		return false;
	}


/**
	 * This is the heart of the signature builder. A reference is used
	 * in a lot of places. It referes to another type.
	 * <pre>
	 *   reference     ::= array | class | primitive | variable
	 *   array         ::= '[' reference
	 *   class         ::=  'L' body ( '.' body )* ';'
	 *   body          ::=  id ( '<' ( wildcard | reference )* '>' )?
	 *   variable      ::=  'T' id ';'
	 *   primitive     ::= PRIMITIVE
	 * </pre>
	 * 
	 * @param sb
	 * @param t
 * @throws Exception 
	 */
	private void reference(StringBuilder sb, Object t) throws Exception {

		if ( isInstance(t.getClass(),"java.lang.reflect.ParameterizedType")) {
			sb.append('L');
			parameterizedType(sb, t);
			sb.append(';');
			return;
		}
		else
			if ( isInstance(t.getClass(), "java.lang.reflect.GenericArrayType")) {
				sb.append('[');
				reference(sb, call(t,"getGenericComponentType"));
			}
			else
				if ( isInstance(t.getClass(), "java.lang.reflect.WildcardType")) {
					Object[] lowerBounds = (Object[]) call(t, "getLowerBounds");
					Object[] upperBounds = (Object[]) call(t, "getUpperBounds");

					if (upperBounds.length == 1
							&& upperBounds[0] == Object.class)
						upperBounds = new Object[0];

					if (upperBounds.length != 0) {
						// extend
						for (Object upper : upperBounds) {
							sb.append('+');
							reference(sb, upper);
						}
					}
					else
						if (lowerBounds.length != 0) {
							// super, can only be one by the language
							for (Object lower : lowerBounds) {
								sb.append('-');
								reference(sb, lower);
							}
						}
						else
							sb.append('*');
				}
				else
					if ( isInstance(t.getClass(),"java.lang.reflect.TypeVariable")) {
						sb.append('T');
						sb.append( call(t,"getName"));
						sb.append(';');
					}
					else
						if (t instanceof Class< ? >) {
							Class< ? > c = (Class< ? >) t;
							if (c.isPrimitive()) {
								sb.append(primitive(c));
							}
							else {
								sb.append('L');
								String name = c.getName().replace('.', '/');
								sb.append(name);
								sb.append(';');
							}
						}
	}

	/**
	 * Creates the signature for a Parameterized Type.
	 * 
	 * A Parameterized Type has a raw class and a set of type variables.
	 * 
	 * @param sb
	 * @param pt
	 * @throws Exception 
	 */
	private void parameterizedType(StringBuilder sb, Object pt) throws Exception {
		Object owner = call(pt,"getOwnerType");
		String name = ((Class< ? >) call(pt,"getRawType")).getName()
				.replace('.', '/');
		if (owner != null) {
			if ( isInstance(owner.getClass(), "java.lang.reflect.ParameterizedType"))
				parameterizedType(sb, owner);
			else
				sb.append(((Class< ? >) owner).getName().replace('.', '/'));
			sb.append('.');
			int n = name.lastIndexOf('$');
			name = name.substring(n + 1);
		}
		sb.append(name);

		sb.append('<');
		for (Object parameterType : (Object[]) call(pt,"getActualTypeArguments")) {
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
	private char primitive(Class< ? > type) {
		if (type == byte.class)
			return 'B';
		else
			if (type == char.class)
				return 'C';
			else
				if (type == double.class)
					return 'D';
				else
					if (type == float.class)
						return 'F';
					else
						if (type == int.class)
							return 'I';
						else
							if (type == long.class)
								return 'J';
							else
								if (type == short.class)
									return 'S';
								else
									if (type == boolean.class)
										return 'Z';
									else
										if (type == void.class)
											return 'V';
										else
											throw new IllegalArgumentException(
													"Unknown primitive type "
															+ type);
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
		Map<String, String> map = new HashMap<String, String>();
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
		}
		else {
			// field or class
			reference(sb, map, rover, true); // field type or super class
			while (!rover.isEOF()) {
				reference(sb, map, rover, true); // interfaces
			}
		}
		return sb.toString();
	}

	/**
	 * The heart of the routine. Handle a reference to a type. Can be
	 * an array, a class, a type variable, or a primitive.
	 * 
	 * @param sb
	 * @param map
	 * @param rover
	 * @param primitivesAllowed
	 */
	private void reference(StringBuilder sb, Map<String, String> map,
			Rover rover, boolean primitivesAllowed) {

		char type = rover.take();
		sb.append(type);

		if (type == '[') {
			reference(sb, map, rover, true);
		}
		else
			if (type == 'L') {
				String fqnb = rover.upTo("<;.");
				sb.append(fqnb);
				body(sb, map, rover);
				while (rover.peek() == '.') {
					sb.append(rover.take('.'));
					sb.append(rover.upTo("<;."));
					body(sb, map, rover);
				}
				sb.append(rover.take(';'));
			}
			else
				if (type == 'T') {
					String name = rover.upTo(";");
					name = assign(map, name);
					sb.append(name);
					sb.append(rover.take(';'));
				}
				else {
					if (!primitivesAllowed)
						throw new IllegalStateException(
								"Primitives are not allowed without an array");
				}
	}

	/**
	 * Because classes can be nested the body handles the part that can 
	 * be nested, the reference handles the enclosing L ... ;
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
	 * Handles the assignment of type variables to index names so that
	 * we have a normalized name for each type var.
	 * 
	 * @param map the map with variables.
	 * @param name The name of the variable
	 * @return the index name, like _1
	 */
	private String assign(Map<String, String> map, String name) {
		if (map.containsKey(name))
			return map.get(name);
		else {
			int n = map.size();
			map.put(name, "_" + n);
			return "_" + n;
		}
	}

	private boolean isInstance(Class<?> type, String string) {
		if ( type == null)
			return false;
		
		if ( type.getName().equals(string))
			return true;
		
		if ( isInstance( type.getSuperclass(), string))
			return true;
		
		for ( Class<?> intf : type.getInterfaces()) {
			if ( isInstance(intf,string))
				return true;
		}
		return false;
	}
	
	private Object call(Object gd, String string) throws Exception {
		Method m = gd.getClass().getMethod(string);
		return m.invoke(gd);
	}

}
