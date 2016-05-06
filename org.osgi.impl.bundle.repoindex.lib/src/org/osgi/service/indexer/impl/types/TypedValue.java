package org.osgi.service.indexer.impl.types;

import java.util.List;

import org.osgi.service.indexer.impl.Schema;
import org.osgi.service.indexer.impl.util.Tag;

public class TypedValue {
	private final Type			type;
	private final Object	value;

	public static TypedValue valueOf(Object value) {
		if (value instanceof TypedValue) {
			return (TypedValue) value;
		}
		return new TypedValue(value);
	}

	private TypedValue(Object value) {
		this.type = Type.typeOf(value);
		this.value = value;
	}

	public TypedValue(ScalarType scalar, List< ? > list) {
		this.type = Type.list(scalar);
		this.value = list;
	}

	public TypedValue(ScalarType scalar, Object value) {
		this.type = Type.scalar(scalar);
		this.value = value;
	}

	public Tag addTo(Tag tag) {
		if (type.isList() || type.getType() != ScalarType.String) {
			tag.addAttribute(Schema.ATTR_TYPE, type.toString());
		}
		tag.addAttribute(Schema.ATTR_VALUE, type.convertToString(value));
		return tag;
	}
}
