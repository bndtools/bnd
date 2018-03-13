package aQute.bnd.repository.p2.provider;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.build.Workspace;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.repository.BaseRepository;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.Refreshable;
import aQute.bnd.service.Registry;
import aQute.bnd.service.RegistryPlugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;
import aQute.lib.converter.Converter;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
import aQute.service.reporter.Reporter;

/**
 * A p2 repository
 */
@BndPlugin(name = "p2", parameters = P2Config.class)
public class P2Repository extends BaseRepository
	implements Plugin, RegistryPlugin, RepositoryPlugin, Refreshable, Closeable {
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

			return new P2Indexer(reporter, location, client, url, name);
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
		return getP2Index().location.getPath();
	}

	@Override
	public void setProperties(Map<String, String> map) throws Exception {
		this.config = Converter.cnv(P2Config.class, map);
		this.name = this.config.name("p2-" + config.url());
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
		return getP2Index().location;
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

}
