package aQute.bnd.service;

import aQute.bnd.build.*;
import aQute.lib.osgi.*;

/**
 * Deploy this artifact to maven.
 */
public interface Deploy {
	boolean deploy(Project project, Jar jar) throws Exception;
}
