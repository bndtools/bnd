package aQute.lib.date;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.Locale;
import java.util.TimeZone;

public class Dates {
	public final static ZoneId					UTC_ZONE_ID				= ZoneId.of("UTC");
	public static final TimeZone				UTC_TIME_ZONE			= TimeZone.getTimeZone("UTC");
	public static final DateTimeFormatter		RFC_7231_DATE_TIME		= DateTimeFormatter
		.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
		.withZone(ZoneId.of("GMT"));
	// old Date toString format in default Locale
	private static final DateTimeFormatter		DATE_TOSTRING_DEFAULT_LOCALE	= DateTimeFormatter
		.ofPattern("EEE MMM dd HH:mm:ss z yyyy");
	// old Date toString format
	public static final DateTimeFormatter		DATE_TOSTRING					= DATE_TOSTRING_DEFAULT_LOCALE
		.withLocale(Locale.ROOT);

	private static final DateTimeFormatter[]	DATE_TIME_FORMATTERS	= new DateTimeFormatter[] {
		// @formatter:off
		DateTimeFormatter.ofPattern("yyyyMMddHHmmss.SSSZ", Locale.ROOT),
		DateTimeFormatter.ISO_OFFSET_DATE.withLocale(Locale.ROOT),
		DateTimeFormatter.ISO_LOCAL_DATE_TIME.withLocale(Locale.ROOT),
		DateTimeFormatter.ISO_LOCAL_DATE.withLocale(Locale.ROOT),
		DateTimeFormatter.ISO_OFFSET_DATE_TIME.withLocale(Locale.ROOT),
		DateTimeFormatter.ISO_ORDINAL_DATE.withLocale(Locale.ROOT),
		DateTimeFormatter.ISO_ZONED_DATE_TIME.withLocale(Locale.ROOT),
		DateTimeFormatter.ISO_WEEK_DATE.withLocale(Locale.ROOT),
		RFC_7231_DATE_TIME,
		DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.ROOT),

		// old Date toString format
		DATE_TOSTRING,

		// old Date toString format in default Locale
		DATE_TOSTRING_DEFAULT_LOCALE,

		DateTimeFormatter.ofPattern("yyyy[-][/][ ]MM[-][/][ ]dd[ ][HH[:]mm[[:]ss][.SSS]][X]", Locale.ROOT),

		DateTimeFormatter.ofPattern("dd[-][/][ ]MM[-][/][ ]yyyy[ HH[:]mm[[:]ss[.SSS]][X]", Locale.ROOT),
		DateTimeFormatter.ofPattern("dd[-][/][ ]MMM[-][/][ ]yyyy[ HH[:]mm[[:]ss[.SSS]][X]", Locale.ROOT),
		DateTimeFormatter.ofPattern("dd[-][/][ ]MMMM[-][/][ ]yyyy[ HH[:]mm[[:]ss[.SSS]][X]", Locale.US),

		// Dont ask why these are needed, seems the DTF has a problem with a long
		// row of digits. The optional characters [] dont work it seems
		DateTimeFormatter.ofPattern("yyyyMMdd[X]", Locale.ROOT),
		DateTimeFormatter.ofPattern("yyyyMMddHHmm[X]", Locale.ROOT),
		DateTimeFormatter.ofPattern("yyyyMMdd[ ][/][-]HHmm[X]", Locale.ROOT),
		DateTimeFormatter.ofPattern("yyyyMMddHHmmss[.SSS][X]", Locale.ROOT),
		DateTimeFormatter.ofPattern("yyyyMMdd[ ][/][-]HHmmss[.SSS][X]", Locale.ROOT),



		// @formatter:on
	};

	private Dates() {}

	/**
	 * Return a ZonedDateTime that is set to the given datestring. This will try
	 * all standard DateTimeFormatter formats and a bunch more. It does not
	 * support formats where the day and month are ambiguous. It is either
	 * year-month-day or day-month-year.
	 *
	 * @param dateString a date formatted string
	 * @return a ZonedDateTime or null if the string cannot be interpreted as a
	 *         date
	 */
	public static ZonedDateTime parse(String dateString) {
		for (DateTimeFormatter df : DATE_TIME_FORMATTERS) {
			try {
				return toZonedDateTime(df.parse(dateString));
			} catch (DateTimeParseException dte) {
				// we ignore wrong formats
				continue;
			}
		}

		try {
			long epochMilli = Long.parseLong(dateString);
			return toZonedDateTime(epochMilli);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * Turn a TemporalAccessor into a ZonedDateTime using defaults for missing
	 * fields. See {@link #toZonedDateTime(TemporalAccessor)} for defaults.
	 *
	 * @param temporal the temporal to turn into {@link ZonedDateTime}
	 * @return a {@link ZonedDateTime}
	 */
	public static ZonedDateTime toZonedDateTime(TemporalAccessor temporal) {
		if (temporal instanceof ZonedDateTime)
			return (ZonedDateTime) temporal;

		LocalDate date = temporal.query(TemporalQueries.localDate());
		LocalTime time = temporal.query(TemporalQueries.localTime());
		ZoneId zone = temporal.query(TemporalQueries.zone());

		return toZonedDateTime(date, time, zone);
	}

	/**
	 * Return a new ZonedDateTime based on a local date, time, and zone. Each
	 * can be null.
	 *
	 * @param date the localdate, when null, the current date is used
	 * @param time the time, when null, 00:00:00.000 is used
	 * @param zone the time zone, when null, UTC is used
	 * @return a {@link ZonedDateTime}
	 */
	public static ZonedDateTime toZonedDateTime(LocalDate date, LocalTime time, ZoneId zone) {
		if (date == null)
			date = LocalDate.now();

		if (time == null)
			time = LocalTime.MIN;

		if (zone == null)
			zone = UTC_ZONE_ID;

		return ZonedDateTime.of(date, time, zone);
	}

	/**
	 * Return a new ZonedDateTime based on a epoch milliseconds.
	 *
	 * @param epochMilli the number of milliseconds from 1970-01-01T00:00:00Z
	 * @return a {@link ZonedDateTime} using UTC time zone.
	 */
	public static ZonedDateTime toZonedDateTime(long epochMilli) {
		return ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), UTC_ZONE_ID);
	}

	/**
	 * Parse a string into epoch milliseconds.
	 *
	 * @param formatter The formatter to parse the string with.
	 * @param time Time string to parse into epoch milliseconds.
	 * @return The number of milliseconds from 1970-01-01T00:00:00Z.
	 */
	public static long parseMillis(DateTimeFormatter formatter, String time) {
		TemporalAccessor temporal = formatter.parse(time);
		return toZonedDateTime(temporal).toInstant()
			.toEpochMilli();
	}

	/**
	 * Format epoch milliseconds to a string.
	 *
	 * @param formatter The formatter to format the epoch milliseconds with.
	 * @param epochMilli Number of milliseconds from 1970-01-01T00:00:00Z.
	 * @return Time string from the epoch milliseconds.
	 */
	public static String formatMillis(DateTimeFormatter formatter, long epochMilli) {
		TemporalAccessor temporal = (formatter.getZone() == null) ? toZonedDateTime(epochMilli)
			: Instant.ofEpochMilli(epochMilli);
		return formatter.format(temporal);
	}
}
