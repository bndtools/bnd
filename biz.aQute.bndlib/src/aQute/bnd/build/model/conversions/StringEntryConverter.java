package aQute.bnd.build.model.conversions;

import java.util.Map.Entry;

public class StringEntryConverter implements Converter<String, Entry<String, ?>> {

	@Override
	public String convert(Entry<String, ?> input) throws IllegalArgumentException {
		if (input == null)
			return null;
		return input.getKey();
	}

	@Override
	public String error(String msg) {
		return msg;
	}

}
