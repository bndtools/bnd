package aQute.lib.date;

import static aQute.lib.exceptions.FunctionWithException.asFunctionOrElse;
import static aQute.lib.exceptions.SupplierWithException.asSupplierOrElse;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.regex.Pattern;

import aQute.lib.strings.Strings;

public class DateUtil {
	private static final Map<Pattern, SimpleDateFormat>	DATE_FORMAT_REGEXPS	= new HashMap<Pattern, SimpleDateFormat>();
	public static final TimeZone						UTC_TIME_ZONE		= TimeZone.getTimeZone("UTC");
	private static final Pattern						IS_NUMERIC_P		= Pattern.compile("[+-]?\\d+");

	static {
		put("\\d{6}", "yyMMdd");
		put("\\d{8}", "yyyyMMdd");
		put("\\d{12}", "yyyyMMddHHmm");
		put("\\d{12}\\.\\d{3}Z?", "yyyyMMddHHmmss.SSSZ");
		put("\\d{14}", "yyyyMMddHHmmss");
		put("\\d{8}\\s\\d{4}", "yyyyMMdd HHmm");
		put("\\d{1,2}-\\d{1,2}-\\d{4}", "dd-MM-yyyy");
		put("\\d{4}-\\d{1,2}-\\d{1,2}", "yyyy-MM-dd");
		put("\\d{1,2}/\\d{1,2}/\\d{4}", "MM/dd/yyyy");
		put("\\d{4}/\\d{1,2}/\\d{1,2}", "yyyy/MM/dd");
		put("\\d{1,2}\\s[a-z]{3}\\s\\d{4}", "dd MMM yyyy");
		put("\\d{1,2}\\s[a-z]{4,}\\s\\d{4}", "dd MMMM yyyy");
		put("\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}", "dd-MM-yyyy HH:mm");
		put("\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}", "yyyy-MM-dd HH:mm");
		put("\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}", "MM/dd/yyyy HH:mm");
		put("\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}", "yyyy/MM/dd HH:mm");
		put("\\d{1,2}\\s[a-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}", "dd MMM yyyy HH:mm");
		put("\\d{1,2}\\s[a-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}", "dd MMMM yyyy HH:mm");
		put("\\d{8}\\s\\d{6}", "yyyyMMdd HHmmss");
		put("\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}", "dd-MM-yyyy HH:mm:ss");
		put("\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}", "yyyy-MM-dd HH:mm:ss");
		put("\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}", "MM/dd/yyyy HH:mm:ss");
		put("\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}", "yyyy/MM/dd HH:mm:ss");
		put("\\d{1,2}\\s[a-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}", "dd MMM yyyy HH:mm:ss");
		put("\\d{1,2}\\s[a-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}", "dd MMMM yyyy HH:mm:ss");
	}

	private static void put(String regex, String simpleDateFormat) {
		DATE_FORMAT_REGEXPS.put(Pattern.compile(regex), new SimpleDateFormat(simpleDateFormat));
	}

	/**
	 * Determine SimpleDateFormat pattern matching with the given date string.
	 * Returns null if format is unknown. You can simply extend DateUtil with
	 * more formats if needed.
	 *
	 * @param dateString The date string to determine the SimpleDateFormat
	 *            pattern for.
	 * @return The matching SimpleDateFormat pattern, or null if format is
	 *         unknown.
	 * @see SimpleDateFormat
	 */
	public static Optional<SimpleDateFormat> determineDateFormat(String dateString) {
		if (dateString == null)
			return Optional.empty();

		String s = Strings.trim(dateString);

		return DATE_FORMAT_REGEXPS.entrySet()
			.stream()
			.filter(e -> e.getKey()
				.matcher(s)
				.matches())
			.map(Map.Entry::getValue)
			.findAny();
	}

	public static Date parse(String dateString) {
		Date date = determineDateFormat(dateString).map(asFunctionOrElse(df -> {
			synchronized (df) {
				df.setTimeZone(UTC_TIME_ZONE);
				return df.parse(dateString);
			}
		}, null))
			.orElseGet(asSupplierOrElse(() -> {
				if (IS_NUMERIC_P.matcher(dateString)
					.matches()) {
					long ldate = Long.parseLong(dateString);
					return new Date(ldate);
				}
				return null;
			}, null));

		return date;
	}

}
