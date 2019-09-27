package aQute.lib.json;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import aQute.lib.date.Dates;

public class DateHandler extends Handler {
	private static final DateTimeFormatter	DATE_TIME_FORMATTER	= DateTimeFormatter.ISO_LOCAL_DATE_TIME
		.withLocale(Locale.ENGLISH)
		.withZone(Dates.UTC_ZONE_ID);

	@Override
	public void encode(Encoder app, Object object, Map<Object, Type> visited) throws IOException, Exception {
		long epochMilli = ((Date) object).getTime();
		String s = Dates.formatMillis(DATE_TIME_FORMATTER, epochMilli);
		StringHandler.string(app, s);
	}

	@Override
	public Object decode(Decoder dec, String s) throws Exception {
		long epochMilli = Dates.parseMillis(DATE_TIME_FORMATTER, s);
		return new Date(epochMilli);
	}

	@Override
	public Object decode(Decoder dec, Number s) throws Exception {
		return new Date(s.longValue());
	}

}
