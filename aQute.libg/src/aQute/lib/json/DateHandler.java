package aQute.lib.json;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class DateHandler extends Handler {
	final static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);

	static {
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	@Override
	public void encode(Encoder app, Object object, Map<Object, Type> visited) throws IOException, Exception {
		String s;
		synchronized (sdf) {
			s = sdf.format((Date) object);
		}
		StringHandler.string(app, s);
	}

	@Override
	public Object decode(Decoder dec, String s) throws Exception {
		synchronized (sdf) {
			return sdf.parse(s);
		}
	}

	@Override
	public Object decode(Decoder dec, Number s) throws Exception {
		return new Date(s.longValue());
	}

}
