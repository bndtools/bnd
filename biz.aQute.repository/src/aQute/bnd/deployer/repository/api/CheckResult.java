package aQute.bnd.deployer.repository.api;

import static aQute.bnd.deployer.repository.api.Decision.accept;
import static aQute.bnd.deployer.repository.api.Decision.reject;

public class CheckResult {

	private Decision	decision;
	private String		message;
	private Throwable	exception;

	public static CheckResult fromBool(boolean match, String matchMsg, String unmatchedMsg, Throwable exception) {
		return new CheckResult(match ? accept : reject, match ? matchMsg : unmatchedMsg, exception);
	}

	public CheckResult(Decision decision, String message, Throwable exception) {
		assert decision != null;
		this.decision = decision;
		this.message = message;
		this.exception = exception;
	}

	public Decision getDecision() {
		return decision;
	}

	public void setDecision(Decision decision) {
		this.decision = decision;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Throwable getException() {
		return exception;
	}

	public void setException(Throwable exception) {
		this.exception = exception;
	}

	@Override
	public String toString() {
		return "CheckResult [decision=" + decision + ", message=" + message + ", exception=" + exception + "]";
	}

}
