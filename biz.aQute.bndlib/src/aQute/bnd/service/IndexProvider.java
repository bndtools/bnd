package aQute.bnd.service;

import java.net.URI;
import java.util.List;
import java.util.Set;

public interface IndexProvider {

	List<URI> getIndexLocations() throws Exception;

	Set<ResolutionPhase> getSupportedPhases();

}
