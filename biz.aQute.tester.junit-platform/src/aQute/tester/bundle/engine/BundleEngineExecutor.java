package aQute.tester.bundle.engine;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.opentest4j.TestAbortedException;

public class BundleEngineExecutor {

	final BundleEngineDescriptor	root;
	final EngineExecutionListener	listener;
	final ExecutionRequest			request;
	final ConfigurationParameters	params;

	public BundleEngineExecutor(ExecutionRequest request) {
		TestDescriptor root = request.getRootTestDescriptor();
		Preconditions.condition(root instanceof BundleEngineDescriptor,
			() -> "Root descriptor should be an instance of BundleEngineDescriptor, was " + root.getClass());
		this.request = request;
		this.root = (BundleEngineDescriptor) root;
		this.listener = request.getEngineExecutionListener();
		this.params = request.getConfigurationParameters();
	}

	void dump(String indent, TestDescriptor desc) {
		System.err.println(indent + desc.getDisplayName());
		for (TestDescriptor child : desc.getChildren()) {
			dump(indent + "  ", child);
		}
	}

	public void execute() {
		dump("", root);

		BundleEngineDescriptor bundleEngineRoot = root;
		listener.executionStarted(root);
		try {
			Optional<StaticFailureDescriptor> staticFailureDescriptor = root.getChildren()
				.stream()
				.filter(StaticFailureDescriptor.class::isInstance)
				.map(StaticFailureDescriptor.class::cast)
				.peek(childDescriptor -> childDescriptor.execute(listener))
				.reduce((first, second) -> first);

			if (staticFailureDescriptor.isPresent()) {
				Stream<? extends TestDescriptor> childDescriptors = root.getChildren()
					.stream()
					.filter(BundleDescriptor.class::isInstance);
				String reason = staticFailureDescriptor.map(StaticFailureDescriptor::getError)
					.map(Throwable::getMessage)
					.orElseGet(() -> staticFailureDescriptor.get()
						.getDisplayName());
				childDescriptors.forEach(childDescriptor -> listener.executionSkipped(childDescriptor, reason));
			} else {
				executeChildren(root);
			}
			listener.executionFinished(root, TestExecutionResult.successful());
		} catch (Throwable t) {
			System.err.println("Unrecoverable error while executing tests: " + t);
			t.printStackTrace(System.err);
			listener.executionFinished(root, TestExecutionResult.failed(t));
		}
	}

	private void executeChildren(TestDescriptor descriptor) {
		descriptor.getChildren()
			.stream()
			.filter(BundleDescriptor.class::isInstance)
			.map(BundleDescriptor.class::cast)
			.forEach(childDescriptor -> executeBundle(childDescriptor));
		descriptor.getChildren()
			.stream()
			.filter(StaticFailureDescriptor.class::isInstance)
			.map(StaticFailureDescriptor.class::cast)
			.forEach(childDescriptor -> childDescriptor.execute(listener));
		descriptor.getChildren()
			.stream()
			.filter(EngineDescriptor.class::isInstance)
			.map(EngineDescriptor.class::cast)
			.forEach(this::executeEngine);

		Stream.of(descriptor.getChildren()
			.stream()
			.filter(childDescriptor -> !(childDescriptor instanceof BundleDescriptor
				|| childDescriptor instanceof StaticFailureDescriptor || childDescriptor instanceof EngineDescriptor))
			.map(root::getSubEngineDescriptorFor)
			.distinct()
			// We have to make a copy because executeChildrenOfPrunedEngine()
			// will add and remove children from descriptor during its
			// operation, which will cause ConcurrentModificationException
			.toArray(TestDescriptor[]::new))
			.forEach(subEngineDescriptor -> root.executeChildrenOfPrunedEngine(subEngineDescriptor, descriptor,
				listener, params));
	}

	private void executeEngine(EngineDescriptor engineDescriptor) {
		root.executeChildren(engineDescriptor, listener, params);
	}

	private void executeBundle(BundleDescriptor descriptor) {
		listener.executionStarted(descriptor);
		TestExecutionResult result;
		if (descriptor.getException() == null) {
			try {
				executeChildren(descriptor);
				result = TestExecutionResult.successful();
			} catch (TestAbortedException abort) {
				result = TestExecutionResult.aborted(abort);
			} catch (OutOfMemoryError | StackOverflowError t) {
				throw t;
			} catch (Throwable t) {
				t.printStackTrace();
				result = TestExecutionResult.failed(t);
			}
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
