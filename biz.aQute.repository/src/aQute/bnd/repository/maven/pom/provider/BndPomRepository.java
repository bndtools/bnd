package aQute.bnd.repository.maven.pom.provider;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Failure;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Success;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.build.Workspace;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.repository.BaseRepository;
import aQute.bnd.osgi.repository.BridgeRepository;
import aQute.bnd.osgi.repository.BridgeRepository.ResourceInfo;
import aQute.bnd.service.Actionable;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.Refreshable;
import aQute.bnd.service.Registry;
import aQute.bnd.service.RegistryPlugin;
import aQute.bnd.service.RepositoryListenerPlugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.url.TaggedData;
import aQute.bnd.util.repository.DownloadListenerPromise;
import aQute.bnd.version.Version;
import aQute.lib.converter.Converter;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.libg.reporter.slf4j.Slf4jReporter;
import aQute.maven.api.Archive;
import aQute.maven.api.Revision;
import aQute.maven.provider.MavenBackingRepository;
import aQute.maven.provider.MavenRepository;
import aQute.service.reporter.Reporter;

/**
 * This is the Bnd repository for Maven.
 */
@BndPlugin(name = "PomRepository")
public class BndPomRepository extends BaseRepository
		implements Plugin, RegistryPlugin, RepositoryPlugin, Refreshable, Actionable, Closeable {
	private final static Logger		logger				= LoggerFactory.getLogger(BndPomRepository.class);
	static final String				MAVEN_REPO_LOCAL	= System.getProperty("maven.repo.local", "~/.m2/repository");
	static int						DEFAULT_POLL_TIME	= (int) TimeUnit.MINUTES.toSeconds(5);

	private HttpClient				client;
	private boolean					inited;
	private PomConfiguration		configuration;
	private Registry				registry;
	private String					name;
	private Reporter				reporter			= new Slf4jReporter(BndPomRepository.class);
	private InnerRepository			repoImpl;
	private List<Revision>			revisions;
	private BridgeRepository		bridge;
	private Map<URI,Long>			pomFiles;
	private String					query;
	private String					queryUrl;
	private ScheduledFuture< ? >	pomPoller;


	public synchronized void init() {
		try {
			if (inited)
				return;
			Workspace workspace = registry.getPlugin(Workspace.class);
			this.client = registry.getPlugin(HttpClient.class);
			File localRepo = IO.getFile(configuration.local(MAVEN_REPO_LOCAL));
			File location = workspace.getFile(getLocation());

			List<MavenBackingRepository> release = MavenBackingRepository.create(configuration.releaseUrls(), reporter,
					localRepo, client);
			List<MavenBackingRepository> snapshot = MavenBackingRepository.create(configuration.snapshotUrls(),
					reporter, localRepo, client);

			MavenRepository repository = new MavenRepository(localRepo, name, release, snapshot,
					Processor.getExecutor(), reporter, null);

			boolean transitive = configuration.transitive(true);

			if (pomFiles != null) {
				repoImpl = new PomRepository(repository, client, location, transitive).uris(pomFiles.keySet());
				startPoll();
			} else if (revisions != null) {
				repoImpl = new PomRepository(repository, client, location, transitive).revisions(revisions);
			} else if (query != null) {
				repoImpl = new SearchRepository(repository, location, query, queryUrl, workspace, client, transitive);
			} else {
				repository.close();
				throw new IllegalStateException("We have neither a pom, revision, or query set!");
			}

			bridge = new BridgeRepository(repoImpl);
			inited = true;
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	private void startPoll() {
		Workspace ws = registry.getPlugin(Workspace.class);
		if ((ws != null) && (ws.getGestalt().containsKey(Constants.GESTALT_BATCH)
				|| ws.getGestalt().containsKey(Constants.GESTALT_CI)
				|| ws.getGestalt().containsKey(Constants.GESTALT_OFFLINE))) {
			return;
		}
		final AtomicBoolean busy = new AtomicBoolean();
		int polltime = configuration.poll_time(DEFAULT_POLL_TIME);
		if (polltime > 0) {
			pomPoller = Processor.getScheduledExecutor().scheduleAtFixedRate(new Runnable() {

				@Override
				public void run() {
					if (busy.getAndSet(true))
						return;
					try {
						boolean needsUpdate = false;
						for (Entry<URI,Long> pomFilesEntry : pomFiles.entrySet()) {
							final URI pomURI = pomFilesEntry.getKey();
							if ("file".equalsIgnoreCase(pomURI.getScheme())) {
								if (pomFilesEntry.getValue() == null) {
									// not interested
									continue;
								}
								File f = new File(pomURI);
								if (f.isFile() && f.lastModified() > pomFilesEntry.getValue()) {
									needsUpdate = true;
									pomFilesEntry.setValue(f.lastModified());
								}
							} else {
								// check for staleness of non-file-URI
								if (isStale(pomURI))
									needsUpdate = true;
							}
						}
						if (needsUpdate) {
							refresh();
							for (RepositoryListenerPlugin listener : registry
									.getPlugins(RepositoryListenerPlugin.class))
								listener.repositoryRefreshed(BndPomRepository.this);
						}

					} catch (Exception e) {
						reporter.exception(e, "Error when polling for %s for change", this);
					} finally {
						busy.set(false);
					}
				}

			}, 1, polltime, TimeUnit.SECONDS);
		}
	}

	/**
	 * Check any of the URL POMs are stale.
	 * 
	 * @param uri remote-URI, must not be "file"
	 * @return true, when URI resolved to be stale
	 * @throws Exception InterruptedException if the tread was canceled during
	 *             waiting
	 */
	boolean isStale(final URI uri) throws Exception {
		final Deferred<Void> freshness = new Deferred<>();
		Promise<Void> promise;
		try {
			Promise<TaggedData> async = client.build().useCache().asTag().async(uri);
			promise = async.then(new Success<TaggedData,Void>() {
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
					freshness.resolve(null);
					return null;
				}
			}, new Failure() {
				@Override
				public void fail(Promise< ? > resolved) throws Exception {
					logger.debug("Could not verify {}: {}", uri, resolved.getFailure());
					freshness.fail(resolved.getFailure());
				}
			});
		} catch (Exception e) {
			logger.debug("Checking stale status: {}: {}", uri, e);
		}

		// Block until freshness is resolved
		return freshness.getPromise().getFailure() != null;
	}

	@Override
	public boolean refresh() throws Exception {
		init();
		repoImpl.refresh();
		bridge = new BridgeRepository(repoImpl);
		return true;
	}

	@Override
	public void setProperties(Map<String,String> map) throws Exception {

		configuration = Converter.cnv(PomConfiguration.class, map);

		if (configuration.name() == null)
			throw new IllegalArgumentException("Must get a name");

		this.name = configuration.name();

		if (configuration.pom() != null) {
			List<String> parts = Strings.split(configuration.pom());
			pomFiles = new LinkedHashMap<>();
			for (String part : parts) {
				File f = IO.getFile(part);
				if (f.isFile()) {
					pomFiles.put(f.toURI(), f.lastModified());
				} else {
					pomFiles.put(URI.create(part), null);
				}
			}
			if (pomFiles.isEmpty()) {
				throw new IllegalArgumentException("Pom is neither a file nor a revision " + configuration.pom());
			}
		} else if (configuration.revision() != null) {
			List<String> parts = Strings.split(configuration.revision());
			revisions = new ArrayList<>();
			for (String part : parts) {
				Revision revision = Revision.valueOf(part);
				if (revision != null) {
					revisions.add(revision);
				}
			}
			if (revisions.isEmpty()) {
				throw new IllegalArgumentException(
						"Revision is neither a file nor a revision " + configuration.revision());
			}
		} else if (configuration.query() != null) {
			this.query = configuration.query();
			this.queryUrl = configuration.queryUrl("http://search.maven.org/solrsearch/select");
		} else {
			throw new IllegalArgumentException("Neither pom, revision nor query property are set");
		}
		synchronized (this) {
			inited = false;
		}
	}

	@Override
	public Map<Requirement,Collection<Capability>> findProviders(Collection< ? extends Requirement> requirements) {
		init();
		return repoImpl.findProviders(requirements);
	}

	@Override
	public void setReporter(Reporter processor) {
		this.reporter = processor;
	}

	@Override
	public void setRegistry(Registry registry) {
		this.registry = registry;
	}

	@Override
	public PutResult put(InputStream stream, PutOptions options) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public File get(String bsn, Version version, Map<String,String> properties, DownloadListener... listeners)
			throws Exception {
		init();
		ResourceInfo resource = bridge.getInfo(bsn, version);
		if (resource == null)
			return null;

		String name = resource.getInfo().name();
		Archive archive = Archive.valueOf(name);

		Promise<File> p = repoImpl.getMavenRepository().get(archive);

		if (listeners.length == 0)
			return p.getValue();

		new DownloadListenerPromise(reporter, name + ": get " + bsn + ";" + version, p, listeners);
		return repoImpl.getMavenRepository().toLocalFile(archive);
	}

	@Override
	public boolean canWrite() {
		return false;
	}

	@Override
	public List<String> list(String pattern) throws Exception {
		init();
		return bridge.list(pattern);
	}

	@Override
	public SortedSet<Version> versions(String bsn) throws Exception {
		init();
		return bridge.versions(bsn);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getLocation() {
		return configuration.location("cnf/cache/pom-" + name + ".xml");
	}

	public String toString() {
		return name;
	}

	@Override
	public File getRoot() throws Exception {
		return repoImpl.getLocation();
	}

	@Override
	public Map<String,Runnable> actions(Object... target) throws Exception {
		return null;
	}

	@Override
	public String tooltip(Object... target) throws Exception {
		init();
		return bridge.tooltip(target);
	}

	@Override
	public String title(Object... target) throws Exception {
		init();
		return bridge.title(target);
	}

	@Override
	public void close() throws IOException {
		if (pomPoller != null)
			pomPoller.cancel(true);
	}
}
