package aQute.bnd.repository.p2.provider;

import static aQute.bnd.service.tags.Tags.parse;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.build.Workspace;
import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.repository.BaseRepository;
import aQute.bnd.service.Actionable;
import aQute.bnd.service.FeatureProvider;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.Refreshable;
import aQute.bnd.service.Registry;
import aQute.bnd.service.RegistryPlugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;
import aQute.lib.converter.Converter;
import aQute.lib.io.IO;
import aQute.p2.packed.Unpack200;
import aQute.p2.provider.Feature;
import aQute.service.reporter.Reporter;

/**
 * A p2 repository
 */
@BndPlugin(name = "P2 Repo", parameters = P2Config.class)
public class P2Repository extends BaseRepository
	implements Plugin, RegistryPlugin, RepositoryPlugin, Refreshable, Closeable, Actionable, FeatureProvider {
	private P2Config	config;
	private Registry	registry;
	private Workspace	workspace;
	private P2Indexer	p2Index;
	private Reporter	reporter;
	private String		name;

	@Override
	public File get(String bsn, Version version, Map<String, String> properties, DownloadListener... listeners)
		throws Exception {
		return getP2Index().get(bsn, version, properties, listeners);

	}

	private synchronized P2Indexer getP2Index() {
		if (p2Index != null)
			return p2Index;

		return p2Index = getP2Index0();
	}

	P2Indexer getP2Index0() {
		this.workspace = registry.getPlugin(Workspace.class);
		HttpClient client = registry.getPlugin(HttpClient.class);
		URI url = config.url();

		if (url == null)
			throw new IllegalArgumentException("For a p2 repository you must set the url parameter to the repository");

		try {
			name = config.name(client.toName(url));
			File location = workspace.getFile(config.location("cnf/cache/p2-" + name));
			IO.mkdirs(location);
			File indexFile = new File(location, "index.xml.gz");

			return new P2Indexer(new Unpack200(this.workspace), reporter, location, client, url, name);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	@Override
	public boolean canWrite() {
		return false;
	}

	@Override
	public List<String> list(String pattern) throws Exception {
		return getP2Index().list(pattern);
	}

	@Override
	public SortedSet<Version> versions(String bsn) throws Exception {
		return getP2Index().versions(bsn);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getLocation() {
		P2Indexer index = getP2Index();
		return index.location.getPath();
	}

	@Override
	public void setProperties(Map<String, String> map) throws Exception {
		this.config = Converter.cnv(P2Config.class, map);
		this.name = this.config.name("p2-" + config.url());
		super.setTags(parse(config.tags(), DEFAULT_REPO_TAGS));
	}

	@Override
	public void setReporter(Reporter reporter) {
		this.reporter = reporter;
	}

	@Override
	public void setRegistry(Registry registry) {
		this.registry = registry;
	}

	@Override
	public boolean refresh() throws Exception {
		getP2Index().refresh();
		return true;
	}

	@Override
	public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
		return getP2Index().getBridge()
			.getRepository()
			.findProviders(requirements);
	}

	@Override
	public File getRoot() throws Exception {
		P2Indexer index = getP2Index();
		return index.location;
	}

	@Override
	public PutResult put(InputStream stream, PutOptions options) throws Exception {
		throw new UnsupportedOperationException("Cannot write to a p2 repo ");
	}

	@Override
	public void close() throws IOException {
		IO.close(p2Index);
	}

	@Override
	public String toString() {
		return "P2Repository [" + getName() + "]";
	}

	@Override
	public Map<String, Runnable> actions(Object... target) throws Exception {
		if (target.length == 0) {
			if (p2Index.indexFile.isFile()) {
				Map<String, Runnable> menu = new LinkedHashMap<>();
				menu.put("Refresh from " + p2Index.url, () -> {
					try {
						workspace.writeLocked(() -> {
							p2Index.reread();
							workspace.refresh();
							return null;
						});
					} catch (Exception e) {
						throw Exceptions.duck(e);
					}
				});
				return menu;
			}
		}
		return null;
	}

	@Override
	public String tooltip(Object... target) throws Exception {
		if (target.length == 0) {

			return "P2: " + name + "\n" //
				+ "index      " + p2Index.indexFile.getAbsolutePath() + "\n" //
				+ "uri        " + p2Index.url + "\n" //
				+ "hash       " + p2Index.urlHash;
		}
		return null;
	}

	@Override
	public String title(Object... target) throws Exception {
		return null;
	}

	@Override
	public List<Feature> getFeatures() throws Exception {
		return getP2Index().getFeatures();
	}

	@Override
	public Feature getFeature(String id, String version) throws Exception {
		return getP2Index().getFeature(id, version);
	}

}
