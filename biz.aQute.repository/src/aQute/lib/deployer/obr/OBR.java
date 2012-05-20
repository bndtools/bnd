package aQute.lib.deployer.obr;

import java.io.*;
import java.net.*;
import java.util.*;

import aQute.bnd.service.*;
import aQute.lib.deployer.repository.*;

/**
 * A simple read-only OBR-based repository that uses a list of index locations
 * and a basic local cache.
 * <p>
 * <h2>Properties</h2>
 * <ul>
 * <li><b>locations:</b> comma-separated list of index URLs. <b>NB:</b> surround
 * with single quotes!</li>
 * <li><b>name:</b> repository name; defaults to the index URLs.
 * <li><b>cache:</b> local cache directory. May be omitted, in which case the
 * repository will only be able to serve resources with {@code file:} URLs.</li>
 * <li><b>location:</b> (deprecated) alias for "locations".
 * </ul>
 * <p>
 * <h2>Example</h2>
 * 
 * <pre>
 * -plugin: aQute.lib.deployer.obr.OBR;locations='http://www.example.com/repository.xml';cache=${workspace}/.cache
 * </pre>
 * 
 * @author Neil Bartlett
 */
@SuppressWarnings("deprecation")
public class OBR extends FixedIndexedRepo implements OBRIndexProvider {

	@Override
	public synchronized void setProperties(Map<String,String> map) {
		super.setProperties(Conversions.convertConfig(map));
	}

	public Collection<URI> getOBRIndexes() throws IOException {
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
