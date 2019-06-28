package aQute.lib.annotations.setter;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import aQute.lib.converter.Converter;

public class AnnotationSetter<T> {
	final static Method TO_STRING;
	static {
		try {
			TO_STRING = Object.class.getMethod("toString");
		} catch (NoSuchMethodException | SecurityException e) {
			throw new UnsupportedOperationException();
		}
	}

	public static class Wrapper {
		public final Object value;

		public Wrapper(Object s) {
			this.value = s;
		}
	}

	final Map<String, Object>	map	= new HashMap<>();
	final Class<T>				type;
	final T						proxy;

	Method						lastUsedMethod;

	@SuppressWarnings("unchecked")
	public AnnotationSetter(Class<T> type) {
		this.type = type;
		proxy = (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[] {
			type
		}, new InvocationHandler() {

			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				if (method.equals(TO_STRING)) {
					return this.toString();
				}

				lastUsedMethod = method;
				return Converter.cnv(method.getGenericReturnType(), null);
			}

			@Override
			public String toString() {
				StringBuilder sb = new StringBuilder();

				sb.append("@")
					.append(getFQN(type));
				String del = "(";
				for (Map.Entry<String, Object> e : map.entrySet()) {
					sb.append(del);
					sb.append(e.getKey())
						.append("=");
					print(sb, e.getValue());
					del = ",";
				}
				if (!del.equals("("))
					sb.append(")");

				return sb.toString();
			}

			private String getFQN(Class<?> type) {
				Class<?> enclosingClass = type.getEnclosingClass();
				if (enclosingClass == null)
					return type.getName();
				else
					return getFQN(enclosingClass) + "." + type.getSimpleName();
			}

			private void print(StringBuilder sb, Object value) {
				if (value instanceof String) {
					sb.append("\"");
					escape(sb, (String) value);
					sb.append("\"");

				} else if (value.getClass()
					.isArray()) {
					int length = Array.getLength(value);
					if (length != 1)
						sb.append("{");
					String del = "";
					for (int i = 0; i < length; i++) {
						sb.append(del);
						Object o = Array.get(value, i);
						print(sb, o);
						del = ",";
					}
					if (length != 1)
						sb.append("}");
				} else if (value instanceof Wrapper) {
					sb.append(((Wrapper) value).value);
				} else if (value instanceof Enum) {
					String name = ((Enum<?>) value).name();
					sb.append(getFQN(value.getClass()))
						.append(".")
						.append(name);
				} else {
					sb.append(value);
				}
			}
		});
	}

	public <X> AnnotationSetter<T> set(X x, X value) {
		if (value != null) {
			assert lastUsedMethod != null;
			Object v;
			if (lastUsedMethod.getReturnType() == Class.class)
				v = new Wrapper(value);
			else if (lastUsedMethod.getReturnType() == Class[].class) {
				Object[] older = (Object[]) value;
				Wrapper[] newer = new Wrapper[older.length];
				for (int i = 0; i < older.length; i++) {
					newer[i] = new Wrapper(older[i]);
				}
				v = newer;
			} else {
				try {
					v = Converter.cnv(lastUsedMethod.getGenericReturnType(), value);
				} catch (Exception e) {
					v = value;
				}
			}
			Object lastDefault = lastUsedMethod.getDefaultValue();
			if (lastDefault == null || !equals(lastDefault, value)) {
				map.put(lastUsedMethod.getName(), v);
			}
			lastUsedMethod = null;

		}
		return this;
	}

	private boolean equals(Object a, Object b) {
		if (Objects.deepEquals(a, b))
			return true;

		return false;
	}

	public T a() {
		return proxy;

	}

	private void escape(StringBuilder sb, String value) {
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
				case '\t' :
					sb.append("\\t");
					break;
				case '\r' :
					sb.append("\\r");
					break;
				case '\b' :
					sb.append("\\b");
					break;
				case '\n' :
					sb.append("\\n");
					break;
				case '\f' :
					sb.append("\\f");
					break;
				case '\\' :
					sb.append("\\\\");
					break;
				case '\"' :
					sb.append("\\\"");
					break;
				default :
					if (c <= 0x20 || c >= 0x7E) {
						sb.append("\\u");
						String hexString = Integer.toHexString(c);
						for (int j = hexString.length(); j < 4; j++)
							sb.append("0");
						sb.append(hexString);
					} else
						sb.append(c);
					break;
			}
		}
	}
}
