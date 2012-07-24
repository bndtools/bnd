package aQute.bnd.service;

import java.net.*;
import java.util.*;

public interface IndexProvider {

	List<URI> getIndexLocations() throws Exception;

	Set<ResolutionPhase> getSupportedPhases();

}
