package aQute.bnd.tool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.lib.io.IO;

public class Tool extends Processor {

	private final Jar	jar;
	private File		tmp;

	public Tool(Processor parent, Jar jar) throws Exception {
		super(parent);
		this.jar = jar;
		tmp = Files.createTempDirectory("tool").toFile();
	}


	public Jar doJavadoc(Jar binary, File file, Map<String,String> options) {
		return null;
	}

	public Jar doSource(Jar binary, File resolve) {
		// TODO Auto-generated method stub
		return null;
	}

	public void close() throws IOException {
		try {
			super.close();
		} finally {
			IO.delete(tmp);
		}
	}

}
