package aQute.bnd.build.model.conversions;

import aQute.bnd.build.model.*;

public class NewlineEscapedStringFormatter implements Converter<String,String> {

	public String convert(String input) throws IllegalArgumentException {
		if (input == null)
			return null;

		// Shortcut the result for the majority of cases where there is no
		// newline
		if (input.indexOf('\n') == -1)
			return input;

		// Build a new string with newlines escaped
		StringBuilder result = new StringBuilder();
		int position = 0;
		while (position < input.length()) {
			int newlineIndex = input.indexOf('\n', position);
			if (newlineIndex == -1) {
				result.append(input.substring(position));
				break;
			}
			result.append(input.substring(position, newlineIndex));
			result.append(BndEditModel.NEWLINE_LINE_SEPARATOR);
			position = newlineIndex + 1;
		}

		return result.toString();
	}

}
