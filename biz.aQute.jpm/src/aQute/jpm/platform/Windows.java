package aQute.jpm.platform;

/**
 * http://support.microsoft.com/kb/814596
 */
import java.io.*;

import aQute.jpm.lib.*;
import aQute.lib.io.*;

public class Windows extends Platform {

	@Override
	public File getGlobal() {
		return IO.getFile("~/.jpm");
	}

	@Override
	public File getLocal() {
		return IO.getFile("~/.jpm");
	}

	@Override
	public void shell(String initial) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void uninstall() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public String createCommand(CommandData data, String ... extra) throws Exception {
		
		return null;
	}

	@Override
	public String createService(ServiceData data) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String remove(CommandData data) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String remove(ServiceData data) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int launchService(ServiceData data) throws Exception {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void installDaemon(boolean user) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void uninstallDaemon(boolean user) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void chown(String user, boolean recursive, File file) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public String user() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteCommand(CommandData cmd) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public String toString() {
		return "Windows";
	}
}
