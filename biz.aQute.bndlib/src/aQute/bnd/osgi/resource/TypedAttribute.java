package aQute.bnd.osgi.resource;

import java.util.Collection;

import aQute.bnd.version.Version;

public class TypedAttribute {
	public final String	value;
	public final String	type;

	public TypedAttribute(String type, String value) {
		this.type = "String".equals(type) ? null : type;
		this.value = value;
	}

	public static TypedAttribute getTypedAttribute(Object value) {
		if (value instanceof Collection) {
			Collection<?> c = (Collection<?>) value;
			if (c.isEmpty())
				return null;

			StringBuilder sb = new StringBuilder();
			String del = "";
			String subType = null;

			for (Object v : c) {
				if (subType == null)
					subType = getType(v);

				sb.append(del);
				escape(sb, v.toString());
				del = ",";
			}
			if (subType == null)
				subType = "String";

			return new TypedAttribute("List<" + subType + ">", sb.toString());
		}

		if (value.getClass()
			.isArray()) {
			Object[] array = (Object[]) value;
			if (array.length == 0)
				return null;

			StringBuilder sb = new StringBuilder();
			String del = "";
			String subType = null;

			for (Object v : array) {
				if (subType == null)
					subType = getType(v);

				sb.append(del);
				escape(sb, v.toString());
				del = ",";
			}
			if (subType == null)
				subType = "String";

			return new TypedAttribute("List<" + subType + ">", sb.toString());
		}

		return new TypedAttribute(getType(value), value.toString());
	}

	private static StringBuilder escape(StringBuilder sb, String value) {
		final int len = value.length();
		for (int i = 0; i < len; i++) {
			char c = value.charAt(i);
			switch (c) {
				case '\\' :
				case ',' :
					sb.append('\\')
						.append(c);
					break;
				default :
					sb.append(c);
					break;
			}
		}
		return sb;
	}

	private static String getType(Object value) {
		if (value instanceof Long || value instanceof Integer || value instanceof Short || value instanceof Byte) {
			return "Long";
		} else if (value instanceof Double || value instanceof Float) {
			return "Double";
		} else if (value instanceof Version || value instanceof org.osgi.framework.Version)
			return "Version";

		return "String";
	}
}
