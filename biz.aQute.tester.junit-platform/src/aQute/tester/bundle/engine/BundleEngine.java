package aQute.tester.bundle.engine;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
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
import org.junit.platform.engine.support.hierarchical.SingleTestExecutor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import aQute.bnd.annotation.Resolution;
import aQute.bnd.annotation.spi.ServiceProvider;
import aQute.tester.bundle.engine.discovery.BundleSelectorResolver;

@ServiceProvider(value = TestEngine.class, resolution = Resolution.OPTIONAL)
public class BundleEngine implements TestEngine {

	public static final String				CHECK_UNRESOLVED	= "aQute.bnd.junit.bundle.engine.checkUnresolved";

	private static final SingleTestExecutor	singleTestExecutor	= new SingleTestExecutor();

	public static final String				ENGINE_ID			= "bnd-bundle-engine";

	BundleContext							context;

	public BundleEngine() {
		Bundle us = FrameworkUtil.getBundle(BundleEngine.class);

		if (us != null) {
			context = us.getBundleContext();
		}
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
		BundleEngineDescriptor b = new BundleEngineDescriptor(uniqueId);

		if (context == null) {
			b.addChild(new StaticFailureDescriptor(uniqueId.append("test", "noFramework"), "Initialization Error",
				new JUnitException("BundleEngine must run inside an OSGi framework")));
			return b;
		}
		new BundleSelectorResolver(context, request, b).resolve();
		return b;
	}

	@Override
	public void execute(ExecutionRequest request) {
		final TestDescriptor root = request.getRootTestDescriptor();
		final EngineExecutionListener listener = request.getEngineExecutionListener();
		final ConfigurationParameters params = request.getConfigurationParameters();

		Preconditions.condition(root instanceof BundleEngineDescriptor,
			"Root descriptor should be an instance of BundleEngineDescriptor, was " + request.getRootTestDescriptor()
				.getClass());
		Preconditions.condition(root.getChildren()
			.stream()
			.allMatch(x -> x instanceof BundleDescriptor || x instanceof StaticFailureDescriptor),
			"Child descriptors should all be BundleDescriptors or StaticFailureDescriptors");
		try {

			listener.executionStarted(root);

			final AtomicReference<StaticFailureDescriptor> matched = new AtomicReference<>(null);
			root.getChildren()
				.stream()
				.filter(x -> x instanceof StaticFailureDescriptor)
				.map(x -> (StaticFailureDescriptor) x)
				.forEach(x -> {
					matched.set(x);
					x.execute(listener);
				});

			Stream<? extends TestDescriptor> s = root.getChildren()
				.stream()
				.filter(x -> !(x instanceof StaticFailureDescriptor));
			if (matched.get() != null) {
				final String reason = matched.get()
					.getError() == null ? matched.get()
						.getDisplayName()
						: matched.get()
							.getError()
							.getMessage();
				s.forEach(x -> listener.executionSkipped(x, reason));
			} else {
				s.map(x -> (BundleDescriptor) x)
					.forEach(b -> executeBundle(b, listener, params));
			}
			listener.executionFinished(root, TestExecutionResult.successful());
		} catch (Throwable t) {
			System.err.println("Unrecoverable error while executing tests: " + t);
			t.printStackTrace();
			listener.executionFinished(root, TestExecutionResult.failed(t));
		}
	}

	private static void executeBundle(BundleDescriptor b, EngineExecutionListener listener,
		ConfigurationParameters params) {
		listener.executionStarted(b);
		TestExecutionResult result;
		if (b.getException() == null) {
			result = singleTestExecutor.executeSafely(() -> {
				b.getChildren()
					.stream()
					.filter(x -> !(x instanceof BundleDescriptor || x instanceof StaticFailureDescriptor))
					.forEach(descriptor -> {
						ExecutionRequest er = new ExecutionRequest(descriptor, listener, params);
						b.getEngineFor(descriptor)
							.execute(er);
					});
			});
			b.getChildren()
				.stream()
				.filter(x -> x instanceof BundleDescriptor)
				.map(x -> (BundleDescriptor) x)
				.forEach(bundleDescriptor -> executeBundle(bundleDescriptor, listener, params));
			b.getChildren()
				.stream()
				.filter(x -> x instanceof StaticFailureDescriptor)
				.map(x -> (StaticFailureDescriptor) x)
				.forEach(x -> x.execute(listener));
		} else {
			result = TestExecutionResult.failed(b.getException());
			b.getChildren()
				.forEach(descriptor -> {
					listener.executionSkipped(descriptor, "Bundle did not resolve");
				});
		}
		listener.executionFinished(b, result);
	}
}
