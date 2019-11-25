package aQute.tester.bundle.engine;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.opentest4j.TestAbortedException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import aQute.bnd.annotation.Resolution;
import aQute.bnd.annotation.spi.ServiceProvider;
import aQute.tester.bundle.engine.discovery.BundleSelectorResolver;

@ServiceProvider(value = TestEngine.class, resolution = Resolution.OPTIONAL)
public class BundleEngine implements TestEngine {

	public static final String				CHECK_UNRESOLVED	= "aQute.bnd.junit.bundle.engine.checkUnresolved";

	public static final String				ENGINE_ID			= "bnd-bundle-engine";

	private final Optional<BundleContext>	context;

	public BundleEngine() {
		context = Optional.ofNullable(FrameworkUtil.getBundle(BundleEngine.class))
			.map(Bundle::getBundleContext);
	}

	@Override
	public String getId() {
		return ENGINE_ID;
	}

	@Override
	public Optional<String> getGroupId() {
		return Optional.of("biz.aQute.bnd");
	}

	@Override
	public Optional<String> getArtifactId() {
		return Optional.of("biz.aQute.tester.junit-platform");
	}

	@Override
	public TestDescriptor discover(EngineDiscoveryRequest request, UniqueId uniqueId) {
		BundleEngineDescriptor descriptor = new BundleEngineDescriptor(uniqueId);

		if (!context.isPresent()) {
			descriptor.addChild(new StaticFailureDescriptor(uniqueId.append("test", "noFramework"), "Initialization Error",
				new JUnitException("BundleEngine must run inside an OSGi framework")));
			return descriptor;
		}
		BundleSelectorResolver.resolve(context.get(), request, descriptor);
		return descriptor;
	}

	@Override
	public void execute(ExecutionRequest request) {
		final TestDescriptor root = request.getRootTestDescriptor();
		final EngineExecutionListener listener = request.getEngineExecutionListener();
		final ConfigurationParameters params = request.getConfigurationParameters();

		Preconditions.condition(root instanceof BundleEngineDescriptor,
			"Root descriptor should be an instance of BundleEngineDescriptor, was " + root.getClass());
		Preconditions.condition(root.getChildren()
			.stream()
			.allMatch(descriptor -> descriptor instanceof BundleDescriptor || descriptor instanceof StaticFailureDescriptor),
			"Child descriptors should all be BundleDescriptors or StaticFailureDescriptors");
		listener.executionStarted(root);
		try {
			Optional<StaticFailureDescriptor> staticFailureDescriptor = root.getChildren()
				.stream()
				.filter(StaticFailureDescriptor.class::isInstance)
				.map(StaticFailureDescriptor.class::cast)
				.peek(childDescriptor -> childDescriptor.execute(listener))
				.reduce((first, second) -> first);

			Stream<BundleDescriptor> childDescriptors = root.getChildren()
				.stream()
				.filter(BundleDescriptor.class::isInstance)
				.map(BundleDescriptor.class::cast);
			if (staticFailureDescriptor.isPresent()) {
				String reason = staticFailureDescriptor.map(StaticFailureDescriptor::getError)
					.map(Throwable::getMessage)
					.orElseGet(() -> staticFailureDescriptor.get()
						.getDisplayName());
				childDescriptors.forEach(childDescriptor -> listener.executionSkipped(childDescriptor, reason));
			} else {
				childDescriptors
					.forEach(childDescriptor -> executeBundle(childDescriptor, listener, params));
			}
			listener.executionFinished(root, TestExecutionResult.successful());
		} catch (Throwable t) {
			System.err.println("Unrecoverable error while executing tests: " + t);
			t.printStackTrace(System.err);
			listener.executionFinished(root, TestExecutionResult.failed(t));
		}
	}

	private static void executeBundle(BundleDescriptor descriptor, EngineExecutionListener listener,
		ConfigurationParameters params) {
		listener.executionStarted(descriptor);
		TestExecutionResult result;
		if (descriptor.getException() == null) {
			try {
				descriptor.getChildren()
					.stream()
					.filter(childDescriptor -> !(childDescriptor instanceof BundleDescriptor
						|| childDescriptor instanceof StaticFailureDescriptor))
					.forEach(childDescriptor -> descriptor.executeChild(childDescriptor, listener, params));
				result = TestExecutionResult.successful();
			} catch (TestAbortedException abort) {
				result = TestExecutionResult.aborted(abort);
			} catch (OutOfMemoryError | StackOverflowError t) {
				throw t;
			} catch (Throwable t) {
				result = TestExecutionResult.failed(t);
			}
			descriptor.getChildren()
				.stream()
				.filter(BundleDescriptor.class::isInstance)
				.map(BundleDescriptor.class::cast)
				.forEach(childDescriptor -> executeBundle(childDescriptor, listener, params));
			descriptor.getChildren()
				.stream()
				.filter(StaticFailureDescriptor.class::isInstance)
				.map(StaticFailureDescriptor.class::cast)
				.forEach(childDescriptor -> childDescriptor.execute(listener));
		} else {
			result = TestExecutionResult.failed(descriptor.getException());
			descriptor.getChildren()
				.forEach(childDescriptor -> {
					listener.executionSkipped(childDescriptor, "Bundle did not resolve");
				});
		}
		listener.executionFinished(descriptor, result);
	}
}
