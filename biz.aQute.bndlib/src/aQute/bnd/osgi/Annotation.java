package aQute.bnd.osgi;

import static java.util.Objects.requireNonNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collections;
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

	public TypeRef getName() {
		return name;
	}

	public ElementType getElementType() {
		return member;
	}

	public RetentionPolicy getRetentionPolicy() {
		return policy;
	}

	@Override
	public String toString() {
		return name + ":" + member + ":" + policy + ":" + (elements == null ? "{}" : elements);
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
			map.forEach((k, v) -> elements.putIfAbsent(k, v));
		}
	}
}
