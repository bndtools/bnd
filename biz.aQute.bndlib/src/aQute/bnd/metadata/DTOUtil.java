package aQute.bnd.metadata;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import org.osgi.dto.DTO;

/**
 * Contains a number of DTO utility functions.
 */
final public class DTOUtil {

	private final static String						LOCALIZATION_FIELD	= "localizations";
	private final static Field[]					EMPTY_FIELDS		= new Field[0];
	private final static Link						ROOT				= new Link(null, null, null);
	private final static Map<Class< ? >,Field[]>	cache				= Collections
			.synchronizedMap(new WeakHashMap<Class< ? >,Field[]>());

	//
	// The link class is to keep track of cycles traversing and to
	// maintain the path at minimum cost.
	//

	static class Link {
		final Link		prev;
		final Object	object;
		final Object	name;

		public Link(Link link, Object name, Object object) {
			this.prev = link;
			this.name = name;
			this.object = object;
		}

		boolean isCycle(Object t) {
			if (this.object == t)
				return true;
			else if (prev == null)
				return false;
			else
				return prev.isCycle(t);
		}

		String[] getPath(int n) {
			if (prev == null) {
				String[] path = new String[n];
				return path;
			}
			String[] path = prev.getPath(n + 1);
			path[path.length - n - 1] = name.toString();
			return path;
		}

		void verifyCycle(Object o) {
			if (isCycle(o)) {
				throw new IllegalArgumentException("Cycle in DTO " + getPath(0));
			}
		}
	}

	private DTOUtil() {

	}

	/**
	 * Create a deep copy of a DTO. This will copy the fields of the DTO. Copied
	 * values will also be created anew if they are complex (Map, Collection, DTO,
	 * or Array). Other objects are assumed to immutable unless they implement
	 * Cloneable.
	 * 
	 * @param object the object to deep copy, can be {@code null}
	 * @return the deep copied object, can be {@code null}
	 * @throws Exception if there is a type mismatch
	 */
	static public <T> T deepCopy(final T object) throws Exception {
		return deepCopy(object, ROOT, null, null);
	}

	/**
	 * Create a localized deep copy of a DTO. This will create a deep copy and try
	 * to translate objects in the specified locale if they have a localization and
	 * translation data for the specified locale.
	 * <p>
	 * An object has a localization if it is a {@code Map} or a {@code DTO} and
	 * contains a field named {@code localizations}.
	 * </p>
	 * <p>
	 * An object has translation data if the localization object is a {@code Map} or
	 * a {@code DTO} whose field is the string representation of a locale as defined
	 * in {@code java.util.Locale} and the value is a {@code Map} or a {@code DTO}
	 * which contains at least one field name equals to one of those of the
	 * translated object, with a non {@code null} or a non empty (for
	 * {@code Collection}) value.
	 * </p>
	 * <p>
	 * The translation process will try to translate all the fields of the object
	 * from the specified locale to a less precise locale by removing the variant
	 * and then the country. At the end, the translation data are removed from the
	 * returned object, and thus the returned object cannot be translated again.
	 * </p>
	 * 
	 * @param object the object to deep copy, can be {@code null}
	 * @param locale the locale used to translate, can be {@code null}
	 * @return the localized deep copied object, can be {@code null}
	 * @throws Exception if any error
	 */
	static public <T> T deepCopy(final T object, final Locale locale) throws Exception {
		return deepCopy(object, ROOT, locale, LOCALIZATION_FIELD);
	}

	/**
	 * Create a localized deep copy of a DTO. This will create a deep copy and try
	 * to translate objects in the specified locale if they have a localization and
	 * translation data for the specified locale.
	 * <p>
	 * An object has a localization if it is a {@code Map} or a {@code DTO} and
	 * contains a field name that is equal to the field name provided in arguments.
	 * </p>
	 * <p>
	 * An object has translation data if the localization object is a {@code Map} or
	 * a {@code DTO} whose field is the string representation of a locale as defined
	 * in {@code java.util.Locale} and the value is a {@code Map} or a {@code DTO}
	 * which contains at least one field name equals to one of those of the
	 * translated object, with a non {@code null} or a non empty (for
	 * {@code Collection}) value.
	 * </p>
	 * <p>
	 * The translation process will try to translate all the fields of the object
	 * from the specified locale to a less precise locale by removing the variant
	 * and then the country. At the end, the translation data are removed from the
	 * returned object, and thus the returned object cannot be translated again.
	 * </p>
	 * 
	 * @param object the object to deep copy, can be {@code null}
	 * @param locale the locale used to translate, can be {@code null}
	 * @param localizationField the localization field name, can be {@code null}
	 * @return the localized deep copied object, can be {@code null}
	 * @throws Exception if any error
	 */
	static public <T> T deepCopy(final T object, final Locale locale, String localizationField) throws Exception {

		if (localizationField != null) {

			return deepCopy(object, ROOT, locale, localizationField);

		} else {

			return deepCopy(object, ROOT, locale, LOCALIZATION_FIELD);
		}
	}

	@SuppressWarnings({
			"unchecked", "rawtypes"
	})
	static private <T> T deepCopy(T source, Link link, Locale locale, String localizationField) throws Exception {
		if (!isComplex(source))
			return source;

		link.verifyCycle(source);

		Class<T> c = (Class<T>) source.getClass();

		if (c.isArray()) {
			int l = Array.getLength(source);
			T dest = (T) Array.newInstance(c.getComponentType(), l);

			for (int i = 0; i < l; i++) {
				Object s = Array.get(source, i);
				Array.set(dest, i, deepCopy(s, new Link(link, i, source), locale, localizationField));
			}
			return dest;
		}

		T dest = c.newInstance();

		if (source instanceof Map) {
			Map<Object,Object> d = (Map<Object,Object>) dest;
			Map<Object,Object> s = (Map<Object,Object>) source;

			Map<String,Object> translations = new HashMap<>();
			Object replacement = null;
			if (locale != null && s.containsKey(localizationField)) {
				replacement = extractTranslation(s.get(localizationField), translations, locale);
			}

			for (Entry< ? , ? > entry : s.entrySet()) {
				Link next = new Link(link, entry.getKey(), source);
				if (replacement != null && localizationField.equals(entry.getKey())) {
					d.put(localizationField, replacement);
				} else if (translations.containsKey(entry.getKey())) {
					d.put(deepCopy(entry.getKey(), next, locale, localizationField),
							deepCopy(translations.get(entry.getKey()), next, locale, localizationField));
				} else {
					d.put(deepCopy(entry.getKey(), next, locale, localizationField),
							deepCopy(entry.getValue(), next, locale, localizationField));
				}
			}
			return dest;
		}

		if (source instanceof Collection) {
			Collection s = (Collection) source;
			Collection d = (Collection) dest;
			int i = 0;
			for (Object o : s) {
				Link next = new Link(link, i++, source);
				d.add(deepCopy(o, next, locale, localizationField));
			}
			return dest;
		}

		Field[] fields = getFields(c);
		Map<String,Object> translations = new HashMap<>();
		Object replacement = null;
		if (locale != null) {
			boolean cont = false;
			for (int i = 0; i < fields.length && !cont; i++) {
				if (fields[i].getName().equals(localizationField)) {
					replacement = extractTranslation(fields[i].get(source), translations, locale);
					cont = true;
				}
			}
		}

		for (Field field : fields) {
			Link next = new Link(link, field.getName(), source);
			if (replacement != null && localizationField.equals(field.getName())) {
				field.set(dest, new HashMap<>());
			} else if (translations.containsKey(field.getName())) {
				field.set(dest, deepCopy(translations.get(field.getName()), next, locale, localizationField));
			} else {
				field.set(dest, deepCopy(field.get(source), next, locale, localizationField));
			}
		}
		return dest;
	}

	static private boolean isComplex(Object a) {
		return a != null && (a instanceof Map || a instanceof Collection || a instanceof DTO || a.getClass().isArray()
				|| getFields(a).length > 0);
	}

	static private Field[] getFields(Object o) {
		if (o == null)
			return EMPTY_FIELDS;
		return getFields(o.getClass());
	}

	static private Field[] getFields(Class< ? > c) {
		Field fields[] = cache.get(c);
		if (fields == null) {
			List<Field> publicFields = new ArrayList<>();

			for (Field field : c.getFields()) {
				if (field.isEnumConstant() || field.isSynthetic() || Modifier.isStatic(field.getModifiers()))
					continue;
				publicFields.add(field);
			}
			Collections.sort(publicFields, new Comparator<Field>() {

				@Override
				public int compare(Field o1, Field o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});

			cache.put(c.getClass(), fields = publicFields.toArray(new Field[publicFields.size()]));
		}
		return fields;
	}

	@SuppressWarnings({
			"unchecked", "rawtypes"
	})
	private static Object extractTranslation(final Object object, final Map<String,Object> translations,
			final Locale locale) throws Exception {

		if (object instanceof Map || object instanceof DTO) {

			Locale l = locale;
			do {

				Object dico = null;

				if (object instanceof Map) {
					dico = ((Map<Object,Object>) object).get(l.toString());
				} else {
					Field[] fields = getFields(object);
					Field result = null;
					String language = l.toString();
					for (int i = 0; i < fields.length && result == null; i++) {
						if (fields[i].getName().equals(language)) {
							result = fields[i];
						}
					}
					if (result != null) {
						dico = result.get(object);
					}
				}

				if (dico instanceof Map) {
					// String cast is fine here, it is normal to throw an exception if not
					Map<String,Object> source = (Map<String,Object>) dico;

					for (Entry<String,Object> entry : source.entrySet()) {
						Object value = entry.getValue();
						if (!translations.containsKey(entry.getKey())) {
							if (value != null || (value instanceof Collection && !((Collection) value).isEmpty())) {
								translations.put(entry.getKey(), value);
							}
						}
					}
				} else if (dico instanceof DTO) {
					for (Field field : getFields(dico)) {
						Object value = field.get(dico);
						if (!translations.containsKey(field.getName())) {
							if (value != null || (value instanceof Collection && !((Collection) value).isEmpty())) {
								translations.put(field.getName(), value);
							}
						}
					}
				}

				l = getNextLocale(l);
			} while (l != null);

			return object.getClass().newInstance();
		}

		return null;
	}

	private static Locale getNextLocale(final Locale locale) {

		if (!locale.getVariant().isEmpty()) {

			return new Locale(locale.getLanguage(), locale.getCountry());
		}

		if (!locale.getCountry().isEmpty()) {

			return new Locale(locale.getLanguage());
		}

		return null;
	}
}
