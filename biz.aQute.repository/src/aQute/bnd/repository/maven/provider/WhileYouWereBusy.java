package aQute.bnd.repository.maven.provider;

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
