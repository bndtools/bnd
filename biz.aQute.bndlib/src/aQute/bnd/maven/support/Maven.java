package aQute.bnd.maven.support;

import java.io.File;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import aQute.lib.io.IO;
import aQute.libg.reporter.ReporterAdapter;
import aQute.service.reporter.Reporter;

/*
http://repository.springsource.com/maven/bundles/external/org/apache/coyote/com.springsource.org.apache.coyote/6.0.24/com.springsource.org.apache.coyote-6.0.24.pom
http://repository.springsource.com/maven/bundles/external/org/apache/coyote/com.springsource.org.apache.coyote/6.0.24/com.springsource.org.apache.coyote-6.0.24.pom
 */
public class Maven {

	final Map<String, MavenEntry>	entries				= new ConcurrentHashMap<>();
	final static String[]			ALGORITHMS			= {
		"md5", "sha1"
	};
	boolean							usecache			= false;
	final Executor					executor;
	final Reporter					reporter;
	static final String				MAVEN_REPO_LOCAL	= System.getProperty("maven.repo.local", "~/.m2/repository");

	File							repository			= IO.getFile(MAVEN_REPO_LOCAL);

	public Maven(Executor executor) {
		this(executor, new ReporterAdapter());
	}

	public Maven(Executor executor, Reporter reporter) {
		if (executor == null)
			this.executor = Executors.newCachedThreadPool();
		else
			this.executor = executor;
		this.reporter = reporter;
	}

	// https://repo.maven.apache.org/maven2/junit/junit/maven-metadata.xml

	public CachedPom getPom(String groupId, String artifactId, String version, URI... extra) throws Exception {
		MavenEntry entry = getEntry(groupId, artifactId, version);
		return entry.getPom(extra);
	}

	/**
	 * @param groupId
	 * @param artifactId
	 * @param version
	 * @throws Exception
	 */
	public MavenEntry getEntry(String groupId, String artifactId, String version) throws Exception {
		String path = dirpath(groupId, artifactId, version);

		MavenEntry entry;
		synchronized (entries) {
			entry = entries.get(path);
			if (entry != null)
				return entry;

			entry = new MavenEntry(this, path);
			entries.put(path, entry);
		}
		return entry;
	}

	private String dirpath(String groupId, String artifactId, String version) {
		return groupId.replace('.', '/') + '/' + artifactId + '/' + version + "/" + artifactId + "-" + version;
	}

	public void schedule(Runnable runnable) {
		if (executor == null)
			runnable.run();
		else
			executor.execute(runnable);
	}

	public ProjectPom createProjectModel(File file) throws Exception {
		ProjectPom pom = new ProjectPom(this, file, reporter);
		pom.parse();
		return pom;
	}

	public MavenEntry getEntry(Pom pom) throws Exception {
		return getEntry(pom.getGroupId(), pom.getArtifactId(), pom.getVersion());
	}

	public void setM2(File dir) {
		this.repository = new File(dir, "repository");
	}

	@Override
	public String toString() {
		return "Maven [" + (repository != null ? "m2=" + repository : "") + "]";
	}
}
