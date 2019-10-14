package aQute.tester.bundle.engine;

import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;

public class StaticFailureDescriptor extends AbstractTestDescriptor {
	private final Throwable error;

	public StaticFailureDescriptor(UniqueId id, String name) {
		this(id, name, null);
	}

	public StaticFailureDescriptor(UniqueId id, String name, Throwable error) {
		super(id, name);
		this.error = error;
	}

	public Throwable getError() {
		return error;
	}

	@Override
	public Type getType() {
		return hasChildren() ? ((error != null) ? Type.CONTAINER_AND_TEST : Type.CONTAINER) : Type.TEST;
	}

	public boolean hasChildren() {
		return !getChildren().isEmpty();
	}

	// Defensive guarding.
	@Override
	public void addChild(TestDescriptor t) {
		if (t instanceof StaticFailureDescriptor) {
			super.addChild(t);
		} else {
			throw new IllegalArgumentException(
				"StaticFailureDescriptor can only take StaticFailureDescriptors as children");
		}
	}

	public void execute(EngineExecutionListener listener) {
		listener.executionStarted(this);
		getChildren().stream()
			.map(StaticFailureDescriptor.class::cast)
			.forEach(descriptor -> descriptor.execute(listener));
		listener.executionFinished(this, error == null ? TestExecutionResult.successful() : TestExecutionResult.failed(error));
	}
}
