package aQute.bnd.build.model.conversions;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;

import aQute.bnd.build.model.clauses.HeaderClause;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Processor;

public class HeaderClauseListConverter<R> implements Converter<List<R>, String> {

	private final Converter<? extends R, ? super HeaderClause> itemConverter;

	public HeaderClauseListConverter(Converter<? extends R, ? super HeaderClause> itemConverter) {
		this.itemConverter = itemConverter;
	}

	@Override
	public List<R> convert(String input) throws IllegalArgumentException {
		if (input == null)
			return null;

		Parameters header = new Parameters(input);
		List<R> result = header.stream()
			.mapKey(Processor::removeDuplicateMarker)
			.mapToObj(HeaderClause::new)
			.map(itemConverter::convert)
			.collect(toList());
		return result;
	}

	@Override
	public List<R> error(String msg) {
		List<R> l = new ArrayList<>();
		l.add(itemConverter.error(msg));
		return l;
	}

}
