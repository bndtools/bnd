package aQute.lib.formatter;

import java.time.ZonedDateTime;
import java.util.Formatter;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.lib.date.Dates;

public class Formatters {
	private final static Pattern PRINTF_P = Pattern.compile(
		"%((?<index>\\d+)\\$|(?<previous><))?(-|\\+|0|\\(|,|\\^|#| )*(\\d*)?(\\.(\\d+))?(?<conversion>a|A|b|B|c|C|d|e|E|f|g|G|h|H|n|o|s|S|x|X|(?:[tT][HIklMSLNpzZsQBbhAaCYyjmdeRTrDFc])|%)");

	/**
	 * Format a string where the arguments are all strings. The string is
	 * formatted with the ROOT Locale.
	 *
	 * @param format the format string
	 * @param isTruthy a function to turn a string into a boolean
	 * @param offset the offset in the arguments array to start at
	 * @param args the arguments
	 * @return a formatted string
	 */

	public static String format(String format, Function<String, Boolean> isTruthy, int offset, String... args) {
		Object[] formatArgs = new Object[args.length - offset + 10];

		Matcher m = PRINTF_P.matcher(format);
		int automatic = offset;
		int index;

		while (m.find()) {

			char conversion = m.group("conversion")
				.charAt(0);

			if (conversion == 'n' || conversion == '%')
				continue;

			String indexString = m.group("index");
			if (indexString != null) {
				index = Integer.parseInt(indexString) + offset - 1;
			} else {
				String previousString = m.group("previous");
				if (previousString != null) {
					continue;
				} else {
					index = automatic++;
				}
			}


			switch (conversion) {
				// d|f|c|s|h|n|x|X|u|o|z|Z|e|E|g|G|p|\n|%)");
				case 'd' :
				case 'o' :
				case 'x' :
				case 'X' :
					formatArgs[index - offset] = Long.parseLong(args[index]);
					break;

				case 'a' :
				case 'A' :
				case 'e' :
				case 'E' :
				case 'f' :
				case 'g' :
				case 'G' :
					formatArgs[index - offset] = Double.parseDouble(args[index]);
					break;

				case 'c' :
				case 'C' :
					if (args[index].length() == 1)
						formatArgs[index - offset] = args[index].charAt(0);
					else {
						try {
							int parseInt = Integer.parseInt(args[index]);
							formatArgs[index - offset] = parseInt;
						} catch (NumberFormatException ne) {
							throw new IllegalArgumentException("Character expected but found '" + args[index] + "'");
						}
					}
					break;

				case 'B' :
				case 'b' :
					String v = args[index].toLowerCase();
					formatArgs[index - offset] = isTruthy.apply(v);
					break;

				case 'h' :
				case 'H' :
				case 's' :
				case 'S' :
					formatArgs[index - offset] = args[index];
					break;

				case 't' :
				case 'T' :
					String inputDate = args[index];
					ZonedDateTime date = Dates.parse(inputDate);
					if (date == null) {
						throw new IllegalArgumentException("Illegal Date Format " + inputDate);
					}
					formatArgs[index - offset] = date;
					break;
			}
		}

		try (Formatter f = new Formatter(Locale.ROOT)) {
			f.format(format, formatArgs);
			return f.toString();
		}

	}

	/**
	 * Format a string using string, numeric, and date conversions while the
	 * input is strings
	 *
	 * @param format the format string
	 * @param args the arguments
	 * @return a formatted string
	 */
	public static String format(String format, String... args) {
		return format(format, Formatters::isTruthy, 0, args);
	}

	private static boolean isTruthy(String arg) {
		if (arg == null)
			return false;
		if (arg.isEmpty())
			return false;

		if ("false".equalsIgnoreCase(arg))
			return false;

		if ("0".equals(arg))
			return false;

		if ("(+|-)?0.0[DFdf]?".matches(arg))
			return false;

		return true;
	}

}
