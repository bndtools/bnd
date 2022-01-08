package aQute.lib.formatter;

import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.lib.date.Dates;

public class Formatters {
	private final static Pattern	FORMATTER_P		= Pattern.compile(
		"%(?<argument>(?<index>\\d+)\\$|<)?[-#+ 0,(]*\\d*(?:\\.\\d+)?(?<conversion>[bBhHsScCdoxXeEfgGaA%n]|(?:[tT][HIklMSLNpzZsQBbhAaCYyjmdeRTrDFc]))");
	private static final Pattern	FLOATING_ZERO	= Pattern.compile("[-+]?0+\\.0+[dDfF]?");

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
		Object[] formatArgs = new Object[args.length - offset];

		Matcher m = FORMATTER_P.matcher(format);

		for (int automatic = 0; m.find();) {
			char conversion = m.group("conversion")
				.charAt(0);

			// %n
			if (conversion == 'n' || conversion == '%')
				continue;

			int index;
			if (m.group("argument") != null) {
				String indexString = m.group("index");
				if (indexString != null) { // n$
					index = Integer.parseInt(indexString) - 1;
				} else { // <
					continue;
				}
			} else {
				index = automatic++;
			}

			String arg = args[index + offset];
			// bBhHsScCdoxXeEfgGaAtT
			switch (conversion) {
				case 'd' :
				case 'o' :
				case 'x' :
				case 'X' :
					formatArgs[index] = Long.valueOf(arg);
					break;

				case 'a' :
				case 'A' :
				case 'e' :
				case 'E' :
				case 'f' :
				case 'g' :
				case 'G' :
					formatArgs[index] = Double.valueOf(arg);
					break;

				case 'c' :
				case 'C' :
					if (arg.length() == 1)
						formatArgs[index] = Character.valueOf(arg.charAt(0));
					else {
						try {
							Integer parseInt = Integer.valueOf(arg);
							formatArgs[index] = parseInt;
						} catch (NumberFormatException ne) {
							throw new IllegalArgumentException("Character expected but found '" + arg + "'");
						}
					}
					break;

				case 'B' :
				case 'b' :
					formatArgs[index] = isTruthy.apply(arg.toLowerCase(Locale.ROOT));
					break;

				case 't' :
				case 'T' :
					ZonedDateTime date = Dates.parse(arg);
					if (date == null) {
						throw new IllegalArgumentException("Illegal Date Format " + arg);
					}
					formatArgs[index] = date;
					break;

				case 'h' :
				case 'H' :
				case 's' :
				case 'S' :
				default :
					formatArgs[index] = arg;
					break;
			}
		}

		return String.format(Locale.ROOT, format, formatArgs);
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

	private static Boolean isTruthy(String arg) {
		if (arg == null)
			return Boolean.FALSE;
		arg = arg.trim();
		if (arg.isEmpty())
			return Boolean.FALSE;

		if ("false".equalsIgnoreCase(arg))
			return Boolean.FALSE;

		if ("0".equals(arg))
			return Boolean.FALSE;

		if (FLOATING_ZERO.matcher(arg).matches())
			return Boolean.FALSE;

		return Boolean.TRUE;
	}

}
