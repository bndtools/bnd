package aQute.bnd.service;

import java.io.InputStream;

import aQute.bnd.build.Project;

/**
 * Deploy this artifact to maven.
 */
public interface Deploy {
	boolean deploy(Project project, String jarName, InputStream jarStream) throws Exception;
}
