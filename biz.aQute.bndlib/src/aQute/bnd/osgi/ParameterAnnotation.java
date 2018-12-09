package aQute.bnd.osgi;

import java.lang.annotation.RetentionPolicy;
import java.util.Map;

import aQute.bnd.osgi.Descriptors.TypeRef;

public class ParameterAnnotation extends Annotation {
	private final int parameter;

	public ParameterAnnotation(int parameter, TypeRef name, Map<String, Object> elements, ElementType member,
		RetentionPolicy policy) {
		super(name, elements, member, policy);
		this.parameter = parameter;
	}

	public int parameter() {
		return parameter;
	}

	@Override
	public String toString() {
		return parameter + ":" + super.toString();
	}
}
