package aQute.bnd.repository.osgi;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.resource.Resource;
import org.osgi.util.function.Function;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;

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
import aQute.libg.reporter.slf4j.Slf4jReporter;
import aQute.service.reporter.Reporter;

class OSGiIndex {
	private Reporter						log	= new Slf4jReporter(OSGiIndex.class);
	private final Promise<BridgeRepository>	repository;
	private final HttpClient				client;
	private final long						staleTime;
	private final File						cache;
	private final String					name;
	private final Collection<URI>			urls;

	OSGiIndex(String name, HttpClient client, File cache, Collection<URI> urls, int staleTime, boolean refresh)
			throws Exception {
		this.name = name;
		this.urls = urls;
		this.client = client;
		this.cache = cache;
		this.staleTime = staleTime * 1000L;
		checkCache(cache);

		repository = readIndexes(urls, refresh);
	}

	private Promise<BridgeRepository> readIndexes(Collection<URI> urls, boolean refresh) throws Exception {
		List<Promise<List<Resource>>> promises = new ArrayList<>();

		for (URI url : urls) {
			promises.add(download(url, refresh));
		}

		Promise<List<List<Resource>>> all = Promises.all(promises);
		return all.map(collect());
	}

	private void checkCache(File cache) {
		cache.mkdirs();
		if (!cache.isDirectory())
			throw new IllegalArgumentException("Cannot create directory for " + cache);
	}

	private Function<List<List<Resource>>,BridgeRepository> collect() {
		return new Function<List<List<Resource>>,BridgeRepository>() {

			@Override
			public BridgeRepository apply(List<List<Resource>> t) {
				try {
					return collect(t);
				} catch (Exception e) {
					throw Exceptions.duck(e);
				}
			}

		};
	}

	private BridgeRepository collect(List<List<Resource>> resources) throws Exception {
		ResourcesRepository rr = new ResourcesRepository();
		for (List<Resource> p : resources) {
			rr.addAll(p);
		}
		return new BridgeRepository(rr);
	}

	private Promise<List<Resource>> download(URI url, boolean refresh) throws Exception {
		HttpRequest<File> req = refresh ? client.build().useCache(-1) : client.build().useCache(staleTime);

		return req.async(url).map(toResources(url));
	}

	private Function<File,List<Resource>> toResources(final URI url) {
		return new Function<File,List<Resource>>() {

			@Override
			public List<Resource> apply(File file) {
				try {
					try (InputStream in = new FileInputStream(file)) {
						try (XMLResourceParser xmlp = new XMLResourceParser(in, name, url);) {
							return xmlp.parse();
						}
					}
				} catch (Exception e) {
					throw Exceptions.duck(e);
				}
			}
		};
	}

	Promise<File> get(String bsn, Version version, File file) throws Exception {
		Resource resource = repository.getValue().get(bsn, version);
		if (resource == null)
			return null;

		ContentCapability content = ResourceUtils.getContentCapability(resource);
		if (content == null) {
			log.warning("%s: No content capability for %s", name, resource);
			return null;
		}

		URI url = content.url();
		if (url == null) {
			log.warning("%s: No content capability for %s", name, resource);
			return null;
		}

		return client.build().useCache(file, staleTime).async(url);
	}

	BridgeRepository getBridge() throws Exception {
		return repository.getValue();
	}

	public File getCache() {
		return cache;
	}

	/**
	 * Check any of the URL indexes are stale.
	 * 
	 * @return
	 * @throws Exception
	 */
	boolean isStale() throws Exception {

		Map<URI,Promise<TaggedData>> promises = new HashMap<>();

		for (URI uri : urls)
			try {
				Promise<TaggedData> async = client.build().useCache().asTag().async(uri);
				promises.put(uri, async);
			} catch (Exception e) {
				log.trace("Checking stale status: %s: %s", e.getMessage(), uri);
			}

		for (Entry<URI,Promise<TaggedData>> entry : promises.entrySet()) {
			URI uri = entry.getKey();
			Promise<TaggedData> p = entry.getValue();
			if (p.getFailure() != null) {
				log.trace("Could not verify %s: %s", uri, p.getFailure().getMessage());
				return true;
			} else {
				TaggedData tag = p.getValue();
				switch (tag.getState()) {
					case OTHER :
						// in the offline case
						// ignore might be best here
						log.trace("Could not verify %s", uri);
						break;

					case UNMODIFIED :
						break;

					case NOT_FOUND :
					case UPDATED :
					default :
						log.trace("Found %s to be stale", uri);
						return true;
				}
			}
		}

		return false;
	}

	public Collection<URI> getUrls() {
		return urls;
	}
}
