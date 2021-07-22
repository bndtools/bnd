package aQute.tester.listeners.slf4j;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class SLF4JListener implements TestExecutionListener {

	final static Logger logger = LoggerFactory.getLogger(SLF4JListener.class);

	@Override
	public void dynamicTestRegistered(TestIdentifier testIdentifier) {
		logger.debug("Dynamic test registered: {}", testIdentifier.getDisplayName());
	}

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		final Status status = testExecutionResult.getStatus();
		switch (status) {
			case ABORTED :
				logger.warn("Test aborted: {}, reason: ", testIdentifier.getDisplayName(),
					testExecutionResult.getThrowable());
				break;
			case FAILED :
				logger.error("Test failed: {}, reason: ", testIdentifier.getDisplayName(),
					testExecutionResult.getThrowable());
				break;
			case SUCCESSFUL :
				logger.info("Test passed: {}", testIdentifier.getDisplayName());
				break;
		}
	}

	@Override
	public void executionSkipped(TestIdentifier testIdentifier, String reason) {
		logger.warn("Test skipped: {}, reason: {}", testIdentifier.getDisplayName(), reason);
	}

	@Override
	public void executionStarted(TestIdentifier testIdentifier) {
		logger.debug("Test started: {}", testIdentifier.getDisplayName());
	}

	@Override
	public void testPlanExecutionFinished(TestPlan testPlan) {
		logger.debug("Test plan finished");
	}

	@Override
	public void testPlanExecutionStarted(TestPlan testPlan) {
		logger.debug("Test plan started");
	}
}
