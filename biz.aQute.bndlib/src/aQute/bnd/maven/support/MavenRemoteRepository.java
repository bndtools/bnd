package aQute.bnd.maven.support;

import java.io.*;
import java.net.*;
import java.util.*;

import aQute.bnd.service.*;
import aQute.bnd.version.*;
import aQute.lib.io.*;
import aQute.service.reporter.*;

public class MavenRemoteRepository implements RepositoryPlugin, RegistryPlugin, Plugin {
	Reporter	reporter;
	URI[]		repositories;
	Registry	registry;
	Maven		maven;

	public File get(String bsn, String version, Strategy strategy, Map<String,String> properties) throws Exception {
		String groupId = null;

		if (properties != null)
			groupId = properties.get("groupId");

		if (groupId == null) {
			int n = bsn.indexOf('+');
			if (n < 0)
				return null;

			groupId = bsn.substring(0, n);
			bsn = bsn.substring(n + 1);
		}

		String artifactId = bsn;

		if (version == null) {
			if (reporter != null)
				reporter.error("Maven dependency version not set for %s - %s", groupId, artifactId);
			return null;
		}

		CachedPom pom = getMaven().getPom(groupId, artifactId, version, repositories);

		String value = properties == null ? null : properties.get("scope");
		if (value == null)
			return pom.getArtifact();

		Pom.Scope action = null;

		try {
			action = Pom.Scope.valueOf(value);
			return pom.getLibrary(action, repositories);
		}
		catch (Exception e) {
			return pom.getArtifact();
		}
	}

	public Maven getMaven() {
		if (maven != null)
			return maven;

		maven = registry.getPlugin(Maven.class);
		return maven;
	}

	public boolean canWrite() {
		return false;
	}

	public PutResult put(InputStream stream, PutOptions options) throws Exception {
		throw new UnsupportedOperationException("cannot do put");
	}

	public List<String> list(String regex) throws Exception {
		throw new UnsupportedOperationException("cannot do list");
	}

	public List<Version> versions(String bsn) throws Exception {
		throw new UnsupportedOperationException("cannot do versions");
	}

	public String getName() {
		return "maven";
	}

	public void setRepositories(URI... urls) {
		repositories = urls;
	}

	public void setProperties(Map<String,String> map) {
		String repoString = map.get("repositories");
		if (repoString != null) {
			String[] repos = repoString.split("\\s*,\\s*");
			repositories = new URI[repos.length];
			int n = 0;
			for (String repo : repos) {
				try {
					URI uri = new URI(repo);
					if (!uri.isAbsolute())
						uri = IO.getFile(new File(""), repo).toURI();
					repositories[n++] = uri;
				}
				catch (Exception e) {
					if (reporter != null)
						reporter.error("Invalid repository %s for maven plugin, %s", repo, e);
				}
			}
		}
	}

	public void setReporter(Reporter reporter) {
		this.reporter = reporter;
	}

	public void setRegistry(Registry registry) {
		this.registry = registry;
	}

	public void setMaven(Maven maven) {
		this.maven = maven;
	}

	public String getLocation() {
		if (repositories == null || repositories.length == 0)
			return "maven central";

		return Arrays.toString(repositories);
	}
}
