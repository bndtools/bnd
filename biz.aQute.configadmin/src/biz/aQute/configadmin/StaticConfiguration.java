package biz.aQute.configadmin;

import java.io.IOException;
import java.util.Dictionary;
import java.util.UUID;

import org.osgi.service.cm.Configuration;

@SuppressWarnings("rawtypes")
public class StaticConfiguration implements Configuration {
	
	private final String factoryPid;
	private final String pid;
	private final Dictionary properties;
	
	public static StaticConfiguration createSingletonConfiguration(String pid, Dictionary properties) {
		return new StaticConfiguration(null, pid, properties);
	}

	public static StaticConfiguration createFactoryConfiguration(String factoryPid, Dictionary properties) {
		String pid = UUID.randomUUID().toString();
		return new StaticConfiguration(factoryPid, pid, properties);
	}

	private StaticConfiguration(String factoryPid, String pid, Dictionary properties) {
		this.factoryPid = factoryPid;
		this.pid = pid;
		this.properties = properties;
	}

	@Override
	public String getPid() {
		return pid;
	}

	@Override
	public Dictionary getProperties() {
		return properties;
	}

	@Override
	public void update(Dictionary properties) throws IOException {
		throw new SecurityException("Cannot update configurations");
	}

	@Override
	public void delete() throws IOException {
		throw new SecurityException("Cannot delete configurations");
	}

	@Override
	public String getFactoryPid() {
		return factoryPid;
	}

	@Override
	public void update() throws IOException {
		throw new SecurityException("Cannot update configurations");
	}

	@Override
	public void setBundleLocation(String bundleLocation) {
		// ignored
	}

	@Override
	public String getBundleLocation() {
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((factoryPid == null) ? 0 : factoryPid.hashCode());
		result = prime * result + ((pid == null) ? 0 : pid.hashCode());
		result = prime * result
				+ ((properties == null) ? 0 : properties.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StaticConfiguration other = (StaticConfiguration) obj;
		if (factoryPid == null) {
			if (other.factoryPid != null)
				return false;
		} else if (!factoryPid.equals(other.factoryPid))
			return false;
		if (pid == null) {
			if (other.pid != null)
				return false;
		} else if (!pid.equals(other.pid))
			return false;
		if (properties == null) {
			if (other.properties != null)
				return false;
		} else if (!properties.equals(other.properties))
			return false;
		return true;
	}

}
