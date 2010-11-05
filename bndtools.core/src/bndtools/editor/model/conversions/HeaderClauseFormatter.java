package bndtools.editor.model.conversions;

import bndtools.model.clauses.HeaderClause;

public class HeaderClauseFormatter implements Converter<String, HeaderClause> {
    public String convert(HeaderClause input) throws IllegalArgumentException {
        StringBuilder buffer = new StringBuilder();
        input.formatTo(buffer);
        return buffer.toString();
    }
}