package aQute.bnd.runtime.util;

import java.beans.Introspector;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceReference;

public class Util {

	public static boolean in(long[] source, long target) {
		for (long l : source)
			if (l == target)
				return true;

		return false;
	}

	public static <T extends Y, Y> T copy(Class<T> class1, Y src) {
		try {
			T newInstance = class1.newInstance();

			for (Field f : src.getClass()
				.getFields()) {
				try {
					Object value = f.get(src);
					f.set(newInstance, value);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}
			return newInstance;
		} catch (InstantiationException | IllegalAccessException e1) {
			throw new RuntimeException(e1);
		}
	}

	public static <T> Map<String, Object> asBean(Class<T> type, T c) {
		try {
			Map<String, Object> bean = new LinkedHashMap<>();
			Stream.of(Introspector.getBeanInfo(type)
				.getPropertyDescriptors())
				.filter(d -> d.getReadMethod() != null && d.getReadMethod()
					.getParameterTypes().length == 0)
				.forEach(d -> {
					try {
						Object value = d.getReadMethod()
							.invoke(c);
						value = neuter(value);
						if (value != null) {
							bean.put(d.getName(), value);
						}
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				});
			return bean;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static class ValueDTO {
		public String	value;
		public String	type;
		public long		id;
	}

	static Object neuter(Object value) {
		if (value == null)
			return "null";

		if (value instanceof Number || value instanceof String || value instanceof Boolean
			|| value instanceof Character) {
			return value;
		}

		if (value instanceof Bundle)
			return ((Bundle) value).getBundleId();

		if (value instanceof Bundle[]) {
			return Stream.of((Bundle[]) value)
				.map(Bundle::getBundleId)
				.toArray(Long[]::new);
		}

		if (value instanceof BundleContext)
			return null;

		if (value instanceof ServiceReference)
			return ((ServiceReference<?>) value).getProperty(Constants.SERVICE_ID);

		if (value instanceof Throwable) {

			return toString((Throwable) value);
		}
		Bundle bundle = FrameworkUtil.getBundle(value.getClass());
		if (bundle == null)
			return value.toString();

		return toValue(value);
	}

	public static String toString(Throwable value) {
		StringWriter writer = new StringWriter();
		PrintWriter pw = new PrintWriter(writer);
		value.printStackTrace(pw);
		pw.flush();
		return writer.toString();
	}

	public static ValueDTO toValue(Object value) {
		ValueDTO dto = new ValueDTO();
		dto.id = FrameworkUtil.getBundle(value.getClass())
			.getBundleId();
		dto.type = value.getClass()
			.getName();
		dto.value = value.toString();
		return dto;
	}

	public static void error(String string, Throwable e) {
		if (e instanceof java.lang.NoClassDefFoundError || e instanceof ServiceException) {
			System.err.println("no snapshotting: " + string);
			return;
		}
		if (e != null) {
			System.err.println("snapshotting: " + string + " : " + e.getMessage());
			e.printStackTrace();
		} else {
			System.err.println("snapshotting: " + string);

		}
	}

}
