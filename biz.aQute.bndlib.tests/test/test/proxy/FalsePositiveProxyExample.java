package test.proxy;

import java.lang.reflect.Proxy;

public class FalsePositiveProxyExample {
	public void tricky() {
		// Create a Class[] array, but NOT for proxy
		Class<?>[] arr = new Class<?>[1];
		arr[0] = javax.servlet.ServletContext.class; // ldc(ServletContext.class),
														// aastore
		doSomething(arr);

		// Later, completely unrelated call to Proxy.newProxyInstance,
		// but the Class[] is built elsewhere, not inline.
		Proxy.newProxyInstance(getClass().getClassLoader(),
			// comes from a variable, not the inline array above
			getInterfaces(), (proxy, method, args) -> null);
	}

	private Class<?>[] getInterfaces() {
		// Constructed in a different method — analyzer cannot see contents
		return new Class<?>[] {
			Runnable.class
		};
	}

	private void doSomething(Class<?>[] a) {
		// no-op
	}
}
