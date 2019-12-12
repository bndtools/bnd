package aQute.bnd.maven.support;

import java.io.File;
import java.net.URI;

import aQute.libg.reporter.ReporterAdapter;
import aQute.service.reporter.Reporter;

public class CachedPom extends Pom {
	final MavenEntry maven;

	CachedPom(MavenEntry mavenEntry, URI repo) throws Exception {
		this(mavenEntry, repo, new ReporterAdapter());
	}

	CachedPom(MavenEntry mavenEntry, URI repo, Reporter reporter) throws Exception {
		super(mavenEntry.maven, mavenEntry.getPomFile(), repo, reporter);
		this.maven = mavenEntry;
	}

	@Override
	public File getArtifact() throws Exception {
		return maven.getArtifact();
	}

	public MavenEntry getMavenEntry() {
		return maven;
	}

}
