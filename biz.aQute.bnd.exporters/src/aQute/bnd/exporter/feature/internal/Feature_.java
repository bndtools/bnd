package aQute.bnd.exporter.feature.internal;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureBundle;
import org.osgi.service.feature.FeatureConfiguration;
import org.osgi.service.feature.FeatureExtension;
import org.osgi.service.feature.ID;

public class Feature_ implements Feature {

	public ID							id;

	public Optional<String>				name		= Optional.empty();

	public List<String>					categories		= new LinkedList<>();

	public Optional<String>				description	= Optional.empty();

	public Optional<String>						docURL			= Optional.empty();

	public Optional<String>				vendor		= Optional.empty();

	public Optional<String>				license		= Optional.empty();

	public Optional<String>				sCM			= Optional.empty();

	public boolean								complete		= false;

	public Map<String, FeatureConfiguration>	configurations	= new HashMap<>();

	public List<FeatureBundle>					bundles			= new LinkedList<>();

	public Map<String, FeatureExtension>		extensions		= new HashMap<>();

	public Map<String, Object>					variables		= new HashMap<>();

	@Override
	public ID getID() {
		return id;
	}

	@Override
	public Optional<String> getName() {
		return name;
	}

	@Override
	public List<String> getCategories() {
		return categories;
	}

	@Override
	public Optional<String> getDescription() {
		return description;
	}

	@Override
	public Optional<String> getDocURL() {
		return docURL;
	}

	@Override
	public Optional<String> getVendor() {
		return vendor;
	}

	@Override
	public Optional<String> getLicense() {
		return license;
	}

	@Override
	public Optional<String> getSCM() {
		return sCM;
	}

	@Override
	public boolean isComplete() {
		return complete;
	}

	@Override
	public List<FeatureBundle> getBundles() {
		return bundles;
	}

	@Override
	public Map<String, FeatureConfiguration> getConfigurations() {
		return configurations;
	}

	@Override
	public Map<String, FeatureExtension> getExtensions() {
		return extensions;
	}

	@Override
	public Map<String, Object> getVariables() {
		return variables;
	}

}
