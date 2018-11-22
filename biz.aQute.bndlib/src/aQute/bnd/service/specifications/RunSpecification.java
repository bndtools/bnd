package aQute.bnd.service.specifications;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import aQute.lib.exceptions.Exceptions;

/**
 * A specification for the run information to start a framework
 */
public class RunSpecification implements Cloneable {
	public String							target;
	public String							bin;
	public String							bin_test;
	public List<String>						runbundles				= new ArrayList<>();
	public List<String>						runpath					= new ArrayList<>();
	public Map<String, Map<String, String>>	extraSystemPackages		= new LinkedHashMap<>();
	public Map<String, Map<String, String>>	extraSystemCapabilities	= new LinkedHashMap<>();
	public Map<String, String>				properties				= new LinkedHashMap<>();
	public List<String>						errors					= new ArrayList<>();
	public List<String>						runfw					= new ArrayList<>();

	public RunSpecification clone() {
		try {
			return (RunSpecification) super.clone();
		} catch (CloneNotSupportedException e) {
			throw Exceptions.duck(e);
		}
	}

	public void mergeWith(RunSpecification spec) {
		if (spec.target != null)
			target = spec.target;
		if (spec.bin != null)
			bin = spec.bin;
		if (spec.bin_test != null)
			bin_test = spec.bin_test;
		runfw = spec.runfw;
		runbundles.addAll(spec.runbundles);
		runpath.addAll(spec.runpath);
		putAll(extraSystemCapabilities, spec.extraSystemCapabilities);
		putAll(extraSystemPackages, spec.extraSystemPackages);
		properties.putAll(spec.properties);
		errors.addAll(spec.errors);
	}

	private void putAll(Map<String, Map<String, String>> to, Map<String, Map<String, String>> from) {
		for (Map.Entry<String, Map<String, String>> e : from.entrySet()) {
			String key = e.getKey();
			while (to.containsKey(key))
				key += "~";
			to.put(key, e.getValue());
		}
	}
}
