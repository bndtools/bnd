package bndtools.editor.model.conversions;

import java.util.Map;

import bndtools.model.clauses.VersionedClause;
import bndtools.types.Pair;

public class VersionedClauseConverter implements Converter<VersionedClause, Pair<String, Map<String, String>>> {
    public VersionedClause convert(Pair<String, Map<String, String>> input) throws IllegalArgumentException {
    	return new VersionedClause(input.getFirst(), input.getSecond());
    }
}