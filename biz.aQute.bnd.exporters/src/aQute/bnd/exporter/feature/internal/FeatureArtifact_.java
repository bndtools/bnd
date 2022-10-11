package aQute.bnd.exporter.feature.internal;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.feature.FeatureArtifact;
import org.osgi.service.feature.ID;

public class FeatureArtifact_ implements FeatureArtifact {
	public ID					id;
	public Map<String, Object>	metadata	= new HashMap<>();

	@Override
	public ID getID() {
		return id;
	}

	@Override
	public Map<String, Object> getMetadata() {
		return metadata;
	}

}
