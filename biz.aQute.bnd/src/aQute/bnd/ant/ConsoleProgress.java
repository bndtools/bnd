package aQute.bnd.ant;

import aQute.bnd.service.progress.*;

public class ConsoleProgress implements ProgressPlugin {

	public Task startTask(String name, int size) {
		System.out.printf(name);
		return new Task() {
			public void worked(int units) {
				// TODO
			}
			public void done(String message, Throwable e) {
				System.out.println(": " + message);
				if (e != null)
					e.printStackTrace();
			}
		};
	}

}
