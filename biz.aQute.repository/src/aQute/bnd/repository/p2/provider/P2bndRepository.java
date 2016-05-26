package aQute.bnd.repository.p2.provider;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.util.function.Function;
import org.osgi.util.promise.Promise;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.build.Workspace;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.repository.BaseRepository;
import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.repository.XMLResourceGenerator;
import aQute.bnd.osgi.repository.XMLResourceParser;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.service.Actionable;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.Refreshable;
import aQute.bnd.service.Registry;
import aQute.bnd.service.RegistryPlugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;
import aQute.lib.converter.Converter;
import aQute.libg.cryptography.SHA256;
import aQute.p2.api.Artifact;
import aQute.p2.provider.P2Impl;
import aQute.service.reporter.Reporter;

@BndPlugin(name = "P2", parameters = P2Config.class)
public class P2bndRepository extends BaseRepository
		implements Plugin, RegistryPlugin, RepositoryPlugin, Refreshable, Actionable {

	private P2Config			config;
	private Reporter			reporter;
	private File				location;
	private boolean				inited;
	private Registry			registry;
	private Workspace			workspace;
	private HttpClient			client;
	private URI					url;
	private String				name;
	private ResourcesRepository	repository;

	@Override
	public Map<String,Runnable> actions(Object... target) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String tooltip(Object... target) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String title(Object... target) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File getRoot() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PutResult put(InputStream stream, PutOptions options) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File get(String bsn, Version version, Map<String,String> properties, DownloadListener... listeners)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean canWrite() {
		// TODO Auto-generated method stub
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
		return location.getAbsolutePath();
	}

	@Override
	public void setProperties(Map<String,String> map) throws Exception {
		this.config = Converter.cnv(P2Config.class, map);
	}

	@Override
	public void setReporter(Reporter reporter) {
		this.reporter = reporter;
	}

	@Override
	public void setRegistry(Registry registry) {
		this.registry = registry;
	}

	void init() {
		if (inited)
			return;

		inited = true;
		this.workspace = registry.getPlugin(Workspace.class);
		this.client = registry.getPlugin(HttpClient.class);
		this.url = config.url();
		if (this.url == null)
			throw new IllegalArgumentException("For a p2 repository you must set the url parameter to the repository");

		try {
			this.name = config.name(this.client.toName(this.url));
			location = workspace.getFile(config.location("cnf/cache/" + name + ".json"));
			if (location.isFile()) {
				readFile();
				return;
			} else {
				refresh();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	void readFile() throws Exception {
		try (XMLResourceParser xp = new XMLResourceParser(this.location.toURI())) {
			this.repository = new ResourcesRepository(xp.parse());
		}
	}

	@Override
	public boolean refresh() throws Exception {
		P2Impl p2 = new P2Impl(client, this.url, Processor.getExecutor());
		Map<Artifact,Promise<Resource>> fetched = new HashMap<>();
		List<URI> uris = new ArrayList<>();

		for (final Artifact a : p2.getArtifacts()) {

			Promise<Resource> promise = client.build()
					.useCache(TimeUnit.DAYS.toMillis(100))
					.async(a.uri.toURL())
					.map(new Function<File,Resource>() {

						@Override
						public Resource apply(File file) {
							try {
								ResourceBuilder rb = new ResourceBuilder();
								Domain manifest = Domain.domain(file);
								rb.addManifest(manifest);

								String sha256 = SHA256.digest(file).asHex();
								rb.addContentCapability(a.uri, sha256, file.length(), "vnd.osgi.bundle");
								return rb.build();
							} catch (Exception e) {
								e.printStackTrace();
								throw new RuntimeException(e);
							}
						}
					});

			fetched.put(a, promise);
		}
		this.repository = new ResourcesRepository();

		for (Entry<Artifact,Promise<Resource>> e : fetched.entrySet()) {
			Promise<Resource> resource = e.getValue();
			Artifact a = e.getKey();

			if (resource.getFailure() == null) {
				this.repository.add(resource.getValue());
			} else {
				System.out.println(a + " : " + resource.getFailure());
			}
		}

		XMLResourceGenerator xrg = new XMLResourceGenerator();
		xrg.setRepository(this.repository);
		xrg.setName(name);
		xrg.save(location);
		return true;
	}

	@Override
	public Map<Requirement,Collection<Capability>> findProviders(Collection< ? extends Requirement> requirements) {
		init();
		return repository.findProviders(requirements);
	}
}
