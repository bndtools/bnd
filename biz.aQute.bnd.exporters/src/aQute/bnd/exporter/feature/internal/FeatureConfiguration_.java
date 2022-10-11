package aQute.bnd.exporter.feature.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.feature.FeatureConfiguration;

public class FeatureConfiguration_ implements FeatureConfiguration {

	public String			pid;

	public Optional<String>	factoryPid	= Optional.empty();

	Map<String, Object>		values		= new HashMap<>();

	@Override
	public String getPid() {
		return pid;
	}

	@Override
	public Optional<String> getFactoryPid() {
		return factoryPid;
	}

	@Override
	public Map<String, Object> getValues() {
		return values;
	}

}
