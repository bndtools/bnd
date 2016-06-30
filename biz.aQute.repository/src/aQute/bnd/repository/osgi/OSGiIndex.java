package aQute.bnd.repository.osgi;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.osgi.resource.Resource;
import org.osgi.util.function.Function;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.http.HttpClient;
import aQute.bnd.http.HttpRequest;
import aQute.bnd.osgi.repository.BridgeRepository;
import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.repository.XMLResourceParser;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.ContentCapability;
import aQute.bnd.version.Version;
import aQute.lib.exceptions.Exceptions;

class OSGiIndex {
	private Logger							log	= LoggerFactory.getLogger(OSGiIndex.class);
	private final Promise<BridgeRepository>	repository;
	private final HttpClient				client;
	private final long						staleTime;
	private final File						cache;
	private final String					name;

	OSGiIndex(String name, HttpClient client, File cache, Collection<URI> urls, int staleTime) throws Exception {
		this.name = name;
		this.client = client;
		this.cache = cache;
		this.staleTime = staleTime * 1000;

		checkCache(cache);

		repository = readIndexes(urls);
	}

	private Promise<BridgeRepository> readIndexes(Collection<URI> urls) throws Exception {
		List<Promise<List<Resource>>> promises = new ArrayList<>();

		for (URI url : urls) {
			promises.add(download(url));
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

	private Promise<List<Resource>> download(URI url) throws Exception {
		HttpRequest<File> req;
		if (staleTime > 0) {
			req = client.build().useCache(staleTime);
		} else {
			req = client.build().useCache(TimeUnit.DAYS.toMillis(365));
		}
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
			log.warn(name + ": No content capability for " + resource);
			return null;
		}

		URI url = content.url();
		if (url == null) {
			log.warn(name + ": No URL in content capability for " + resource);
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
}
