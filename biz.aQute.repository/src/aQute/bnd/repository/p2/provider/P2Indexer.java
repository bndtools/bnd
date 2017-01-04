package aQute.bnd.repository.p2.provider;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
	private final static Logger			logger		= LoggerFactory.getLogger(P2Indexer.class);
	private static final long	MAX_STALE	= TimeUnit.DAYS.toMillis(100);
	private final Reporter					reporter;
	final File								location;
	private final HttpClient				client;
	private final URI						url;
	private final String					name;
	private final File						indexFile;
	private volatile BridgeRepository		bridge;
	private static final Resource		RECOVERY	= new ResourceBuilder().build();

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
		final Path sourcePath = source.toPath();
		final File link = new File(location, bsn + "-" + version + ".jar");
		final Path linkPath = link.toPath();

		// links can fail if they span disparate Windows drives
		// so we need to handle them differently
		final AtomicBoolean createdSymlink = new AtomicBoolean();

		if (link.isFile() && Files.isSymbolicLink(linkPath))
			Files.delete(linkPath);

		try {
			Files.createLink(linkPath, sourcePath);
			createdSymlink.set(true);
		} catch (FileSystemException e) {
			createdSymlink.set(false);
		}

		Promise<File> go = client.build().useCache(MAX_STALE).async(url.toURL()).map(new Function<File,File>() {
			@Override
			public File apply(File t) {
				// if we can't create symlinks we need to copy this into link
				// path as a fallback
				if (!createdSymlink.get() && needsCopy(sourcePath, linkPath)) {
					try {
						Files.copy(sourcePath, linkPath, StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException e) {
						return null;
					}
				}

				return link;
			}
		});

		if (listeners.length == 0)
			return go.getValue();

		new DownloadListenerPromise(reporter, name + ": get " + bsn + ";" + version + " " + url, go, listeners);
		return link;
	}

	protected boolean needsCopy(Path sourcePath, Path destPath) {
		final File dest = destPath.toFile();

		return !dest.exists() || dest.length() != sourcePath.toFile().length();
	}

	List<String> list(String pattern) throws Exception {
		return getBridge().list(pattern);
	}

	SortedSet<Version> versions(String bsn) throws Exception {
		return getBridge().versions(bsn);
	}

	private Repository readFile() throws Exception {
		try (XMLResourceParser xp = new XMLResourceParser(this.indexFile.toURI())) {
			List<Resource> resources = xp.parse();
			return new ResourcesRepository(resources);
		}
	}

	private Repository readRepository() throws Exception {
		P2Impl p2 = new P2Impl(client, this.url, Processor.getExecutor());
		List<Artifact> artifacts = p2.getArtifacts();
		List<Promise<Resource>> fetched = new ArrayList<>(artifacts.size());
		Set<URI> visited = new HashSet<>(artifacts.size());

		for (final Artifact a : artifacts) {
			if (!visited.add(a.uri))
				continue;

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

	private void save(Repository repository) throws IOException, Exception {
		XMLResourceGenerator xrg = new XMLResourceGenerator();
		xrg.repository(repository).name(name).save(indexFile);
	}

	Map<Requirement,Collection<Capability>> findProviders(Collection< ? extends Requirement> requirements) {
		return getBridge().getRepository().findProviders(requirements);
	}

	public void refresh() throws Exception {
		Repository repository = readRepository();
		save(repository);
		this.bridge = new BridgeRepository(repository);
	}

	@Override
	public void close() throws IOException {}

	BridgeRepository getBridge() {
		return bridge;
	}
}
