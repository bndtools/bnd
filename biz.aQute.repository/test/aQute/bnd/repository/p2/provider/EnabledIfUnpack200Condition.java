package aQute.bnd.repository.p2.provider;

import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import aQute.lib.io.IO;
import aQute.p2.packed.Unpack200;

class EnabledIfUnpack200Condition implements ExecutionCondition {
	private static final ConditionEvaluationResult	ENABLED_BY_DEFAULT		= enabled(
		"@EnabledIfUnpack200 is not present");
	private static final ConditionEvaluationResult	ENABLED_ON_CURRENT_JDK	=			//
		enabled("Enabled on JDK version: " + System.getProperty("java.version"));

	private static final ConditionEvaluationResult	DISABLED_ON_CURRENT_JDK	=			//
		disabled("Disabled on JDK version: " + System.getProperty("java.version"));

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		Unpack200 unpack200 = new Unpack200();
		try {
			return findAnnotation(context.getElement(), EnabledIfUnpack200.class)
				.map(a -> unpack200.canUnpack() ? ENABLED_ON_CURRENT_JDK : DISABLED_ON_CURRENT_JDK)
				.orElse(ENABLED_BY_DEFAULT);
		} finally {
			IO.close(unpack200);
		}
	}
}
