package aQute.bnd.build.model.conversions;

import aQute.bnd.build.model.clauses.*;
import aQute.libg.header.*;
import aQute.libg.tuple.*;

public class HeaderClauseConverter implements Converter<HeaderClause,Pair<String,Attrs>> {

	public HeaderClause convert(Pair<String,Attrs> input) throws IllegalArgumentException {
		return new HeaderClause(input.getFirst(), input.getSecond());
	}

}
