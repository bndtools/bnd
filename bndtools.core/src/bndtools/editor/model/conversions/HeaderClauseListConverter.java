package bndtools.editor.model.conversions;

import bndtools.model.clauses.HeaderClause;

public class HeaderClauseListConverter extends ClauseListConverter<HeaderClause> {

    public HeaderClauseListConverter() {
        super(new HeaderClauseConverter());
    }

}
