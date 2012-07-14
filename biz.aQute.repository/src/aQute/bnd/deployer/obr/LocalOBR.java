package aQute.bnd.deployer.obr;

import java.util.*;

import aQute.bnd.deployer.repository.*;

public class LocalOBR extends LocalIndexedRepo {

	@Override
	public synchronized void setProperties(Map<String,String> map) {
		super.setProperties(Conversions.convertConfig(map));
	}

}
