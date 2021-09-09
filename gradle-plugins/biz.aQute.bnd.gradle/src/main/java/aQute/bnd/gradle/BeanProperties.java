package aQute.bnd.gradle;

import static aQute.bnd.gradle.BndUtils.unwrapFile;
import static aQute.bnd.gradle.BndUtils.unwrapOrNull;
import static java.lang.invoke.MethodHandles.publicLookup;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.internal.DynamicObjectAware;
import org.gradle.api.provider.Provider;
import org.gradle.internal.metaobject.DynamicInvokeResult;
import org.gradle.internal.metaobject.DynamicObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Properties subclass which introspects objects for field values.
 */
public class BeanProperties extends Properties {
	private static final Logger		logger				= LoggerFactory.getLogger(BeanProperties.class);

	private static final Pattern	KEY_P				= Pattern
		.compile("(?<name>[^\\.\\[]+)(?:\\[(?<index>\\d+)\\])?\\.?");
	private static final long		serialVersionUID	= 1L;

	/**
	 * Default values for properties. May be {@code null}.
	 */
	protected Properties			defaults;

	/**
	 * Create a new BeanProperties with no defaults.
	 */
	public BeanProperties() {
		this(null);
	}

	/**
	 * Create a new BeanProperties with the specified defaults.
	 *
	 * @param defaults Default values for properties. May be {@code null}.
	 */
	public BeanProperties(Properties defaults) {
		this.defaults = defaults;
	}

	@Override
	public String getProperty(String key) {
		final Matcher m = KEY_P.matcher(key);
		if (!m.find()) {
			return defaultValue(key);
		}
		String name = m.group("name");
		Object value = value(name, get(name), m.group("index"));
		while ((value != null) && m.find()) {
			name = m.group("name");
			value = value(name, getField(value, name), m.group("index"));
		}
		value = unwrap(value);
		return (value != null) ? value.toString() : defaultValue(key);
	}

	private String defaultValue(String key) {
		return (defaults != null) ? defaults.getProperty(key) : null;
	}

	private static Object unwrap(Object value) {
		if (value instanceof Provider) {
			value = unwrapOrNull((Provider<?>) value);
		}
		if (value instanceof FileSystemLocation) {
			value = unwrapFile((FileSystemLocation) value);
		}
		return value;
	}

	private static Object getField(Object target, String fieldName) {
		try {
			if (target instanceof DynamicObjectAware) {
				DynamicObject dynamicObject = ((DynamicObjectAware) target).getAsDynamicObject();
				DynamicInvokeResult result = dynamicObject.tryGetProperty(fieldName);
				return result.isFound() ? result.getValue() : null;
			}
			String getterSuffix = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
			Class<?> targetClass = target.getClass();
			while (!Modifier.isPublic(targetClass.getModifiers())) {
				targetClass = targetClass.getSuperclass();
			}
			MethodHandle mh;
			try {
				mh = publicLookup().unreflect(targetClass.getMethod("get" + getterSuffix));
			} catch (NoSuchMethodException nsme) {
				mh = publicLookup().unreflect(targetClass.getMethod("is" + getterSuffix));
			}
			return mh.invoke(target);
		} catch (Error e) {
			throw e;
		} catch (Throwable e) {
			logger.debug("Could not find getter method for field {}", fieldName, e);
		}
		return null;
	}

	private static Object value(String name, Object value, String index) {
		if ((value == null) || (index == null)) {
			return value;
		}
		try {
			int i = Integer.parseInt(index);
			if (value instanceof List) {
				return ((List<?>) value).get(i);
			} else if (value instanceof Iterable) {
				if (i < 0) {
					throw new IndexOutOfBoundsException("index < 0");
				}
				Iterator<?> iter = ((Iterable<?>) value).iterator();
				for (; i > 0; i--) {
					iter.next();
				}
				return iter.next();
			} else if (value.getClass()
				.isArray()) {
				return Array.get(value, i);
			}
		} catch (Exception e) {
			logger.debug("Could not find field {}[{}]", name, index, e);
		}
		return value;
	}
}
