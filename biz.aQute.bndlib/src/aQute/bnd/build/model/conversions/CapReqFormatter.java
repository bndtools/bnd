package aQute.bnd.build.model.conversions;

import java.util.Collection;
import java.util.Map;

import org.osgi.framework.Version;

import aQute.bnd.header.OSGiHeader;
import aQute.bnd.stream.MapStream;
import aQute.lib.strings.Strings;

abstract class CapReqFormatter {
	String convert(String namespace, Map<String, String> directives, Map<String, Object> attributes) {
		StringBuilder builder = new StringBuilder(namespace);
		MapStream.of(directives)
			.sortedByKey() // reproducible output
			.forEachOrdered((key, value) -> {
				builder.append(';')
					.append(key)
					.append(':')
					.append('=');
				OSGiHeader.quote(builder, value, '\'');
			});
		MapStream.of(attributes)
			.sortedByKey() // reproducible output
			.forEachOrdered((key, value) -> {
				builder.append(';')
					.append(key)
					.append(valueType(value))
					.append('=');
				OSGiHeader.quote(builder, valueString(value), '\'');
			});
		return builder.toString();
	}

	private static String valueType(Object value) {
		if (value instanceof String) {
			return "";
		}
		if (value instanceof Version) {
			return ":Version";
		}
		if (value instanceof Long) {
			return ":Long";
		}
		if (value instanceof Collection<?> v) {
			if (v.isEmpty()) {
				return ":List<String>";
			}
			Object first = v.iterator()
				.next();
			if (first instanceof String) {
				return ":List<String>";
			}
			if (first instanceof Version) {
				return ":List<Version>";
			}
			if (first instanceof Long) {
				return ":List<Long>";
			}
			if (first instanceof Double) {
				return ":List<Double>";
			}
		}
		if (value instanceof Double) {
			return ":Double";
		}
		return "";
	}

	private static String valueString(Object value) {
		if (value instanceof Collection<?> v) {
			return v.stream()
				.map(String::valueOf)
				.collect(Strings.joining());
		}
		return String.valueOf(value);
	}

	public String error(String msg) {
		return msg;
	}
}
