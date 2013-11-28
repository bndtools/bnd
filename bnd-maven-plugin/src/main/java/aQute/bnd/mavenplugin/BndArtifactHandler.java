package aQute.bnd.mavenplugin;

import org.apache.maven.artifact.handler.*;

/**
 * Have no clue what this is supposed to do but we seem to have needed it.
 */
public class BndArtifactHandler extends DefaultArtifactHandler {

	public BndArtifactHandler() {
		super("bundle");
		setExtension("jar");
		setLanguage("java");
		setAddedToClasspath(true);
		setIncludesDependencies(true);
	}
	
}
