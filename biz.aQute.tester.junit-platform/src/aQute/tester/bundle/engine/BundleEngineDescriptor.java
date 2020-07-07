package aQute.tester.bundle.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

import aQute.lib.collections.MultiMap;

public class BundleEngineDescriptor extends EngineDescriptor {
	final private Map<TestDescriptor, TestDescriptor>		engineDescriptorMap			= new HashMap<>();
	final private MultiMap<TestDescriptor, TestDescriptor>	reverseEngineDescriptorMap	= new MultiMap<>(
		TestDescriptor.class, TestDescriptor.class, true);
	final private Map<TestDescriptor, TestEngine>			engineMap					= new HashMap<>();

	public BundleEngineDescriptor(UniqueId uniqueId) {
		super(uniqueId, "Bnd JUnit Platform Bundle Engine");
	}

	public void registerEngineFor(TestDescriptor ed, TestEngine engine) {
		engineMap.put(ed, engine);
	}

	public void registerSubEngineDescriptorFor(TestDescriptor td, TestDescriptor engineDescriptor) {
		engineDescriptorMap.put(td, engineDescriptor);
		reverseEngineDescriptorMap.add(engineDescriptor, td);
	}

	public TestDescriptor getSubEngineDescriptorFor(TestDescriptor td) {
		return engineDescriptorMap.get(td);
	}

	public void executeChildren(TestDescriptor descriptor, EngineExecutionListener listener,
		ConfigurationParameters params) {
		TestEngine engine = engineMap.get(descriptor);
		ExecutionRequest er = new ExecutionRequest(descriptor, listener, params);
		engine.execute(er);

	}

	public static class FilteredListener implements EngineExecutionListener {

		final EngineExecutionListener	listener;
		final TestDescriptor			filtered;
		final TestDescriptor			altParent;

		FilteredListener(EngineExecutionListener listener, TestDescriptor filtered, TestDescriptor altParent) {
			this.listener = listener;
			this.filtered = filtered;
			this.altParent = altParent;
		}

		@Override
		public void dynamicTestRegistered(TestDescriptor testDescriptor) {
			listener.dynamicTestRegistered(testDescriptor);
		}

		@Override
		public void executionSkipped(TestDescriptor testDescriptor, String reason) {
			if (testDescriptor != filtered) {
				listener.executionSkipped(testDescriptor, reason);
			}
		}

		@Override
		public void executionStarted(TestDescriptor testDescriptor) {
			if (testDescriptor != filtered) {
				listener.executionStarted(testDescriptor);
			}
		}

		@Override
		public void executionFinished(TestDescriptor testDescriptor, TestExecutionResult testExecutionResult) {
			if (testDescriptor != filtered) {
				listener.executionFinished(testDescriptor, testExecutionResult);
			}
		}

		@Override
		public void reportingEntryPublished(TestDescriptor testDescriptor, ReportEntry entry) {
			if (testDescriptor != filtered) {
				listener.reportingEntryPublished(testDescriptor, entry);
			}
		}
	}

	public void executeChildrenOfPrunedEngine(TestDescriptor engineDescriptor, TestDescriptor parent,
		EngineExecutionListener listener, ConfigurationParameters params) {
		TestEngine engine = engineMap.get(engineDescriptor);
		if (parent != engineDescriptor) {
			// Re-insert the engine node into the hierarchy of the engine
			// descriptor node before we pass it to the sub-engine for
			// execution, because the engine may have preconditions that expect
			// the same engine root node that it generated during the discovery
			// phase.
			reverseEngineDescriptorMap.get(engineDescriptor)
				.forEach(child -> {
					parent.removeChild(child);
					engineDescriptor.addChild(child);
				});
		}
		FilteredListener filtered = new FilteredListener(listener, engineDescriptor, parent);
		ExecutionRequest er = new ExecutionRequest(engineDescriptor, filtered, params);
		try {
			engine.execute(er);
		} finally {
			if (parent != engineDescriptor) {
				// Put them back into the original parent when we've finished so
				// that the sub-engine node doesn't appear in the test output.
				// Note that we don't simply re-use the original list from
				// reverseEngineDescriptorMap above, as/ the sub-engine may have
				// added dynamic tests to the test hierarchy during execution.
				// Need to copy it though to avoid concurrent modification
				// exception.
				Stream.of(engineDescriptor.getChildren()
					.stream()
					.toArray(TestDescriptor[]::new))
					.forEach(child -> {
						engineDescriptor.removeChild(child);
						parent.addChild(child);
					});
			}
		}
	}
}
