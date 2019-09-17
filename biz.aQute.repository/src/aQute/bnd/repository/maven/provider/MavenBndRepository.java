package aQute.bnd.repository.maven.provider;

import static aQute.bnd.osgi.Constants.BSN_SOURCE_SUFFIX;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.jar.Manifest;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.http.HttpClient;
import aQute.bnd.maven.PomResource;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.bnd.osgi.repository.BaseRepository;
import aQute.bnd.repository.maven.provider.ReleaseDTO.JavadocPackages;
import aQute.bnd.repository.maven.provider.ReleaseDTO.ReleaseType;
import aQute.bnd.service.Actionable;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.Refreshable;
import aQute.bnd.service.Registry;
import aQute.bnd.service.RegistryPlugin;
import aQute.bnd.service.RepositoryListenerPlugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.maven.PomOptions;
import aQute.bnd.service.maven.ToDependencyPom;
import aQute.bnd.service.release.ReleaseBracketingPlugin;
import aQute.bnd.util.repository.DownloadListenerPromise;
import aQute.bnd.version.Version;
import aQute.lib.converter.Converter;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.exceptions.FunctionWithException;
import aQute.lib.io.IO;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.libg.cryptography.SHA1;
import aQute.libg.glob.PathSet;
import aQute.maven.api.Archive;
import aQute.maven.api.IMavenRepo;
import aQute.maven.api.IPom;
import aQute.maven.api.Program;
import aQute.maven.api.Release;
import aQute.maven.api.Revision;
import aQute.maven.provider.MavenBackingRepository;
import aQute.maven.provider.MavenRepository;
import aQute.maven.provider.PomGenerator;
import aQute.service.reporter.Reporter;

/**
 * This is the Bnd repository for Maven.
 */
@BndPlugin(name = "MavenBndRepository")
public class MavenBndRepository extends BaseRepository implements RepositoryPlugin, RegistryPlugin, Plugin, Closeable,
	Refreshable, Actionable, ToDependencyPom, ReleaseBracketingPlugin {

	private final static Logger	logger				= LoggerFactory.getLogger(MavenBndRepository.class);
	private static final int	DEFAULT_POLL_TIME	= 5;

	private static final String	NONE				= "NONE";
	private static final String	MAVEN_REPO_LOCAL	= System.getProperty("maven.repo.local", "~/.m2/repository");
	private Configuration		configuration;
	private Registry			registry;
	private File				localRepo;
	private Reporter			reporter;
	IMavenRepo					storage;
	private boolean				inited;
	IndexFile					index;
	private ScheduledFuture<?>	indexPoller;
	private RepoActions			actions				= new RepoActions(this);
	private String				name;
	private HttpClient			client;
	private ReleasePluginImpl	releasePlugin		= new ReleasePluginImpl(this, null);
	private File				base;

	/**
	 * Put result
	 */
	static class LocalPutResult extends PutResult {
		Archive			binaryArchive;
		PutOptions		options;
		String			failed;
		public Archive	pomArchive;
	}

	/**
	 * Put a bundle
	 */
	@Override
	public PutResult put(InputStream stream, PutOptions options) throws Exception {
		init();
		File binaryFile = File.createTempFile("put", ".jar");
		File pomFile = File.createTempFile(Archive.POM_EXTENSION, ".xml");
		LocalPutResult result = new LocalPutResult();
		try {

			if (options == null)
				options = new PutOptions();
			else {
				result.options = options;
			}

			if (options.context == null) {

				options.context = registry.getPlugin(Workspace.class);

				if (options.context == null)
					options.context = new Processor();
			}

			IO.copy(stream, binaryFile);

			if (options.digest != null) {
				byte[] digest = SHA1.digest(binaryFile)
					.digest();
				if (!Arrays.equals(options.digest, digest))
					throw new IllegalArgumentException("The given sha-1 does not match the contents sha-1");
			}

			ReleaseDTO instructions = getReleaseDTO(options.context);

			try (Jar binary = new Jar(binaryFile)) {

				Resource resource = getPom(options, instructions, binary);
				if (resource == null) {
					throw new IllegalArgumentException(
						"Could not create a pom from Maven metainfo properties nor manifest information");
				}

				IPom pom = storage.getPom(resource.openInputStream());
				if (pom == null || !pom.hasValidGAV()) {
					throw new IllegalArgumentException("Could not create a pom");
				}
				IO.copy(resource.openInputStream(), pomFile);

				Archive binaryArchive = pom.binaryArchive();

				checkRemotePossible(instructions, binaryArchive.isSnapshot());

				if (!binaryArchive.isSnapshot()) {
					releasePlugin.add(options.context, pom);
					if (storage.exists(binaryArchive)) {
						result.alreadyReleased = true;
						if (!configuration.redeploy()) {
							logger.debug("Already released {} to {}", pom.getRevision(), this);
							return result;
						}
						logger.debug("Redeploying {}", pom.getRevision(), this);
					}
				}

				logger.debug("Put release {}", pom.getRevision());
				try (Release releaser = storage.release(pom.getRevision(), options.context.getProperties())) {
					if (releaser == null) {
						logger.debug("Already released {}", pom.getRevision());
						return result;
					}

					if (instructions.passphrase != null && !instructions.passphrase.trim()
						.isEmpty()) {
						releaser.setPassphrase(instructions.passphrase);
					}
					if (instructions.snapshot >= 0)
						releaser.setBuild(instructions.snapshot, null);

					if (isLocal(instructions))
						releaser.setLocalOnly();

					result.pomArchive = pom.getRevision()
						.pomArchive();
					releaser.add(result.pomArchive, pomFile);
					result.binaryArchive = binaryArchive;
					result.artifact = storage.toRemoteURI(binaryArchive);
					releaser.add(binaryArchive, binaryFile);

					if (!isLocal(instructions)) {
						try (Tool tool = new Tool(options.context, binary)) {
							if (instructions.sources != null) {
								if (!NONE.equals(instructions.sources.path)) {
									try (Jar jar = getSources(tool, options.context, instructions.sources.path)) {
										save(releaser, pom.getRevision(), jar);
									}
								}
							}

							if (instructions.javadoc != null) {
								if (!NONE.equals(instructions.javadoc.path)) {
									try (Jar jar = getJavadoc(tool, options.context, instructions.javadoc.path,
										instructions.javadoc.options,
										instructions.javadoc.packages == JavadocPackages.EXPORT)) {
										save(releaser, pom.getRevision(), jar);
									}
								}
							}
						}
					}
				}
				if (configuration.noupdateOnRelease() == false && !binaryArchive.isSnapshot())
					index.add(binaryArchive);
			}
			return result;
		} catch (Exception e) {
			result.failed = e.getMessage();
			throw e;
		} finally {
			IO.delete(binaryFile);
			IO.delete(pomFile);
		}

	}

	private Resource getPom(PutOptions options, ReleaseDTO instructions, Jar binary) throws Exception, IOException {
		Resource pom = null;

		if (instructions.pom.path != null) {
			if (instructions.pom.path.equals("JAR")) {
				pom = binary.getPomXmlResources()
					.findFirst()
					.orElse(null);
			} else {
				pom = createPomFromFile(options.context.getFile(instructions.pom.path));
			}
		} else {
			if (!configuration.ignore_metainf_maven()) {
				if (options.context.is(Constants.POM)) {
					pom = binary.getPomXmlResources()
						.findFirst()
						.orElse(null);
				} else {
					pom = createPomFromFirstMavenPropertiesInJar(binary, options.context);
				}
			}
			if (pom == null) {
				logger.info("No properties in binary or invalid GAV");
				pom = createPomFromContextAndManifest(binary.getManifest(), options.context);
			}
			if (pom == null) {
				pom = createPomFromFirstMavenPropertiesInJar(binary, options.context);
			}
		}
		return pom;
	}

	private void checkRemotePossible(ReleaseDTO instructions, boolean snapshot) {
		if (instructions.type == ReleaseType.REMOTE) {
			if (snapshot) {
				if (this.storage.getSnapshotRepositories()
					.isEmpty())
					throw new IllegalArgumentException(
						"Remote snapshot release requested but no snapshot repository set for " + getName());
			} else if (this.storage.getReleaseRepositories()
				.isEmpty())
				throw new IllegalArgumentException(
					"Remote release requested but no release repository set for " + getName());
		}
	}

	private boolean isLocal(ReleaseDTO instructions) {
		return instructions.type == ReleaseType.LOCAL;
	}

	private Jar getSources(Tool tool, Processor context, String path) throws Exception {
		Jar jar = toJar(context, path);
		if (jar != null) {
			tool.setSources(jar, "");
		} else {
			jar = tool.doSource();
		}
		jar.ensureManifest();
		jar.setName(Archive.SOURCES_CLASSIFIER); // set jar name to classifier
		tool.addClose(jar);
		return jar;
	}

	private Jar getJavadoc(Tool tool, Processor context, String path, Map<String, String> options, boolean exports)
		throws Exception {
		Jar jar = toJar(context, path);
		if (jar == null) {
			jar = tool.doJavadoc(options, exports);
		}
		jar.ensureManifest();
		jar.setName(Archive.JAVADOC_CLASSIFIER); // set jar name to classifier
		tool.addClose(jar);
		return jar;
	}

	private Jar toJar(Processor context, String path) throws Exception {
		if (path == null)
			return null;

		File f = context.getFile(path);
		if (f.exists())
			return new Jar(f);

		return null;
	}

	private void save(Release releaser, Revision revision, Jar jar) throws Exception {
		String classifier = jar.getName(); // jar name is classifier
		String extension = "jar";
		File tmp = File.createTempFile(classifier, extension);
		try {
			jar.write(tmp);
			releaser.add(revision.archive(extension, classifier), tmp);
		} finally {
			IO.delete(tmp);
		}

	}

	/*
	 * Parse the -maven-release header.
	 */
	private ReleaseDTO getReleaseDTO(Processor context) {
		ReleaseDTO release = new ReleaseDTO();
		if (context == null)
			return release;

		Parameters p = new Parameters(context.getProperty(Constants.MAVEN_RELEASE), reporter);

		release.type = storage.isLocalOnly() ? ReleaseType.LOCAL : ReleaseType.REMOTE;

		Attrs attrs = p.remove("remote");
		if (attrs != null) {
			release.type = ReleaseType.REMOTE;
			String s = attrs.get("snapshot");
			if (s != null)
				release.snapshot = Long.parseLong(s);

		} else {
			attrs = p.remove("local");
			if (attrs != null) {
				release.type = ReleaseType.LOCAL;
			}
		}

		Attrs javadoc = p.remove("javadoc");
		if (javadoc != null) {
			release.javadoc.path = javadoc.remove("path");
			if (NONE.equals(release.javadoc.path)) {
				release.javadoc = null;
			} else {
				String packages = javadoc.remove("packages");
				if (packages != null) {
					try {
						release.javadoc.packages = JavadocPackages.valueOf(packages.toUpperCase(Locale.ROOT));
					} catch (Exception e) {
						reporter.warning(
							"The -maven-release instruction contains unrecognized javadoc packages option: %s",
							packages);
					}
				}
				release.javadoc.options = javadoc;
			}
		}

		Attrs sources = p.remove("sources");
		if (sources != null) {
			release.sources.path = sources.get("path");
			if (NONE.equals(release.sources.path))
				release.sources = null;
		}

		Attrs pom = p.remove("pom");
		if (pom != null) {
			release.pom.path = pom.get("path");
		}

		Attrs sign = p.remove("sign");
		if (sign != null) {
			release.passphrase = sign.get("passphrase");
		}

		if (!p.isEmpty()) {
			reporter.warning("The -maven-release instruction contains unrecognized options: %s", p);
		}

		return release;
	}

	private Resource createPomFromFile(File file) throws Exception {
		if (!file.isFile())
			return null;

		return new FileResource(file);
	}

	private static final Predicate<String> pomPropertiesFilter = new PathSet("META-INF/maven/*/*/pom.properties")
		.matches();

	private PomResource createPomFromFirstMavenPropertiesInJar(Jar jar, Processor context) throws Exception {
		return jar.getResources(pomPropertiesFilter)
			.findFirst()
			.map(FunctionWithException.asFunction(r -> {
				UTF8Properties utf8p = new UTF8Properties();
				try (InputStream in = r.openInputStream()) {
					utf8p.load(in);
				}
				String version = utf8p.getProperty("version");
				String groupId = utf8p.getProperty("groupId");
				String artifactId = utf8p.getProperty("artifactId");

				try (Processor scoped = new Processor(context)) {
					scoped.addProperties(utf8p);
					return new PomResource(scoped, jar.getManifest(), groupId, artifactId, version);
				}
			}))
			.orElse(null);
	}

	private PomResource createPomFromContextAndManifest(Manifest manifest, Processor context) throws Exception {
		try (Processor scoped = context == null ? new Processor() : new Processor(context)) {
			if (scoped.getProperty(Constants.GROUPID) == null)
				scoped.setProperty(Constants.GROUPID, "osgi-bundle");
			return new PomResource(scoped, manifest);
		}
	}

	/**
	 * Get a bundle
	 */

	@Override
	public File get(String bsn, Version version, Map<String, String> properties, final DownloadListener... listeners)
		throws Exception {
		init();
		index.sync(); // make sure all is downloaded & parsed

		Archive archive = index.find(bsn, version);
		if (archive == null) {
			return trySources(bsn, version, listeners);
		}

		File f = storage.toLocalFile(archive);
		File withSources = new File(f.getParentFile(), "+" + f.getName());
		if (withSources.isFile() && withSources.lastModified() > f.lastModified())
			f = withSources;

		if (listeners.length == 0) {
			return f;
		}

		for (DownloadListener dl : listeners) {
			try {
				dl.success(f);
			} catch (Exception e) {
				logger.warn("updating listener has error", e);
			}
		}
		return f;
	}

	@Override
	public boolean canWrite() {
		return !configuration.readOnly();
	}

	@Override
	public List<String> list(String pattern) throws Exception {
		init();
		return index.getBridge()
			.list(pattern);
	}

	@Override
	public SortedSet<Version> versions(String bsn) throws Exception {
		init();
		return index.versions(bsn);
	}

	@Override
	public String getName() {
		init();
		return name;
	}

	private synchronized void init() {
		try {
			if (inited)
				return;

			client = registry.getPlugin(HttpClient.class);
			inited = true;
			name = configuration.name("Maven");

			localRepo = IO.getFile(configuration.local(MAVEN_REPO_LOCAL));

			List<MavenBackingRepository> release = MavenBackingRepository.create(configuration.releaseUrl(), reporter,
				localRepo, client);
			List<MavenBackingRepository> snapshot = MavenBackingRepository.create(configuration.snapshotUrl(), reporter,
				localRepo, client);
			storage = new MavenRepository(localRepo, getName(), release, snapshot, client.promiseFactory()
				.executor(), reporter);

			base = IO.work;
			if (registry != null) {
				Workspace ws = registry.getPlugin(Workspace.class);
				if (ws != null)
					base = ws.getBuildDir();
			}

			File indexFile = getIndexFile();
			String tmp = configuration.multi();
			String[] multi;
			if (tmp == null) {
				multi = new String[0];
			} else {
				multi = tmp.trim()
					.split("\\s*,\\s*");
			}
			Processor domain = (registry != null) ? registry.getPlugin(Processor.class) : null;
			String source = configuration.source();
			if (source != null) {
				source = source.replaceAll("(\\s|,|;|\n|\r)+", "\n");
			}
			this.index = new IndexFile(domain, reporter, indexFile, source, storage, client.promiseFactory(), multi);
			this.index.open();

			startPoll();
			logger.debug("initing {}", this);
		} catch (Exception e) {
			reporter.exception(e, "Init for MavenBndRepository failed %s", configuration);
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
			indexPoller = Processor.getScheduledExecutor()
				.scheduleAtFixedRate(() -> {
					if (inPoll.getAndSet(true))
						return;
					try {
						poll();
					} catch (Exception e) {
						reporter.error("Error when polling index for %s for change", this);
					} finally {
						inPoll.set(false);
					}
				}, polltime, polltime, TimeUnit.SECONDS);
		}
	}

	private void poll() throws Exception {
		refresh();
	}

	@Override
	public String getLocation() {
		return configuration.releaseUrl() == null ? configuration.local(MAVEN_REPO_LOCAL) : configuration.releaseUrl();
	}

	@Override
	public void setProperties(Map<String, String> map) throws Exception {
		configuration = Converter.cnv(Configuration.class, map);

	}

	@Override
	public void setReporter(Reporter reporter) {
		this.reporter = reporter;
	}

	@Override
	public void setRegistry(Registry registry) {
		this.registry = registry;
	}

	@Override
	public void close() throws IOException {
		IO.close(storage);
		if (indexPoller != null)
			indexPoller.cancel(true);
	}

	@Override
	public boolean refresh() throws Exception {
		init();

		return index.refresh(() -> {
			for (RepositoryListenerPlugin listener : registry.getPlugins(RepositoryListenerPlugin.class)) {
				try {
					listener.repositoryRefreshed(this);
				} catch (Exception e) {
					reporter.exception(e, "Updating listener plugin %s", listener);
				}
			}
		});
	}

	@Override
	public File getRoot() throws Exception {
		return localRepo;
	}

	@Override
	public String toString() {
		return "MavenBndRepository [localRepo=" + localRepo + ", storage=" + getName() + ", inited=" + inited
			+ ", redeploy=" + configuration.redeploy() + "]";
	}

	@Override
	public Map<String, Runnable> actions(Object... target) throws Exception {
		init();
		switch (target.length) {
			case 0 :
				return null;
			case 1 :
				return actions.getProgramActions((String) target[0]);
			case 2 :
				Archive archive = getArchive(target);
				return actions.getRevisionActions(archive);
			default :
		}
		return null;
	}

	@Override
	public String tooltip(Object... target) throws Exception {
		init();
		switch (target.length) {
			case 0 :
				try (Formatter f = new Formatter()) {
					f.format("%s (MavenBndRepository)\n", getName());
					f.format("Revisions %s\n", index.archives.size());
					for (MavenBackingRepository mbr : storage.getReleaseRepositories())
						f.format("Release %s  (%s)\n", mbr, getUser(mbr));
					for (MavenBackingRepository mbr : storage.getSnapshotRepositories())
						f.format("Snapshot %s (%s)\n", mbr, getUser(mbr));
					f.format("Storage %s\n", localRepo);
					f.format("Index %s\n", index.indexFile);
					return f.toString();
				}
			default :
				return index.tooltip(target);
		}
	}

	private Object getUser(MavenBackingRepository remote) {
		if (remote == null)
			return "";

		try {
			return remote.getUser();
		} catch (Exception e) {
			return "error: " + e.toString();
		}
	}

	Archive getArchive(Object... target) throws Exception {
		String bsn = (String) target[0];
		Version version = (Version) target[1];
		return index.find(bsn, version);
	}

	@Override
	public String title(Object... target) throws Exception {
		init();
		return index.getBridge()
			.title(target);
	}

	public boolean dropTarget(URI uri) throws Exception {
		init();

		String t = uri.toString()
			.trim();
		int n = t.indexOf('\n');
		if (n > 0) {
			uri = new URI(t.substring(0, n));
			logger.debug("dropTarget cleaned up from {} to {}", t, uri);
		}

		if ("search.maven.org".equals(uri.getHost()) && "/remotecontent".equals(uri.getPath())) {
			return doSearchMaven(uri);
		}

		if (uri.getPath() != null && uri.getPath()
			.endsWith(".pom"))
			return addPom(uri);

		return false;
	}

	public boolean dropTarget(File file) throws Exception {
		if (file.getName()
			.equals("pom.xml")) {
			return addPom(file.toURI());
		}
		return false;
	}

	private boolean addPom(URI uri) throws Exception {
		try {
			// http://search.maven.org/remotecontent?filepath=com/netflix/governator/governator-commons-cli/1.12.10/governator-commons-cli-1.12.10.pom
			IPom pom = storage.getPom(client.connect(uri.toURL()));
			Archive binaryArchive = pom.binaryArchive();
			index.add(binaryArchive);
			return true;
		} catch (FileNotFoundException e) {
			return false;
		} catch (Exception e) {
			logger.debug("Failure to parse {}", uri, e);
			return false;
		}
	}

	private boolean doSearchMaven(URI uri) throws UnsupportedEncodingException, Exception {
		Map<String, String> map = getMapFromQuery(uri);
		String filePath = map.get("filepath");
		if (filePath != null) {
			Archive archive = Archive.fromFilepath(filePath);
			if (archive != null) {
				if (archive.extension.equals(Archive.POM_EXTENSION))
					archive = archive.revision.archive(Archive.JAR_EXTENSION, null);
				index.add(archive);
				return true;
			}
		}
		return false;
	}

	private Map<String, String> getMapFromQuery(URI uri) throws UnsupportedEncodingException {
		String rawQuery = uri.getRawQuery();
		Map<String, String> map = new HashMap<>();
		if (rawQuery != null) {
			String parts[] = rawQuery.split("&");
			for (String part : parts) {
				String kv[] = part.split("=");
				String key = URLDecoder.decode(kv[0], "UTF-8");
				String value = kv.length > 1 ? URLDecoder.decode(kv[1], "UTF-8") : "";
				String previous = map.put(key, value);
				if (previous != null) {
					map.put(key, previous + "," + value);
				}
			}
		}
		return map;
	}

	@Override
	public void toPom(OutputStream out, PomOptions options) throws Exception {
		init();
		PomGenerator pg = new PomGenerator(index.getArchives());
		pg.name(Revision.valueOf(options.gav))
			.parent(Revision.valueOf(options.parent))
			.dependencyManagement(options.dependencyManagement)
			.out(out);

	}

	@Override
	public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
		init();
		return index.getBridge()
			.getRepository()
			.findProviders(requirements);
	}

	@Override
	public void begin(Project project) {
		releasePlugin = new ReleasePluginImpl(this, project);
	}

	@Override
	public void end(Project p) {
		try {
			releasePlugin.end(p, storage);
		} catch (Exception e) {
			p.exception(e, "Could not end the release for project %s", releasePlugin.indexProject);
		} finally {
			releasePlugin = new ReleasePluginImpl(this, null);
		}
	}

	@Deprecated
	public IndexFile.BundleDescriptor getDescriptor(String bsn, aQute.bnd.version.Version version) {
		throw new UnsupportedOperationException();
	}

	/*
	 * Bndtools ask for a JAR with a bsn that as ".source" appended to get the
	 * source code. We can automate that by looking for it. The bsn - ".source"
	 * must exist in the index
	 */
	private File trySources(String sourceBsn, Version version, DownloadListener... listeners) throws Exception {
		if (!sourceBsn.endsWith(BSN_SOURCE_SUFFIX))
			return null;

		String originalBsn = sourceBsn.substring(0, sourceBsn.length() - BSN_SOURCE_SUFFIX.length());

		Archive primaryArchive = index.find(originalBsn, version);
		if (primaryArchive == null)
			return null; // don't get sources when not in index

		Archive sources = primaryArchive.getOther("jar", Archive.SOURCES_CLASSIFIER);
		if (sources == null)
			return null;

		Promise<File> promise = storage.get(primaryArchive);
		if (listeners.length != 0) {
			new DownloadListenerPromise(reporter, "Get sources " + sourceBsn + "-" + version + " for " + getName(),
				promise, listeners);
			return storage.toLocalFile(primaryArchive);
		} else
			return promise.getValue();
	}

	public File getIndexFile() {
		return IO.getFile(base, configuration.index(name.toLowerCase() + ".mvn"));
	}

	public Set<Archive> getArchives() {
		init();
		return index.archives.keySet();
	}

	public List<Revision> getRevisions(Program program) throws Exception {
		return storage.getRevisions(program);
	}
}
