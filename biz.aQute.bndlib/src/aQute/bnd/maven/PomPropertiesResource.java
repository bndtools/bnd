package aQute.bnd.maven;

import java.io.IOException;
import java.io.OutputStream;

import aQute.bnd.osgi.WriteResource;
import aQute.lib.utf8properties.UTF8Properties;

public class PomPropertiesResource extends WriteResource {
	private final UTF8Properties	pomProperties;
	private final String			where;

	public PomPropertiesResource(PomResource pomResource) {
		pomProperties = new UTF8Properties();
		pomProperties.setProperty("groupId", pomResource.getGroupId());
		pomProperties.setProperty("artifactId", pomResource.getArtifactId());
		pomProperties.setProperty("version", pomResource.getVersion());
		where = pomResource.getWhere()
			.replaceFirst("(?<=^|/)pom\\.xml$", "pom\\.properties");
	}

	public String getWhere() {
		return where;
	}

	@Override
	public long lastModified() {
		return 0;
	}

	@Override
	public void write(OutputStream out) throws IOException {
		pomProperties.store(out);
	}
}
