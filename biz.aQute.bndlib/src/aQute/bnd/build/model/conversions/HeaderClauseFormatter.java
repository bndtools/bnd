package aQute.bnd.build.model.conversions;

import aQute.bnd.build.model.clauses.HeaderClause;

public class HeaderClauseFormatter implements Converter<String, HeaderClause> {

	private final boolean newlinesBetweenAttrs;

	public HeaderClauseFormatter(boolean newlinesBetweenAttrs) {
		this.newlinesBetweenAttrs = newlinesBetweenAttrs;
	}

	public HeaderClauseFormatter() {
		this(false);
	}

	@Override
	public String convert(HeaderClause input) throws IllegalArgumentException {
		StringBuilder buffer = new StringBuilder();
		input.formatTo(buffer, newlinesBetweenAttrs);
		return buffer.toString();
	}

	@Override
	public String error(String msg) {
		return msg;
	}
}
