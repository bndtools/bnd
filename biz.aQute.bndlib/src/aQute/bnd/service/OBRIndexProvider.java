package aQute.bnd.service;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Set;

public interface OBRIndexProvider {
	Collection<URL> getOBRIndexes() throws IOException;
	Set<OBRResolutionMode> getSupportedModes();
}
