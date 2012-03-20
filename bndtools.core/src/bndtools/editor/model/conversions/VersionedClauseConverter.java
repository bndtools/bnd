package bndtools.editor.model.conversions;

import aQute.libg.header.Attrs;
import bndtools.model.clauses.VersionedClause;
import bndtools.types.Pair;

public class VersionedClauseConverter implements Converter<VersionedClause, Pair<String, Attrs>> {
    public VersionedClause convert(Pair<String, Attrs> input) throws IllegalArgumentException {
    	return new VersionedClause(input.getFirst(), input.getSecond());
    }
}