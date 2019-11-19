package aQute.tester.test.utils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;

import aQute.launchpad.BundleBuilder;
import aQute.launchpad.BundleSpecBuilder;
import aQute.launchpad.Launchpad;
import aQute.lib.exceptions.Exceptions;

/**
 * Utility class for reflectively accessing class definitions inside a bundle.
 * <p>
 * Testing the tester requires test classes to feed it. The tests in this
 * project have the feeder test classes in the src directory and they are
 * accessible on the classpath at runtime, but in order to create a true test
 * they are copied into the framework-under-test rather than imported from the
 * framework classloader. This utility class allows you to reflectively access
 * the class objects inside the framework.
 *
 * @author Fr Jeremy Krieg (fr.jkrieg@greekwelfaresa.org.au)
 */
public class TestBundler {

	final Launchpad			lp;

	Map<Class<?>, Class<?>>	classMap	= new HashMap<>();
	Map<Class<?>, Bundle>	bundleMap	= new HashMap<>();

	public TestBundler(Launchpad lp) {
		this.lp = lp;
	}

	public Bundle startTestBundle(Class<?>... testClasses) {
		return buildTestBundle(testClasses).start();
	}

	public Bundle installTestBundle(Class<?>... testClasses) throws Exception {
		return buildTestBundle(testClasses).install();
	}

	public BundleSpecBuilder buildTestBundle(Class<?>... testClasses) {
		BundleBuilder underlying = lp.bundle();
		BundleSpecBuilder bb = new BundleSpecBuilder() {
			@Override
			public BundleBuilder x() {
				return underlying;
			}

			@Override
			public Bundle install() throws Exception {
				Bundle retval = BundleSpecBuilder.super.install();
				Stream.of(testClasses)
					.forEach(x -> bundleMap.put(x, retval));
				return retval;
			}
		};
		final StringBuilder sb = new StringBuilder(128);
		boolean first = true;
		for (Class<?> testClass : testClasses) {
			if (first) {
				first = false;
			} else {
				sb.append(',');
			}
			sb.append(testClass.getName());
			bb.addResourceWithCopy(testClass);
		}
		System.err.println("Creating bundle: " + bb + ", " + sb);
		withEE(bb.header("Test-Cases", sb.toString()));
		// Don't chain the return value of withEE in this case - we need to
		// return actual custom instance above
		return bb;
	}

	public static BundleSpecBuilder withEE(BundleSpecBuilder bb) {
		return bb.requireCapability(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE)
			.filter("(&(osgi.ee=JavaSE)(version=1.8))");
	}

	public BundleSpecBuilder bundleWithEE() {
		return withEE(lp.bundle());

	}

	public Bundle getBundleOf(Class<?> testClass) {
		return bundleMap.get(testClass);
	}

	public Class<?> getBundleClass(Class<?> testClass) {
		Bundle b = bundleMap.get(testClass);
		try {
			return b.loadClass(testClass.getName());
		} catch (ClassNotFoundException e) {
			throw Exceptions.duck(e);
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T getStatic(Class<?> testClass, String field) {
		try {
			Field f = getBundleClass(testClass).getField(field);
			f.setAccessible(true);
			return (T) f.get(null);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * Convenience version that allows you to avoid explicit cast (neater for
	 * chaining).
	 */
	@SuppressWarnings("unchecked")
	public <T> T getStatic(Class<?> testClass, Class<T> fieldType, String field) {
		try {
			Field f = getBundleClass(testClass).getField(field);
			f.setAccessible(true);
			return (T) f.get(null);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	// Convenience wrappers for common fields in the test classes
	@SuppressWarnings("unchecked")
	public BundleContext getBundleContext(Class<?> testClass) {
		return ((AtomicReference<BundleContext>) getStatic(testClass, "bundleContext")).get();
	}

	@SuppressWarnings("unchecked")
	public BundleContext getActualBundleContext(Class<?> testClass) {
		return ((AtomicReference<BundleContext>) getStatic(testClass, "actualBundleContext")).get();
	}

	@SuppressWarnings("unchecked")
	public Thread getCurrentThread(Class<?> testClass) {
		return ((AtomicReference<Thread>) getStatic(testClass, "currentThread")).get();
	}

	public int getInteger(String fieldName, Class<?> testClass) {
		return ((AtomicInteger) getStatic(testClass, fieldName)).get();
	}

	public boolean getFlag(Class<?> testClass, String flag) {
		return ((AtomicBoolean) getStatic(testClass, flag)).get();
	}
}
