package aQute.bnd.mavenplugin;

import org.apache.maven.artifact.handler.*;

public class BndArtifactHandler extends DefaultArtifactHandler {

	public BndArtifactHandler() {
		super("bundle");
		setExtension("jar");
		setLanguage("java");
		setAddedToClasspath(true);
		setIncludesDependencies(true);
	}
	
}
