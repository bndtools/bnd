package aQute.junit.runtime;

import java.text.MessageFormat;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

public abstract class OSGiTestCase extends TestCase {
	/**
	 * Returns the {@link BundleContext} for the current class. This method
	 * should be called by subclasses as their point of entry into the OSGi
	 * Framework, but it may return {@code null} if the class is not associated
	 * by a bundle -- for example, if the Test is executed outside of an OSGi
	 * Framework.
	 *
	 * @return The {@link BundleContext} of the receiver, or {@code
		 * null} if the
	 */
	protected BundleContext getBundleContext() {
		Bundle bundle = FrameworkUtil.getBundle(this.getClass());
		return bundle != null ? bundle.getBundleContext() : null;
	}

	/**
	 * Asserts that at least one service of the specified type is currently
	 * available. If not, an {@link AssertionFailedError} is thrown.
	 *
	 * @param service The service interface type.
	 * @param filter An additional service filter, which may be {@code null}.
	 */
	protected void assertSvcAvail(Class<?> service, String filter) {
		assertSvcAvail(null, service, filter);
	}

	/**
	 * Asserts that at least one service of the specified type is currently
	 * available. If not, an {@link AssertionFailedError} is thrown with the
	 * given message.
	 *
	 * @param message
	 * @param service The service interface type.
	 * @param filter An additional service filter, which may be {@code
		 * null}.
	 */
	protected void assertSvcAvail(String message, Class<?> service, String filter) {
		BundleContext context = getBundleContext();
		ServiceReference<?>[] refs = null;
		try {
			refs = context.getServiceReferences(service.getName(), filter);
		} catch (InvalidSyntaxException e) {
			fail("Invalid filter syntax");
		}

		if (refs == null || refs.length == 0) {
			fail(message);
			return;
		}

		Object svcObj = context.getService(refs[0]);
		if (svcObj == null)
			fail(message);

		try {
			if (!service.isInstance(svcObj))
				fail(message);
		} finally {
			context.ungetService(refs[0]);
		}
	}

	/**
	 * <p>
	 * Perform the specified operation against a service, or fail immediately if
	 * a matching service is not available.
	 * </p>
	 * <p>
	 * <strong>Example:</strong>
	 * </p>
	 * <p>
	 * <strong>Example:</strong>
	 * </p>
	 *
	 * <pre>
	 * String reply = withService(HelloService.class, null, new Operation&lt;HelloService, String&gt;() {
	 * 	public String call(HelloService service) {
	 * 		return service.sayHello();
	 * 	}
	 * });
	 * </pre>
	 *
	 * @param <S> The service type.
	 * @param <R> The result type.
	 * @param service The service class.
	 * @param filter An additional filter expression, or {@code
	 * null}.
	 * @param operation The operation to perform against the service.
	 * @throws Exception
	 */
	protected <S, R> R withService(Class<S> service, String filter, Operation<? super S, R> operation)
		throws Exception {
		return withService(service, filter, 0, operation);
	}

	/**
	 * <p>
	 * Perform the specified operation against a service, if available.
	 * </p>
	 * <p>
	 * <strong>Example:</strong>
	 * </p>
	 *
	 * <pre>
	 * String reply = withService(HelloService.class, null, 0, new Operation&lt;HelloService, String&gt;() {
	 * 	public String call(HelloService service) {
	 * 		return service.sayHello();
	 * 	}
	 * });
	 * </pre>
	 *
	 * @param <S> The service type.
	 * @param <R> The result type.
	 * @param service The service class.
	 * @param filter An additional filter expression, or {@code
	 * null}.
	 * @param timeout The maximum time to wait (in ms) for a service to become
	 *            available; a zero or negative timeout implies we should fail
	 *            if the service is not immediatelt available.
	 * @param operation The operation to perform against the service.
	 * @throws Exception
	 */
	protected <S, R> R withService(Class<S> service, String filter, long timeout, Operation<? super S, R> operation)
		throws Exception {
		BundleContext context = getBundleContext();

		ServiceTracker<S, R> tracker = null;
		if (filter != null) {
			try {
				Filter combined = FrameworkUtil
					.createFilter("(" + Constants.OBJECTCLASS + "=" + service.getName() + ")");
				tracker = new ServiceTracker<>(context, combined, null);
			} catch (InvalidSyntaxException e) {
				fail("Invalid filter syntax.");
				return null;
			}
		} else {
			tracker = new ServiceTracker<>(context, service.getName(), null);
		}
		try {
			tracker.open();
			Object instance;
			if (timeout <= 0) {
				instance = tracker.getService();
			} else {
				instance = tracker.waitForService(timeout);
			}

			if (instance == null || !service.isInstance(instance))
				fail(MessageFormat.format("Service \"{0}\" not available.", service.getName()));

			@SuppressWarnings("unchecked")
			S casted = (S) instance;
			return operation.perform(casted);
		} catch (InterruptedException e) {
			fail("Interrupted.");
		} finally {
			tracker.close();
		}

		// unreachable
		return null;
	}

	/**
	 * Default wait timeout is 10 seconds
	 */
	public static long DEFAULT_TIMEOUT = 10000;

}
