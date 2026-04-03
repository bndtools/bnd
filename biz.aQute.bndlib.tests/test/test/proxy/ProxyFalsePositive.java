package test.proxy;

import java.lang.reflect.Proxy;
import java.util.function.Supplier;
import javax.servlet.ServletContext;

/**
 * Test case to demonstrate a false positive in proxy detection that has been fixed.
 * This class creates a Class[] array for a purpose other than Proxy.newProxyInstance,
 * followed by a lambda (invokedynamic), and then uses newProxyInstance with
 * an array from a field.
 * 
 * Before the fix: ServletContext would be incorrectly associated with the Runnable proxy
 * After the fix: The astore instruction resets the proxy tracking, preventing the false positive
 */
public class ProxyFalsePositive {

	// Array stored in a field - this won't be detected by the inline array pattern
	private static final Class<?>[] RUNNABLE_ARRAY = new Class<?>[] { Runnable.class };

	public static void main(String[] args) {
		// Create a Class[] array with ServletContext (this triggers anewarray, ldc, aastore pattern)
		// This sets inProxyArray=true and adds ServletContext to proxyInterfaces
		Class<?>[] unrelatedClasses = new Class<?>[] { ServletContext.class };
		
		// The astore instruction (storing the array) resets inProxyArray and clears proxyInterfaces
		// This prevents the false positive from occurring
		Supplier<String> lambda = () -> "test";
		lambda.get();
		
		// Now create a proxy using an array from a field (getstatic)
		// After the fix: ServletContext is not incorrectly associated with this proxy
		// because the proxy tracking was reset when the array was stored
		Runnable proxy = (Runnable) Proxy.newProxyInstance(
			ProxyFalsePositive.class.getClassLoader(),
			RUNNABLE_ARRAY,  // From field - not detected by inline pattern
			(proxy1, method, args1) -> null
		);
		
		System.err.println(proxy);
		System.err.println(unrelatedClasses[0]);
	}
}
