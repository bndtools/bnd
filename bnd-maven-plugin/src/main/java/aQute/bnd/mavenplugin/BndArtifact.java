package aQute.bnd.mavenplugin;

import org.apache.maven.artifact.*;

import aQute.bnd.build.*;

/**
 * Provides a Maven artifact based on a bnd Container.
 *
 */
public class BndArtifact extends DefaultArtifact {

	private final Container container;

	public BndArtifact(Container entry) {
		super("osgi", entry.getBundleSymbolicName(), entry.getVersion(), "system", "jar", "", new BndArtifactHandler());
		this.container = entry;
		this.setFile(container.getFile());
		this.setResolved(true);
	}

}
