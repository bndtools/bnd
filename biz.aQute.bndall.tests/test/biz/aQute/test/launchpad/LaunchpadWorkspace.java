package biz.aQute.test.launchpad;

import aQute.bnd.build.Workspace;
import aQute.launchpad.LaunchpadBuilder;
import aQute.lib.io.IO;

public class LaunchpadWorkspace {
	@SuppressWarnings("unused")
	private static Workspace	ws;

	static LaunchpadBuilder		builder	= new LaunchpadBuilder().runfw("org.apache.felix.framework;version=@5");

	static {
		try {
			ws = Workspace.findWorkspace(IO.work);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws InterruptedException {
		Thread.sleep(100000000);
	}
}
