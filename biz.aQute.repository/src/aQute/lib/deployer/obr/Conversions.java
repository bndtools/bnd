package aQute.lib.deployer.obr;

import java.util.Map;

import aQute.lib.deployer.repository.AbstractIndexedRepo;
import aQute.lib.deployer.repository.FixedIndexedRepo;

@SuppressWarnings("deprecation")
public class Conversions {

	public static final Map<String,String> convertConfig(Map<String,String> map) {
		if (!map.containsKey(AbstractIndexedRepo.PROP_REPO_TYPE))
			map.put(AbstractIndexedRepo.PROP_REPO_TYPE, AbstractIndexedRepo.REPO_TYPE_OBR);

		String location = map.get("location");
		if (location != null)
			map.put(FixedIndexedRepo.PROP_LOCATIONS, location);
		
		String mode = map.get("mode");
		if (mode != null)
			map.put(AbstractIndexedRepo.PROP_RESOLUTION_PHASE, mode);

		return map;
	}

}
