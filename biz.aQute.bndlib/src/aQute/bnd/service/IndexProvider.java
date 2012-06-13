package aQute.bnd.service;

import java.net.*;
import java.util.*;

public interface IndexProvider {

	List<URL> getIndexLocations() throws Exception;

	Set<ResolutionPhase> getSupportedPhases();

}
