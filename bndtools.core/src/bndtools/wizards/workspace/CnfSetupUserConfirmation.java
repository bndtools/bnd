package bndtools.wizards.workspace;

class CnfSetupUserConfirmation {

    enum Decision {
        SETUP, SKIP, NEVER
    }

    private Decision decision = Decision.SETUP;

    public Decision getDecision() {
        return decision;
    }

    public void setDecision(Decision decision) {
        this.decision = decision;
    }

}
