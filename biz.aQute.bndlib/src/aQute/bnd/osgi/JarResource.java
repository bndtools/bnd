package aQute.bnd.osgi;

import java.io.*;

public class JarResource extends WriteResource {
	Jar		jar;
	long	size	= -1;

	public JarResource(Jar jar) {
		this.jar = jar;
	}

	@Override
	public long lastModified() {
		return jar.lastModified();
	}

	@Override
	public void write(OutputStream out) throws Exception {
		try {
			jar.write(out);
		}
		catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	public Jar getJar() {
		return jar;
	}

	@Override
	public String toString() {
		return ":" + jar.getName() + ":";
	}

}
