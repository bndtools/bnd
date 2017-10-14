package aQute.bnd.repository.p2.provider;

import static aQute.bnd.osgi.resource.ResourceUtils.toVersion;
import static java.util.Collections.singleton;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;
import org.osgi.util.function.Function;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.repository.BridgeRepository;
import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.repository.XMLResourceGenerator;
import aQute.bnd.osgi.repository.XMLResourceParser;
import aQute.bnd.osgi.resource.CapabilityBuilder;
import aQute.bnd.osgi.resource.FilterBuilder;
import aQute.bnd.osgi.resource.RequirementBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.ContentCapability;
import aQute.bnd.osgi.resource.ResourceUtils.IdentityCapability;
import aQute.bnd.service.RepositoryPlugin.DownloadListener;
import aQute.bnd.util.repository.DownloadListenerPromise;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;
import aQute.p2.api.Artifact;
import aQute.p2.provider.P2Impl;
import aQute.service.reporter.Reporter;

class P2Indexer implements Closeable {
	private final static Logger			logger		= LoggerFactory.getLogger(P2Indexer.class);
	private static final long	MAX_STALE	= TimeUnit.DAYS.toMillis(100);
	private final Reporter					reporter;
	final File								location;
	private final HttpClient				client;
	private final URI						url;
	private final String					name;
	private final String				urlHash;
	private final File						indexFile;
	private volatile BridgeRepository		bridge;
	private static final Resource		RECOVERY	= new ResourceBuilder().build();
	private static final String			P2_CAPABILITY_NAMESPACE	= "bnd.p2";
	private static final String			MD5_ATTRIBUTE			= "md5";
	private static final Requirement	MD5_REQUIREMENT			= new RequirementBuilder(P2_CAPABILITY_NAMESPACE)
			.addFilter(new FilterBuilder().isPresent(MD5_ATTRIBUTE))
			.buildSyntheticRequirement();

	P2Indexer(Reporter reporter, File location, HttpClient client, URI url, String name) throws Exception {
		this.reporter = reporter;
		this.location = location;
		this.indexFile = new File(location, "index.xml.gz");
		this.client = client;
		this.url = url;
		this.name = name;
		this.urlHash = client.toName(url);
		IO.mkdirs(this.location);

		validate();

		bridge = new BridgeRepository(readRepository(indexFile));
	}

	private void validate() {
		if (!this.location.isDirectory())
			throw new IllegalArgumentException("%s cannot be made a directory" + this.location);
	}

	File get(String bsn, Version version, Map<String,String> properties, DownloadListener... listeners)
			throws Exception {
		Resource resource = getBridge().get(bsn, version);
		if (resource == null)
			return null;

		ContentCapability contentCapability = ResourceUtils.getContentCapability(resource);

		if (contentCapability == null)
			return null;

		URI url = contentCapability.url();

		final File source = client.getCacheFileFor(url);
		final File link = new File(location, bsn + "-" + version + ".jar");

		IO.createSymbolicLinkOrCopy(link, source);

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
		return getBridge().list(pattern);
	}

	SortedSet<Version> versions(String bsn) throws Exception {
		return getBridge().versions(bsn);
	}

	private Repository readRepository(File index) throws Exception {
		if (index.isFile()) {
			try (XMLResourceParser xp = new XMLResourceParser(index.toURI())) {
				List<Resource> resources = xp.parse();
				if (urlHash.equals(xp.name())) {
					return new ResourcesRepository(resources);
				}
			}
		}
		return save(readRepository());
	}

	private Repository readRepository() throws Exception {
		P2Impl p2 = new P2Impl(client, this.url, client.executor());
		List<Artifact> artifacts = p2.getArtifacts();
		List<Promise<Resource>> fetched = new ArrayList<>(artifacts.size());
		Set<URI> visitedURIs = new HashSet<>(artifacts.size());
		Set<ArtifactID> visitedArtifacts = new HashSet<>(artifacts.size());
		Map<ArtifactID,Resource> knownResources = new HashMap<>();

		if (getBridge() != null) {
			for (Capability capability : getBridge().getRepository()
					.findProviders(singleton(MD5_REQUIREMENT))
					.get(MD5_REQUIREMENT)) {
				Resource resource = capability.getResource();
				IdentityCapability identity = ResourceUtils.getIdentityCapability(resource);
				ArtifactID artifact = new ArtifactID(identity.osgi_identity(), identity.version(),
						(String) capability.getAttributes().get(MD5_ATTRIBUTE));

				knownResources.put(artifact, resource);
			}
		}

		for (final Artifact a : artifacts) {
			if (!visitedURIs.add(a.uri))
				continue;
			if (a.md5 != null) {
				ArtifactID id = new ArtifactID(a.id, toVersion(a.version), a.md5);
				if (!visitedArtifacts.add(id))
					continue;
				if (knownResources.containsKey(id)) {
					fetched.add(Promises.resolved(knownResources.get(id)));
					continue;
				}
			}

			Promise<Resource> promise = client.build()
					.useCache(MAX_STALE)
					.async(a.uri.toURL())
					.map(new Function<File,Resource>() {
						@Override
						public Resource apply(File file) {
							try {
								ResourceBuilder rb = new ResourceBuilder();
								rb.addFile(file, a.uri);
								if (a.md5 != null)
									rb.addCapability(new CapabilityBuilder(P2_CAPABILITY_NAMESPACE)
											.addAttribute(MD5_ATTRIBUTE, a.md5));
								return rb.build();
							} catch (Exception e) {
								logger.debug("{}: Failed to create resource for %s from {}", name, a, file, e);
								return RECOVERY;
							}
						}
					})
					.recover(new Function<Promise< ? >,Resource>() {
						@Override
						public Resource apply(Promise< ? > failed) {
							try {
								logger.debug("{}: Failed to create resource for {}", name, a,
										failed.getFailure());
							} catch (InterruptedException e) {
								// impossible
							}
							return RECOVERY;
						}
					});
			fetched.add(promise);
		}

		Promise<List<Resource>> all = Promises.all(fetched);
		return all.map(new Function<List<Resource>,ResourcesRepository>() {
			@Override
			public ResourcesRepository apply(List<Resource> resources) {
				ResourcesRepository rr = new ResourcesRepository();
				for (Resource resource : resources) {
					if (resource != RECOVERY) {
						rr.add(resource);
					}
				}
				return rr;
			}
		}).getValue();
	}

	private Repository save(Repository repository) throws IOException, Exception {
		XMLResourceGenerator xrg = new XMLResourceGenerator();
		xrg.repository(repository).name(urlHash).save(indexFile);
		return repository;
	}

	Map<Requirement,Collection<Capability>> findProviders(Collection< ? extends Requirement> requirements) {
		return getBridge().getRepository().findProviders(requirements);
	}

	public void refresh() throws Exception {
		bridge = new BridgeRepository(save(readRepository()));
	}

	@Override
	public void close() throws IOException {}

	BridgeRepository getBridge() {
		return bridge;
	}
}
