package bndtools.wizards.workspace;

public class CnfSetupStartupParticipant implements Runnable {

	public void run() {
		CnfSetupWizard.showIfNeeded();
	}

}
