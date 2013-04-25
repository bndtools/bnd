package aQute.jpm.platform;

import java.io.*;

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
	public String installCompletion(Object target) throws Exception {
		File dir = new File(COMPLETION_DIRECTORY);
		
		if(!dir.exists() || !dir.canWrite()) {
			return "Bash completion directory does not exist or cannot be written to";
		}
		
		File f = new File(dir, "jpm-completion.bash");
		parseCompletion(target, f);
		
		return "Bash completion file installed in "+COMPLETION_DIRECTORY;
	}
	
}