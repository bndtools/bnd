package aQute.bnd.repository.osgi;

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
import org.osgi.util.function.Function;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Failure;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Success;
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
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
import aQute.lib.promise.PromiseExecutor;

class OSGiIndex {
	private final static Logger				logger	= LoggerFactory.getLogger(OSGiIndex.class);
	private final Promise<BridgeRepository>	repository;
	private final HttpClient				client;
	private final PromiseExecutor			executor;
	private final long						staleTime;
	private final File						cache;
	private final String					name;
	private final List<URI>					uris;

	OSGiIndex(String name, HttpClient client, File cache, List<URI> uris, int staleTime, boolean refresh)
			throws Exception {
		this.name = name;
		this.uris = uris;
		this.client = client;
		this.executor = new PromiseExecutor(client.executor());
		this.cache = checkCache(cache);
		this.staleTime = staleTime * 1000L;
		this.repository = readIndexes(refresh);
	}

	private Promise<BridgeRepository> readIndexes(boolean refresh) throws Exception {
		List<Promise<List<Resource>>> promises = new ArrayList<>(getURIs().size());

		for (URI uri : getURIs()) {
			promises.add(download(uri, refresh));
		}

		Promise<List<List<Resource>>> all = executor.all(promises);
		return all.map(new Function<List<List<Resource>>,BridgeRepository>() {
			@Override
			public BridgeRepository apply(List<List<Resource>> resources) {
				try {
					ResourcesRepository rr = new ResourcesRepository();
					for (List<Resource> p : resources) {
						rr.addAll(p);
					}
					return new BridgeRepository(rr);
				} catch (Exception e) {
					throw Exceptions.duck(e);
				}
			}
		});
	}

	private static File checkCache(File cache) throws Exception {
		IO.mkdirs(cache);
		if (!cache.isDirectory())
			throw new IllegalArgumentException("Cannot create directory for " + cache);
		return cache;
	}

	private Promise<List<Resource>> download(final URI uri, boolean refresh) throws Exception {
		HttpRequest<File> req = client.build().useCache(refresh ? -1 : staleTime);

		return req.async(uri).map(new Function<File,List<Resource>>() {
			@Override
			public List<Resource> apply(File file) {
				try {
					if (file == null) {
						logger.debug("{}: No file downloaded for {}", name, uri);
						return Collections.emptyList();
					}
					try (XMLResourceParser xmlp = new XMLResourceParser(IO.stream(file), name, uri)) {
						return xmlp.parse();
					}
				} catch (Exception e) {
					throw Exceptions.duck(e);
				}
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

		return client.build().useCache(file, staleTime).async(url);
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
		final Deferred<List<Void>> freshness = executor.deferred();
		List<Promise<Void>> promises = new ArrayList<>(getURIs().size());
		for (final URI uri : getURIs()) {
			if (freshness.getPromise().isDone()) {
				break; // early exit if staleness already detected
			}
			try {
				Promise<TaggedData> async = client.build().useCache().asTag().async(uri);
				promises.add(async.then(new Success<TaggedData,Void>() {
					@Override
					public Promise<Void> call(Promise<TaggedData> resolved) throws Exception {
						switch (resolved.getValue().getState()) {
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
					}
				}, new Failure() {
					@Override
					public void fail(Promise< ? > resolved) throws Exception {
						logger.debug("Could not verify {}: {}", uri, resolved.getFailure());
						freshness.fail(resolved.getFailure());
					}
				}));
			} catch (Exception e) {
				logger.debug("Checking stale status: {}: {}", uri, e);
			}
		}

		// Resolve when all uris checked
		Promise<List<Void>> all = executor.all(promises);
		freshness.resolveWith(all);

		// Block until freshness is resolved
		return freshness.getPromise().getFailure() != null;
	}

	Collection<URI> getURIs() {
		return uris;
	}

	Map<Requirement,Collection<Capability>> findProviders(Collection< ? extends Requirement> requirements)
			throws Exception {
		return getBridge().getRepository().findProviders(requirements);
	}
}
