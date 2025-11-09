package test.proxy;

import java.lang.reflect.Proxy;
import javax.servlet.ServletContext;

public class ProxyTest {

	public static void main(String[] args) {
		// ServletContext has methods that reference types from javax.servlet.descriptor
		ServletContext proxy = (ServletContext) Proxy.newProxyInstance(
			ProxyTest.class.getClassLoader(),
			new Class<?>[] { ServletContext.class },
			(proxy1, method, args1) -> null
		);
		System.err.println(proxy);
	}
}
