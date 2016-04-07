package aQute.bnd.repository.maven.provider;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.util.promise.Promise;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.build.Workspace;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
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
import aQute.bnd.service.repository.InfoRepository;
import aQute.bnd.version.Version;
import aQute.lib.converter.Converter;
import aQute.lib.hex.Hex;
import aQute.lib.io.IO;
import aQute.libg.cryptography.SHA1;
import aQute.libg.glob.Glob;
import aQute.maven.api.Archive;
import aQute.maven.api.IMavenRepo;
import aQute.maven.api.IPom;
import aQute.maven.api.Release;
import aQute.maven.api.Revision;
import aQute.maven.provider.MavenRemoteRepository;
import aQute.maven.provider.MavenRepository;
import aQute.service.reporter.Reporter;

/**
 * This is the Bnd repository for Maven.
 */
@BndPlugin(name = "MavenBndRepository")
public class MavenBndRepository
		implements RepositoryPlugin, RegistryPlugin, Plugin, Closeable, Refreshable, InfoRepository, Actionable {

	private static final String		NONE	= "NONE";
	private Configuration			configuration;
	private Registry				registry;
	private File					localRepo;
	Reporter						reporter;
	IMavenRepo						storage;
	private boolean					inited;
	private boolean					ok		= true;
	IndexFile						index;
	private ScheduledFuture< ? >	indexPoller;
	private RepoActions				actions	= new RepoActions(this);
	private MavenRemoteRepository	snapshot;
	private MavenRemoteRepository	release;
	private String					name;

	@Override
	public PutResult put(InputStream stream, PutOptions options) throws Exception {

		init();
		File binaryFile = File.createTempFile("put", ".jar");
		File pomFile = File.createTempFile("pom", ".xml");
		try {
			PutResult result = new PutResult();
			if (options == null)
				options = new PutOptions();

			IO.copy(stream, binaryFile);

			if (options.digest != null) {
				byte[] digest = SHA1.digest(binaryFile).digest();
				if (!Arrays.equals(options.digest, digest))
					throw new IllegalArgumentException("The given sha-1 does not match the contents sha-1");
			}

			if (options.context == null)
				options.context = new Processor();

			ReleaseDTO instructions = getReleaseDTO(options.context);

			try (Jar binary = new Jar(binaryFile);) {
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
						throw new IllegalArgumentException(
								"No POM resource in META-INF/maven/... The Maven Bnd Repository requires this pom.");
					}
				}

				IO.copy(pomResource.openInputStream(), pomFile);
				IPom pom;
				try (FileInputStream fin = new FileInputStream(pomFile)) {
					pom = storage.getPom(fin);
				}
				Archive binaryArchive = pom.binaryArchive();

				try (Release releaser = storage.release(pom.getRevision());) {

					if (instructions.snapshot >= 0)
						releaser.setBuild(instructions.snapshot, "1");

					if (isLocal(instructions))
						releaser.setLocalOnly();

					releaser.add(pom.getRevision().pomArchive(), pomFile);
					releaser.add(binaryArchive, binaryFile);

					result.artifact = isLocal(instructions) ? storage.toLocalFile(binaryArchive).toURI()
							: storage.toRemoteURI(binaryArchive);

					if (!isLocal(instructions)) {

						try (Tool tool = new Tool(options.context, binary);) {

							if (instructions.javadoc != null) {
								if (!NONE.equals(instructions.javadoc.path)) {
									try (Jar jar = getJavadoc(tool, options.context, instructions.javadoc.path,
											instructions.javadoc.options,
											instructions.javadoc.packages == JavadocPackages.EXPORT);) {
										save(releaser, pom.getRevision(), jar, "javadoc");
									}
								}
							}

							if (instructions.sources != null) {
								if (!NONE.equals(instructions.javadoc.path)) {
									try (Jar jar = getSource(tool, options.context, instructions.javadoc.path);) {
										save(releaser, pom.getRevision(), jar, "sources");
									}
								}
							}
						}
					}
				}
				index.add(binaryArchive);
			}
			return result;
		} finally {
			binaryFile.delete();
			pomFile.delete();
		}

	}

	boolean isLocal(ReleaseDTO instructions) {
		return instructions.type == ReleaseType.LOCAL;
	}

	private Jar getSource(Tool tool, Processor context, String path) throws Exception {
		Jar jar = toJar(context, path);
		if (jar != null)
			return jar;

		return tool.doSource();
	}

	private Jar getJavadoc(Tool tool, Processor context, String path, Map<String,String> options, boolean exports)
			throws Exception {
		Jar jar = toJar(context, path);
		if (jar != null)
			return jar;

		return tool.doJavadoc(options, exports);
	}

	private Jar toJar(Processor context, String path) {
		if (path == null)
			return null;

		File f = context.getFile(path);
		if (f.exists())
			return new Jar(path);

		return null;
	}

	private void save(Release releaser, Revision revision, Jar jar, String classifier) throws Exception {
		String extension = IO.getExtension(jar.getName(), "jar");
		File tmp = File.createTempFile(classifier, extension);
		try {
			jar.write(tmp);
			releaser.add(revision.archive(extension, classifier), tmp);
		} finally {
			tmp.delete();
		}

	}

	/*
	 * Parse the -maven-release header.
	 */
	private ReleaseDTO getReleaseDTO(Processor context) {
		ReleaseDTO release = new ReleaseDTO();
		if (context == null)
			return release;

		Parameters p = new Parameters(context.getProperty(Constants.MAVEN_RELEASE));

		Attrs attrs = p.remove("remote");
		if (attrs != null) {

			release.type = ReleaseType.REMOTE;
			String s = attrs.get("snapshot");
			if (s != null)
				release.snapshot = Long.parseLong(s);

		} else {
			release.type = ReleaseType.LOCAL;
			attrs = p.remove("local");
			if (attrs == null)
				attrs = new Attrs();
		}

		Attrs javadoc = p.remove("javadoc");
		if (javadoc != null) {
			release.javadoc.path = javadoc.get("path");
			if (NONE.equals(release.javadoc.path))
				release.javadoc = null;
			else
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
			reporter.warning("The -maven-release instruction contains unrecognized options: ", p);
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

			promise.onResolve(getResolveable(file, promise, listeners));
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

			inited = true;
			name = configuration.name("Maven");

			localRepo = IO.getFile(configuration.local("~/.m2/repository"));

			HttpClient client = registry.getPlugin(HttpClient.class);
			Executor executor = registry.getPlugin(Executor.class);
			release = configuration.releaseUrl() != null
					? new MavenRemoteRepository(localRepo, client, clean(configuration.releaseUrl()), reporter) : null;
			snapshot = configuration.snapshotUrl() != null
					? new MavenRemoteRepository(localRepo, client, clean(configuration.snapshotUrl()), reporter) : null;
			storage = new MavenRepository(localRepo, getName(), release, snapshot, executor, reporter,
					getRefreshCallback());

			File base = IO.work;
			if (registry != null) {
				Workspace ws = registry.getPlugin(Workspace.class);
				if (ws != null)
					base = ws.getBase();
			}
			File indexFile = IO.getFile(base, configuration.index(name.toLowerCase() + ".mvn"));
			IndexFile ixf = new IndexFile(reporter, indexFile, storage);
			ixf.open();
			ixf.sync();
			this.index = ixf;
			startPoll(index);

			reporter.trace("Maven Bnd repository initing %s", this);
		} catch (Exception e) {
			reporter.exception(e, "Init for maven repo failed %s", configuration);
			throw new RuntimeException(e);
		}
	}

	private void startPoll(final IndexFile index) {
		final AtomicBoolean busy = new AtomicBoolean();
		indexPoller = Processor.getScheduledExecutor().scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				if (busy.getAndSet(true))
					return;

				try {
					poll();
				} catch (Exception e) {
					reporter.error("Error when polling index for %s for change", this);
				} finally {
					busy.set(false);
				}
			}

		}, 5000, 5000, TimeUnit.MILLISECONDS);
	}

	void poll() throws Exception {
		if (index.refresh()) {
			for (RepositoryListenerPlugin listener : registry.getPlugins(RepositoryListenerPlugin.class))
				listener.repositoryRefreshed(this);
		}
	}

	private String clean(String url) {
		if (url.endsWith("/"))
			return url;

		return url + "/";
	}

	@Override
	public String getLocation() {
		return configuration.releaseUrl() == null ? configuration.local("~/.m2/repository")
				: configuration.releaseUrl();
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
		if (storage != null)
			storage.close();
		if (indexPoller != null)
			indexPoller.cancel(true);
	}

	private Runnable getResolveable(final File file, final Promise<File> promise, final DownloadListener... listeners) {
		return new Runnable() {

			@Override
			public void run() {
				try {
					if (promise.getFailure() == null) {
						doSuccess(promise, listeners);
					} else {
						doFailure(file, promise, listeners);
					}
				} catch (Exception e) {
					// can't happen, we checked
					throw new RuntimeException(e);
				}
			}

			void doFailure(final File file, final Promise<File> promise, final DownloadListener... listeners) {
				for (DownloadListener dl : listeners)
					try {
						dl.failure(file, promise.getFailure().getMessage());
					} catch (Exception e) {
						reporter.exception(e, "Download listener failed in failure callback %s", dl);
					}
			}

			void doSuccess(final Promise<File> promise, final DownloadListener... listeners)
					throws InvocationTargetException, InterruptedException {
				File file = promise.getValue();
				for (DownloadListener dl : listeners)
					try {
						dl.success(file);
					} catch (Exception e) {
						reporter.exception(e, "Download listener failed in success callback %s ", dl);
					}
			}

		};
	}

	private Callable<Boolean> getRefreshCallback() {
		return new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				for (RepositoryListenerPlugin rp : registry.getPlugins(RepositoryListenerPlugin.class)) {
					try {
						rp.repositoryRefreshed(MavenBndRepository.this);
					} catch (Exception e) {
						reporter.exception(e, "Updating listener plugin %s", rp);
					}
				}
				return ok;
			}
		};
	}

	@Override
	public boolean refresh() throws Exception {
		return index.refresh();
	}

	@Override
	public File getRoot() throws Exception {
		return localRepo;
	}

	@Override
	public BundleDescriptor getDescriptor(String bsn, Version version) throws Exception {
		return index.getDescriptor(bsn, version);
	}

	@Override
	public String toString() {
		return "MavenBndRepository [localRepo=" + localRepo + ", storage=" + storage.getName() + ", inited=" + inited
				+ "]";
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
					f.format("Release %s  (%s)\n", release, getUser(release));
					f.format("Snapshot %s (%s)\n", snapshot, getUser(snapshot));
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

	private Object getUser(MavenRemoteRepository remote) {
		if (remote == null)
			return "";

		try {
			return remote.getUser();
		} catch (Exception e) {
			return "error: " + e.getMessage();
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
					return bd.version.getWithoutQualifier().toString();
				} else
					return bd.version.getWithoutQualifier().toString() + " ?";

			default :
		}
		return null;
	}

	private boolean isLocal(Archive archive) {
		return storage.toLocalFile(archive).isFile();
	}

	public boolean dropTarget(URI uri) throws Exception {
		if (uri.getHost().equals("search.maven.org") && uri.getPath().equals("/remotecontent")) {
			return doSearchMaven(uri);
		}

		if (uri.getPath().endsWith(".pom"))
			return addPom(uri);

		System.out.println("Data " + uri);
		return false;
	}

	private boolean addPom(URI uri) throws Exception {
		try {
			// http://search.maven.org/remotecontent?filepath=com/netflix/governator/governator-commons-cli/1.12.10/governator-commons-cli-1.12.10.pom
			IPom pom = storage.getPom(uri.toURL().openStream());
			Archive binaryArchive = pom.binaryArchive();
			index.add(binaryArchive);
			return true;
		} catch (FileNotFoundException e) {
			return false;
		} catch (Exception e) {
			e.printStackTrace();
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
}
