package aQute.jpm.remote.lib;

import aQute.lib.getopt.*;

public interface SlaveOptions extends Options {
	int port();

	String cwd(String absolutePath);
}
