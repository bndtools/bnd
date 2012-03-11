package aQute.bnd.service;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Set;

public interface IndexProvider {

	List<URL> getIndexLocations() throws IOException;

	Set<ResolutionPhase> getSupportedPhases();

}
