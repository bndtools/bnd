package aQute.bnd.build.model.conversions;

import aQute.bnd.build.model.clauses.*;

public class HeaderClauseFormatter implements Converter<String,HeaderClause> {
	public String convert(HeaderClause input) throws IllegalArgumentException {
		StringBuilder buffer = new StringBuilder();
		input.formatTo(buffer);
		return buffer.toString();
	}
}