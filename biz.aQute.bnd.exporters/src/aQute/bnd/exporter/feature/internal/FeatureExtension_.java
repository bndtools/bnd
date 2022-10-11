package aQute.bnd.exporter.feature.internal;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.osgi.service.feature.FeatureArtifact;
import org.osgi.service.feature.FeatureConfiguration;
import org.osgi.service.feature.FeatureExtension;

public class FeatureExtension_ implements FeatureExtension {

	public String								name;

	public Map<String, FeatureConfiguration>	configurations	= new HashMap<>();

	public List<String>							text			= new LinkedList<>();
	public String								json;

	public List<FeatureArtifact>				artifacts		= new LinkedList<>();

	public Type									type;
	public Kind									kind;

	@Override
	public String getName() {

		return name;
	}

	@Override
	public Type getType() {

		return type;
	}

	@Override
	public Kind getKind() {

		return kind;
	}

	@Override
	public String getJSON() {

		return json;
	}

	@Override
	public List<String> getText() {

		return text;
	}

	@Override
	public List<FeatureArtifact> getArtifacts() {

		return artifacts;
	}

}
