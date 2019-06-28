package bndtools.tasks;

import java.io.File;
import java.io.IOException;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;

public class JarFileCapReqLoader extends BndBuilderCapReqLoader {

	private Builder builder;

	public JarFileCapReqLoader(File jarFile) {
		super(jarFile);
	}

	@Override
	protected synchronized Builder getBuilder() throws Exception {
		if (builder == null) {
			Builder b = new Builder();
			Jar jar = new Jar(file);
			b.setJar(jar);
			b.analyze();

			builder = b;
		}
		return builder;
	}

	@Override
	public synchronized void close() throws IOException {
		if (builder != null)
			builder.close();
		builder = null;
	}

}
