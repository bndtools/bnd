package aQute.lib.osgi;

import java.lang.annotation.*;
import java.util.*;

import aQute.bnd.annotation.metatype.*;

public class Annotation {
	String				name;
	Map<String, Object>	elements;
	ElementType			member;
	RetentionPolicy		policy;

	public Annotation(String name, Map<String, Object> elements, ElementType member,
			RetentionPolicy policy) {
		this.name = name;
		if ( elements == null)
			this.elements = Collections.emptyMap();
		else
			this.elements = elements;
		this.member = member;
		this.policy = policy;
	}

	public String getName() {
		return name;
	}

	public String toString() {
		return name + ":" + member + ":" + policy + ":" + elements;
	}

	@SuppressWarnings("unchecked") public <T> T get(String string) {
		if (elements == null)
			return null;

		return (T) elements.get(string);
	}

	public <T> void put(String string, Object v) {
		if (elements == null)
			return;

		elements.put(string, v);
	}

	@SuppressWarnings("unchecked") public <T extends java.lang.annotation.Annotation> T getAnnotation() throws Exception {
		String cname = Clazz.objectDescriptorToFQN(name);
		Class<T> c = (Class<T>) getClass().getClassLoader().loadClass(cname);
		return getAnnotation(c);
	}
	public <T extends java.lang.annotation.Annotation> T getAnnotation(Class<T> c)
			throws Exception {
		String cname = Clazz.objectDescriptorToFQN(name);
		if ( ! c.getName().equals(cname))
			return null;
		return Configurable.createConfigurable(c, elements );
	}
}
