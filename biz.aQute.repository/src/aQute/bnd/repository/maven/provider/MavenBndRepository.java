package aQute.bnd.repository.maven.provider;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.Manifest;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.util.promise.Failure;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Success;
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
import aQute.bnd.repository.maven.provider.IndexFile.BundleDescriptor;
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
import aQute.bnd.version.Version;
import aQute.lib.converter.Converter;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.hex.Hex;
import aQute.lib.io.IO;
import aQute.libg.cryptography.SHA1;
import aQute.libg.glob.Glob;
import aQute.maven.api.Archive;
import aQute.maven.api.IMavenRepo;
import aQute.maven.api.IPom;
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
	private final static Logger		logger						= LoggerFactory.getLogger(MavenBndRepository.class);

	private static final String		NONE						= "NONE";
	static final String				MAVEN_REPO_LOCAL			= System.getProperty("maven.repo.local",
			"~/.m2/repository");
	private Configuration			configuration;
	private Registry				registry;
	private File					localRepo;
	private Reporter				reporter;
	IMavenRepo						storage;
	private boolean					inited;
	IndexFile						index;
	private ScheduledFuture< ? >	indexPoller;
	private RepoActions				actions						= new RepoActions(this);
	private String					name;
	private HttpClient				client;
	private ReleasePluginImpl		releasePlugin		= new ReleasePluginImpl(this, null);

	static class LocalPutResult extends PutResult {
		Archive			binaryArchive;
		PutOptions		options;
		String			failed;
		public Archive	pomArchive;
	}

	@Override
	public PutResult put(InputStream stream, PutOptions options) throws Exception {
		init();
		File binaryFile = File.createTempFile("put", ".jar");
		File pomFile = File.createTempFile("pom", ".xml");
		LocalPutResult result = new LocalPutResult();
		try {

			if (options == null)
				options = new PutOptions();
			else {
				result.options = options;
			}

			IO.copy(stream, binaryFile);

			if (options.digest != null) {
				byte[] digest = SHA1.digest(binaryFile).digest();
				if (!Arrays.equals(options.digest, digest))
					throw new IllegalArgumentException("The given sha-1 does not match the contents sha-1");
			}

			if (options.context == null) {

				options.context = registry.getPlugin(Workspace.class);

				if (options.context == null)
					options.context = new Processor();
			}

			ReleaseDTO instructions = getReleaseDTO(options.context);

			try (Jar binary = new Jar(binaryFile)) {
				Resource pomResource;

				if (instructions.pom.path != null) {
					File f = options.context.getFile(instructions.pom.path);
					if (!f.isFile())
						throw new IllegalArgumentException(
								"-maven-release specifies " + f + " as pom file but this file is not found");

					pomResource = new FileResource(f);
				} else {

					pomResource = getPomResource(binary);
					if (pomResource == null) {
						pomResource = createPomResource(binary, options.context);
						if (pomResource == null)
							throw new IllegalArgumentException(
									"No POM resource in META-INF/maven/... The Maven Bnd Repository requires this pom.");
					}
				}

				IO.copy(pomResource.openInputStream(), pomFile);
				IPom pom;
				try (InputStream fin = IO.stream(pomFile)) {
					pom = storage.getPom(fin);
				}
				Archive binaryArchive = pom.binaryArchive();

				checkRemotePossible(instructions, binaryArchive.isSnapshot());

				if (!binaryArchive.isSnapshot()) {
					releasePlugin.add(options.context, pom);
					if (storage.exists(binaryArchive)) {
						logger.debug("Already released {} to {}", pom.getRevision(), this);
						result.alreadyReleased = true;
						return result;
					}
				}

				logger.debug("Put release {}", pom.getRevision());
				try (Release releaser = storage.release(pom.getRevision(), options.context.getProperties())) {
					if (releaser == null) {
						logger.debug("Already released {}", pom.getRevision());
						return result;
					}
					if (instructions.snapshot >= 0)
						releaser.setBuild(instructions.snapshot, null);

					if (isLocal(instructions))
						releaser.setLocalOnly();

					result.pomArchive = pom.getRevision().pomArchive();
					releaser.add(result.pomArchive, pomFile);
					result.binaryArchive = binaryArchive;
					result.artifact = storage.toRemoteURI(binaryArchive);
					releaser.add(binaryArchive, binaryFile);

					if (!isLocal(instructions)) {

						try (Tool tool = new Tool(options.context, binary)) {

							if (instructions.javadoc != null) {
								if (!NONE.equals(instructions.javadoc.path)) {
									try (Jar jar = getJavadoc(tool, options.context, instructions.javadoc.path,
											instructions.javadoc.options,
											instructions.javadoc.packages == JavadocPackages.EXPORT)) {
										save(releaser, pom.getRevision(), jar, "javadoc");
									}
								}
							}

							if (instructions.sources != null) {
								if (!NONE.equals(instructions.sources.path)) {
									try (Jar jar = getSource(tool, options.context, instructions.sources.path)) {
										save(releaser, pom.getRevision(), jar, "sources");
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

	void checkRemotePossible(ReleaseDTO instructions, boolean snapshot) {
		if (instructions.type == ReleaseType.REMOTE) {
			if (snapshot) {
				if (this.storage.getSnapshotRepositories().isEmpty())
					throw new IllegalArgumentException(
							"Remote snapshot release requested but no snapshot repository set for " + getName());
			} else if (this.storage.getReleaseRepositories().isEmpty())
				throw new IllegalArgumentException(
						"Remote release requested but no release repository set for " + getName());
		}
	}

	boolean isLocal(ReleaseDTO instructions) {
		return instructions.type == ReleaseType.LOCAL;
	}

	private Jar getSource(Tool tool, Processor context, String path) throws Exception {
		Jar jar = toJar(context, path);
		if (jar == null) {
			jar = tool.doSource();
		}
		jar.ensureManifest();
		tool.addClose(jar);
		return jar;
	}

	private Jar getJavadoc(Tool tool, Processor context, String path, Map<String,String> options, boolean exports)
			throws Exception {
		Jar jar = toJar(context, path);
		if (jar == null) {
			jar = tool.doJavadoc(options, exports);
		}
		jar.ensureManifest();
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

	private void save(Release releaser, Revision revision, Jar jar, String classifier) throws Exception {
		String extension = IO.getExtension(jar.getName(), "jar");
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
			release.javadoc.path = javadoc.get("path");
			if (NONE.equals(release.javadoc.path)) {
				release.javadoc = null;
			} else
				release.javadoc.options = javadoc;
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

		if (!p.isEmpty()) {
			reporter.warning("The -maven-release instruction contains unrecognized options: %s", p);
		}

		return release;
	}

	private Resource getPomResource(Jar jar) {
		for (Map.Entry<String,Resource> e : jar.getResources().entrySet()) {
			String path = e.getKey();

			if (path.startsWith("META-INF/maven/") && path.endsWith("/pom.xml")) {
				return e.getValue();
			}
		}
		return null;
	}

	private Resource createPomResource(Jar binary, Processor context) throws Exception {
		Manifest manifest = binary.getManifest();
		if (manifest == null)
			return null;

		try (Processor scoped = context == null ? new Processor() : new Processor(context)) {
			if (scoped.getProperty(Constants.GROUPID) == null)
				scoped.setProperty(Constants.GROUPID, "osgi-bundle");
			return new PomResource(scoped, manifest);
		}
	}

	@Override
	public File get(String bsn, Version version, Map<String,String> properties, final DownloadListener... listeners)
			throws Exception {
		init();

		BundleDescriptor descriptor = index.getDescriptor(bsn, version);
		if (descriptor == null)
			return null;

		Archive archive = descriptor.archive;

		if (archive != null) {
			final File file = storage.toLocalFile(archive);

			final File withSources = new File(file.getParentFile(), "+" + file.getName());
			if (withSources.isFile() && withSources.lastModified() > file.lastModified()) {

				if (listeners.length == 0)
					return withSources;

				for (DownloadListener dl : listeners)
					dl.success(withSources);

				return withSources;
			}

			Promise<File> promise = index.updateAsync(descriptor, storage.get(archive));

			if (listeners.length == 0)
				return promise.getValue();

			promise.then(new Success<File,Void>() {
				@Override
				public Promise<Void> call(Promise<File> resolved) throws Exception {
					File value = resolved.getValue();
					if (value == null) {
						throw new FileNotFoundException("Download failed");
					}
					for (DownloadListener dl : listeners) {
						try {
							dl.success(value);
						} catch (Exception e) {
							reporter.exception(e, "Download listener failed in success callback %s", dl);
						}
					}
					return null;
				}
			}).then(null, new Failure() {
				@Override
				public void fail(Promise< ? > resolved) throws Exception {
					String reason = Exceptions.toString(resolved.getFailure());
					for (DownloadListener dl : listeners) {
						try {
							dl.failure(file, reason);
						} catch (Exception e) {
							reporter.exception(e, "Download listener failed in failure callback %s", dl);
						}
					}
				}
			});
			return file;
		}
		return null;
	}

	@Override
	public boolean canWrite() {
		return !configuration.readOnly();
	}

	@Override
	public List<String> list(String pattern) throws Exception {
		init();
		Glob g = pattern == null ? null : new Glob(pattern);

		List<String> bsns = new ArrayList<>();

		for (String bsn : index.list()) {
			if (g == null || g.matcher(bsn).matches())
				bsns.add(bsn);
		}
		return bsns;
	}

	@Override
	public SortedSet<Version> versions(String bsn) throws Exception {
		init();
		TreeSet<Version> versions = new TreeSet<Version>();

		for (Version version : index.list(bsn)) {
			versions.add(version);
		}

		return versions;
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
			storage = new MavenRepository(localRepo, getName(), release, snapshot, client.promiseFactory().executor(),
				reporter);

			File base = IO.work;
			if (registry != null) {
				Workspace ws = registry.getPlugin(Workspace.class);
				if (ws != null)
					base = ws.getBuildDir();
			}

			File indexFile = IO.getFile(base, configuration.index(name.toLowerCase() + ".mvn"));
			IndexFile ixf = new IndexFile(reporter, indexFile, storage, client.promiseFactory());
			ixf.open();
			this.index = ixf;
			startPoll(index);

			logger.debug("initing {}", this);
		} catch (Exception e) {
			reporter.exception(e, "Init for maven repo failed %s", configuration);
			throw new RuntimeException(e);
		}
	}

	private void startPoll(final IndexFile index) {
		Workspace ws = registry.getPlugin(Workspace.class);
		if ((ws != null) && (ws.getGestalt().containsKey(Constants.GESTALT_BATCH)
				|| ws.getGestalt().containsKey(Constants.GESTALT_CI)
				|| ws.getGestalt().containsKey(Constants.GESTALT_OFFLINE))) {
			return;
		}
		final AtomicBoolean busy = new AtomicBoolean();
		indexPoller = Processor.getScheduledExecutor().scheduleAtFixedRate(() -> {
			if (busy.getAndSet(true))
				return;
			try {
				poll();
			} catch (Exception e) {
				reporter.error("Error when polling index for %s for change", this);
			} finally {
				busy.set(false);
			}
		}, 5000, 5000, TimeUnit.MILLISECONDS);
	}

	void poll() throws Exception {
		refresh();
	}

	@Override
	public String getLocation() {
		return configuration.releaseUrl() == null ? configuration.local(MAVEN_REPO_LOCAL) : configuration.releaseUrl();
	}

	@Override
	public void setProperties(Map<String,String> map) throws Exception {
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
		boolean refreshed = index.refresh();
		if (refreshed) {
			for (RepositoryListenerPlugin listener : registry.getPlugins(RepositoryListenerPlugin.class)) {
				try {
					listener.repositoryRefreshed(this);
				} catch (Exception e) {
					reporter.exception(e, "Updating listener plugin %s", listener);
				}
			}
		}
		return refreshed;
	}

	@Override
	public File getRoot() throws Exception {
		return localRepo;
	}

	public BundleDescriptor getDescriptor(String bsn, Version version) throws Exception {
		return index.getDescriptor(bsn, version);
	}

	@Override
	public String toString() {
		return "MavenBndRepository [localRepo=" + localRepo + ", storage=" + getName() + ", inited=" + inited + "]";
	}

	@Override
	public Map<String,Runnable> actions(Object... target) throws Exception {
		switch (target.length) {
			case 0 :
				return null;
			case 1 :
				return actions.getProgramActions((String) target[0]);
			case 2 :
				BundleDescriptor bd = getBundleDescriptor(target);
				return actions.getRevisionActions(bd);
			default :
		}
		return null;
	}

	@Override
	public String tooltip(Object... target) throws Exception {
		switch (target.length) {
			case 0 :
				try (Formatter f = new Formatter()) {
					f.format("%s\n", getName());
					f.format("Revisions %s\n", index.descriptors.size());
					for (MavenBackingRepository mbr : storage.getReleaseRepositories())
						f.format("Release %s  (%s)\n", mbr, getUser(mbr));
					for (MavenBackingRepository mbr : storage.getSnapshotRepositories())
						f.format("Snapshot %s (%s)\n", mbr, getUser(mbr));
					f.format("Storage %s\n", localRepo);
					f.format("Index %s\n", index.indexFile);
					f.format("Index Cache %s\n", index.cacheDir);
					return f.toString();
				}
			case 1 :

				try (Formatter f = new Formatter()) {
					String name = (String) target[0];
					Set<aQute.maven.api.Program> programs = index.getProgramsForBsn(name);
					return programs.toString();
				}
			case 2 :
				BundleDescriptor bd = getBundleDescriptor(target);
				try (Formatter f = new Formatter()) {
					f.format("%s\n", bd.archive);
					f.format("Bundle-Version %s\n", bd.version);
					f.format("Last Modified %s\n", new Date(bd.lastModified));
					f.format("URL %s\n", bd.url);
					f.format("SHA-1 %s\n", Hex.toHexString(bd.id).toLowerCase());
					f.format("SHA-256 %s\n", Hex.toHexString(bd.sha256).toLowerCase());
					File localFile = storage.toLocalFile(bd.archive);
					f.format("Local %s%s\n", localFile, localFile.isFile() ? "" : " ?");
					if (bd.description != null)
						f.format("Description\n%s", bd.description);
					return f.toString();
				}
			default :
		}
		return null;
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

	BundleDescriptor getBundleDescriptor(Object... target) throws Exception {
		String bsn = (String) target[0];
		Version version = (Version) target[1];
		BundleDescriptor bd = getDescriptor(bsn, version);
		return bd;
	}

	@Override
	public String title(Object... target) throws Exception {
		switch (target.length) {
			case 0 :
				String name = getName();
				int n = index.getErrors(null);
				if (n > 0)
					return name += " [" + n + "!]";
				return name;

			case 1 :
				name = (String) target[0];
				n = index.getErrors(name);
				if (n > 0)
					name += " [!]";

				return name;
			case 2 :
				BundleDescriptor bd = getBundleDescriptor(target);
				if (bd.error != null)
					return bd.version + " [" + bd.error + "]";
				else if (isLocal(bd.archive)) {
					return bd.version.toString();
				} else
					return bd.version.toString() + " [?]";

			default :
		}
		return null;
	}

	private boolean isLocal(Archive archive) {
		return storage.toLocalFile(archive).isFile();
	}

	public boolean dropTarget(URI uri) throws Exception {

		String t = uri.toString().trim();
		int n = t.indexOf('\n');
		if (n > 0) {
			uri = new URI(t.substring(0, n));
			logger.debug("dropTarget cleaned up from {} to {}", t, uri);
		}

		if ("search.maven.org".equals(uri.getHost()) && "/remotecontent".equals(uri.getPath())) {
			return doSearchMaven(uri);
		}

		if (uri.getPath() != null && uri.getPath().endsWith(".pom"))
			return addPom(uri);

		return false;
	}

	public boolean dropTarget(File file) throws Exception {
		if (file.getName().equals("pom.xml")) {
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

	boolean doSearchMaven(URI uri) throws UnsupportedEncodingException, Exception {
		Map<String,String> map = getMapFromQuery(uri);
		String filePath = map.get("filepath");
		if (filePath != null) {
			Archive archive = Archive.fromFilepath(filePath);
			if (archive != null) {
				if (archive.extension.equals("pom"))
					archive = archive.revision.archive("jar", null);
				index.add(archive);
				return true;
			}
		}
		return false;
	}

	Map<String,String> getMapFromQuery(URI uri) throws UnsupportedEncodingException {
		String rawQuery = uri.getRawQuery();
		Map<String,String> map = new HashMap<>();
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
	public Map<Requirement,Collection<Capability>> findProviders(Collection< ? extends Requirement> requirements) {
		init();
		return index.findProviders(requirements);
	}

	@Override
	public void begin(Project project) {
		releasePlugin = new ReleasePluginImpl(this, project);
	}

	@Override
	public void end(Project p) {
		System.out.println("Project ending is " + p);
		try {
			releasePlugin.end(p, storage);
		} catch (Exception e) {
			e.printStackTrace();
			p.error("Could not end the release", e);
		}
	}

}
