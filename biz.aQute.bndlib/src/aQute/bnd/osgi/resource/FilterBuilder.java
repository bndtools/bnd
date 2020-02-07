package aQute.bnd.osgi.resource;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.VersionRange;

public class FilterBuilder {

	public enum Operator {
		EQ("="),
		APPROX("~="),
		GE(">="),
		LE("<=");

		String name;

		Operator(String name) {
			this.name = name;
		}
	}

	static class Sub {
		Sub				previous;
		String			op;
		List<Object>	members	= new ArrayList<>();

		public Sub(String op, Sub current) {
			this.op = op;
			this.previous = current;
		}

		@Override
		public String toString() {
			if (members.isEmpty())
				return "";

			if (!op.equals("!") && members.size() == 1)
				return members.get(0)
					.toString();

			StringBuilder sb = new StringBuilder();
			sb.append("(")
				.append(op);
			for (Object top : members) {
				sb.append(top);
			}
			sb.append(")");

			return sb.toString();
		}
	}

	Sub current = new Sub("&", null);

	public FilterBuilder or() {
		current = new Sub("|", current);
		return this;
	}

	public FilterBuilder and() {
		current = new Sub("&", current);
		return this;
	}

	public FilterBuilder not() {
		current = new Sub("!", current);
		return this;
	}

	public FilterBuilder end() {
		current.previous.members.add(current);
		current = current.previous;
		return this;
	}

	public FilterBuilder eq(String key, Object value) {
		return simple(key, Operator.EQ, value);
	}

	public FilterBuilder neq(String key, Object value) {
		return not().simple(key, Operator.EQ, value)
			.end();
	}

	public FilterBuilder gt(String key, Object value) {
		return not().simple(key, Operator.LE, value)
			.end();
	}

	public FilterBuilder lt(String key, Object value) {
		return not().simple(key, Operator.GE, value)
			.end();
	}

	public FilterBuilder ge(String key, Object value) {
		return simple(key, Operator.GE, value);
	}

	public FilterBuilder le(String key, Object value) {
		return simple(key, Operator.LE, value);
	}

	public FilterBuilder isSet(String key) {
		return isPresent(key);
	}

	public FilterBuilder approximate(String key, Object value) {
		return simple(key, Operator.APPROX, value);
	}

	public FilterBuilder simple(String key, Operator op, Object value) {
		return literal("(" + key + op.name + escape(value) + ")");
	}

	public FilterBuilder literal(String string) {
		current.members.add(string);
		return this;
	}

	/**
	 * If value must contain one of the characters reverse solidus ('\' \u005C),
	 * asterisk ('*' \u002A), parentheses open ('(' \u0028) or parentheses close
	 * (')' \u0029), then these characters should be preceded with the reverse
	 * solidus ('\' \u005C) character. Spaces are significant in value. Space
	 * characters are defined by Character.isWhiteSpace().
	 */
	private static String escape(Object value) {
		String s = value.toString();
		final int len = s.length();
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			char c = s.charAt(i);
			// we don't escape * to allow users to use it
			switch (c) {
				case '(' :
				case '\\' :
				case ')' :
					sb.append('\\')
						.append(c);
					break;
				default :
					sb.append(c);
					break;
			}
		}
		return (len == sb.length()) ? s : sb.toString();
	}

	@Override
	public String toString() {
		return current.toString();
	}

	public FilterBuilder isPresent(String key) {
		return literal("(" + key + "=*)");
	}

	public FilterBuilder in(String key, VersionRange range) {
		and();

		if (range.getLeftType() == '[')
			ge(key, range.getLeft());
		else
			gt(key, range.getLeft());

		if (range.getRightType() == ']')
			le(key, range.getRight());
		else
			lt(key, range.getRight());

		end();
		return this;
	}

	public FilterBuilder in(String key, aQute.bnd.version.VersionRange range) {
		and();

		if (range.includeLow())
			ge(key, range.getLow());
		else
			gt(key, range.getLow());

		if (range.includeHigh())
			le(key, range.getHigh());
		else
			lt(key, range.getHigh());

		end();
		return this;
	}

	public void endAnd() {
		if (!current.op.equals("&"))
			throw new IllegalStateException("Expected an & but had " + current.op);
		end();
	}

	public void endOr() {
		if (!current.op.equals("|"))
			throw new IllegalStateException("Expected an | but had " + current.op);
		end();
	}
}
