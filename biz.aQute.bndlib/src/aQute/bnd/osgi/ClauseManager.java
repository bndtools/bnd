package aQute.bnd.osgi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.service.parameters.ConsolidateParameters;

class ClauseManager {
	final static Logger				logger		= LoggerFactory.getLogger(ClauseManager.class);
	final Processor					processor;

	final Map<String, Parameters>	parameters	= new HashMap<>();

	ClauseManager(Processor processor) {
		this.processor = processor;
	}

	void addClause(String key, String name, Attrs ps) {
		Parameters p = getParameters(key);
		p.add(name, ps);
	}

	Parameters getParameters(String key) {
		return parameters.computeIfAbsent(key, k -> {
			String property = processor.getProperty(key);
			return new Parameters(property);
		});
	}

	Optional<Parameters> getOptionalParameters(String key) {
		return Optional.ofNullable(parameters.get(key));
	}

	void reset(String key) {
		if (parameters.remove(key) != null) {
			logger.info(
				"a clause was set but then cleared due a property overwrite. This might indicate that not all code treats the  header {} the same",
				key);
		}
	}

	void reset() {
		parameters.clear();
	}

	void consolidate(String key) {
		Parameters parameters = this.parameters.get(key);
		if (parameters == null)
			return;

		try {
			for (ConsolidateParameters cp : processor.getPlugins(ConsolidateParameters.class)) {
				Parameters consolidated = cp.consolidate(key, parameters);
				if (consolidated != null) {
					processor.setProperty(key, consolidated.toString());
					return;
				}
			}
			processor.setProperty(key, parameters.toString());
		} finally {
			assert !this.parameters.containsKey(key) : "should be cleared by processor";
		}
	}

	void consolidate() {
		for (String key : new ArrayList<>(parameters.keySet())) {
			consolidate(key);
		}
	}
}
