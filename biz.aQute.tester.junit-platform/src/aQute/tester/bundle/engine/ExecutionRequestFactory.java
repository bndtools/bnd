package aQute.tester.bundle.engine;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;

/**
 * Factory for creating ExecutionRequest instances that are compatible with both
 * JUnit Platform < 1.13 and >= 1.13. In JUnit Platform 1.13+, the internal
 * NamespacedHierarchicalStore must be properly propagated to child execution
 * requests. This class use reflection magic, but unfortunately this is
 * necessary to support wide range of runtime JUnit version where we need to use
 * internal API which drastically changes between versions.
 */
public class ExecutionRequestFactory {

	private static final boolean	USE_REFLECTION;
	/**
	 * Reference to the getStore() method (new in 1.13+)
	 */
	private static final Method		GET_STORE_METHOD;

	/**
	 * reference to static create() methods with 3-arguments
	 * org.junit.platform.engine.ExecutionRequest.create(TestDescriptor,
	 * EngineExecutionListener, ConfigurationParameters)
	 */
	static final Method CREATE_3;

	/**
	 * reference to static create() methods with 5-arguments
	 * org.junit.platform.engine.ExecutionRequest.create(TestDescriptor,
	 * EngineExecutionListener, ConfigurationParameters)
	 */
	static final Method CREATE_5;


	static {
		boolean useReflection = false;
		Method getStoreMethod = null;
		Method c3 = null, c5 = null;

		try {

			// Look for 5-parameter create() method (1.13+): descriptor,
			// listener, params, outputProvider, store
			// or 4-parameter constructor (earlier versions): descriptor,
			// listener, params, store
		    for (Method m : ExecutionRequest.class.getDeclaredMethods()) {
		        if (!m.getName().equals("create")
		            || !Modifier.isStatic(m.getModifiers())
		            || m.getReturnType() != ExecutionRequest.class) {
		            continue;
		        }

		        Class<?>[] p = m.getParameterTypes();

		        if (p.length == 3
		            && p[0] == TestDescriptor.class
		            && p[1] == EngineExecutionListener.class
		            && p[2] == ConfigurationParameters.class) {
					// this is the deprecated 3-arg
		            c3 = m;
		        }
		        else if (p.length == 5
		            && p[0] == TestDescriptor.class
		            && p[1] == EngineExecutionListener.class
		            && p[2] == ConfigurationParameters.class) {
					// 5-arg version
		            c5 = m;
		        }
		    }


			// Try to access the internal Store from ExecutionRequest
			// This accesses JUnit Platform's internal API which is necessary for
			// 1.13+ compatibility.
			// The Store class and getStore() method are internal implementation
			// details
			// that may change in future versions, but are required to properly
			// propagate
			// execution context to nested test engines.
			getStoreMethod = ExecutionRequest.class.getDeclaredMethod("getStore");
			getStoreMethod.setAccessible(true);

			// Get the Store class from the method's return type
			Class<?> storeClass = getStoreMethod.getReturnType();

			// Only use reflection if we found both the getter one of the create methods
			useReflection = (getStoreMethod != null && (c3 != null || c5 != null));
		} catch (Exception e) {
			// Reflection not available or not needed, fall back to public
			// constructor
		}

		USE_REFLECTION = useReflection;
		GET_STORE_METHOD = getStoreMethod;
		CREATE_3 = c3;
	    CREATE_5 = c5;

	}

	/**
	 * Create a new ExecutionRequest for a child engine, properly propagating
	 * the execution context from the parent request.
	 *
	 * @param descriptor The root test descriptor for the child engine
	 * @param parentRequest The parent execution request to derive context from
	 * @return A new ExecutionRequest for the child engine
	 */
	static ExecutionRequest createChildRequest(TestDescriptor descriptor, ExecutionRequest parentRequest) {
		EngineExecutionListener listener = parentRequest.getEngineExecutionListener();
		ConfigurationParameters params = parentRequest.getConfigurationParameters();

		if (USE_REFLECTION) {
			try {
				// Get the Store from the parent request
				Object store = GET_STORE_METHOD.invoke(parentRequest);

				// Create a new ExecutionRequest with the Store

				if (CREATE_5 != null) {
					// JUnit Platform 1.13+: need to pass
					// OutputDirectoryProvider as 4th param
					// Get the OutputDirectoryProvider from parent request
					// TODO from 1.14+ getOutputDirectoryCreator() replaces
					// "getOutputDirectoryProvider". So we may have to adopt
					// that in the future
					Method getOutputProvider = ExecutionRequest.class.getDeclaredMethod("getOutputDirectoryProvider");
					getOutputProvider.setAccessible(true);
					Object outputProvider = getOutputProvider.invoke(parentRequest);

					return (ExecutionRequest) CREATE_5.invoke(parentRequest, descriptor, listener, params,
						outputProvider,
						store);
				}
				else {
					// Earlier versions: 3-param static create() method
					return (ExecutionRequest) CREATE_3.invoke(parentRequest, descriptor, listener, params);
				}
			} catch (Exception e) {
				// Fall back to public constructor if reflection fails
				// Using System.err because this is a test framework component that
				// needs
				// to report issues even when no logging framework is available
				System.err.println(
					"Warning: BundleEngine failed to propagate execution context for " + descriptor.getDisplayName()
						+ " using reflection, falling back to public constructor. " + "JUnit Platform extensions may not work correctly. Cause: "
						+ e.getClass()
							.getName()
						+ ": " + e.getMessage());
				e.printStackTrace();
			}
		}

		// Use the public constructor (works for JUnit Platform < 1.13)
		return new ExecutionRequest(descriptor, listener, params);
	}

	/**
	 * Check if this factory is using reflection to propagate execution context.
	 * This is primarily for testing and debugging purposes.
	 *
	 * @return true if reflection is being used, false if using public API only
	 */
	static boolean isUsingReflection() {
		return USE_REFLECTION;
	}
}
