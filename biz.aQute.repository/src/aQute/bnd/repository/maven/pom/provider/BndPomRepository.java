package aQute.bnd.repository.maven.pom.provider;

import static aQute.bnd.osgi.Constants.BSN_SOURCE_SUFFIX;
import static aQute.bnd.service.tags.Tags.parse;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.util.promise.Promise;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.build.Workspace;
import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.http.HttpClient;
import aQute.bnd.memoize.Memoize;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.repository.BaseRepository;
import aQute.bnd.osgi.repository.BridgeRepository;
import aQute.bnd.osgi.repository.BridgeRepository.ResourceInfo;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.service.Actionable;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.Refreshable;
import aQute.bnd.service.Registry;
import aQute.bnd.service.RegistryPlugin;
import aQute.bnd.service.RepositoryListenerPlugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.clipboard.Clipboard;
import aQute.bnd.service.repository.Prepare;
import aQute.bnd.util.repository.DownloadListenerPromise;
import aQute.bnd.version.Version;
import aQute.lib.converter.Converter;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.libg.reporter.slf4j.Slf4jReporter;
import aQute.maven.api.Archive;
import aQute.maven.provider.MavenBackingRepository;
import aQute.maven.provider.MavenRepository;
import aQute.service.reporter.Reporter;

/**
 * This is the Bnd repository for Maven.
 */
@BndPlugin(name = "BndPomRepository", parameters = PomConfiguration.class)
public class BndPomRepository extends BaseRepository
	implements Plugin, RegistryPlugin, RepositoryPlugin, Refreshable, Actionable, Closeable, Prepare {
	private static final String	MAVEN_REPO_LOCAL	= System.getProperty("maven.repo.local", "~/.m2/repository");
	private static final int	DEFAULT_POLL_TIME	= 300;

	private final Memoize<Promise<Boolean>>	prepared;

	private PomConfiguration	configuration;
	private Registry			registry;
	private String				name;
	private Reporter			reporter			= new Slf4jReporter(BndPomRepository.class);
	private File				localRepo;
	private InnerRepository		repoImpl;
	private List<Archive>		archives;
	private BridgeRepository	bridge;
	private List<URI>			pomFiles;
	private String				query;
	private String				queryUrl;
	private ScheduledFuture<?>	pomPoller;

	private String				status;

	public BndPomRepository() {
		prepared = Memoize.supplier(this::preparePromise);
	}

	private boolean init() {
		try {
			return prepared.get() // start preparation
				.getValue(); // wait for completion
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	@Override
	public void prepare() {
		prepared.get(); // start preparation
	}

	private Promise<Boolean> preparePromise() {
		if (configuration.name() == null) {
			status("Must get a name");
		}

		this.name = configuration.name();

		if (configuration.snapshotUrl() != null) {
			@SuppressWarnings("deprecation")
			String snapshotUrls = configuration.snapshotUrls();
			if (snapshotUrls != null) {
				status("snapshotUrl and snapshotUrls property is set. Please only use snapshotUrl.");
			}
		}

		if (configuration.releaseUrl() != null) {
			@SuppressWarnings("deprecation")
			String releaseUrls = configuration.releaseUrls();
			if (releaseUrls != null) {
				status("releaseUrl and releaseUrls property is set. Please only use releaseUrl.");
			}
		}

		if (configuration.pom() != null) {
			pomFiles = Strings.split(configuration.pom())
				.stream()
				.map(part -> {
					File f = IO.getFile(part);
					return f.isFile() ? f.toURI() : URI.create(part);
				})
				.collect(toList());
			if (pomFiles.isEmpty()) {
				status("Pom is neither a file nor a archive " + configuration.pom());
			}
		} else if (configuration.revision() != null) {
			archives = Strings.split(configuration.revision())
				.stream()
				.map(Archive::valueOf)
				.filter(Objects::nonNull)
				.collect(toList());
			if (archives.isEmpty()) {
				status("Archive is neither a file nor a revision " + configuration.revision());
			}
		} else if (configuration.query() != null) {
			this.query = configuration.query();
			this.queryUrl = configuration.queryUrl("https://search.maven.org/solrsearch/select");
		} else {
			status("Neither pom, archive nor query property are set");
		}

		return Processor.getPromiseFactory()
			.submit(this::prepareAsync);
	}

	private boolean prepareAsync() {
		if (!isOk()) {
			return false;
		}
		try {
			Workspace workspace = registry.getPlugin(Workspace.class);
			HttpClient client = registry.getPlugin(HttpClient.class);
			localRepo = IO.getFile(configuration.local(MAVEN_REPO_LOCAL));
			File location = workspace.getFile(getLocation());

			String releaseUrl = configuration.releaseUrl();
			if (releaseUrl == null) {
				@SuppressWarnings("deprecation")
				String releaseUrls = configuration.releaseUrls();
				releaseUrl = releaseUrls;
			}
			String snapshotUrl = configuration.snapshotUrl();
			if (snapshotUrl == null) {
				@SuppressWarnings("deprecation")
				String snapshotUrls = configuration.snapshotUrls();
				snapshotUrl = snapshotUrls;
			}

			List<MavenBackingRepository> release = MavenBackingRepository.create(releaseUrl, reporter, localRepo,
				client);
			List<MavenBackingRepository> snapshot = MavenBackingRepository.create(snapshotUrl, reporter, localRepo,
				client);

			MavenRepository repository = new MavenRepository(localRepo, name, release, snapshot, client.promiseFactory()
				.executor(), reporter);

			boolean transitive = configuration.transitive(true);
			boolean dependencyManagement = configuration.dependencyManagement(false);

			if (pomFiles != null) {
				repoImpl = new PomRepository(repository, client, location, transitive, dependencyManagement)
					.uris(pomFiles);
			} else if (archives != null) {
				repoImpl = new PomRepository(repository, client, location, transitive, dependencyManagement)
					.archives(archives);
			} else if (query != null) {
				repoImpl = new SearchRepository(repository, location, query, queryUrl, workspace, client, transitive,
					dependencyManagement);
			} else {
				repository.close();
				return false;
			}
			bridge = new BridgeRepository(repoImpl);

			startPoll();
			return true;
		} catch (Exception e) {
			reporter.exception(e, "Init for BndPomRepository failed %s", configuration);
			status(Exceptions.causes(e));
			return false;
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
					if (inPoll.getAndSet(true)) {
						return;
					}
					Processor.getExecutor()
						.execute(() -> {
							try {
								poll();
							} catch (Exception e) {
								reporter.exception(e, "Error when polling for %s for change", this);
							} finally {
								inPoll.set(false);
							}
						});
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
		if (!init()) {
			return false;
		}
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
		super.setTags(parse(configuration.tags(), DEFAULT_REPO_TAGS));
	}

	@Override
	public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
		if (!init()) {
			return ResourceUtils.emptyProviders(requirements);
		}
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
		if (!init()) {
			return null;
		}

		Archive archive;

		if (isMavenGAV(bsn)) {

			// Handle the GAV. This "hack" is borrowed from
			// aQute.bnd.repository.maven.provider.IndexFile.find(String,
			// Version)
			// because MavenBndRepository can handle both bsn and GAV too

			if (repoImpl instanceof PomRepository pr) {

				Archive spec = Archive.valueOf(bsn + ":" + version);

				archive = pr.archives.stream()
					.filter(a -> matches(a, spec))
					.findAny()
					.orElse(null);
				if (archive == null)
					return null;
			} else {
				return null;
			}

		} else {

			ResourceInfo resource = bridge.getInfo(bsn, version);
			if (resource == null) {
				archive = trySources(bsn, version);
				if (archive == null)
					return null;
			} else {

				String from = resource.getInfo()
					.from();
				archive = Archive.valueOf(from);
			}
		}

		Promise<File> p = repoImpl.getMavenRepository()
			.get(archive);

		if (listeners.length == 0)
			return p.getValue();

		Map<String, String> attrs = archive.attributes();
		new DownloadListenerPromise(reporter, name + ": get " + bsn + ";" + version, p, attrs, listeners);
		return repoImpl.getMavenRepository()
			.toLocalFile(archive);
	}

	private boolean isMavenGAV(String bsn) {
		return bsn != null && bsn.indexOf(':') != -1;
	}

	private boolean matches(Archive archive, Archive spec) {
		return archive.revision.program.equals(spec.revision.program) && archive.revision.version.getOSGiVersion()
			.equals(spec.revision.version.getOSGiVersion()) && spec.classifier.equals(archive.classifier);
	}

	private boolean matchesGAV(Archive archive, String groupId, String artifactId) {
		return archive.revision.group.equals(groupId) && archive.revision.artifact.equals(artifactId);
	}


	@Override
	public boolean canWrite() {
		return false;
	}

	@Override
	public List<String> list(String pattern) throws Exception {
		if (!init()) {
			return Collections.emptyList();
		}
		return bridge.list(pattern);
	}

	@Override
	public SortedSet<Version> versions(String bsn) throws Exception {
		if (!init()) {
			return Collections.emptySortedSet();
		}

		if (isMavenGAV(bsn)) {
			if (repoImpl instanceof PomRepository pr) {

				String[] split = bsn.split(":");
				String groupId = split[0];
				String artifactId = split[1];


				SortedSet<Version> versions = pr.archives.stream()
					.filter(a -> matchesGAV(a, groupId, artifactId))
					.map(a -> a.revision.version.getOSGiVersion())
					.collect(Collectors.toCollection(TreeSet::new));

				return versions;

			} else {
				return Collections.emptySortedSet();
			}
		}
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
		return "BndPomRepository [name=" + getName() + ", localRepo=" + localRepo + ", location=" + getLocation() + "]";
	}

	@Override
	public File getRoot() throws Exception {
		return repoImpl.getLocation();
	}

	@Override
	public String getStatus() {
		return status;
	}

	private void status(String s) {
		if (s == null)
			return;

		if (status == null)
			status = s;
		else {
			status = status.concat("\n")
				.concat(s);
		}
	}

	@Override
	public Map<String, Runnable> actions(Object... target) throws Exception {
		if (!init()) {
			return Collections.emptyMap();
		}
		Clipboard cb = registry.getPlugin(Clipboard.class);
		if (cb == null)
			return null;

		Map<String, Runnable> menu = new TreeMap<>();
		menu.put("Copy GAVs", () -> {
			String gavs = bridge.getResourceInfos()
				.stream()
				.map(ri -> ri.getName())
				.collect(joining("\n"));
			cb.copy(gavs);
		});
		return menu;
	}

	@Override
	public String tooltip(Object... target) throws Exception {
		if (!init()) {
			return getStatus();
		}
		return bridge.tooltip(target);
	}

	@Override
	public String title(Object... target) throws Exception {
		if (!init()) {
			return null;
		}
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

		String from = resource.getInfo()
			.from();
		return Archive.valueOf(from)
			.getOther(Archive.JAR_EXTENSION, Archive.SOURCES_CLASSIFIER);
	}


}
