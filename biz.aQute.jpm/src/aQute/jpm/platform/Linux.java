package aQute.jpm.platform;

import java.io.*;

import aQute.lib.io.*;

class Linux extends Unix {

	static final String COMPLETION_DIRECTORY = "/etc/bash_completion.d";
	
	@Override
	public void shell(String initial) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getName() {
		return "Linux";
	}

	@Override
	public void uninstall() {

	}
	
	public String toString() {
		return "Linux";
	}

	@Override
	public String installCompletion() throws Exception {
		File f = new File(COMPLETION_DIRECTORY);
		
		if(!f.exists() || !f.canWrite()) {
			return "Bash completion directory does not exist or cannot be written to";
		}
		
		
		
		IO.copy(getClass().getResource("unix/jpm-completion.bash"), new File(f, "jpm-completion.bash"));
		return "Bash completion file installed in "+COMPLETION_DIRECTORY;
	}

	
}
