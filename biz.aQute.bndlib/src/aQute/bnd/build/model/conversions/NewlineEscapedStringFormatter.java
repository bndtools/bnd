package aQute.bnd.build.model.conversions;

/**
 * Turns newlines to textual escaped newlines and orphaned backslashes to double
 * backslashes.
 *
 * @author aqute
 */
public class NewlineEscapedStringFormatter implements Converter<String, String> {

	private static final String CONTINUE_STRING = "\\\n\t";

	@Override
	public String convert(String input) throws IllegalArgumentException {
		if (input == null)
			return null;
		StringBuilder result = new StringBuilder();
		int pos = 0;

		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			switch (c) {
				case '\r' :
					break;

				case '\n' :
					result.append("\\n")
						.append(CONTINUE_STRING);
					pos = 0;
					break;

				case '\\' :
					char next = 0;
					if (i < input.length() - 1)
						next = input.charAt(++i);

					switch (next) {
						case 'n' :
						case 'r' :
						case 't' :
						case 'u' :
						case '\\' :
						case '\n' :
							result.append('\\');
							result.append(next);
							break;

						default :
							result.append('\\');
							result.append('\\');

							if (next > 0)
								result.append(next);
							break;
					}
					pos++;
					break;

				case '\t' :
				case ' ' :
					result.append(' ');
					if (pos > 70) {
						result.append(CONTINUE_STRING);
						pos = 0;
					} else
						pos++;
					break;

				default :
					pos++;
					result.append(c);
					break;
			}
		}

		return result.toString();
	}

	@Override
	public String error(String msg) {
		return msg;
	}

}
