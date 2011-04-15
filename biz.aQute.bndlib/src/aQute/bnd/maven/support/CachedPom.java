package aQute.bnd.maven.support;

import java.io.*;
import java.net.*;

public class CachedPom extends Pom {
	final MavenEntry				maven;

	CachedPom(MavenEntry mavenEntry, File file, URI repo) throws Exception {
		super(mavenEntry.maven, file, repo);
		this.maven = mavenEntry;
	}

	public File getArtifact() throws Exception {
		return maven.getArtifact();
	}

}
