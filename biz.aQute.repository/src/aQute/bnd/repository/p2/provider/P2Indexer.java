package aQute.bnd.repository.p2.provider;

import static aQute.bnd.osgi.repository.ResourcesRepository.toResourcesRepository;
import static aQute.bnd.osgi.resource.ResourceUtils.toVersion;
import static aQute.lib.promise.PromiseCollectors.toPromise;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toMap;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;
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
import aQute.bnd.service.url.State;
import aQute.bnd.service.url.TaggedData;
import aQute.bnd.util.repository.DownloadListenerPromise;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;
import aQute.libg.cryptography.MD5;
import aQute.p2.api.Artifact;
import aQute.p2.api.ArtifactProvider;
import aQute.p2.provider.P2Impl;
import aQute.p2.provider.TargetImpl;
import aQute.service.reporter.Reporter;

/**
 * This class maintains an OBR index but gets its sources from a P2 or
 * TargetPlatform.
 */
class P2Indexer implements Closeable {
	private final static Logger			logger					= LoggerFactory.getLogger(P2Indexer.class);
	private static final long			MAX_STALE				= TimeUnit.DAYS.toMillis(100);
	private final Reporter				reporter;
	final File							location;
	private final HttpClient			client;
	private final PromiseFactory		promiseFactory;
	private final URI					url;
	private final String				name;
	private final String				urlHash;
	private final File					indexFile;
	private volatile BridgeRepository	bridge;
	private static final Resource		RECOVERY				= new ResourceBuilder().build();
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
		this.promiseFactory = client.promiseFactory();
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

	File get(String bsn, Version version, Map<String, String> properties, DownloadListener... listeners)
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

		Promise<File> go = client.build()
			.useCache(MAX_STALE)
			.async(url.toURL())
			.map(file -> link);

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
		Map<ArtifactID, Resource> knownResources = (getBridge() != null) ? getBridge().getRepository()
			.findProviders(singleton(MD5_REQUIREMENT))
			.get(MD5_REQUIREMENT)
			.stream()
			.collect(toMap(capability -> {
				IdentityCapability identity = ResourceUtils.getIdentityCapability(capability.getResource());
				return new ArtifactID(identity.osgi_identity(), identity.version(), (String) capability.getAttributes()
					.get(MD5_ATTRIBUTE));
			}, Capability::getResource, (u, v) -> v)) : new HashMap<>();

		ArtifactProvider p2;
		if (this.url.getPath()
			.endsWith(".target"))
			p2 = new TargetImpl(client, this.url, promiseFactory);
		else
			p2 = new P2Impl(client, this.url, promiseFactory);

		List<Artifact> artifacts = p2.getBundles();
		Set<ArtifactID> visitedArtifacts = new HashSet<>(artifacts.size());
		Set<URI> visitedURIs = new HashSet<>(artifacts.size());

		Promise<List<Resource>> all = artifacts.stream()
			.map(a -> {
				if (!visitedURIs.add(a.uri))
					return null;
				if (a.md5 != null) {
					ArtifactID id = new ArtifactID(a.id, toVersion(a.version), a.md5);
					if (!visitedArtifacts.add(id))
						return null;
					if (knownResources.containsKey(id)) {
						return promiseFactory.resolved(knownResources.get(id));
					}
				}
				return fetch(a, 2, 1000L).map(tag -> {
					ResourceBuilder rb = new ResourceBuilder();
					rb.addFile(tag.getFile(), a.uri);
					if (a.md5 != null) {
						rb.addCapability(
							new CapabilityBuilder(P2_CAPABILITY_NAMESPACE).addAttribute(MD5_ATTRIBUTE, a.md5));
					}
					return rb.build();
				})
					.recover(failed -> {
						logger.info("{}: Failed to create resource for {}", name, a.uri, failed.getFailure());
						return RECOVERY;
					});
			})
			.filter(Objects::nonNull)
			.collect(toPromise(promiseFactory));

		return all.map(resources -> resources.stream()
			.filter(resource -> resource != RECOVERY)
			.collect(toResourcesRepository()))
			.getValue();
	}

	private Promise<TaggedData> fetch(Artifact a, int retries, long delay) {
		return client.build()
			.useCache(MAX_STALE)
			.asTag()
			.async(a.uri)
			.then(success -> success.thenAccept(tag -> checkDownload(a, tag))
				.recoverWith(failed -> {
					if (retries < 1) {
						return null; // no recovery
					}
					logger.info("Retrying invalid download: {}. delay={}, retries={}", failed.getFailure()
						.getMessage(), delay, retries);
					@SuppressWarnings("unchecked")
					Promise<TaggedData> delayed = (Promise<TaggedData>) failed.delay(delay);
					return delayed
						.recoverWith(f -> fetch(a, retries - 1, Math.min(delay * 2L, TimeUnit.MINUTES.toMillis(10))));
				}));
	}

	private void checkDownload(Artifact a, TaggedData tag) throws Exception {
		if (tag.getState() != State.UPDATED) {
			return;
		}
		File file = tag.getFile();
		String remoteDigest = a.md5;
		if (remoteDigest != null) {
			String fileDigest = MD5.digest(file)
				.asHex();
			int start = 0;
			while (start < remoteDigest.length() && Character.isWhitespace(remoteDigest.charAt(start))) {
				start++;
			}
			for (int i = 0; i < fileDigest.length(); i++) {
				if (start + i < remoteDigest.length()) {
					char us = fileDigest.charAt(i);
					char them = remoteDigest.charAt(start + i);
					if (us == them || Character.toLowerCase(us) == Character.toLowerCase(them)) {
						continue;
					}
				}
				IO.delete(file);
				throw new IOException(
					String.format("Invalid content checksum %s for %s; expected %s", fileDigest, a.uri, remoteDigest));
			}
		} else if (a.download_size != -1L) {
			long download_size = file.length();
			if (download_size != a.download_size) {
				IO.delete(file);
				throw new IOException(String.format("Invalid content size %s for %s; expected %s", download_size, a.uri,
					a.download_size));
			}
		}
	}

	private Repository save(Repository repository) throws IOException, Exception {
		XMLResourceGenerator xrg = new XMLResourceGenerator();
		xrg.repository(repository)
			.name(urlHash)
			.save(indexFile);
		return repository;
	}

	Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
		return getBridge().getRepository()
			.findProviders(requirements);
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
