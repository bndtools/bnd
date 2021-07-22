package aQute.tester.listeners.osgi;

import java.util.Optional;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;

import aQute.lib.strings.Strings;

@Component
public class OSGiLogListener implements TestExecutionListener {

	@Reference(service = LoggerFactory.class)
	Logger		logger;

	TestPlan	testPlan;

	String indentedName(TestIdentifier testIdentifier) {
		int depth = 0;
		Optional<TestIdentifier> current = testPlan.getParent(testIdentifier);
		while (current.isPresent()) {
			depth++;
			current = testPlan.getParent(current.get());
		}
		depth--;

		return Strings.times("  ", depth) + testIdentifier.getDisplayName();
	}

	@Override
	public void dynamicTestRegistered(TestIdentifier testIdentifier) {
		logger.debug(l -> l.debug("Test registered: " + indentedName(testIdentifier)));
	}

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		final Status status = testExecutionResult.getStatus();
		switch (status) {
			case ABORTED :
				logger.warn(l -> l.warn("Test aborted:     " + indentedName(testIdentifier) + ", reason: "
					+ testExecutionResult.getThrowable()));
				break;
			case FAILED :
				logger.error(l -> l.error("Test failed:     " + indentedName(testIdentifier) + ", reason: "
					+ testExecutionResult.getThrowable()));
				break;
			case SUCCESSFUL :
				logger.info(l -> l.info("Test passed:     " + indentedName(testIdentifier)));
				break;
		}
	}

	@Override
	public void executionSkipped(TestIdentifier testIdentifier, String reason) {
		logger.warn(l -> l.warn("Test skipped:    " + indentedName(testIdentifier) + ", reason: " + reason));
	}

	@Override
	public void executionStarted(TestIdentifier testIdentifier) {
		logger.debug(l -> l.debug("Test started:    " + indentedName(testIdentifier)));
	}

	@Override
	public void testPlanExecutionFinished(TestPlan testPlan) {
		logger.debug("Test plan finished");
		this.testPlan = null;
	}

	@Override
	public void testPlanExecutionStarted(TestPlan testPlan) {
		logger.debug("Test plan started");
		this.testPlan = testPlan;
	}
}
