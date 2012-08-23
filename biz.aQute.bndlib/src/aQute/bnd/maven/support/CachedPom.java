package aQute.bnd.maven.support;

import java.io.*;
import java.net.*;

public class CachedPom extends Pom {
	final MavenEntry	maven;

	CachedPom(MavenEntry mavenEntry, URI repo) throws Exception {
		super(mavenEntry.maven, mavenEntry.getPomFile(), repo);
		this.maven = mavenEntry;
	}

	@Override
	public File getArtifact() throws Exception {
		return maven.getArtifact();
	}

}
