package aQute.bnd.osgi;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.osgi.Descriptors.TypeRef;

/*
 * This class is referenced in aQute.bnd.annotation.metatype.Configurable in constant
 * BND_ANNOTATION_CLASS_NAME
 */
public class Annotation {
	private TypeRef				name;
	private Map<String,Object>	elements;
	private ElementType			member;
	private RetentionPolicy		policy;

	public Annotation(TypeRef name, Map<String,Object> elements, ElementType member, RetentionPolicy policy) {
		this.name = name;
		if (elements == null)
			this.elements = null;
		else
			this.elements = elements;
		this.member = member;
		this.policy = policy;
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
		if (elements == null)
			return null;

		return (T) elements.get(string);
	}

	public <T> void put(String string, Object v) {
		if (elements == null)
			elements = new LinkedHashMap<String,Object>();

		elements.put(string, v);
	}

	public Set<String> keySet() {
		if (elements == null)
			return Collections.emptySet();

		return elements.keySet();
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
		} catch (ClassNotFoundException e) {} catch (NoClassDefFoundError e) {}
		return null;
	}

	public <T extends java.lang.annotation.Annotation> T getAnnotation(Class<T> c) throws Exception {
		String cname = name.getFQN();
		if (!c.getName().equals(cname))
			return null;
		return Configurable.createConfigurable(c,
				elements == null ? elements = new LinkedHashMap<String,Object>() : elements);
	}

	public void merge(Annotation annotation) {
		if (annotation.elements == null)
			return;

		for (Map.Entry<String,Object> e : annotation.elements.entrySet()) {
			if (!elements.containsKey(e.getKey()))
				elements.put(e.getKey(), e.getValue());
		}
	}

	public void addDefaults(Clazz c) throws Exception {
		Map<String,Object> defaults = c.getDefaults();
		if (defaults == null || defaults.isEmpty())
			return;

		for (Map.Entry<String,Object> e : defaults.entrySet()) {
			if (elements == null || !elements.containsKey(e.getKey())) {
				put(e.getKey(), e.getValue());
			}
		}
	}
}
