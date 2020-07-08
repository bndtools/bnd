package aQute.bnd.ant;

import aQute.bnd.service.progress.ProgressPlugin;

public class ConsoleProgress implements ProgressPlugin {

	@Override
	public Task startTask(String name, int size) {
		System.out.print(name);
		return new Task() {
			@Override
			public void worked(int units) {
				// TODO
			}

			@Override
			public void done(String message, Throwable e) {
				System.out.println(": " + message);
				if (e != null)
					e.printStackTrace();
			}

			@Override
			public boolean isCanceled() {
				// TODO Auto-generated method stub
				return false;
			}
		};
	}

}
