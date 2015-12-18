package aQute.bnd.maven.baseline.plugin;

public class Base {

	private String	groupId;

	private String	artifactId;

	private String	version;

	private String	classifier;

	private String	extension;

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getClassifier() {
		return classifier;
	}

	public void setClassifier(String classifier) {
		this.classifier = classifier;
	}

	public String getExtension() {
		return extension;
	}

	public void setExtension(String extension) {
		this.extension = extension;
	}

	@Override
	public String toString() {
		return "Base [groupId=" + groupId + ", artifactId=" + artifactId + ", version=" + version + ", classifier="
				+ classifier + ", extension=" + extension + "]";
	}
}
