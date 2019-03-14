package aQute.bnd.repository.maven.pom.provider;

import static aQute.bnd.osgi.Constants.BSN_SOURCE_SUFFIX;
import static java.util.stream.Collectors.toList;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.util.promise.Promise;

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
@BndPlugin(name = "BndPomRepository")
public class BndPomRepository extends BaseRepository
	implements Plugin, RegistryPlugin, RepositoryPlugin, Refreshable, Actionable, Closeable {
	private static final String	MAVEN_REPO_LOCAL	= System.getProperty("maven.repo.local", "~/.m2/repository");
	private static final int	DEFAULT_POLL_TIME	= 300;

	private boolean				inited;
	private PomConfiguration	configuration;
	private Registry			registry;
	private String				name;
	private Reporter			reporter			= new Slf4jReporter(BndPomRepository.class);
	private File				localRepo;
	private InnerRepository		repoImpl;
	private List<Revision>		revisions;
	private BridgeRepository	bridge;
	private List<URI>			pomFiles;
	private String				query;
	private String				queryUrl;
	private ScheduledFuture<?>	pomPoller;

	public synchronized void init() {
		try {
			if (inited)
				return;
			inited = true;

			Workspace workspace = registry.getPlugin(Workspace.class);
			HttpClient client = registry.getPlugin(HttpClient.class);
			localRepo = IO.getFile(configuration.local(MAVEN_REPO_LOCAL));
			File location = workspace.getFile(getLocation());

			List<MavenBackingRepository> release = MavenBackingRepository.create(configuration.releaseUrls(), reporter,
				localRepo, client);
			List<MavenBackingRepository> snapshot = MavenBackingRepository.create(configuration.snapshotUrls(),
				reporter, localRepo, client);

			MavenRepository repository = new MavenRepository(localRepo, name, release, snapshot, client.promiseFactory()
				.executor(), reporter);

			boolean transitive = configuration.transitive(true);

			if (pomFiles != null) {
				repoImpl = new PomRepository(repository, client, location, transitive).uris(pomFiles);
			} else if (revisions != null) {
				repoImpl = new PomRepository(repository, client, location, transitive).revisions(revisions);
			} else if (query != null) {
				repoImpl = new SearchRepository(repository, location, query, queryUrl, workspace, client, transitive);
			} else {
				repository.close();
				throw new IllegalStateException("We have neither a pom, revision, or query set!");
			}
			bridge = new BridgeRepository(repoImpl);

			startPoll();
		} catch (Exception e) {
			reporter.exception(e, "Init for BndPomRepository failed %s", configuration);
			throw Exceptions.duck(e);
		}
	}

	private void startPoll() {
		Workspace ws = registry.getPlugin(Workspace.class);
		if ((ws != null) && (ws.getGestalt()
			.containsKey(Constants.GESTALT_BATCH)
			|| ws.getGestalt()
				.containsKey(Constants.GESTALT_CI)
			|| ws.getGestalt()
				.containsKey(Constants.GESTALT_OFFLINE))) {
			return;
		}
		int polltime = configuration.poll_time(DEFAULT_POLL_TIME);
		if (polltime > 0) {
			AtomicBoolean inPoll = new AtomicBoolean();
			pomPoller = Processor.getScheduledExecutor()
				.scheduleAtFixedRate(() -> {
					if (inPoll.getAndSet(true))
						return;
					try {
						poll();
					} catch (Exception e) {
						reporter.exception(e, "Error when polling for %s for change", this);
					} finally {
						inPoll.set(false);
					}
				}, polltime, polltime, TimeUnit.SECONDS);
		}
	}

	private void poll() throws Exception {
		if (repoImpl.isStale()) {
			refresh();
		}
	}

	@Override
	public boolean refresh() throws Exception {
		init();
		repoImpl.refresh();
		bridge = new BridgeRepository(repoImpl);
		for (RepositoryListenerPlugin listener : registry.getPlugins(RepositoryListenerPlugin.class)) {
			try {
				listener.repositoryRefreshed(this);
			} catch (Exception e) {
				reporter.exception(e, "Updating listener plugin %s", listener);
			}
		}
		return true;
	}

	@Override
	public void setProperties(Map<String, String> map) throws Exception {

		configuration = Converter.cnv(PomConfiguration.class, map);

		if (configuration.name() == null)
			throw new IllegalArgumentException("Must get a name");

		this.name = configuration.name();

		if (configuration.pom() != null) {
			pomFiles = Strings.split(configuration.pom())
				.stream()
				.map(part -> {
					File f = IO.getFile(part);
					return f.isFile() ? f.toURI() : URI.create(part);
				})
				.collect(toList());
			if (pomFiles.isEmpty()) {
				throw new IllegalArgumentException("Pom is neither a file nor a revision " + configuration.pom());
			}
		} else if (configuration.revision() != null) {
			revisions = Strings.split(configuration.revision())
				.stream()
				.map(Revision::valueOf)
				.filter(Objects::nonNull)
				.collect(toList());
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
	public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
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
	public File get(String bsn, Version version, Map<String, String> properties, DownloadListener... listeners)
		throws Exception {
		init();

		Archive archive;
		ResourceInfo resource = bridge.getInfo(bsn, version);
		if (resource == null) {
			archive = trySources(bsn, version);
			if (archive == null)
				return null;
		} else {

			String name = resource.getInfo()
				.name();
			archive = Archive.valueOf(name);
		}

		Promise<File> p = repoImpl.getMavenRepository()
			.get(archive);

		if (listeners.length == 0)
			return p.getValue();

		new DownloadListenerPromise(reporter, name + ": get " + bsn + ";" + version, p, listeners);
		return repoImpl.getMavenRepository()
			.toLocalFile(archive);
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

	@Override
	public String toString() {
		return "BndPomRepository [name=" + getName() + ", localRepo=" + localRepo + ", location=" + getLocation()
			+ ", inited=" + inited + "]";
	}

	@Override
	public File getRoot() throws Exception {
		return repoImpl.getLocation();
	}

	@Override
	public Map<String, Runnable> actions(Object... target) throws Exception {
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

	/*
	 * Try to fetch sources
	 */
	private Archive trySources(String bsn, Version version) throws Exception {
		if (!bsn.endsWith(BSN_SOURCE_SUFFIX))
			return null;

		String rawBsn = bsn.substring(0, bsn.length() - BSN_SOURCE_SUFFIX.length());
		ResourceInfo resource = bridge.getInfo(rawBsn, version);
		if (resource == null)
			return null;

		return Archive.valueOf(resource.getInfo()
			.name())
			.getOther("jar", Archive.SOURCES_CLASSIFIER);
	}

}
