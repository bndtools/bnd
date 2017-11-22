package aQute.bnd.deployer.obr;

import java.util.Map;

import aQute.bnd.deployer.repository.AbstractIndexedRepo;

public class Conversions {

	/**
	 * Converts legacy repository properties to new property names.
	 * 
	 * @param map
	 */
	public static final Map<String,String> convertConfig(Map<String,String> map) {
		if (!map.containsKey(AbstractIndexedRepo.PROP_REPO_TYPE))
			map.put(AbstractIndexedRepo.PROP_REPO_TYPE, AbstractIndexedRepo.REPO_TYPE_OBR);

		String location = map.get("location");
		if (location != null)
			map.put(OBR.PROP_LOCATIONS, location);

		String mode = map.get("mode");
		if (mode != null)
			map.put(AbstractIndexedRepo.PROP_RESOLUTION_PHASE, mode);

		return map;
	}

}
