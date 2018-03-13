package aQute.bnd.build.model.conversions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import aQute.bnd.build.model.clauses.HeaderClause;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Processor;

public class HeaderClauseListConverter<R> implements Converter<List<R>, String> {

	private final Converter<? extends R, ? super HeaderClause> itemConverter;

	public HeaderClauseListConverter(Converter<? extends R, ? super HeaderClause> itemConverter) {
		this.itemConverter = itemConverter;
	}

	public List<R> convert(String input) throws IllegalArgumentException {
		if (input == null)
			return null;

		List<R> result = new ArrayList<>();

		Parameters header = new Parameters(input);
		for (Entry<String, Attrs> entry : header.entrySet()) {
			String key = Processor.removeDuplicateMarker(entry.getKey());
			HeaderClause clause = new HeaderClause(key, entry.getValue());
			result.add(itemConverter.convert(clause));
		}

		return result;
	}

	@Override
	public List<R> error(String msg) {
		List<R> l = new ArrayList<>();
		l.add(itemConverter.error(msg));
		return l;
	}

}
