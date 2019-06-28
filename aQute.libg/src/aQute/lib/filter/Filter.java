/*
 * This used to have a license header that it was licensed by Gatespace in the year 2000. However, this was licensed
 * to the OSGi Alliance. A member donated this as ASL 2.0 licensed matching this project's default license.
 */
package aQute.lib.filter;

import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;

import aQute.lib.exceptions.Exceptions;

public class Filter {

	static final MethodHandles.Lookup	publicLookup		= MethodHandles.publicLookup();
	static final MethodType				stringConstructor	= methodType(void.class, String.class);

	static final String					GARBAGE				= "Trailing garbage";
	static final String					MALFORMED			= "Malformed query";
	static final String					EMPTY				= "Empty list";
	static final String					SUBEXPR				= "No subexpression";
	static final String					OPERATOR			= "Undefined operator";
	static final String					TRUNCATED			= "Truncated expression";
	static final String					EQUALITY			= "Only equality supported";

	final static char					WILDCARD			= 65535;

	final static int					EQ					= 0;
	final static int					LE					= 1;
	final static int					GE					= 2;

	// Extended operators
	final static int					NEQ					= 100;
	final static int					LT					= 101;
	final static int					GT					= 102;

	final static int					APPROX				= 3;

	final String						filter;
	final boolean						extended;
	final Node							node;
	final Exception						parseException;
	private String						tail;

	interface Arguments {
		Object getProp(String key) throws Exception;
	}

	class Query {
		private Node doQuery() throws Exception {
			if (tail.length() < 3 || !prefix("("))
				error(MALFORMED);
			Node val;

			switch (tail.charAt(0)) {
				case '&' :
					val = doAnd();
					break;
				case '|' :
					val = doOr();
					break;
				case '!' :
					val = doNot();
					break;
				default :
					val = doSimple();
					break;
			}

			if (!prefix(")"))
				error(MALFORMED);
			return val;
		}

		private Node doAnd() throws Exception {
			tail = skip(1);
			boolean val = true;
			if (!tail.startsWith("("))
				error(EMPTY);
			And and = new And();
			do {
				and.children.add(doQuery());
			} while (tail.startsWith("("));
			return and;
		}

		String skip(int skip) {
			String a = tail;
			do {
				a = a.substring(skip);
				skip = 1;
			} while (a.length() > 0 && Character.isWhitespace(a.charAt(0)));
			return a;
		}

		private Node doOr() throws Exception {
			tail = skip(1);
			boolean val = false;
			if (!tail.startsWith("("))
				error(EMPTY);
			Or or = new Or();
			do {
				or.children.add(doQuery());
			} while (tail.startsWith("("));
			return or;
		}

		private Node doNot() throws Exception {
			tail = skip(1);
			if (!tail.startsWith("("))
				error(SUBEXPR);
			return new Not(doQuery());
		}

		Node doSimple() throws Exception {
			int op = 0;
			String key = getKey();

			if (prefix("="))
				op = EQ;
			else if (prefix("<="))
				op = LE;
			else if (prefix(">="))
				op = GE;
			else if (prefix("~="))
				op = APPROX;
			else if (extended && prefix("!="))
				op = NEQ;
			else if (extended && prefix(">"))
				op = GT;
			else if (extended && prefix("<"))
				op = LT;
			else
				error(OPERATOR);

			return new Simple(key, op, getValue());
		}

		boolean prefix(String pre) {
			if (!tail.startsWith(pre))
				return false;
			tail = skip(pre.length());
			return true;
		}

		String getKey() throws Exception {
			int len = tail.length();
			int ix = 0;
			label: for (; ix < len; ix++) {
				switch (tail.charAt(ix)) {
					case '(' :
					case ')' :
					case '<' :
					case '>' :
					case '=' :
					case '~' :
					case '*' :
					case '\\' :
						break label;
					case '!' :
						if (extended) {
							break label;
						}
				}
			}
			String attr = tail.substring(0, ix);
			tail = tail.substring(ix);
			return attr;
		}

		private String getValue() {
			StringBuilder sb = new StringBuilder();
			int len = tail.length();
			int ix = 0;
			label: for (; ix < len; ix++) {
				char c = tail.charAt(ix);
				switch (c) {
					case '(' :
					case ')' :
						break label;
					case '*' :
						sb.append(WILDCARD);
						break;
					case '\\' :
						if (ix == len - 1)
							break label;
						sb.append(tail.charAt(++ix));
						break;
					default :
						sb.append(c);
						break;
				}
			}
			tail = tail.substring(ix);
			return sb.toString();
		}
	}

	void error(String m) throws IllegalArgumentException {
		throw new IllegalArgumentException(m + " " + tail);
	}

	boolean compare(Object obj, int op, String s) {
		if (obj == null) {
			return false;
		}
		if ((op == EQ) && (s.length() == 1) && (s.charAt(0) == WILDCARD)) {
			return true;
		}
		try {
			Class<?> numClass = obj.getClass();
			if (numClass == String.class) {
				return compareString((String) obj, op, s);
			} else if (numClass == Character.class) {
				return compareString(obj.toString(), op, s);
			} else if (numClass == Long.class) {
				return compareSign(op, Long.valueOf(s)
					.compareTo((Long) obj));
			} else if (numClass == Integer.class) {
				return compareSign(op, Integer.valueOf(s)
					.compareTo((Integer) obj));
			} else if (numClass == Short.class) {
				return compareSign(op, Short.valueOf(s)
					.compareTo((Short) obj));
			} else if (numClass == Byte.class) {
				return compareSign(op, Byte.valueOf(s)
					.compareTo((Byte) obj));
			} else if (numClass == Double.class) {
				return compareSign(op, Double.valueOf(s)
					.compareTo((Double) obj));
			} else if (numClass == Float.class) {
				return compareSign(op, Float.valueOf(s)
					.compareTo((Float) obj));
			} else if (numClass == Boolean.class) {
				switch (op) {
					case LE :
					case GE :
					case EQ :
					case APPROX :
						return ((Boolean) obj).booleanValue() == Boolean.parseBoolean(s);
					case NEQ :
						return ((Boolean) obj).booleanValue() != Boolean.parseBoolean(s);
					case LT :
					case GT :
					default :
						return false;
				}
			} else if (numClass == BigInteger.class) {
				return compareSign(op, new BigInteger(s).compareTo((BigInteger) obj));
			} else if (numClass == BigDecimal.class) {
				return compareSign(op, new BigDecimal(s).compareTo((BigDecimal) obj));
			} else if (obj instanceof Collection<?>) {
				for (Object x : (Collection<?>) obj) {
					if (compare(x, op, s)) {
						return true;
					}
				}
				return false;
			} else if (numClass.isArray()) {
				int len = Array.getLength(obj);
				for (int i = 0; i < len; i++) {
					if (compare(Array.get(obj, i), op, s)) {
						return true;
					}
				}
				return false;
			} else if (obj instanceof Comparable<?>) {
				Object source = valueOf(numClass, s);
				@SuppressWarnings("unchecked")
				Comparable<Object> a = (Comparable<Object>) source;
				return compareSign(op, a.compareTo(obj));
			} else {
				Object source = valueOf(numClass, s);
				switch (op) {
					case LE :
					case GE :
					case EQ :
					case APPROX :
						return source.equals(obj);
					case NEQ :
						return !source.equals(obj);
					case LT :
					case GT :
					default :
						return false;
				}
			}
		} catch (Exception e) {
			return false;
		}
	}

	private static Object valueOf(Class<?> numClass, String s) throws Exception {
		MethodHandle mh;
		try {
			mh = publicLookup.findStatic(numClass, "valueOf", methodType(numClass, String.class));
		} catch (NoSuchMethodException | IllegalAccessException e) {
			mh = publicLookup.findConstructor(numClass, stringConstructor);
		}
		try {
			return mh.invoke(s);
		} catch (Throwable e) {
			throw Exceptions.duck(e);
		}
	}

	class DictQuery implements Arguments {
		private Dictionary<?, ?> dict;

		DictQuery(Dictionary<?, ?> dict) {
			this.dict = dict;
		}

		@Override
		public Object getProp(String key) {
			return dict.get(key);
		}
	}

	class MapQuery implements Arguments {
		private Map<?, ?> map;

		MapQuery(Map<?, ?> dict) {
			this.map = dict;
		}

		@Override
		public Object getProp(String key) {
			return map.get(key);
		}
	}

	class GetQuery implements Arguments {
		private Get get;

		GetQuery(Get get) {
			this.get = get;
		}

		@Override
		public Object getProp(String key) throws Exception {
			return get.get(key);
		}
	}

	abstract class Node {
		public abstract boolean match(Arguments arguments) throws Exception;
	}

	class Simple extends Node {
		final String	key;
		final int		op;
		final String	value;

		public Simple(String key, int op, String value) {
			this.key = key;
			this.op = op;
			this.value = value;
		}

		@Override
		public boolean match(Arguments arguments) throws Exception {
			Object attr = arguments.getProp(key);
			return compare(attr, op, value);
		}
	}

	class Not extends Node {
		final Node target;

		public Not(Node target) {
			this.target = target;
		}

		@Override
		public boolean match(Arguments arguments) throws Exception {
			return !target.match(arguments);
		}
	}

	class Or extends Node {
		final List<Node> children = new ArrayList<>();

		@Override
		public boolean match(Arguments arguments) throws Exception {
			for (Node node : children) {
				if (node.match(arguments))
					return true;
			}
			return false;
		}
	}

	class And extends Node {
		final List<Node> children = new ArrayList<>();

		@Override
		public boolean match(Arguments arguments) throws Exception {
			for (Node node : children) {
				if (!node.match(arguments))
					return false;
			}
			return true;
		}
	}

	public Filter(String filter, boolean extended) throws IllegalArgumentException {
		this.filter = filter;
		this.extended = extended;
		this.tail = filter;
		if (filter == null || filter.length() == 0)
			throw new IllegalArgumentException("Null query");

		Node node = null;
		Exception parseException = null;
		try {
			node = new Query().doQuery();
			if (tail.length() > 0)
				error(GARBAGE);
		} catch (Exception e) {
			parseException = e;
		}
		this.node = node;
		this.parseException = parseException;
	}

	public Filter(String filter) throws IllegalArgumentException {
		this(filter, false);
	}

	public boolean match(Dictionary<?, ?> dict) throws Exception {
		try {
			if (parseException != null) {
				throw parseException;
			}
			return node.match(new DictQuery(dict));
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	public boolean matchMap(Map<?, ?> dict) throws Exception {
		try {
			if (parseException != null) {
				throw parseException;
			}
			return node.match(new MapQuery(dict));
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	public boolean match(Get get) throws Exception {
		try {
			if (parseException != null) {
				throw parseException;
			}
			return node.match(new GetQuery(get));
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	public String verify() throws Exception {
		if (parseException != null) {
			return parseException.getMessage();
		}
		return null;
	}

	@Override
	public String toString() {
		return filter;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Filter && filter.equals(((Filter) obj).filter);
	}

	@Override
	public int hashCode() {
		return filter.hashCode();
	}

	boolean compareString(String s1, int op, String s2) {
		switch (op) {
			case EQ :
				return patSubstr(s1, s2);
			case APPROX :
				return fixupString(s2).equals(fixupString(s1));
			default :
				return compareSign(op, s2.compareTo(s1));
		}
	}

	boolean compareSign(int op, int cmp) {
		switch (op) {
			case LE :
				return cmp >= 0;
			case GE :
				return cmp <= 0;
			case EQ :
				return cmp == 0;
			case NEQ :
				return cmp != 0;
			case LT :
				return cmp > 0;
			case GT :
				return cmp < 0;
			default : /* APPROX */
				return cmp == 0;
		}
	}

	String fixupString(String s) {
		StringBuilder sb = new StringBuilder();
		int len = s.length();
		boolean isStart = true;
		boolean isWhite = false;
		for (int i = 0; i < len; i++) {
			char c = s.charAt(i);
			if (Character.isWhitespace(c)) {
				isWhite = true;
			} else {
				if (!isStart && isWhite)
					sb.append(' ');
				if (Character.isUpperCase(c))
					c = Character.toLowerCase(c);
				sb.append(c);
				isStart = false;
				isWhite = false;
			}
		}
		return sb.toString();
	}

	boolean patSubstr(String s, String pat) {
		if (s == null)
			return false;
		if (pat.length() == 0)
			return s.length() == 0;
		if (pat.charAt(0) == WILDCARD) {
			pat = pat.substring(1);
			for (;;) {
				if (patSubstr(s, pat))
					return true;
				if (s.length() == 0)
					return false;
				s = s.substring(1);
			}
		}
		if (s.length() == 0 || s.charAt(0) != pat.charAt(0))
			return false;
		return patSubstr(s.substring(1), pat.substring(1));
	}
}
