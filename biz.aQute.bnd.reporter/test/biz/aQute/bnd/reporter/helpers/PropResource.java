package biz.aQute.bnd.reporter.helpers;

import java.io.OutputStream;
import java.util.Properties;

import aQute.bnd.osgi.WriteResource;

public class PropResource extends WriteResource {

	Properties prop = new Properties();

	@Override
	public void write(OutputStream out) throws Exception {
		prop.store(out, null);
		out.flush();
	}

	public void add(String key, String value) {
		prop.setProperty(key, value);
	}

	@Override
	public long lastModified() {
		return 0;
	}
}
