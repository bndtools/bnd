package bndtools.wizards.workspace;

class CnfSetupUserConfirmation {

	enum Operation { CREATE, UPDATE }

	enum Decision { SETUP, SKIP, NEVER }

	private final Operation operation;
	private Decision decision = Decision.SETUP;

	public CnfSetupUserConfirmation(Operation operation) {
		this.operation = operation;
	}

	public Operation getOperation() {
		return operation;
	}

	public Decision getDecision() {
		return decision;
	}

	public void setDecision(Decision decision) {
		this.decision = decision;
	}

}
