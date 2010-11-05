package bndtools.editor.model.conversions;

import java.util.Map;
import java.util.Map.Entry;

import bndtools.model.clauses.VersionedClause;

public class VersionedClauseConverter implements Converter<VersionedClause, Entry<String, Map<String, String>>> {
    public VersionedClause convert(Entry<String, Map<String, String>> input) throws IllegalArgumentException {
    	return new VersionedClause(input.getKey(), input.getValue());
    }
}