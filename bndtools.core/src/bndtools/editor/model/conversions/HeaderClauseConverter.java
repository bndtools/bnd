package bndtools.editor.model.conversions;

import aQute.libg.header.Attrs;
import bndtools.model.clauses.HeaderClause;
import bndtools.types.Pair;

public class HeaderClauseConverter implements Converter<HeaderClause, Pair<String, Attrs>> {

    public HeaderClause convert(Pair<String, Attrs> input) throws IllegalArgumentException {
        return new HeaderClause(input.getFirst(), input.getSecond());
    }

}
