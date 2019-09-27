package aQute.bnd.build.model.conversions;

import java.util.Map.Entry;

public class PropertiesEntryFormatter implements Converter<String, Entry<String, String>> {
	@Override
	public String convert(Entry<String, String> entry) {
		StringBuilder buffer = new StringBuilder();

		String name = entry.getKey();
		buffer.append(name)
			.append('=');

		String value = entry.getValue();
		if (value != null && value.length() > 0) {
			int quotableIndex = value.indexOf(',');
			if (quotableIndex == -1)
				quotableIndex = value.indexOf('=');

			if (quotableIndex >= 0) {
				buffer.append('\'')
					.append(value)
					.append('\'');
			} else {
				buffer.append(value);
			}
		}
		return buffer.toString();
	}

	@Override
	public String error(String msg) {
		return msg;
	}
}
