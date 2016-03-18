package aQute.maven.bnd;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import org.osgi.util.promise.Promise;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.build.ProjectBuilder;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.Registry;
import aQute.bnd.service.RegistryPlugin;
import aQute.bnd.service.RepositoryListenerPlugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.tool.Tool;
import aQute.bnd.version.MavenVersion;
import aQute.bnd.version.Version;
import aQute.lib.converter.Converter;
import aQute.lib.io.IO;
import aQute.libg.glob.Glob;
import aQute.maven.repo.api.Archive;
import aQute.maven.repo.api.IMavenRepo;
import aQute.maven.repo.api.POM;
import aQute.maven.repo.api.Program;
import aQute.maven.repo.api.Release;
import aQute.maven.repo.api.Revision;
import aQute.maven.repo.provider.MavenStorage;
import aQute.maven.repo.provider.RemoteRepo;
import aQute.service.reporter.Reporter;

/**
 * This is the Bnd repository for Maven.
 */
@BndPlugin(name = "MavenBndRepository")
public class MavenBndRepository implements RepositoryPlugin, RegistryPlugin, Plugin, Closeable {

	public enum Classifiers {
		SOURCE, JAVADOC;
	}

	public interface Configuration {

		/**
		 * The url to the remote release repository. If this is not specified,
		 * the repository is only local.
		 */
		String url();

		/**
		 * The url to the remote snapshot repository. If this is not specified,
		 * it falls back to the release repository or just local if this is also
		 * not specified.
		 */
		String snapshotUrl();

		/**
		 * The path to the local repository
		 */
		// default "~/.m2/repository"
		String local();

		/**
		 * The classifiers to release. These will be generated automatically if
		 * sufficient information is available.
		 */
		// default { Classifiers.BINARY}
		Classifiers[] generate();

		// default false
		boolean readOnly();

		String name(String deflt);
	}

	private Configuration	configuration;
	private Registry		registry;
	private File			localRepo;
	private Reporter		reporter;
	private IMavenRepo		storage;
	private boolean			inited;
	private boolean			ok	= true;
	private boolean			local;

	@Override
	public PutResult put(InputStream stream, PutOptions options) throws Exception {

		init();
		File out = File.createTempFile("put", ".jar");
		File pomFile = File.createTempFile("pom", ".jar");
		try {
			PutResult result = new PutResult();
			IO.copy(stream, out);
			try (Jar binary = new Jar(out);) {
				Resource pomResource = getPomResource(binary);
				if (pomResource == null)
					throw new IllegalArgumentException("No POM resource in META-INF/maven/...");

				IO.copy(pomResource.openInputStream(), pomFile);
				POM pom = new POM(pomFile);

				try (Release releaser = storage.release(pom.getRevision());) {

					if (configuration.url() == null)
						releaser.setLocalOnly();

					Archive binaryArchive = pom.binaryArchive();

					releaser.add(pom.getRevision().pomArchive(), pomFile);
					releaser.add(binaryArchive, out);

					result.artifact = local ? storage.toLocalFile(binaryArchive).toURI()
							: storage.toRemoteURI(binaryArchive);

					if (!local) {
						ReleaseDTO info = getReleaseDTO(options.context);

						try (Tool tool = new Tool(reporter);) {
							if (info.javadoc != null) {
								try (Jar jar = tool.doJavadoc(binary, resolve(options.context, info.javadoc.path),
										info.javadoc.options);) {
									save(releaser, pom.getRevision(), jar, "javadoc");
								}
							}

							if (info.source != null) {
								try (Jar jar = tool.doSource(binary, resolve(options.context, info.javadoc.path));) {
									save(releaser, pom.getRevision(), jar, "source");
								}
							}
						}
					}
				}
			}
			return result;
		} finally {
			out.delete();
			pomFile.delete();
		}

	}

	private void save(Release releaser, Revision revision, Jar jar, String classifier) throws Exception {
		String extension = IO.getExtension(jar.getName(), ".jar");
		File tmp = File.createTempFile(classifier, extension);
		try {
			jar.write(tmp);
			releaser.add(revision.archive(extension, classifier), tmp);
		} finally {
			tmp.delete();
		}

	}

	private File resolve(ProjectBuilder project, String path) {
		if (project == null)
			return IO.getFile(path);

		return project.getFile(path);
	}

	private ReleaseDTO getReleaseDTO(ProjectBuilder project) {
		// TODO Auto-generated method stub
		return null;
	}

	private Resource getPomResource(Jar jar) {
		for (Map.Entry<String,Resource> e : jar.getResources().entrySet()) {
			String path = e.getKey();

			if (path.startsWith("META-INF/maven/")) {
				return e.getValue();
			}
		}
		return null;
	}

	@Override
	public File get(String bsn, Version version, Map<String,String> properties, final DownloadListener... listeners)
			throws Exception {
		init();

		Archive archive = getArchive(bsn, version);
		if (archive != null) {
			final File file = storage.toLocalFile(archive);
			final Promise<File> promise = storage.get(archive);
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

		List<Program> localPrograms = storage.getLocalPrograms();
		List<String> bsns = new ArrayList<>();

		for (Program p : localPrograms) {
			String ga = p.getCoordinate();
			if (g == null || g.matcher(ga).matches())
				bsns.add(ga);
		}
		return bsns;
	}

	@Override
	public SortedSet<Version> versions(String bsn) throws Exception {
		init();
		TreeSet<Version> versions = new TreeSet<Version>();
		Program program = Program.valueOf(bsn);
		if (program == null)
			return versions;

		for (Revision revision : storage.getRevisions(program)) {
			MavenVersion v = revision.version;
			Version osgi = v.getOSGiVersion();
			versions.add(osgi);
		}

		return versions;
	}

	@Override
	public String getName() {
		init();
		return configuration.name(getLocation());
	}

	private synchronized void init() {
		try {
			if (inited)
				return;
			inited = true;

			localRepo = IO.getFile(configuration.local());

			HttpClient client = registry.getPlugin(HttpClient.class);
			Executor executor = registry.getPlugin(Executor.class);
			RemoteRepo remote = configuration.url() != null ? new RemoteRepo(client, configuration.url()) : null;
			storage = new MavenStorage(localRepo, getName(), remote, null, executor, reporter, getRefreshCallback());

		} catch (Exception e) {
			reporter.exception(e, "Init for maven repo failed %s", configuration);
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getLocation() {
		return configuration.url() == null ? configuration.local() : configuration.url();
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
		storage.close();
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

	private Archive getArchive(String bsn, Version version) throws Exception {
		String parts[] = bsn.split(":");
		if (parts.length == 2) {
			MavenVersion mavenVersion = new MavenVersion(version);
			bsn += ":" + mavenVersion;
		}

		return storage.getArchive(bsn);
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
}
