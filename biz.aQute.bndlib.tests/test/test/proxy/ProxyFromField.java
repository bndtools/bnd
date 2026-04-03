package test.proxy;

import java.lang.reflect.Proxy;
import javax.servlet.ServletContext;

/**
 * Test that we don't incorrectly detect proxy interfaces when the Class[] array
 * comes from a static field rather than being created inline.
 */
public class ProxyFromField {

	private static final Class<?>[] INTERFACES = new Class<?>[] { ServletContext.class };

	public static void main(String[] args) {
		ServletContext proxy = (ServletContext) Proxy.newProxyInstance(
			ProxyFromField.class.getClassLoader(),
			INTERFACES,  // Array from field - we cannot detect interfaces reliably
			(proxy1, method, args1) -> null
		);
		System.err.println(proxy);
	}
}
