package aQute.bnd.classfile.builder;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import aQute.bnd.classfile.Attribute;
import aQute.bnd.classfile.FieldInfo;
import aQute.bnd.classfile.MethodInfo;

/**
 * In some cases it is necessary to have a ClassFile for a built in Java class.
 * This builder will create a reflect builder that is based on a Class object.
 * Unfortunately, restrictions in the Java API make it impossible to get all the
 * details. For example, there are no code attributes.
 * <p>
 * This is far from complete but it can be used in analysis cases. All
 * information about the types of methods and fields are captured.
 */
public class ReflectBuilder {
	final static int	javaSpecificationVersion	= Integer
		.parseInt(System.getProperty("java.specification.version"));
	final static int	major_version				= javaSpecificationVersion + 44;

	/**
	 * Create a ClassFileBuilder based on the given class
	 *
	 * @param c the given class
	 * @return a ClassFileBuilder initialized with the class
	 */
	public static ClassFileBuilder of(Class<?> c) {
		ClassFileBuilder cfb = new ClassFileBuilder(c.getModifiers(), major_version, 0, toName(c),
			toName(c.getSuperclass()), Stream.of(c.getInterfaces())
				.map(ReflectBuilder::toName)
				.collect(Collectors.toSet()));

		for (Field f : c.getDeclaredFields()) {
			FieldInfo fi = new FieldInfo(f.getModifiers(), f.getName(), descriptor(f, f.getType()), attributes(f));
			cfb.fields(fi);
		}

		for (Method m : c.getDeclaredMethods()) {
			MethodInfo mi = new MethodInfo(m.getModifiers(), m.getName(),
				descriptor(m, m.getReturnType(), m.getParameterTypes()), attributes(m));
			cfb.methods(mi);
		}

		cfb.attributes(attributes(c));

		return cfb;
	}

	private static Attribute[] attributes(Object f) {
		return new Attribute[0];
	}

	@SuppressWarnings("rawtypes")
	private static String descriptor(Member m, Class<?> type, Class... parameters) {
		StringBuilder sb = new StringBuilder();
		if (m instanceof Method) {
			sb.append("(");
			for (Class p : parameters) {
				encode(p);
			}
			sb.append(")");
		}
		sb.append(encode(type));
		return sb.toString();
	}

	private static String encode(Class<?> type) {
		if (type == void.class)
			return "V";
		if (type == boolean.class)
			return "Z";
		if (type == byte.class)
			return "B";
		if (type == short.class)
			return "S";
		if (type == char.class)
			return "C";
		if (type == int.class)
			return "I";
		if (type == long.class)
			return "J";
		if (type == float.class)
			return "F";
		if (type == double.class)
			return "D";

		return "L".concat(toName(type))
			.concat(";");
	}

	private static String toName(Class<?> c) {
		if (c == null)
			return null;
		return c.getName();
	}
}
