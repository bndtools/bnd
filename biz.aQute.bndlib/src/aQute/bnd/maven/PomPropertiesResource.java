package aQute.bnd.maven;

import java.util.Properties;

import aQute.bnd.osgi.PropertiesResource;

public class PomPropertiesResource extends PropertiesResource {
	private final String			where;

	public PomPropertiesResource(PomResource pomResource) {
		super();
		Properties properties = getProperties();
		properties.setProperty("groupId", pomResource.getGroupId());
		properties.setProperty("artifactId", pomResource.getArtifactId());
		properties.setProperty("version", pomResource.getVersion());
		where = pomResource.getWhere()
			.replaceFirst("(?<=^|/)pom\\.xml$", "pom\\.properties");
	}

	public PomPropertiesResource(String groupId, String artifactId, String version) {
		super();
		Properties properties = getProperties();
		properties.setProperty("groupId", groupId);
		properties.setProperty("artifactId", artifactId);
		properties.setProperty("version", version);
		where = String.format("META-INF/maven/%s/%s/pom.properties", groupId, artifactId);
	}

	public String getWhere() {
		return where;
	}
}
