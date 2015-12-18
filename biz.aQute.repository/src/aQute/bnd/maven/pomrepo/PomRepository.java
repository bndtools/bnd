package aQute.bnd.maven.pomrepo;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.build.Workspace;
import aQute.bnd.maven.support.Maven;
import aQute.bnd.maven.support.MavenEntry;
import aQute.bnd.maven.support.Pom;
import aQute.bnd.maven.support.Pom.Scope;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.Refreshable;
import aQute.bnd.service.Registry;
import aQute.bnd.service.RegistryPlugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;
import aQute.lib.converter.Converter;
import aQute.service.reporter.Reporter;

@BndPlugin(name = "MavenPomRepo")
public class PomRepository implements RepositoryPlugin, Plugin, RegistryPlugin, Refreshable {
	interface Config {
		String location();

		URI repository();
	}

	Config				config;
	@SuppressWarnings("unused")
	private Registry	registry;
	private Workspace	workspace;
	@SuppressWarnings("unused")
	private Reporter	processor;
	private Maven		maven;
	private boolean		inited;

	@Override
	public PutResult put(InputStream stream, PutOptions options) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File get(String bsn, Version version, Map<String,String> properties, DownloadListener... listeners)
			throws Exception {
		String groupId = properties.get("group");
		String artifactId = properties.get("artifact");
		String mversion = properties.get("version");
		if (groupId == null && artifactId != null && mversion != null) {
			MavenEntry entry = maven.getEntry(groupId, artifactId, mversion);
			return entry.getArtifact();
		}

		init();

		return null;
	}

	@Override
	public boolean canWrite() {
		return false;
	}

	@Override
	public List<String> list(String pattern) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SortedSet<Version> versions(String bsn) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getLocation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setProperties(Map<String,String> map) throws Exception {
		config = Converter.cnv(Config.class, map);
	}

	@Override
	public void setReporter(Reporter processor) {
		this.processor = processor;

	}

	@Override
	public void setRegistry(Registry registry) {
		this.registry = registry;
		this.workspace = registry.getPlugin(Workspace.class);
		this.maven = this.workspace.getMaven();
	}

	void init() throws Exception {
		if (inited)
			return;

		inited = true;
		File f = workspace.getFile(config.location());
		if (f.isFile()) {
			Pom pom = maven.createProjectModel(f);
			for (Pom dep : pom.getDependencies(Scope.compile, config.repository())) {

			}
		}
	}

	@Override
	public boolean refresh() throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public File getRoot() {
		// TODO Auto-generated method stub
		return null;
	}
}
