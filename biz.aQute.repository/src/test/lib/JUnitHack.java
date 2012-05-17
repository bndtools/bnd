package test.lib;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class JUnitHack {

	public static int countTests(Class<?> clazz) {
		int count = 0;
		Method[] methods = clazz.getDeclaredMethods();
		for (Method method : methods) {
			if (method.getName().startsWith("test")) {
				int mods = method.getModifiers();
				if (Modifier.isPublic(mods) && !Modifier.isAbstract(mods))
					count++;
			}
		}
		return count;
	}

}
