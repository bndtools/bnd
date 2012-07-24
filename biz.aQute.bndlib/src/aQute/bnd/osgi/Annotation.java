package aQute.bnd.osgi;

import java.lang.annotation.*;
import java.util.*;

import aQute.bnd.annotation.metatype.*;
import aQute.bnd.osgi.Descriptors.TypeRef;

public class Annotation {
	TypeRef				name;
	Map<String,Object>	elements;
	ElementType			member;
	RetentionPolicy		policy;

	public Annotation(TypeRef name, Map<String,Object> elements, ElementType member, RetentionPolicy policy) {
		this.name = name;
		if (elements == null)
			this.elements = Collections.emptyMap();
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

	public String toString() {
		return name + ":" + member + ":" + policy + ":" + elements;
	}

	public <T> T get(String string) {
		if (elements == null)
			return null;

		return (T) elements.get(string);
	}

	public <T> void put(String string, Object v) {
		if (elements == null)
			return;

		elements.put(string, v);
	}

	public Set<String> keySet() {
		if (elements == null)
			return Collections.emptySet();

		return elements.keySet();
	}

	public <T extends java.lang.annotation.Annotation> T getAnnotation() throws Exception {
		String cname = name.getFQN();
		Class<T> c = (Class<T>) getClass().getClassLoader().loadClass(cname);
		return getAnnotation(c);
	}

	public <T extends java.lang.annotation.Annotation> T getAnnotation(Class<T> c) throws Exception {
		String cname = name.getFQN();
		if (!c.getName().equals(cname))
			return null;
		return Configurable.createConfigurable(c, elements);
	}
}
