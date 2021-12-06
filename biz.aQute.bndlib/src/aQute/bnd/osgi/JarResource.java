package aQute.bnd.osgi;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Path;

public class JarResource extends WriteResource {
	private final Jar		jar;
	private final boolean	closeJar;

	public JarResource(Jar jar) {
		this(jar, false);
	}

	public JarResource(Jar jar, boolean closeJar) {
		this.jar = jar;
		this.closeJar = closeJar;
	}

	@Override
	public long lastModified() {
		return jar.lastModified();
	}

	@Override
	public void write(OutputStream out) throws Exception {
		jar.write(out);
	}

	@Override
	public void write(File file) throws Exception {
		jar.write(file);
	}

	@Override
	public void write(Path path) throws Exception {
		jar.write(path.toFile());
	}

	public Jar getJar() {
		return jar;
	}

	@Override
	public String toString() {
		return ":" + jar.getName() + ":";
	}

	@Override
	public void close() {
		if (closeJar) {
			jar.close();
		}
		super.close();
	}

}
