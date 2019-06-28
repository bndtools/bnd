package aQute.bnd.build.model.conversions;

import aQute.bnd.build.model.clauses.HeaderClause;

public class HeaderClauseFormatter implements Converter<String, HeaderClause> {
	@Override
	public String convert(HeaderClause input) throws IllegalArgumentException {
		StringBuilder buffer = new StringBuilder();
		input.formatTo(buffer);
		return buffer.toString();
	}

	@Override
	public String error(String msg) {
		return msg;
	}
}
