package aQute.bnd.build.model.conversions;

import aQute.bnd.build.model.clauses.*;

public class HeaderClauseListConverter extends ClauseListConverter<HeaderClause> {

	public HeaderClauseListConverter() {
		super(new HeaderClauseConverter());
	}

}
