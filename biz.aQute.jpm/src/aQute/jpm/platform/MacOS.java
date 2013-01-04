package aQute.jpm.platform;

import java.io.*;

import aQute.jpm.lib.*;
import aQute.lib.io.*;

class MacOS extends Unix {
	File	home	= new File(System.getProperty("user.home"));

	@Override
	public File getGlobal() {
		return new File("/Library/Java/PackageManager").getAbsoluteFile();
	}

	@Override
	public File getLocal() {
		return new File(home, "Library/PackageManager").getAbsoluteFile();
	}

	@Override
	public void shell(String initial) throws Exception {
		run("open -n /Applications/Utilities/Terminal.app");
	}

	@Override
	public String getName() {
		return "MacOS";
	}

	@Override
	public String createService(ServiceData data) throws Exception {
		// File initd = getInitd(data);
		File launch = getLaunch(data);
		if (!data.force && launch.exists())
			return "Cannot create service " + data.name + " because it exists";

		process("macos/launch.sh", data, launch, data.serviceLib);
		return null;
	}

	@Override
	public String remove(ServiceData data) {
		// File initd = getInitd(data);
		File launch = getLaunch(data);
		
		if (launch.exists() && !launch.delete())
			return "Cannot delete service " + data.name + " because it exists and cannot be deleted: " + launch;

		return null;
	}

	@Override
	public void installDaemon(boolean user) throws IOException {
		String dest = "~/Library/LaunchAgents/org.jpm4j.run.plist";
		if ( !user) {
			dest = "/Library/LaunchAgents/org.jpm4j.run.plist";
		}
		IO.copy(getClass().getResource("macos/daemon.plist"), IO.getFile(dest));
	}

	@Override
	public void uninstallDaemon(boolean user) throws IOException {
		if ( user)
			IO.delete( new File("~/Library/LaunchAgents/org.jpm4j.run.plist"));
		else
			IO.delete( new File("/Library/LaunchAgents/org.jpm4j.run.plist"));
	}

	@Override
	public void uninstall() throws IOException {
	}
	
	public String defaultCacertsPassword() { return "changeit"; }

	
	public String toString() {
		return "MacOS";
	}
}
