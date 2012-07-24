package aQute.bnd.build.model.conversions;

import java.util.*;
import java.util.Map.Entry;

import aQute.bnd.header.*;
import aQute.bnd.osgi.*;
import aQute.libg.tuple.*;

public class ClauseListConverter<R> implements Converter<List<R>,String> {

	private final Converter< ? extends R, ? super Pair<String,Attrs>>	itemConverter;

	public ClauseListConverter(Converter< ? extends R, ? super Pair<String,Attrs>> itemConverter) {
		this.itemConverter = itemConverter;
	}

	public List<R> convert(String input) throws IllegalArgumentException {
		List<R> result = new ArrayList<R>();

		Parameters header = new Parameters(input);
		for (Entry<String,Attrs> entry : header.entrySet()) {
			String key = Processor.removeDuplicateMarker(entry.getKey());
			Pair<String,Attrs> pair = Pair.newInstance(key, entry.getValue());
			result.add(itemConverter.convert(pair));
		}

		return result;
	}

}
