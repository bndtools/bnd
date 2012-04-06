package aQute.jpm.platform;

import java.io.*;

class Linux extends Unix {

		
	@Override public void deleteCommand(String name) throws IOException {
		String path = "/usr/local/bin/"+name;
		File link = new File( path);
		link.delete();
	}

	@Override public void shell(String initial) throws IOException {
		throw new UnsupportedOperationException();
	}
	
	@Override public String getName() { return "Linux"; }
	
	@Override public void uninstall() {
		
	}

}
