package aQute.lib.json;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

public class NumberHandler extends Handler {
	final Class<?> type;

	NumberHandler(Class<?> clazz) {
		this.type = clazz;
	}

	@Override
	public void encode(Encoder app, Object object, Map<Object, Type> visited) throws Exception {
		String s = object.toString();
		if (s.endsWith(".0"))
			s = s.substring(0, s.length() - 2);

		app.append(s);
	}

	@Override
	public Object decode(Decoder dec, boolean s) {
		return decode(dec, s ? 1d : 0d);
	}

	@Override
	public Object decode(Decoder dec, String s) {
		double d = Double.parseDouble(s);
		return decode(dec, d);
	}

	@Override
	public Object decode(Decoder dec) {
		return decode(dec, 0d);
	}

	@Override
	public Object decode(Decoder dec, Number s) {
		double dd = s.doubleValue();

		if (type == double.class || type == Double.class)
			return s.doubleValue();

		if ((type == int.class || type == Integer.class) && within(dd, Integer.MIN_VALUE, Integer.MAX_VALUE))
			return s.intValue();

		if ((type == long.class || type == Long.class) && within(dd, Long.MIN_VALUE, Long.MAX_VALUE))
			return s.longValue();

		if ((type == byte.class || type == Byte.class) && within(dd, Byte.MIN_VALUE, Byte.MAX_VALUE))
			return s.byteValue();

		if ((type == short.class || type == Short.class) && within(dd, Short.MIN_VALUE, Short.MAX_VALUE))
			return s.shortValue();

		if (type == float.class || type == Float.class)
			return s.floatValue();

		if (type == BigDecimal.class)
			return BigDecimal.valueOf(dd);

		if (type == BigInteger.class)
			return BigInteger.valueOf(s.longValue());

		throw new IllegalArgumentException("Unknown number format: " + type);
	}

	private boolean within(double s, double minValue, double maxValue) {
		return s >= minValue && s <= maxValue;
	}
}
