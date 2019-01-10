package aQute.bnd.osgi;

import static java.util.Objects.requireNonNull;

import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.lib.converter.Converter;

/*
 * This class is referenced in aQute.bnd.annotation.metatype.Configurable in constant
 * BND_ANNOTATION_CLASS_NAME
 */
public class Annotation {
	/**
	 * Bnd ElememtType
	 * <p>
	 * We use this instead of java.lang.annotation.ElementType so we can
	 * reference element types introduced in versions of Java later than the
	 * current compile/runtime version of Java.
	 */
	public enum ElementType {
		TYPE, // Java 5
		FIELD, // Java 5
		METHOD, // Java 5
		PARAMETER, // Java 5
		CONSTRUCTOR, // Java 5
		LOCAL_VARIABLE, // Java 5
		ANNOTATION_TYPE, // Java 5
		PACKAGE, // Java 5
		TYPE_PARAMETER, // Java 8
		TYPE_USE, // Java 8
		MODULE; // Java 9
	}

	private static final Converter CONVERTER;

	static {
		CONVERTER = new Converter();
		CONVERTER.hook(null, (t, o) -> {
			if (o instanceof Annotation && t instanceof Class<?> && ((Class<?>) t).isAnnotation()) {
				Annotation a = (Annotation) o;
				@SuppressWarnings("unchecked")
				Class<java.lang.annotation.Annotation> c = (Class<java.lang.annotation.Annotation>) t;
				return a.getAnnotation(c);
			}
			return null;
		});
	}

	private final TypeRef			name;
	private Map<String, Object>		elements;
	private final ElementType		member;
	private final RetentionPolicy	policy;

	public Annotation(TypeRef name, Map<String, Object> elements, ElementType member, RetentionPolicy policy) {
		this.name = requireNonNull(name);
		this.elements = elements;
		this.member = requireNonNull(member);
		this.policy = requireNonNull(policy);
	}

	@Deprecated
	public Annotation(TypeRef name, Map<String, Object> elements, java.lang.annotation.ElementType member,
		RetentionPolicy policy) {
		this(name, elements, ElementType.valueOf(member.name()), policy);
	}

	public TypeRef getName() {
		return name;
	}

	public ElementType elementType() {
		return member;
	}

	@Deprecated
	public java.lang.annotation.ElementType getElementType() {
		return java.lang.annotation.ElementType.valueOf(elementType().name());
	}

	public RetentionPolicy getRetentionPolicy() {
		return policy;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(name)
			.append(':')
			.append(member)
			.append(':')
			.append(policy)
			.append(':')
			.append('{');
		if (elements != null) {
			Iterator<Entry<String, Object>> i = elements.entrySet()
				.iterator();
			if (i.hasNext()) {
				for (Entry<String, Object> e = i.next();; e = i.next()) {
					sb.append(e.getKey())
						.append('=');
					Object v = e.getValue();
					if (v instanceof Object[]) {
						sb.append(Arrays.toString((Object[]) v));
					} else {
						sb.append(v);
					}
					if (!i.hasNext()) {
						break;
					}
					sb.append(',')
						.append(' ');
				}
			}
		}
		return sb.append('}')
			.toString();
	}

	@SuppressWarnings("unchecked")
	public <T> T get(String string) {
		if (elements == null) {
			return null;
		}
		return (T) elements.get(string);
	}

	public <T> Stream<T> stream(String key, Class<? extends T> type) {
		Object v = get(key);
		if (v == null) {
			return Stream.empty();
		}
		if (v.getClass()
			.isArray()) {
			return Arrays.stream((Object[]) v)
				.map(type::cast);
		}
		return Stream.of(v)
			.map(type::cast);
	}

	public void put(String string, Object v) {
		if (elements == null) {
			elements = new LinkedHashMap<>();
		}
		elements.put(string, v);
	}

	public boolean containsKey(String key) {
		if (elements == null) {
			return false;
		}
		return elements.containsKey(key);
	}

	public Set<String> keySet() {
		if (elements == null) {
			return Collections.emptySet();
		}
		return elements.keySet();
	}

	public Set<Entry<String, Object>> entrySet() {
		if (elements == null) {
			return Collections.emptySet();
		}
		return elements.entrySet();
	}

	public <T extends java.lang.annotation.Annotation> T getAnnotation() throws Exception {
		return getAnnotation(getClass().getClassLoader());
	}

	public <T extends java.lang.annotation.Annotation> T getAnnotation(ClassLoader cl) throws Exception {
		String cname = name.getFQN();
		try {
			@SuppressWarnings("unchecked")
			Class<T> c = (Class<T>) cl.loadClass(cname);
			return getAnnotation(c);
		} catch (ClassNotFoundException | NoClassDefFoundError e) {
			return null;
		}
	}

	public <T extends java.lang.annotation.Annotation> T getAnnotation(Class<T> c) throws Exception {
		if (elements == null) {
			elements = new LinkedHashMap<>();
		}
		return CONVERTER.convert(c, elements);
	}

	public void merge(Annotation annotation) {
		merge(annotation.elements);
	}

	public void addDefaults(Clazz c) throws Exception {
		merge(c.getDefaults());
	}

	private void merge(Map<String, Object> map) {
		if (map == null || map.isEmpty()) {
			return;
		}
		if (elements == null) {
			elements = new LinkedHashMap<>(map);
		} else {
			map.forEach(elements::putIfAbsent);
		}
	}
}
