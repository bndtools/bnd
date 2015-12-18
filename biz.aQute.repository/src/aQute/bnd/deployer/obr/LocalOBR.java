package aQute.bnd.deployer.obr;

import java.util.Map;

import aQute.bnd.deployer.repository.LocalIndexedRepo;

public class LocalOBR extends LocalIndexedRepo {

	@Override
	public synchronized void setProperties(Map<String,String> map) {
		super.setProperties(Conversions.convertConfig(map));
	}

}
