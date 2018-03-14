package aQute.bnd.build.model.conversions;

import java.util.Map;
import java.util.Map.Entry;

public class MapFormatter implements Converter<String, Map<String, String>> {

	private CollectionFormatter<Entry<String, String>> entrySetFormatter;

	public MapFormatter(String listSeparator, Converter<String, ? super Entry<String, String>> entryFormatter,
		String emptyOutput) {
		entrySetFormatter = new CollectionFormatter<>(listSeparator, entryFormatter, emptyOutput);
	}

	@Override
	public String convert(Map<String, String> input) throws IllegalArgumentException {
		return entrySetFormatter.convert(input.entrySet());
	}

	@Override
	public String error(String msg) {
		return msg;
	}
}
