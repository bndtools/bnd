package aQute.bnd.repository.p2.provider;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
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
import org.osgi.service.repository.Repository;
import org.osgi.util.function.Function;
import org.osgi.util.promise.Promise;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.repository.BridgeRepository;
import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.repository.XMLResourceGenerator;
import aQute.bnd.osgi.repository.XMLResourceParser;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.ContentCapability;
import aQute.bnd.service.RepositoryPlugin.DownloadListener;
import aQute.bnd.util.repository.DownloadListenerPromise;
import aQute.bnd.version.Version;
import aQute.p2.api.Artifact;
import aQute.p2.provider.P2Impl;
import aQute.service.reporter.Reporter;

class P2Indexer implements Closeable {
	private static final long	MAX_STALE	= TimeUnit.DAYS.toMillis(100);
	final Reporter				reporter;
	final File					location;
	final HttpClient			client;
	final URI					url;
	final String				name;
	final File					indexFile;
	volatile BridgeRepository	bridge;

	P2Indexer(Reporter reporter, File location, HttpClient client, URI url, String name) throws Exception {
		this.reporter = reporter;
		this.location = location;
		this.indexFile = new File(location, "index.xml.gz");
		this.client = client;
		this.url = url;
		this.name = name;
		this.location.mkdirs();

		validate();

		Repository r;
		if (this.indexFile.isFile())
			r = readFile();
		else {
			r = readRepository();
			save(r);
		}

		bridge = new BridgeRepository(r);
	}

	void validate() {
		if (!this.location.isDirectory())
			throw new IllegalArgumentException("%s cannot be made a directory" + this.location);
	}

	File get(String bsn, Version version, Map<String,String> properties, DownloadListener... listeners)
			throws Exception {
		Resource resource = bridge.get(bsn, version);
		if (resource == null)
			return null;

		ContentCapability contentCapability = ResourceUtils.getContentCapability(resource);

		if (contentCapability == null)
			return null;

		URI url = contentCapability.url();

		final File source = client.getCacheFileFor(url);
		final File link = new File(location, bsn + "-" + version + ".jar");
		if (link.isFile())
			Files.delete(link.toPath());
		Files.createLink(link.toPath(), source.toPath());

		Promise<File> go = client.build().useCache(MAX_STALE).async(url.toURL()).map(new Function<File,File>() {

			@Override
			public File apply(File t) {
				return link;
			}
		});
		if (listeners.length == 0)
			return go.getValue();

		new DownloadListenerPromise(reporter, name + ": get " + bsn + ";" + version + " " + url, go, listeners);
		return link;
	}

	List<String> list(String pattern) throws Exception {
		return bridge.list(pattern);
	}

	SortedSet<Version> versions(String bsn) throws Exception {
		return bridge.versions(bsn);
	}

	Repository readFile() throws Exception {
		try (XMLResourceParser xp = new XMLResourceParser(this.indexFile.toURI())) {
			List<Resource> resources = xp.parse();
			return new ResourcesRepository(resources);
		}
	}

	Repository readRepository() throws Exception {
		P2Impl p2 = new P2Impl(client, this.url, Processor.getExecutor());
		Map<Artifact,Promise<Resource>> fetched = new HashMap<>();
		List<URI> uris = new ArrayList<>();

		for (final Artifact a : p2.getArtifacts()) {

			Promise<Resource> promise = client.build()
					.useCache(MAX_STALE)
					.async(a.uri.toURL())
					.map(new Function<File,Resource>() {

						@Override
						public Resource apply(File file) {
							try {
								ResourceBuilder rb = new ResourceBuilder();
								rb.addFile(file, a.uri);
								return rb.build();
							} catch (Exception e) {
								throw new RuntimeException(e);
							}
						}
					});

			fetched.put(a, promise);
		}

		ResourcesRepository repository = new ResourcesRepository();

		for (Entry<Artifact,Promise<Resource>> e : fetched.entrySet()) {
			Promise<Resource> resource = e.getValue();
			Artifact a = e.getKey();

			if (resource.getFailure() == null) {
				repository.add(resource.getValue());
			} else {
				System.out.println(a + " : " + resource.getFailure());
			}
		}

		return repository;
	}

	void save(Repository repository) throws IOException, Exception {
		XMLResourceGenerator xrg = new XMLResourceGenerator();
		xrg.repository(repository).name(name).save(indexFile);
	}

	Map<Requirement,Collection<Capability>> findProviders(Collection< ? extends Requirement> requirements) {
		return bridge.getRepository().findProviders(requirements);
	}

	public void refresh() throws Exception {
		Repository repository = readRepository();
		this.bridge = new BridgeRepository(repository);
	}

	@Override
	public void close() throws IOException {}
}
