/**
 * Copyright (c) 2000 Gatespace AB. All Rights Reserved.
 *
 * Gatespace grants Open Services Gateway Initiative (OSGi) an irrevocable,
 * perpetual, non-exclusive, worldwide, paid-up right and license to
 * reproduce, display, perform, prepare and have prepared derivative works
 * based upon and distribute and sublicense this material and derivative
 * works thereof as set out in the OSGi MEMBER AGREEMENT as of January 24
 * 2000, for use in accordance with Section 2.2 of the BY-LAWS of the
 * OSGi MEMBER AGREEMENT.
 */

package aQute.lib.filter;

import java.lang.reflect.*;
import java.math.*;
import java.util.*;

public class Filter {
	final char			WILDCARD	= 65535;

	final static int	EQ			= 0;
	final static int	LE			= 1;
	final static int	GE			= 2;
	final static int	APPROX		= 3;

	String		filter;

	abstract class Query {
		static final String	GARBAGE		= "Trailing garbage";
		static final String	MALFORMED	= "Malformed query";
		static final String	EMPTY		= "Empty list";
		static final String	SUBEXPR		= "No subexpression";
		static final String	OPERATOR	= "Undefined operator";
		static final String	TRUNCATED	= "Truncated expression";
		static final String	EQUALITY	= "Only equality supported";

		private String		tail;

		boolean match() throws IllegalArgumentException {
			tail = filter;
			boolean val = doQuery();
			if (tail.length() > 0)
				error(GARBAGE);
			return val;
		}

		private boolean doQuery() throws IllegalArgumentException {
			if (tail.length() < 3 || !prefix("("))
				error(MALFORMED);
			boolean val;

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

		private boolean doAnd() throws IllegalArgumentException {
			tail = tail.substring(1);
			boolean val = true;
			if (!tail.startsWith("("))
				error(EMPTY);
			do {
				if (!doQuery())
					val = false;
			} while (tail.startsWith("("));
			return val;
		}

		private boolean doOr() throws IllegalArgumentException {
			tail = tail.substring(1);
			boolean val = false;
			if (!tail.startsWith("("))
				error(EMPTY);
			do {
				if (doQuery())
					val = true;
			} while (tail.startsWith("("));
			return val;
		}

		private boolean doNot() throws IllegalArgumentException {
			tail = tail.substring(1);
			if (!tail.startsWith("("))
				error(SUBEXPR);
			return !doQuery();
		}

		private boolean doSimple() throws IllegalArgumentException {
			int op = 0;
			Object attr = getAttr();

			if (prefix("="))
				op = EQ;
			else if (prefix("<="))
				op = LE;
			else if (prefix(">="))
				op = GE;
			else if (prefix("~="))
				op = APPROX;
			else
				error(OPERATOR);

			return compare(attr, op, getValue());
		}

		private boolean prefix(String pre) {
			if (!tail.startsWith(pre))
				return false;
			tail = tail.substring(pre.length());
			return true;
		}

		private Object getAttr() {
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
				}
			}
			String attr = tail.substring(0, ix).toLowerCase();
			tail = tail.substring(ix);
			return getProp(attr);
		}

		abstract Object getProp(String key);

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

		private void error(String m) throws IllegalArgumentException {
			throw new IllegalArgumentException(m + " " + tail);
		}

		private boolean compare(Object obj, int op, String s) {
			if (obj == null)
				return false;
			try {
				Class< ? > numClass = obj.getClass();
				if (numClass == String.class) {
					return compareString((String) obj, op, s);
				} else if (numClass == Character.class) {
					return compareString(obj.toString(), op, s);
				} else if (numClass == Long.class) {
					return compareSign(op, Long.valueOf(s).compareTo((Long) obj));
				} else if (numClass == Integer.class) {
					return compareSign(op, Integer.valueOf(s).compareTo((Integer) obj));
				} else if (numClass == Short.class) {
					return compareSign(op, Short.valueOf(s).compareTo((Short) obj));
				} else if (numClass == Byte.class) {
					return compareSign(op, Byte.valueOf(s).compareTo((Byte) obj));
				} else if (numClass == Double.class) {
					return compareSign(op, Double.valueOf(s).compareTo((Double) obj));
				} else if (numClass == Float.class) {
					return compareSign(op, Float.valueOf(s).compareTo((Float) obj));
				} else if (numClass == Boolean.class) {
					if (op != EQ)
						return false;
					int a = Boolean.valueOf(s).booleanValue() ? 1 : 0;
					int b = ((Boolean) obj).booleanValue() ? 1 : 0;
					return compareSign(op, a - b);
				} else if (numClass == BigInteger.class) {
					return compareSign(op, new BigInteger(s).compareTo((BigInteger) obj));
				} else if (numClass == BigDecimal.class) {
					return compareSign(op, new BigDecimal(s).compareTo((BigDecimal) obj));
				} else if (obj instanceof Collection< ? >) {
					for (Object x : (Collection< ? >) obj)
						if (compare(x, op, s))
							return true;
				} else if (numClass.isArray()) {
					int len = Array.getLength(obj);
					for (int i = 0; i < len; i++)
						if (compare(Array.get(obj, i), op, s))
							return true;
				}
			}
			catch (Exception e) {}
			return false;
		}
	}

	class DictQuery extends Query {
		private Dictionary< ? , ? >	dict;

		DictQuery(Dictionary< ? , ? > dict) {
			this.dict = dict;
		}

		Object getProp(String key) {
			return dict.get(key);
		}
	}

	public Filter(String filter) throws IllegalArgumentException {
		// NYI: Normalize the filter string?
		this.filter = filter;
		if (filter == null || filter.length() == 0)
			throw new IllegalArgumentException("Null query");
	}

	public boolean match(Dictionary< ? , ? > dict) {
		try {
			return new DictQuery(dict).match();
		}
		catch (IllegalArgumentException e) {
			return false;
		}
	}

	public String verify() {
		try {
			new DictQuery(new Hashtable<Object,Object>()).match();
		}
		catch (IllegalArgumentException e) {
			return e.getMessage();
		}
		return null;
	}

	public String toString() {
		return filter;
	}

	public boolean equals(Object obj) {
		return obj != null && obj instanceof Filter && filter.equals(((Filter) obj).filter);
	}

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
