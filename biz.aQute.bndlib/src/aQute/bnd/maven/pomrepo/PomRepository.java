package aQute.bnd.maven.pomrepo;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;

import aQute.bnd.annotation.plugin.*;
import aQute.bnd.build.*;
import aQute.bnd.header.*;
import aQute.bnd.maven.support.*;
import aQute.bnd.maven.support.Pom.Scope;
import aQute.bnd.osgi.*;
import aQute.bnd.service.*;
import aQute.bnd.util.dto.*;
import aQute.bnd.version.*;
import aQute.lib.collections.*;
import aQute.lib.converter.*;
import aQute.service.reporter.*;

@BndPlugin(name = "MavenPomRepo")
public class PomRepository implements RepositoryPlugin, Plugin, RegistryPlugin, Refreshable {
	interface Config {
		String location();

		URI repository();
	}

	Config				config;
	private Registry	registry;
	private Workspace	workspace;
	private Reporter	processor;
	private Maven		maven;
	private boolean		inited;
	private MultiMap<String,Artifact>	index	= new MultiMap<String,Artifact>(true);

	static class Artifact extends DTO {
		public String	groupId;
		public String	artifactId;
		public String	classifier;
		public String	mversion;
		public Version	version;
		public String	bsn;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bsn.hashCode();
			result = prime * result + version.hashCode();
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Artifact other = (Artifact) obj;
			if (!bsn.equals(other.bsn))
				return false;

			if (!version.equals(other.version))
				return false;

			return true;
		}
	}

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

		List<Artifact> artifacts = index.get(bsn);
		for (Artifact artifact : artifacts) {
			if (artifact.version.equals(version)) {
				MavenEntry entry = maven.getEntry(artifact.groupId, artifact.artifactId, artifact.mversion);
				if (entry != null) {
					return entry.getArtifact();
				}
			}
		}
		return null;
	}

	@Override
	public boolean canWrite() {
		return false;
	}

	@Override
	public List<String> list(String pattern) throws Exception {
		Instructions instrs = new Instructions(pattern);
		return instrs.select(index.keySet(), true);
	}

	@Override
	public SortedSet<Version> versions(String bsn) throws Exception {
		List<Artifact> list = index.get(bsn);
		if (list == null)
			return null;

		TreeSet<Version> versions = new TreeSet<Version>();

		for (Artifact e : list) {
			versions.add(e.version);
		}
		return versions;
	}

	@Override
	public String getName() {
		return "MavenPomRepo";
	}

	@Override
	public String getLocation() {
		return config.location();
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
				MavenEntry entry = maven.getEntry(pom);
				if (entry != null) {
					File artifact = entry.getArtifact();
					if (artifact != null && artifact.isFile()) {
						Artifact dto = new Artifact();
						dto.groupId = dep.getGroupId();
						dto.artifactId = dep.getArtifactId();
						dto.classifier = null;
						dto.mversion = dep.getVersion();
						dto.version = new Version(Analyzer.cleanupVersion(dto.mversion));

						Domain domain = Domain.domain(artifact);
						if (domain != null) {
							Entry<String,Attrs> bsn = domain.getBundleSymbolicName();
							if (bsn != null) {
								dto.bsn = bsn.getKey();
							}
						}

						if (dto.bsn == null)
							dto.bsn = dto.groupId + "__" + dto.artifactId;

						index.add(dto.bsn, dto);
					}
				}
			}
		}
	}

	@Override
	public boolean refresh() throws Exception {
		inited = false;
		return false;
	}

	@Override
	public File getRoot() {
		return null;
	}
}
