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
		simple(key, Operator.EQ, value);
		return this;
	}

	public FilterBuilder neq(String key, Object value) {
		not();
		simple(key, Operator.EQ, value);
		end();
		return this;
	}

	public FilterBuilder gt(String key, Object value) {
		not();
		simple(key, Operator.LE, value);
		end();
		return this;
	}

	public FilterBuilder lt(String key, Object value) {
		not();
		simple(key, Operator.GE, value);
		end();
		return this;
	}

	public FilterBuilder ge(String key, Object value) {
		simple(key, Operator.GE, value);
		return this;
	}

	public FilterBuilder le(String key, Object value) {
		simple(key, Operator.LE, value);
		return this;
	}

	public FilterBuilder isSet(String key) {
		simple(key, Operator.EQ, "*");
		return this;
	}

	public FilterBuilder approximate(String key, Object value) {
		simple(key, Operator.APPROX, value);
		return this;
	}

	public FilterBuilder simple(String key, Operator op, Object value) {
		current.members.add("(" + key + op.name + escape(value) + ")");
		return this;
	}

	public FilterBuilder literal(String string) {
		current.members.add(string);
		return this;
	}

	/**
	 * If value must contain one of the characters reverse solidus ('\' \u005C),
	 * asterisk ('*' \u002A), paren- theses open ('(' \u0028) or parentheses
	 * close (')' \u0029), then these characters should be preceded with the
	 * reverse solidus ('\' \u005C) character. Spaces are significant in value.
	 * Space characters are defined by Character.isWhiteSpace().
	 *
	 * @param value
	 * @return
	 */
	static String escape(Object value) {
		String s = value.toString();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
				case '(' :
				case '\\' :
				case ')' :
					sb.append("\\");

					// FALL BACK

				default :
					sb.append(c);
					break;
			}
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return current.toString();
	}

	public FilterBuilder isPresent(String key) {
		return simple(key, Operator.EQ, "*");
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
