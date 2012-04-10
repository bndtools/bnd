package aQute.jpm.platform;

import java.io.*;

class Linux extends Unix {

		
	@Override public void shell(String initial) throws IOException {
		throw new UnsupportedOperationException();
	}
	
	@Override public String getName() { return "Linux"; }
	
	@Override public void uninstall() {
		
	}

}
