package aQute.bnd.service;

import java.io.*;
import java.net.*;
import java.util.*;

@Deprecated
public interface OBRIndexProvider {
	Collection<URL> getOBRIndexes() throws IOException;
	Set<OBRResolutionMode> getSupportedModes();
}
