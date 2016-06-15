package aQute.bnd.repository.maven.pom.provider;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.util.promise.Promise;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.build.Workspace;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.repository.BaseRepository;
import aQute.bnd.osgi.repository.BridgeRepository;
import aQute.bnd.osgi.repository.BridgeRepository.ResourceInfo;
import aQute.bnd.service.Actionable;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.Refreshable;
import aQute.bnd.service.Registry;
import aQute.bnd.service.RegistryPlugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.util.repository.DownloadListenerPromise;
import aQute.bnd.version.Version;
import aQute.lib.converter.Converter;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
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
		implements Plugin, RegistryPlugin, RepositoryPlugin, Refreshable, Actionable {
	boolean						inited;
	private PomConfiguration	configuration;
	private Registry			registry;
	private String				name;
	private Reporter			reporter	= new Slf4jReporter();
	private PomRepository		pomRepo;
	private Revision			revision;
	private BridgeRepository	bridge;
	private URI					pomFile;

	public synchronized void init() {
		try {
			if (inited)
				return;
			inited = true;
			Workspace workspace = registry.getPlugin(Workspace.class);
			HttpClient client = registry.getPlugin(HttpClient.class);
			File localRepo = IO.getFile(configuration.local("~/.m2/repository"));
			File location = workspace.getFile(getLocation());

			List<MavenBackingRepository> release = MavenBackingRepository.create(configuration.releaseUrls(), reporter,
					localRepo, client);
			List<MavenBackingRepository> snapshot = MavenBackingRepository.create(configuration.snapshotUrls(),
					reporter, localRepo, client);

			MavenRepository repository = new MavenRepository(localRepo, name, release, snapshot,
					Processor.getExecutor(), reporter, null);

			pomRepo = pomFile != null ? new PomRepository(repository, location, pomFile)
					: new PomRepository(repository, location, revision);
			bridge = new BridgeRepository(pomRepo);
		} catch (Exception e) {
			e.printStackTrace();
			throw Exceptions.duck(e);
		}
	}

	@Override
	public boolean refresh() throws Exception {
		pomRepo.refresh();
		bridge = new BridgeRepository(pomRepo);
		return true;
	}

	@Override
	public void setProperties(Map<String,String> map) throws Exception {

		configuration = Converter.cnv(PomConfiguration.class, map);

		if (configuration.name() == null)
			throw new IllegalArgumentException("Must get a name");

		this.name = configuration.name();

		if (configuration.pom() != null) {

			File f = IO.getFile(configuration.pom());
			if (f.isFile()) {
				this.pomFile = f.toURI();
				if (name == null)
					name = f.getParentFile().getName();
			} else {

				// TODO check uri?

				throw new IllegalArgumentException("pom property is not a file" + f);
			}
		} else if (configuration.revision() != null) {
			revision = Revision.valueOf(configuration.revision());
			if (revision == null)
				throw new IllegalArgumentException(
						"Revision is neither a file nor a revision " + configuration.revision());

			if (name == null)
				name = revision.toString().replace(':', '_');
		} else {
			throw new IllegalArgumentException("Neither pom property nor revision are set");
		}
	}

	@Override
	public Map<Requirement,Collection<Capability>> findProviders(Collection< ? extends Requirement> requirements) {
		init();
		return pomRepo.findProviders(requirements);
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

		Promise<File> p = pomRepo.repo.get(archive);

		if (listeners.length == 0)
			return p.getValue();

		new DownloadListenerPromise(reporter, name + ": get " + bsn + ";" + version, p, listeners);
		return new File("does_not_yet_exist");
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
		return null;
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
}
