package aQute.bnd.maven.support;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import aQute.bnd.service.Plugin;
import aQute.bnd.service.Registry;
import aQute.bnd.service.RegistryPlugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.Strategy;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;
import aQute.service.reporter.Reporter;

public class MavenRemoteRepository implements RepositoryPlugin, RegistryPlugin, Plugin {
	Reporter	reporter;
	URI[]		repositories;
	Registry	registry;
	Maven		maven;

	public File get(String bsn, String version, Strategy strategy, Map<String, String> properties) throws Exception {
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
		} catch (Exception e) {
			return pom.getArtifact();
		}
	}

	public Maven getMaven() {
		if (maven != null)
			return maven;

		maven = registry.getPlugin(Maven.class);
		return maven;
	}

	@Override
	public boolean canWrite() {
		return false;
	}

	@Override
	public PutResult put(InputStream stream, PutOptions options) throws Exception {
		throw new UnsupportedOperationException("cannot do put");
	}

	@Override
	public List<String> list(String regex) throws Exception {
		throw new UnsupportedOperationException("cannot do list");
	}

	@Override
	public SortedSet<Version> versions(String bsn) throws Exception {
		throw new UnsupportedOperationException("cannot do versions");
	}

	@Override
	public String getName() {
		return "maven";
	}

	public void setRepositories(URI... urls) {
		repositories = urls;
	}

	@Override
	public void setProperties(Map<String, String> map) {
		String repoString = map.get("repositories");
		if (repoString != null) {
			String[] repos = repoString.split("\\s*,\\s*");
			repositories = new URI[repos.length];
			int n = 0;
			for (String repo : repos) {
				try {
					URI uri = new URI(repo);
					if (!uri.isAbsolute())
						uri = IO.getFile(new File(""), repo)
							.toURI();
					repositories[n++] = uri;
				} catch (Exception e) {
					if (reporter != null)
						reporter.error("Invalid repository %s for maven plugin, %s", repo, e);
				}
			}
		}
	}

	@Override
	public void setReporter(Reporter reporter) {
		this.reporter = reporter;
	}

	@Override
	public void setRegistry(Registry registry) {
		this.registry = registry;
	}

	public void setMaven(Maven maven) {
		this.maven = maven;
	}

	@Override
	public String getLocation() {
		if (repositories == null || repositories.length == 0)
			return "maven central";

		return Arrays.toString(repositories);
	}

	@Override
	public File get(String bsn, Version version, Map<String, String> properties, DownloadListener... listeners)
		throws Exception {
		File f = get(bsn, version.toString(), Strategy.EXACT, properties);
		if (f == null)
			return null;

		for (DownloadListener l : listeners) {
			try {
				l.success(f);
			} catch (Exception e) {
				reporter.exception(e, "Download listener for %s", f);
			}
		}
		return f;
	}

}
