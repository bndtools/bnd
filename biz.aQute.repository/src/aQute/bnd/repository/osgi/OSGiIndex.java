package aQute.bnd.repository.osgi;

import static aQute.lib.promise.PromiseCollectors.toPromise;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
import aQute.bnd.service.url.TaggedData;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;

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
				try (XMLResourceParser xmlp = new XMLResourceParser(IO.stream(file), name, uri)) {
					return xmlp.parse();
				}
			});
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
			logger.warn("{}: No content capability for {}", name, resource);
			return null;
		}

		return client.build()
			.useCache(file, staleTime)
			.async(url);
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
