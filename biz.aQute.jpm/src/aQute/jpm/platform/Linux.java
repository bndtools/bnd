package aQute.jpm.platform;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import aQute.lib.getopt.*;
import aQute.lib.io.*;
import aQute.libg.reporter.*;
import aQute.libg.sed.*;
import aQute.service.reporter.*;

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
		
		IO.copy(getClass().getResource("unix/jpm-completion.bash"), f);
		
		Sed sed = new Sed(f);
		sed.setBackup(false);
		
		Reporter r = new ReporterAdapter();
		CommandLine c = new CommandLine(r);
		Map<String,Method> commands = c.getCommands(target);
		StringBuilder sb = new StringBuilder();
		for(String commandName : commands.keySet()) {
			sb.append(" "+commandName);
		}
		
		sed.replace("%listJpmCommands%", sb.toString().substring(1));
		sed.doIt();
		
		
		return "Bash completion file installed in "+COMPLETION_DIRECTORY;
	}
	
}