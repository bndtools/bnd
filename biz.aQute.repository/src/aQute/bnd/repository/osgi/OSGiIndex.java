package aQute.bnd.repository.osgi;

import static aQute.lib.promise.PromiseCollectors.toPromise;
import static java.util.stream.Collectors.toList;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.http.HttpClient;
import aQute.bnd.http.HttpRequest;
import aQute.bnd.osgi.repository.BridgeRepository;
import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.repository.XMLResourceParser;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.ContentCapability;
import aQute.bnd.service.url.State;
import aQute.bnd.service.url.TaggedData;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;
import aQute.libg.cryptography.SHA256;

class OSGiIndex {
	private final static Logger				logger	= LoggerFactory.getLogger(OSGiIndex.class);
	private final Promise<BridgeRepository>	repository;
	private final HttpClient				client;
	private final PromiseFactory			promiseFactory;
	private final long						staleTime;
	private final File						cache;
	private final String					name;
	private final List<URI>					uris;

	OSGiIndex(String name, HttpClient client, File cache, List<URI> uris, int staleTime, boolean refresh)
		throws Exception {
		this.name = name;
		this.uris = uris;
		this.client = client;
		this.promiseFactory = client.promiseFactory();
		this.cache = checkCache(cache);
		this.staleTime = staleTime * 1000L;
		this.repository = readIndexes(refresh);
	}

	private Promise<BridgeRepository> readIndexes(boolean refresh) throws Exception {
		Promise<List<Resource>> resources = getURIs().stream()
			.map(uri -> download(uri, refresh))
			.collect(toPromise(promiseFactory))
			.map(ll -> ll.stream()
				.flatMap(List::stream)
				.collect(toList()));
		Promise<BridgeRepository> bridge = resources.map(ResourcesRepository::new)
			.map(BridgeRepository::new);
		return bridge;
	}

	/**
	 * Return the bridge repository in {@link Promise} form.
	 *
	 * @return promise representing the repository
	 */
	Promise<BridgeRepository> getBridgeRepository() {
		return this.repository;
	}

	private static File checkCache(File cache) throws Exception {
		IO.mkdirs(cache);
		if (!cache.isDirectory())
			throw new IllegalArgumentException("Cannot create directory for " + cache);
		return cache;
	}

	private Promise<List<Resource>> download(URI uri, boolean refresh) {
		HttpRequest<File> req = client.build()
			.useCache(refresh ? -1 : staleTime);

		return req.async(uri)
			.map(file -> {
				if (file == null) {
					logger.debug("{}: No file downloaded for {}", name, uri);
					return Collections.emptyList();
				}
				// file could be xml, gzipped xml, OR zip with index.xml or
				// index.xml.gz entry
				try (InputStream in = new BufferedInputStream(IO.stream(file))) {
					in.mark(2);
					int magic = readUnsignedShort(in);
					in.reset();
					if (magic == 0x504b) { // "PK" means a zip file
						try (ZipInputStream zin = new ZipInputStream(in)) {
							for (ZipEntry entry; (entry = zin.getNextEntry()) != null;) {
								switch (entry.getName()) {
									case "index.xml" :
									case "index.xml.gz" :
										try (XMLResourceParser xrp = new XMLResourceParser(zin, name, uri)) {
											return xrp.parse();
										}
									default :
										break;
								}
							}
							logger.debug("{}: No index.xml or index.xml.gz entry found in zip file {}", name, uri);
							return Collections.emptyList();
						}
					}
					try (XMLResourceParser xrp = new XMLResourceParser(in, name, uri)) {
						return xrp.parse();
					}
				}
			});
	}

	private static final int readUnsignedShort(InputStream in) throws IOException {
		int b1 = in.read();
		int b2 = in.read();
		if ((b1 | b2) < 0)
			throw new EOFException();
		return (b1 << 8) + (b2 & 0xff);
	}

	Promise<File> get(String bsn, Version version, File file) throws Exception {
		Resource resource = getBridge().get(bsn, version);
		if (resource == null)
			return null;

		ContentCapability content = ResourceUtils.getContentCapability(resource);
		if (content == null) {
			logger.warn("{}: No content capability for {}", name, resource);
			return null;
		}

		URI url = content.url();
		if (url == null) {
			logger.warn("{}: No content URL for {}", name, resource);
			return null;
		}

		String remoteDigest = content.osgi_content();
		return get(url, file, remoteDigest, 2, 1000L).map(TaggedData::getFile);
	}

	private Promise<TaggedData> get(URI url, File file, String remoteDigest, int retries, long delay) {
		Promise<TaggedData> promise = client.build()
			.useCache(file, staleTime)
			.asTag()
			.async(url);
		if (remoteDigest == null) {
			return promise;
		}
		return promise.then(success -> success.thenAccept(tag -> {
			if (tag.getState() != State.UPDATED) {
				return;
			}
			String fileDigest = SHA256.digest(file)
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
					String.format("Invalid content checksum %s for %s; expected %s", fileDigest, url, remoteDigest));
			}
		})
			.recoverWith(failed -> {
				if (retries < 1) {
					return null; // no recovery
				}
				logger.info("Retrying invalid download: {}. delay={}, retries={}", failed.getFailure()
					.getMessage(), delay, retries);
				@SuppressWarnings("unchecked")
				Promise<TaggedData> delayed = (Promise<TaggedData>) failed.delay(delay);
				return delayed.recoverWith(f -> get(url, file, remoteDigest, retries - 1,
					Math.min(delay * 2L, TimeUnit.MINUTES.toMillis(10))));
			}));
	}

	BridgeRepository getBridge() throws Exception {
		return repository.getValue();
	}

	File getCache() {
		return cache;
	}

	/**
	 * Check any of the URL indexes are stale.
	 *
	 * @return
	 * @throws Exception
	 */
	boolean isStale() throws Exception {
		final Deferred<List<Void>> freshness = promiseFactory.deferred();
		List<Promise<Void>> promises = new ArrayList<>(getURIs().size());
		for (final URI uri : getURIs()) {
			if (freshness.getPromise()
				.isDone()) {
				break; // early exit if staleness already detected
			}
			try {
				Promise<TaggedData> async = client.build()
					.useCache()
					.asTag()
					.async(uri);
				promises.add(async.then(resolved -> {
					switch (resolved.getValue()
						.getState()) {
						case OTHER :
							// in the offline case
							// ignore might be best here
							logger.debug("Could not verify {}", uri);
							break;

						case UNMODIFIED :
							break;

						case NOT_FOUND :
						case UPDATED :
						default :
							logger.debug("Found {} to be stale", uri);
							freshness.fail(new Exception("stale"));
					}
					return null;
				}, resolved -> {
					logger.debug("Could not verify {}: {}", uri, resolved.getFailure());
					freshness.fail(resolved.getFailure());
				}));
			} catch (Exception e) {
				logger.debug("Checking stale status: {}: {}", uri, e);
			}
		}

		// Resolve when all uris checked
		Promise<List<Void>> all = promiseFactory.all(promises);
		freshness.resolveWith(all);

		// Block until freshness is resolved
		return freshness.getPromise()
			.getFailure() != null;
	}

	List<URI> getURIs() {
		return uris;
	}

	Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements)
		throws Exception {
		return getBridge().getRepository()
			.findProviders(requirements);
	}
}
