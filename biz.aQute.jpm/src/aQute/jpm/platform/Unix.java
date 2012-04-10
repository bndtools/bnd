package aQute.jpm.platform;

import static aQute.lib.io.IO.*;

import java.io.*;
import java.lang.reflect.*;

import aQute.jpm.lib.*;
import aQute.libg.sed.*;

public abstract class Unix extends Platform {

	@Override public File getGlobal() {
		return new File("/var/jpm");
	}

	@Override public File getLocal() {
		File home = new File(System.getProperty("user.home"));
		return new File(home, ".jpm");
	}

	@Override public String createCommand(CommandData data) throws Exception {
		File executable = getExecutable(data);
		if ( !data.force && executable.exists() )
			return "Command already exists " + executable;
		
		process("unix/command.sh", data, executable);
		return null;
	}

	@Override public String createService(ServiceData data) throws Exception {
		File initd = getInitd(data);
		File launch = getLaunch(data);
		
		if ( !data.force && (initd.exists() || launch.exists()))
			return "Service already exists " + initd + " & " + launch;
		
		process("unix/launch.sh", data, launch);	
		process("unix/initd.sh", data, initd);
		return null;
	}
	
	private File getInitd(ServiceData data) {
		return new File("/etc/init.d/" + data.name);
	}

	protected File getLaunch(ServiceData data) {
		return new File( data.sdir, "launch.sh");
	}

	private File getExecutable(CommandData data) {
		return new File( "/usr/local/bin/" + data.name );
	}

	@Override public String remove(ServiceData data) {
		if ( !getInitd(data).delete() )
			return "Cannot delete " + getInitd(data);
		
		if ( !getExecutable(data).delete() )
			return "Cannot delete " + getExecutable(data);
		
		return null;
	}

	@Override public String remove(CommandData data) throws Exception {
		if ( !getExecutable(data).delete() )
			return "Cannot delete " + getExecutable(data);
		
		return null;
	}

	@Override public int launchService(ServiceData data) throws Exception {
		File launch = getLaunch(data);
		Process p = Runtime.getRuntime().exec(launch.getAbsolutePath(), null, data.work);
		return p.waitFor();
	}
	protected void process(String resource, Object data, File file) throws Exception {
		copy(getClass().getResourceAsStream(resource), file);
		Sed sed = new Sed(file);
		sed.setBackup(false);
		for (Field key : data.getClass().getFields()) {
			sed.replace("%"+key.getName()+"%", ""+key.get(data));
		}
		sed.doIt();
		run("chmod a+x " + file.getAbsolutePath());
	}
	
}
