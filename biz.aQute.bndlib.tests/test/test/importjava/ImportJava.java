package test.importjava;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class ImportJava {

	public ImportJava() {
		Collection<Object> coll = new ArrayList<>();

		Method[] methods = coll.getClass()
			.getMethods();

		Arrays.stream(methods)
			.forEach(m -> System.out.printf("Method %s%n", m));
	}

}
