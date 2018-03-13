package aQute.bnd.build.model.conversions;

import java.util.HashMap;
import java.util.Map;

import aQute.bnd.header.OSGiHeader;

public class PropertiesConverter implements Converter<Map<String, String>, String> {

	@Override
	public Map<String, String> convert(String input) throws IllegalArgumentException {
		if (input == null)
			return null;
		return OSGiHeader.parseProperties(input);
	}

	@Override
	public Map<String, String> error(String msg) {
		HashMap<String, String> result = new HashMap<>();
		result.put("ERROR", msg);
		return result;
	}

}
