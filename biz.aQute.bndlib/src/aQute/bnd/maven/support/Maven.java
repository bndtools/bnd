package aQute.bnd.maven.support;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/*
 http://repository.springsource.com/maven/bundles/external/org/apache/coyote/com.springsource.org.apache.coyote/6.0.24/com.springsource.org.apache.coyote-6.0.24.pom
 http://repository.springsource.com/maven/bundles/external/org/apache/coyote/com.springsource.org.apache.coyote/6.0.24/com.springsource.org.apache.coyote-6.0.24.pom
 */
public class Maven {
	final File						userHome	= new File(System.getProperty("user.home"));
	final Map<String,MavenEntry>	entries		= new ConcurrentHashMap<String,MavenEntry>();
	final static String[]			ALGORITHMS	= {
			"md5", "sha1"
												};
	boolean							usecache	= false;
	final Executor					executor;
	File							m2			= new File(userHome, ".m2");
	File							repository	= new File(m2, "repository");

	public Maven(Executor executor) {
		if (executor == null)
			this.executor = Executors.newCachedThreadPool();
		else
			this.executor = executor;
	}

	// http://repo1.maven.org/maven2/junit/junit/maven-metadata.xml

	static Pattern	MAVEN_RANGE	= Pattern.compile("(\\[|\\()(.+)(,(.+))(\\]|\\))");

	public CachedPom getPom(String groupId, String artifactId, String version, URI... extra) throws Exception {
		MavenEntry entry = getEntry(groupId, artifactId, version);
		return entry.getPom(extra);
	}

	/**
	 * @param groupId
	 * @param artifactId
	 * @param version
	 * @param extra
	 * @return
	 * @throws Exception
	 */
	public MavenEntry getEntry(String groupId, String artifactId, String version) throws Exception {
		String path = path(groupId, artifactId, version);

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

	private String path(String groupId, String artifactId, String version) {
		return groupId.replace('.', '/') + '/' + artifactId + '/' + version + "/" + artifactId + "-" + version;
	}

	public void schedule(Runnable runnable) {
		if (executor == null)
			runnable.run();
		else
			executor.execute(runnable);
	}

	public ProjectPom createProjectModel(File file) throws Exception {
		ProjectPom pom = new ProjectPom(this, file);
		pom.parse();
		return pom;
	}

	public MavenEntry getEntry(Pom pom) throws Exception {
		return getEntry(pom.getGroupId(), pom.getArtifactId(), pom.getVersion());
	}

	public void setM2(File dir) {
		this.m2 = dir;
		this.repository = new File(dir, "repository");
	}

}
