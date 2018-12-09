package aQute.bnd.osgi;

import static java.util.Objects.requireNonNull;

import java.lang.annotation.RetentionPolicy;
import java.util.Map;

import aQute.bnd.osgi.Descriptors.TypeRef;

public class TypeAnnotation extends Annotation {
	private final int		target_type;
	private final byte[]	target_info;
	private final int		target_index;
	private final byte[]	type_path;

	public TypeAnnotation(int target_type, byte[] target_info, int target_index, byte[] type_path, TypeRef name,
		Map<String, Object> elements, ElementType member, RetentionPolicy policy) {
		super(name, elements, member, policy);
		this.target_type = target_type;
		this.target_info = requireNonNull(target_info);
		this.target_index = target_index;
		this.type_path = requireNonNull(type_path);
	}

	public int targetType() {
		return target_type;
	}

	public byte[] targetInfo() {
		return target_info;
	}

	public int targetIndex() {
		return target_index;
	}

	public byte[] typePath() {
		return type_path;
	}
}
