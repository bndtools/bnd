package aQute.lib.deployer.obr;

import java.util.Map;

import aQute.lib.deployer.repository.LocalIndexedRepo;

@SuppressWarnings("deprecation")
public class LocalOBR extends LocalIndexedRepo {

	@Override
	public synchronized void setProperties(Map<String,String> map) {
		super.setProperties(Conversions.convertConfig(map));
	}

}
