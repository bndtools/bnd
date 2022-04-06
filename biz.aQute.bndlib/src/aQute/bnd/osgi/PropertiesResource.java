package aQute.bnd.osgi;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import aQute.lib.utf8properties.UTF8Properties;

public class PropertiesResource extends WriteResource {
	private final Properties properties;

	public PropertiesResource(Properties properties) {
		this.properties = requireNonNull(properties);
	}

	public PropertiesResource() {
		this(new UTF8Properties());
	}

	public Properties getProperties() {
		return properties;
	}

	@Override
	public long lastModified() {
		return 0L;
	}

	@Override
	public void write(OutputStream out) throws IOException {
		properties.store(out, null);
	}
}
