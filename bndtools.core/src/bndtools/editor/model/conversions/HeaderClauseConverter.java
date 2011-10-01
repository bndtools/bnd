package bndtools.editor.model.conversions;

import java.util.Map;

import bndtools.model.clauses.HeaderClause;
import bndtools.types.Pair;

public class HeaderClauseConverter implements Converter<HeaderClause, Pair<String, Map<String, String>>> {

    public HeaderClause convert(Pair<String, Map<String, String>> input) throws IllegalArgumentException {
        return new HeaderClause(input.getFirst(), input.getSecond());
    }

}
