package aQute.bnd.exporter.feature.internal;

import java.util.Optional;

import org.osgi.service.feature.ID;

public class ID_ implements ID {


	public String			groupId;
	public String			artifactId;
	public String			version;
	public Optional<String>	type	= Optional.empty();
	public Optional<String>	classifier	= Optional.empty();

	@Override
	public String getGroupId() {
		return groupId;
	}

	@Override
	public String getArtifactId() {
		return artifactId;
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public Optional<String> getType() {
		return type;
	}

	@Override
	public Optional<String> getClassifier() {
		return classifier;
	}

}
