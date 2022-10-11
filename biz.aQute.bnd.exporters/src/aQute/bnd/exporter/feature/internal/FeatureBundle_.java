package aQute.bnd.exporter.feature.internal;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.feature.FeatureBundle;
import org.osgi.service.feature.ID;

public class FeatureBundle_ implements FeatureBundle {

	public ID			id;

	Map<String, Object>	metadata	= new HashMap<>();

	@Override
	public ID getID() {
		return id;
	}

	@Override
	public Map<String, Object> getMetadata() {
		return metadata;
	}

}
