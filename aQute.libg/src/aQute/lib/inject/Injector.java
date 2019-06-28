package aQute.lib.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import aQute.lib.converter.Converter;

/**
 * This class is a simple injector with a parameterized annotation. A domain
 * function is provided to retrieve the values.
 *
 * @param <T> the annotation type
 */
public class Injector<T extends Annotation> {

	final Converter						converter;
	final Class<T>						annotation;
	final Function<Target<T>, Object>	domain;

	/**
	 * The Target class describes the target injection point.
	 *
	 * @param <T> the annotation type
	 */
	public static class Target<T> {
		/**
		 * The passed annotation for this Injector
		 */
		public T		annotation;
		/**
		 * The member to be injected, either a Method or Field
		 */
		public Member	member;
		/**
		 * The primary type. This is the type of the first argument of a method
		 * or the type of a field
		 */
		public Type		primaryType;

		/**
		 * The type for which to get a value
		 */
		public Type		type;
		/**
		 * The target object being injected
		 */
		public Object	target;

		@Override
		public String toString() {
			return "Target [" + (annotation != null ? "annotation=" + annotation + ", " : "")
				+ (member != null ? "member=" + member + ", " : "")
				+ (primaryType != null ? "primaryType=" + primaryType + ", " : "")
				+ (type != null ? "type=" + type + ", " : "") + (target != null ? "target=" + target : "") + "]";
		}
	}

	/**
	 * Create a new Injector
	 *
	 * @param converter the converter to use for conversions
	 * @param domain the domain function that retrieves values
	 * @param annotation the annotation that triggers a call to the domain
	 */
	public Injector(Converter converter, Function<Target<T>, Object> domain, Class<T> annotation) {
		this.converter = converter;
		this.domain = domain;
		this.annotation = annotation;
	}

	/**
	 * Create a new Injector with a default converter
	 *
	 * @param domain the domain function that retrieves values
	 * @param annotation the annotation that triggers a call to the domain
	 */
	public Injector(Function<Target<T>, Object> domain, Class<T> annotation) {
		this(new Converter(), domain, annotation);
	}

	/**
	 * Inject an object. This will inject fields and methods. Methods must have
	 * one or more arguments. The first argument is special and is always passed
	 * to the domain function as the primaryType.
	 *
	 * @param target the target object to inject
	 */
	public void inject(Object target) throws Exception {
		Class<?> clazz = target.getClass();

		Target<T> param = new Target<>();

		for (Field field : getFields(clazz)) {

			param.annotation = field.getAnnotation(this.annotation);
			if (param.annotation != null) {

				param.target = target;
				param.member = field;
				param.primaryType = field.getGenericType();
				param.type = param.primaryType;
				field.setAccessible(true);
				field.set(target, getValue(param));
			}
		}

		for (Method method : getMethods(clazz)) {
			if (method.getParameterTypes().length > 0) {

				param.annotation = method.getAnnotation(this.annotation);
				if (param.annotation != null) {

					Object values[] = invoke(target, param, method);
					method.invoke(target, values);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public <F> F newInstance(Class<F> type) throws Exception {
		Target<T> param = new Target<>();

		for (Constructor<?> c : type.getDeclaredConstructors()) {
			param.annotation = c.getAnnotation(this.annotation);
			if (param.annotation != null) {
				if (c.getParameterTypes().length == 0)
					return (F) c.newInstance();

				Object[] values = invoke(null, param, c);
				F newInstance = (F) c.newInstance(values);
				inject(newInstance);
				return newInstance;
			}
		}

		Constructor<F> f = type.getDeclaredConstructor();
		f.setAccessible(true);
		F newInstance = f.newInstance();
		inject(newInstance);
		return newInstance;
	}

	private Collection<Field> getFields(Class<?> clazz) {
		return getAbove(clazz).stream()
			.map(Class::getDeclaredFields)
			.flatMap(Stream::of)
			.collect(Collectors.toSet());
	}

	private Collection<Method> getMethods(Class<?> clazz) {
		return getAbove(clazz).stream()
			.map(Class::getDeclaredMethods)
			.flatMap(Stream::of)
			.collect(Collectors.toSet());
	}

	private List<Class<?>> getAbove(Class<?> clazz) {
		if (clazz == Object.class)
			return new ArrayList<>();

		List<Class<?>> result = getAbove(clazz.getSuperclass());
		result.add(clazz);
		return result;
	}

	private Object getValue(Target<T> param) throws Exception {
		return converter.convert(param.type, domain.apply(param));
	}

	private Object[] invoke(Object target, Target<T> param, Executable method)
		throws Exception, IllegalAccessException, InvocationTargetException {
		Type[] parameters = method.getGenericParameterTypes();
		Object[] values = new Object[parameters.length];
		for (int i = 0; i < parameters.length; i++) {

			param.target = target;
			param.member = method;
			param.primaryType = parameters[0];
			param.type = parameters[i];

			values[i] = getValue(param);
		}

		method.setAccessible(true);
		return values;
	}
}
