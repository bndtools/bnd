package biz.aQute.bnd.reporter.matcher;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import aQute.lib.converter.Converter;

public class IsDTODeepEquals extends BaseMatcher<Object> {

	private Converter	converter	= new Converter();
	private Object		expected;
	private String		failurePath	= "";
	private String		failureExpectedValue;
	private String		failureActualValue;

	public IsDTODeepEquals(Object expected) {
		this.expected = expected;
	}

	static public IsDTODeepEquals deepEqualsTo(Object expected) {
		return new IsDTODeepEquals(expected);
	}

	@Override
	public void describeTo(Description description) {
		description.appendValue(failureExpectedValue);

		if (!failurePath.isEmpty()) {
			description.appendText(" at path <" + failurePath + ">");
		}
	}

	@Override
	public void describeMismatch(Object item, Description description) {
		description.appendText(
			"found ")
			.appendValue(failureActualValue);
	}

	@Override
	public boolean matches(Object actual) {
		try {
			anySameAs(expected, actual, "");
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}

	private void anySameAs(Object expected, Object actual, String path) {
		if (expected == null) {
			nullSameAs(actual, path);
		} else if (!isComplex(expected)) {
			primitiveSameAs(expected, actual, path);
		} else if (expected.getClass()
			.isArray()) {
			arraySameAs((Object[]) expected, actual, path);
		} else if (expected instanceof Map) {
			mapLikeSameAs(expected, actual, path);
		} else if (expected instanceof List) {
			listSameAs((List<?>) expected, actual, path);
		} else if (expected instanceof Set) {
			setSameAs((Set<?>) expected, actual, path);
		} else {
			mapLikeSameAs(expected, actual, path);
		}
	}

	private void arraySameAs(Object[] expected, Object actual, String path) {
		fail(() -> actual
			.getClass()
			.isArray(), expected, actual, path);

		Object[] actualArray = (Object[]) actual;

		fail(() -> expected.length == actualArray.length, expected, actualArray, path);

		for (int i = 0; i < expected.length; i++) {
			anySameAs(expected[i], actualArray[i], path + "." + i);
		}
	}

	private void listSameAs(List<?> expected, Object actual, String path) {
		fail(() -> actual instanceof List, expected, actual, path);

		List<?> actualList = (List<?>) actual;
		Object[] expectedArray = expected.toArray();

		fail(() -> expected.size() == actualList.size(), expected, actualList, path);

		for (int i = 0; i < expected.size(); i++) {
			anySameAs(expected.get(i), actualList.get(i), path + "." + i);
		}
	}

	private void setSameAs(Set<?> expected, Object actual, String path) {
		fail(() -> actual instanceof Set, expected, actual, path);

		Set<?> actualSet = (Set<?>) actual;
		Object[] expectedArray = expected.toArray();

		fail(() -> expected.size() == actualSet.size(), expected, actualSet, path);

		for (int i = 0; i < expectedArray.length; i++) {
			if (!actualSet.contains(expectedArray[i])) {
				fail(expected, actualSet, path + "." + i);
			}
		}
	}

	private void primitiveSameAs(Object expected, Object actual, String path) {
		fail(() -> expected.equals(actual), expected, actual, path);
	}

	private void nullSameAs(Object actual, String path) {
		fail(() -> actual == null, null, actual, path);
	}

	private void mapLikeSameAs(Object expected, Object actual, String path) {
		fail(() -> isMapLike(actual), expected, actual, path);

		try {
			mapSameAsMap(converter.toMap(expected), converter.toMap(actual), path);
		} catch (Exception e) {
			throw new RuntimeException();
		}
	}

	private void mapSameAsMap(Map<?, ?> expected, Map<?, ?> actual, String path) {
		fail(() -> expected.size() == actual.size(), expected, actual, path);

		expected.keySet()
			.forEach(k -> {
				anySameAs(expected.get(k), actual.get(k), path + "." + k);
			});
	}

	private boolean isComplex(Object a) {
		return a instanceof Map || a instanceof Collection || a.getClass()
			.isArray() || getFields(a).length > 0;
	}

	private boolean isMapLike(Object a) {
		return a instanceof Map || getFields(a).length > 0;
	}

	Field[] getFields(Object o) {
		return getFields(o.getClass());
	}

	private Field[] getFields(Class<?> c) {
		List<Field> publicFields = new ArrayList<>();

		for (Field field : c.getFields()) {
			if (field.isEnumConstant() || field.isSynthetic() || Modifier.isStatic(field.getModifiers()))
				continue;
			publicFields.add(field);
		}
		Collections.sort(publicFields, new Comparator<Field>() {

			@Override
			public int compare(Field o1, Field o2) {
				return o1.getName()
					.compareTo(o2.getName());
			}
		});

		return publicFields.toArray(new Field[publicFields.size()]);
	}

	private void fail(Object expected, Object actual, String path) {
		failurePath = path.isEmpty() ? path : path.substring(1);
		failureExpectedValue = expected != null ? expected.toString() : "null";
		failureActualValue = actual != null ? actual.toString() : "null";
		throw new RuntimeException();
	}

	private void fail(Supplier<Boolean> condition, Object expected, Object actual, String path) {
		if (!condition.get()) {
			fail(expected, actual, path);
		}
	}
}
