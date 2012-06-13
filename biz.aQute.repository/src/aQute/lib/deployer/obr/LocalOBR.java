package aQute.lib.deployer.obr;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import aQute.bnd.service.OBRIndexProvider;
import aQute.bnd.service.OBRResolutionMode;
import aQute.lib.deployer.repository.LocalIndexedRepo;

@SuppressWarnings("deprecation")
public class LocalOBR extends LocalIndexedRepo implements OBRIndexProvider {

	@Override
	public synchronized void setProperties(Map<String, String> map) {
		super.setProperties(Conversions.convertConfig(map));
	}

	public Collection<URL> getOBRIndexes() throws IOException {
		try {
			return getIndexLocations();
		} catch (Exception e) {
			throw new IOException(e.toString());
		}
	}

	public Set<OBRResolutionMode> getSupportedModes() {
		return Conversions.convertResolutionPhases(supportedPhases);
	}

}
