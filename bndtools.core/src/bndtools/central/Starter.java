package bndtools.central;

import org.eclipse.ui.IStartup;

public class Starter implements IStartup {

	@Override
	public void earlyStartup() {
		try {
			Central.getWorkspace();
		} catch (Exception e) {
		}
	}

}
