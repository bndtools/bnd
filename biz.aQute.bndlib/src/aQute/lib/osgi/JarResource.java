package aQute.lib.osgi;

import java.io.*;

public class JarResource extends WriteResource {
	Jar		jar;
	long size = -1;
	public JarResource(Jar jar ) {
		this.jar = jar;
	}
	

	public long lastModified() {
		return jar.lastModified();
	}


	public void write(OutputStream out) throws Exception {
		jar.write(out);
	}
	
	public Jar getJar() { 
	    return jar;
	}
	
	public String toString() {
	    return ":" + jar.getName() + ":";
	}

}
