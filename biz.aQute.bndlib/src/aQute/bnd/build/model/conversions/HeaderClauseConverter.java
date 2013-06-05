package aQute.bnd.build.model.conversions;

import aQute.bnd.build.model.clauses.*;
import aQute.bnd.header.*;
import aQute.libg.tuple.*;

public class HeaderClauseConverter implements Converter<HeaderClause,Pair<String,Attrs>> {

	public HeaderClause convert(Pair<String,Attrs> input) throws IllegalArgumentException {
		if (input == null) 
			return null;
		return new HeaderClause(input.getFirst(), input.getSecond());
	}

}
