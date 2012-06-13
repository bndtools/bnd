package aQute.lib.deployer.obr;

import java.io.*;
import java.net.*;
import java.util.*;

import aQute.bnd.service.*;
import aQute.lib.deployer.repository.*;

@SuppressWarnings("deprecation")
public class LocalOBR extends LocalIndexedRepo implements OBRIndexProvider {

	@Override
	public synchronized void setProperties(Map<String,String> map) {
		super.setProperties(Conversions.convertConfig(map));
	}

	public Collection<URL> getOBRIndexes() throws IOException {
		try {
			return getIndexLocations();
		}
		catch (Exception e) {
			throw new IOException(e.toString());
		}
	}

	public Set<OBRResolutionMode> getSupportedModes() {
		return Conversions.convertResolutionPhases(supportedPhases);
	}

}
