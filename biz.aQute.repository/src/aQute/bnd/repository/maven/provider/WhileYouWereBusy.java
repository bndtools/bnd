package aQute.bnd.repository.maven.provider;

/**
 * {@link WhileYouWereBusy} handles the problem that you want to be sure an
 * action is executed but that action can still be going on and they cannot be
 * run in parallel. If nobody is executing the action, it will use the current
 * thread, if not, the currently running action will repeat until there is no
 * more request.
 */
public abstract class WhileYouWereBusy {
	boolean	busy;
	boolean	request;

	public void doAction() throws Exception {
		synchronized (this) {
			request = true;
			if (busy)
				return;
			busy = true;
			request = false;
		}
		while (true)
			try {
				run();
			} finally {
				synchronized (this) {
					if (request) {
						while (request) {
							Thread.sleep(100); // coalesce
							request = false;
						}
					} else {
						busy = false;
						return;
					}
				}
			}
	}

	public abstract void run() throws Exception;
}
