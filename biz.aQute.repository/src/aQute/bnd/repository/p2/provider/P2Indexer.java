package aQute.bnd.repository.p2.provider;

import static aQute.bnd.osgi.repository.ResourcesRepository.toResourcesRepository;
import static aQute.bnd.osgi.resource.ResourceUtils.toVersion;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
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
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.repository.BridgeRepository;
import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.repository.XMLResourceGenerator;
import aQute.bnd.osgi.repository.XMLResourceParser;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.ContentCapability;
import aQute.bnd.service.RepositoryPlugin.DownloadListener;
import aQute.bnd.service.resource.SupportingResource;
import aQute.bnd.service.url.State;
import aQute.bnd.service.url.TaggedData;
import aQute.bnd.util.repository.DownloadListenerPromise;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;
import aQute.libg.cryptography.MD5;
import aQute.p2.api.Artifact;
import aQute.p2.api.ArtifactProvider;
import aQute.p2.packed.Unpack200;
import aQute.p2.provider.P2Impl;
import aQute.p2.provider.TargetImpl;
import aQute.service.reporter.Reporter;

/**
 * This class maintains an OBR index but gets its sources from a P2 or
 * TargetPlatform.
 */
class P2Indexer implements Closeable {
	private final static Logger				logger		= LoggerFactory.getLogger(P2Indexer.class);
	private static final long				MAX_STALE	= TimeUnit.DAYS.toMillis(100);
	private final Reporter					reporter;
	private final Unpack200					processor;
	final File								location;
	final URI								url;
	final String							name;
	final String							urlHash;
	final File								indexFile;
	private final HttpClient				client;
	private final PromiseFactory			promiseFactory;
	private volatile BridgeRepository		bridge;
	private static final SupportingResource	RECOVERY	= new ResourceBuilder().build();

	P2Indexer(Unpack200 processor, Reporter reporter, File location, HttpClient client, URI url, String name)
		throws Exception {
		this.processor = processor;
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
		init();
	}

	private void init() throws Exception {
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
			.asTag()
			.async(url.toURL())
			.map(tag -> processor.unpackAndLinkIfNeeded(tag, link));

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

	private ResourcesRepository readRepository(File index) throws Exception {
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

	private ResourcesRepository readRepository() throws Exception {
		ArtifactProvider p2;
		if (this.url.getPath()
			.endsWith(".target"))
			p2 = new TargetImpl(processor, client, this.url, promiseFactory);
		else
			p2 = new P2Impl(processor, client, this.url, promiseFactory);

		List<Artifact> artifacts = p2.getAllArtifacts();
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
				}
				Promise<SupportingResource> fetched = fetch(a, 2, 1000L)
					.map(tag -> processor.unpackAndLinkIfNeeded(tag, null))
					.map(file -> processArtifact(a, file))
					.recover(failed -> {
						logger.info("{}: Failed to create resource for {}", name, a.uri, failed.getFailure());
						return RECOVERY;
					});
				return fetched;
			})
			.map(a -> a)
			.filter(Objects::nonNull)
			.collect(promiseFactory.toPromise());

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

	/**
	 * Process an artifact (bundle or feature) and convert it to an OSGi
	 * Resource
	 */
	private SupportingResource processArtifact(Artifact artifact, File file) throws Exception {
		ResourceBuilder rb = new ResourceBuilder();

		if (artifact.classifier == aQute.p2.api.Classifier.FEATURE) {
			// Process feature: parse it and add capabilities/requirements
			try (java.io.InputStream in = IO.stream(file)) {
				aQute.p2.provider.Feature feature = new aQute.p2.provider.Feature(in);
				feature.parse();
				Resource featureResource = feature.toResource();

				// Copy all capabilities and requirements from the feature resource
				for (Capability cap : featureResource.getCapabilities(null)) {
					aQute.bnd.osgi.resource.CapReqBuilder cb = new aQute.bnd.osgi.resource.CapReqBuilder(
						cap.getNamespace());
					cap.getAttributes()
						.forEach(cb::addAttribute);
					cap.getDirectives()
						.forEach(cb::addDirective);
					rb.addCapability(cb);
				}

				for (Requirement req : featureResource.getRequirements(null)) {
					aQute.bnd.osgi.resource.CapReqBuilder crb = new aQute.bnd.osgi.resource.CapReqBuilder(
						req.getNamespace());
					req.getAttributes()
						.forEach(crb::addAttribute);
					req.getDirectives()
						.forEach(crb::addDirective);
					rb.addRequirement(crb);
				}
			}
		}

		// Add content capability for the artifact (bundle or feature JAR)
		rb.addFile(file, artifact.uri);

		return rb.build();
	}

	private ResourcesRepository save(ResourcesRepository repository) throws IOException, Exception {
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
		init();
	}

	@Override
	public void close() throws IOException {}

	BridgeRepository getBridge() {
		return bridge;
	}

	void reread() throws Exception {
		indexFile.delete();
		init();
	}
}
