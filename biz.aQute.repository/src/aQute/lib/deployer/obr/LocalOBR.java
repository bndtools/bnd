package aQute.lib.deployer.obr;

import java.util.*;

import aQute.lib.deployer.repository.*;

public class LocalOBR extends LocalIndexedRepo {

	@Override
	public synchronized void setProperties(Map<String,String> map) {
		super.setProperties(Conversions.convertConfig(map));
	}

}
