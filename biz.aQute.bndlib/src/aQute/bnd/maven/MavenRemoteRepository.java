package aQute.bnd.maven;

import java.io.*;
import java.util.*;

import aQute.bnd.build.*;
import aQute.bnd.service.*;
import aQute.lib.osgi.*;
import aQute.libg.reporter.*;
import aQute.libg.version.*;

public class MavenRemoteRepository implements RepositoryPlugin {
	Reporter	reporter;
	Maven		maven	= new Maven();

	public Container getBundle(String groupId, String artifactId, String version, int strategyx,
			Map<String, String> attrs) {
		return null;
	}

	public File[] get(String bsn, String range) throws Exception {
		File f = get(bsn, range, Strategy.HIGHEST);
		if (f == null)
			return null;

		return new File[] { f };
	}

	public File get(String bsn, String version, Strategy strategy) throws Exception {
		String[] parts = bsn.split("+");
		if (parts.length != 2)
			return null;

		String groupId = parts[0];
		String artifactId = parts[1];

		if (version != null) {
			reporter.error("Maven dependency version not set for %s - %s", groupId, artifactId);
			return null;
		}
		if (!Verifier.VERSION.matcher(version).matches()) {
			reporter.error(
					"Invalid version %s for maven dependency %s - %s. For maven, ranges are not allowed",
					version, groupId, artifactId);
			return null;
		}

		Pom pom = maven.getPom(groupId, artifactId, version);

		return null;
	}

	public boolean canWrite() {
		// TODO Auto-generated method stub
		return false;
	}

	public File put(Jar jar) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	public List<String> list(String regex) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Version> versions(String bsn) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

}
