package aQute.bnd.jpm;

import static aQute.lib.io.IO.*;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.*;

import aQute.bnd.build.*;
import aQute.bnd.build.Container;
import aQute.bnd.osgi.*;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.osgi.resource.*;
import aQute.bnd.service.*;
import aQute.bnd.service.repository.*;
import aQute.bnd.version.*;
import aQute.jpm.facade.repo.*;
import aQute.jsonrpc.proxy.*;
import aQute.lib.collections.*;
import aQute.lib.converter.*;
import aQute.lib.hex.*;
import aQute.lib.io.*;
import aQute.lib.justif.*;
import aQute.lib.settings.*;
import aQute.libg.cryptography.*;
import aQute.libg.glob.*;
import aQute.libg.reporter.*;
import aQute.rest.urlclient.*;
import aQute.service.library.*;
import aQute.service.library.Library.Program;
import aQute.service.library.Library.Revision;
import aQute.service.library.Library.RevisionRef;
import aQute.service.reporter.*;

/**
 * A bnd repository based on the jpm4j server.
 */
public class Repository implements Plugin, RepositoryPlugin, Closeable, Refreshable, Actionable, RegistryPlugin, SearchableRepository, InfoRepository {
	public static final String						REPO_DEFAULT_URI			= "http://repo.jpm4j.org/";

	private static final PutOptions					DEFAULT_OPTIONS				= new PutOptions();
	private final String							DOWN_ARROW					= " \u21E9";
	protected final DownloadListener[]				EMPTY_LISTENER				= new DownloadListener[0];
	private Pattern									SHA							= Pattern.compile(
																						"([A-F0-9][a-fA-F0-9]){20,20}",
																						Pattern.CASE_INSENSITIVE);
	private final Justif							j							= new Justif(80, new int[] {
			20, 28, 36, 44
																				});
	private Settings								settings					= new Settings();
	private boolean									canwrite;
	final MultiMap<File,DownloadListener>	queues						= new MultiMap<File,RepositoryPlugin.DownloadListener>();

	private final Pattern							JPM_REVISION_URL_PATTERN	= Pattern
																						.compile("https?://.+#!?/p/([^/]+)/([^/]+)/([^/]*)/([^/]+)");
	private Options									options;
	Reporter								reporter					= new ReporterAdapter(System.out);

	/**
	 * Maintains the index of what we've downloaded so far.
	 */
	private File									indexFile;
	Index									index;
	private boolean									offline;
	private Registry								registry;
	StoredRevisionCache						cache;
	Set<File>								notfound					= new HashSet<File>();
	private Set<String>								notfoundref					= new HashSet<String>();
	final Semaphore							limitDownloads				= new Semaphore(12);
	private JpmRepo									library;

	private String									depositoryGroup;
	private String									depositoryName;
	private URLClient								urlc;
	private String									location;

	private URLClient								depository;

	private String									email;

	private String									name;

	URI										url;

	/**
	 * Reports downloads but does never block on them. This is a best effort, if
	 * it fails, we can still get them later.
	 */
	class LocalDownloadListener implements DownloadListener {

		@Override
		public void success(File file) throws Exception {
			reporter.trace("downloaded %s", file);
		}

		@Override
		public void failure(File file, String reason) throws Exception {
			reporter.trace("failed to downloaded %s", file);
		}

		@Override
		public boolean progress(File file, int percentage) throws Exception {
			reporter.trace("Downloadedin %s %s%", file, percentage);
			return true;
		}

	}

	interface Options {
		/**
		 * The URL to the remote repository. Default is http://repo.jpm4j.org
		 * 
		 * @return
		 */
		URI url();

		/**
		 * The group of a depository,optional.
		 * 
		 * @return
		 */
		String depository_group();

		/**
		 * The name of the depository
		 * 
		 * @return
		 */
		String depository_name();

		/**
		 * The email address of the user
		 * 
		 * @return
		 */
		String email();

		/**
		 * Where the index file is stored. The default should reside in the
		 * workspace and be part of the scm
		 * 
		 * @return
		 */
		String index();

		/**
		 * The cache location, default is ~/.bnd/cache. This file is relative
		 * from the users home directory if not absolute.
		 * 
		 * @return
		 */
		String location();

		/**
		 * Set the settings
		 */
		String settings();

		/**
		 * The name of the repo
		 * 
		 * @return
		 */
		String name();

		boolean trace();
	}

	/**
	 * Get a revision.
	 */
	@Override
	public File get(String bsn, Version version, Map<String,String> attrs, final DownloadListener... listeners)
			throws Exception {

		init();
		// Check if we're supposed to have this
		RevisionRef resource = index.getRevisionRef(bsn, version);
		if (resource == null)
			return null;
		else
			return getLocal(resource, attrs, listeners);
	}

	/**
	 * The index indicates we're allowed to have this one. So check if we have
	 * it cached or if we need to download it.
	 */
	private File getLocal(RevisionRef resource, Map<String,String> attrs, DownloadListener... downloadListeners)
			throws Exception {
		File sources = cache.getPath(resource.bsn, Index.toVersion(resource).toString(), resource.revision, true);
		if (sources.isFile()) {
			for (DownloadListener dl : downloadListeners) {
				dl.success(sources);
			}
			return sources;
		}
		File file = cache.getPath(resource.bsn, Index.toVersion(resource).toString(), resource.revision);
		scheduleDownload(file, resource.revision, resource.size, resource.urls, downloadListeners);
		return file;
	}

	/**
	 * Schedule a download, handling the listeners
	 * 
	 * @param url
	 */
	private void scheduleDownload(final File file, final byte[] sha, final long size, final Set<URI> urls,
			DownloadListener... listeners) throws Exception {

		synchronized (notfound) {
			if (notfound.contains(file)) {
				failure(listeners, file, "Not found");
				return;
			}
		}

		if (file.isFile()) {
			if (file.length() == size) {
				// Already exists, done
				success(listeners, file);
				reporter.trace("was in cache");
				return;
			}
			reporter.error("found file but of different length %s, will refetch", file);
		} else {
			reporter.trace("not in cache %s", file + " " + queues);
		}

		// Check if we need synchronous
		if (listeners.length == 0) {
			reporter.trace("in cache, no listeners");
			cache.download(file, urls, sha);
			return;
		}

		//
		// With download listeners we need to be careful to queue them
		// appropriately. Don't want to download n times because
		// requests arrive during downloads.
		//

		synchronized (queues) {
			List<DownloadListener> list = queues.get(file);
			boolean first = list == null || list.isEmpty();
			for (DownloadListener l : listeners) {
				queues.add(file, l);
			}

			if (!first) {
				// return, file is being downloaded by another and that
				// other will signal the download listener.
				reporter.trace("someone else is downloading our file " + queues.get(file));
				return;
			}
		}
		try {
			reporter.trace("starting thread for " + file);

			// Limit the total downloads going on at the same time
			limitDownloads.acquire();

			Thread t = new Thread("Downloading " + file) {
				public void run() {
					try {
						reporter.trace("downloading in background " + file);
						cache.download(file, urls, sha);
						success(queues.get(file).toArray(EMPTY_LISTENER), file);
					}
					catch (FileNotFoundException e) {
						synchronized (notfound) {
							reporter.error("Not found %s", e, file);
							notfound.add(file);
						}
						synchronized (queues) {
							failure(queues.get(file).toArray(EMPTY_LISTENER), file, e.toString());
						}
					}
					catch (Throwable e) {
						e.printStackTrace();
						reporter.error("failed to download %s: %s", e, file);
						synchronized (queues) {
							failure(queues.get(file).toArray(EMPTY_LISTENER), file, e.toString());
						}
					}
					finally {
						synchronized (queues) {
							queues.remove(file);
						}
						reporter.trace("downloaded " + file);

						// Allow other downloads to start
						limitDownloads.release();
					}
				}
			};
			t.start();
		}
		catch (Exception e) {
			// Is very unlikely to happen but we must ensure the
			// listeners are called and we're at the head of the queue
			reporter.error("Starting a download for %s failed %s", file, e);
			synchronized (queues) {
				failure(queues.get(file).toArray(EMPTY_LISTENER), file, e.toString());
				queues.remove(file);
			}
		}
	}

	/**
	 * API method
	 */

	@Override
	public boolean canWrite() {
		return canwrite;
	}

	/**
	 * Put an artifact in the repo
	 */
	@Override
	public PutResult put(InputStream in, PutOptions options) throws Exception {
		if (!canwrite)
			throw new UnsupportedOperationException(
					"This is not a writeable repo, s"
							+ "et depository.group, depository.name and properties and ensure the email property is in your global settings");

		assert in != null;
		assert depositoryGroup != null;
		assert depositoryName != null;

		init();

		if (options == null)
			options = DEFAULT_OPTIONS;

		reporter.trace("syncing");
		sync();

		File file = File.createTempFile("put", ".jar");
		file.deleteOnExit();
		try {
			reporter.trace("creating tmp copy");
			copy(in, file);
			if (depository == null) {
				URI url = library.depository(depositoryGroup, depositoryName);
				reporter.trace("send to url " + url);
				depository = new URLClient(url.toString());
				setCredentials(depository);
				reporter.trace("credentials " + depository);
			}

			byte[] digest = options.digest == null ? SHA1.digest(file).digest() : options.digest;
			String path = Hex.toHexString(digest);
			reporter.trace("putting " + path);
			Library.RevisionRef d = depository.put(path, file, Library.RevisionRef.class, null);
			if (d == null) {
				reporter.error("Cant deposit %s", file);
				return null;
			}
			if (!Arrays.equals(digest, d.revision))
				throw new Exception("Invalid digest");

			// Copy it to our cache
			cache.add(d, file);
			index.addRevision(d);
			index.save(); // Coordinator

			PutResult putr = new PutResult();
			putr.artifact = depository.getUri(path);
			putr.digest = digest;
			return putr;
		}
		catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		finally {
			file.delete();
		}
	}

	/**
	 * If we have no search or an empty search we list our index. Otherwise we
	 * query remotely.
	 */

	Pattern	COMMAND_P	= Pattern.compile("^([^/]*)/(!?[lmsprw])([^/]*)$");

	@Override
	public List<String> list(String query) throws Exception {
		init();
		Set<String> bsns = new HashSet<String>();
		if (query == null || query.trim().isEmpty())
			query = "*";
		else
			query = query.trim();

		Library.Phase phase = null;
		boolean negated = false;
		Matcher m = COMMAND_P.matcher(query);
		if (m.matches()) {
			query = m.group(1) + m.group(3);
			String cmd = m.group(2);
			if (cmd.startsWith("!")) {
				negated = true;
				cmd = cmd.substring(1);
			}
			char c = Character.toLowerCase(cmd.charAt(0));
			switch (c) {
				case 'l' :
					phase = Library.Phase.LOCKED;
					break;
				case 'p' :
					phase = Library.Phase.PENDING;
					break;
				case 's' :
					phase = Library.Phase.STAGING;
					break;
				case 'm' :
					phase = Library.Phase.MASTER;
					break;
				case 'r' :
					phase = Library.Phase.RETIRED;
					break;
				case 'w' :
					phase = Library.Phase.WITHDRAWN;
					break;
			}
			reporter.trace("Phase is " + c + " " + phase);
		}

		Glob glob = null;
		try {
			glob = new Glob(query);
		}
		catch (Exception e) {
			glob = new Glob("*");
		}

		bsn: for (String bsn : index.getBsns()) {
			if (glob.matcher(bsn).matches()) {
				if (phase != null) {
					boolean hasPhase = false;
					revision: for (Version version : index.getVersions(bsn)) {
						RevisionRef ref = index.getRevisionRef(bsn, version);
						if (ref.phase == phase) {
							hasPhase = true;
							break revision;
						}
					}
					if (hasPhase == negated)
						continue bsn;
				}
				bsns.add(bsn);
			}
		}

		List<String> result = new ArrayList<String>(bsns);
		Collections.sort(result);
		return result;
	}

	/**
	 * List the versions belonging to a bsn
	 */
	@Override
	public SortedSet<Version> versions(String bsn) throws Exception {
		init();
		SortedSet<Version> versions = index.getVersions(bsn);
		if (!versions.isEmpty() || !index.isLearning()) {
			return versions;
		}

		return versions;
	}

	/*
	 * Convert a baseline/qualifier to a version
	 */
	static Version toVersion(String baseline, String qualifier) {
		if (qualifier == null || qualifier.isEmpty())
			return new Version(baseline);
		else
			return new Version(baseline + "." + qualifier);
	}

	/*
	 * Return if bsn is a SHA
	 */
	private boolean isSha(String bsn) {
		return SHA.matcher(bsn).matches();
	}

	@Override
	public String getName() {
		return name == null ? "jpm4j" : name;
	}

	@Override
	public void setProperties(Map<String,String> map) {
		reporter.trace("CLs " + getClass().getClassLoader() + " " + URLClient.class.getClassLoader());
		try {
			options = Converter.cnv(Options.class, map);
			setOptions(options);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void setOptions(Options options) {
		try {
			if (options.location() == null)
				throw new IllegalArgumentException(
						"The location property must be set to the location of the cache directory, e.g. ~/.bnd/cache");

			this.name = options.name();
			if (options.settings() != null) {
				settings = new Settings(options.settings());
			}
			email = options.email();
			if (email == null)
				email = settings.getEmail();

			url = options.url();
			if (url == null)
				url = new URI(REPO_DEFAULT_URI);

			urlc = new URLClient(url.toString());
			if (email != null && !email.contains("anonymous"))
				setCredentials(urlc);
			urlc.setReporter(reporter);

			this.location = options.location();
			if (location == null)
				location = "~/.bnd/shacache";

			File cacheDir = IO.getFile(IO.home, location);
			cacheDir.mkdirs();
			if (!cacheDir.isDirectory())
				throw new IllegalArgumentException("Not able to create cache directory " + cacheDir);

			String indexPath = options.index();
			if (indexPath == null)
				throw new IllegalArgumentException("Index file not set (index) ");

			indexFile = IO.getFile(indexPath);
			if (indexFile.isDirectory())
				throw new IllegalArgumentException("Index file is a directory instead of a file "
						+ indexFile.getAbsolutePath());

			cache = new StoredRevisionCache(cacheDir, settings);

			library = JSONRPCProxy.createRPC(JpmRepo.class, urlc, "jpm");

			if (options.index() == null)
				throw new IllegalArgumentException("Index file not set");

			canwrite = false;
			if (options.depository_group() != null) {
				depositoryGroup = options.depository_group();
				depositoryName = options.depository_name();
				if (depositoryName == null)
					depositoryName = "home";
				canwrite = email != null;
			}

		}
		catch (Exception e) {
			if (reporter != null)
				reporter.exception(e, "Creating options");
			throw new RuntimeException(e);
		}
	}

	private void setCredentials(URLClient urlc) throws UnknownHostException, Exception {
		urlc.credentials(email, InetAddress.getLocalHost().getHostName(), settings.getPublicKey(),
				settings.getPrivateKey());
	}

	@Override
	public void setReporter(Reporter processor) {
		reporter = processor;
		if (index != null)
			index.setReporter(reporter);
		if (urlc != null)
			urlc.setReporter(processor);
	}

	@Override
	public boolean refresh() throws Exception {
		index = new Index(indexFile);
		cache.refresh();
		notfound.clear();
		notfoundref.clear();
		return true;
	}

	/**
	 * Return the actions for this repository
	 */
	@Override
	public Map<String,Runnable> actions(Object... target) throws Exception {
		init();

		boolean connected = isConnected();

		if (target == null)
			return null;

		if (target.length == 0)
			return getRepositoryActions();

		final String bsn = (String) target[0];
		Program careful = null;

		if (connected)
			try {
				careful = getProgram(bsn, true);
			}
			catch (Exception e) {
				reporter.error("Offline? %s", e);
			}

		final Program p = careful;
		if (target.length == 1)
			return getProgramActions(bsn, p);

		if (target.length >= 2) {
			final Version version = (Version) target[1];

			return getRevisionActions(p, bsn, version);
		}
		return null;
	}

	/**
	 * @param p
	 * @param bsn
	 * @param version
	 * @return
	 * @throws Exception
	 */
	static Pattern	JAR_FILE_P	= Pattern.compile("(https?:.+)(\\.jar)");

	private Map<String,Runnable> getRevisionActions(final Program program, final String bsn, final Version version)
			throws Exception {
		final Library.RevisionRef resource = index.getRevisionRef(bsn, version);
		Map<String,Runnable> map = new LinkedHashMap<String,Runnable>();
		map.put("Inspect Revision", new Runnable() {
			public void run() {
				open(url + "#!/p/sha/" + Hex.toHexString(resource.revision) + "//0.0.0");
			}
		});
		map.put("Copy reference", new Runnable() {

			@Override
			public void run() {
				toClipboard(bsn, version);
			}

		});

		Runnable doUpdate = getUpdateAction(program, resource);

		if (doUpdate != null) {
			map.put("Update to " + doUpdate, doUpdate);
		} else {
			map.put("-Update", null);
		}
		map.put("Delete", new Runnable() {
			public void run() {
				try {
					delete(bsn, version, true);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}

		});
		if (isConnected()) {
			final File sourceFile = cache.getPath(bsn, version.toString(), resource.revision, true);
			Runnable run = null;

			if (!sourceFile.isFile()) {
				URL sourceURI = null;

				for (URI uri : resource.urls) {
					try {
						Matcher m = JAR_FILE_P.matcher(uri.toString());
						if (m.matches()) {
							String stem = m.group(1);
							URL src = new URL(stem + "-sources.jar");
							HttpURLConnection conn = (HttpURLConnection) src.openConnection();
							conn.setRequestMethod("HEAD");
							if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
								sourceURI = src;
								continue;
							}
						}
					}
					catch (Exception e) {
						// ignore
					}
				}
				if (sourceURI != null) {
					run = createAddSourceAction(bsn, version, resource, sourceFile, sourceURI);
				}
			} else
				reporter.trace("sources in %s", sourceFile);

			if (run != null)
				map.put("Add Sources", run);
			else
				map.put("-Add Sources", null);
		}
		if (cache.hasSources(bsn, version.toString(), resource.revision)) {
			map.put("Remove Sources", new Runnable() {

				@Override
				public void run() {
					cache.removeSources(bsn, version.toString(), resource.revision);
				}

			});
		}

		return map;
	}

	/**
	 * @param bsn
	 * @param version
	 * @param resource
	 * @param withSources
	 * @param src
	 * @return
	 */
	protected Runnable createAddSourceAction(final String bsn, final Version version,
			final Library.RevisionRef resource, final File withSources, final URL src) {
		Runnable run;
		run = new Runnable() {
			public void run() {

				try {

					// Sync downloads so that we do not assume the
					// binary is already there ... so call without
					// listeners.
					get(bsn, version, null);

					File file = cache.getPath(bsn, version.toString(), resource.revision);
					Jar binary = new Jar(file);
					try {
						Jar sources = new Jar(src.getFile(), src.openStream());
						binary.setDoNotTouchManifest();
						try {
							binary.addAll(sources, null, "OSGI-OPT/src");
							binary.write(withSources);
						}
						finally {
							sources.close();
						}
					}
					finally {
						binary.close();
					}
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		};
		return run;
	}

	/**
	 * @param bsn
	 * @param version
	 * @param resource
	 * @param withSources
	 * @param src
	 * @return
	 */
	protected Runnable createRemoveSourceAction(final String bsn, final Version version,
			final Library.RevisionRef resource, final File withSources, final URL src) {
		Runnable run;
		run = new Runnable() {
			public void run() {

				try {

					// Sync downloads so that we do not assume the
					// binary is already there ... so call without
					// listeners.
					get(bsn, version, null);

					File file = cache.getPath(bsn, version.toString(), resource.revision);
					Jar binary = new Jar(file);
					try {
						Jar sources = new Jar(src.getFile(), src.openStream());
						try {
							binary.addAll(sources, null, "OSGI-OPT/src");
							binary.write(withSources);
						}
						finally {
							sources.close();
						}
					}
					finally {
						binary.close();
					}
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		};
		return run;
	}

	/**
	 * @param bsn
	 * @param p
	 * @return
	 * @throws Exception
	 */
	private Map<String,Runnable> getProgramActions(final String bsn, final Program p) throws Exception {
		Map<String,Runnable> map = new LinkedHashMap<String,Runnable>();

		if (p != null) {
			map.put("Inspect Program", new Runnable() {
				public void run() {
					open(url + "#!/p/osgi/" + bsn);
				}
			});

			final SortedSet<Version> versions = index.getVersions(bsn);
			if (versions.isEmpty())
				map.put("-Copy reference", null);
			else
				map.put("Copy reference", new Runnable() {

					@Override
					public void run() {
						toClipboard(bsn, versions.first());
					}

				});

			RevisionRef ref = p.revisions.get(0);
			Version latest = toVersion(ref.baseline, ref.qualifier);
			for (Version v : index.getVersions(bsn)) {
				if (v.equals(latest)) {
					latest = null;
					break;
				}
			}
			final Version l = latest;
			String title = "Get Latest";
			if (latest == null)
				title = "-" + title;
			else
				title += " " + l + ref.phase;

			map.put(title, new Runnable() {
				public void run() {
					try {
						add(bsn, l);
					}
					catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			});

			Runnable updateAction = getUpdateAction(p, bsn);
			if (updateAction != null)
				map.put("Update " + updateAction, updateAction);
			else
				map.put("-Update", null);
		} else {
			map.put("-Update (offline)", null);
		}
		map.put("Delete", new Runnable() {
			public void run() {
				try {
					delete(bsn);
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});
		return map;
	}

	/**
	 * @return
	 * @throws Exception
	 */
	private Map<String,Runnable> getRepositoryActions() throws Exception {
		Map<String,Runnable> map = new LinkedHashMap<String,Runnable>();

		map.put("Inspect", new Runnable() {
			public void run() {
				try {
					byte[] revisions = sync();
					open(url + "#!/revisions/" + Hex.toHexString(revisions));
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}

		});

		map.put("Delete Cache", new Runnable() {

			@Override
			public void run() {
				try {
					cache.deleteAll();
				}
				catch (Exception e) {
					reporter.error("Deleting cache %s", e);
				}
			}
		});
		map.put("Refresh", new Runnable() {

			@Override
			public void run() {
				try {
					refresh();
				}
				catch (Exception e) {
					reporter.error("Refreshing %s", e);
				}
			}
		});
		map.put("Update All", new Runnable() {

			@Override
			public void run() {
				try {
					updateAll();
				}
				catch (Exception e) {
					reporter.error("Update all %s", e);
				}
			}
		});

		map.put("Download All", new Runnable() {

			@Override
			public void run() {
				try {
					DownloadListener dl = new DownloadListener() {

						@Override
						public void success(File file) throws Exception {
							reporter.trace("downloaded %s", file);
						}

						@Override
						public void failure(File file, String reason) throws Exception {
							reporter.trace("failed to download %s becasue %s", file, reason);
						}

						@Override
						public boolean progress(File file, int percentage) throws Exception {
							reporter.progress(((float) percentage) / 100, "downloading %s", file);
							return true;
						}

					};
					for (String bsn : list(null)) {
						for (Version v : versions(bsn)) {
							get(bsn, v, null, dl);
						}
					}
				}
				catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		map.put("Remove unused/Add missing", new Runnable() {

			@Override
			public void run() {
				try {
					cleanUp();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}

		});

		String title = "Learning{Unknown resources are an error, select to learn}";
		if (index.isLearning()) {
			title = "!Learning{Will attempt to fetch unknown resources, select to make this an error}";
		}
		map.put(title, new Runnable() {

			@Override
			public void run() {
				try {
					index.setLearning(!index.isLearning());
					index.save();
				}
				catch (Exception e) {
					reporter.error("Learning %s", e);
				}
			}
		});
		title = "Recurse{Do not fetch dependencies automatically}";
		if (index.isRecurse()) {
			title = "!Recurse{Fetch dependencies automatically}";
		}
		map.put(title, new Runnable() {

			@Override
			public void run() {
				try {
					index.setRecurse(!index.isRecurse());
					index.save();
				}
				catch (Exception e) {
					reporter.error("Learning %s", e);
				}
			}
		});
		return map;
	}

	@Override
	public String tooltip(Object... target) throws Exception {
		init();
		if (target == null || target.length == 0)
			return repositoryTooltip();

		if (target.length == 1)
			return programTooltip((String) target[0]);

		if (target.length == 2)
			return revisionTooltip((String) target[0], (Version) target[1]);

		return "Hmm, have no idea on what object you want a tooltip ...";
	}

	private String repositoryTooltip() throws Exception {
		Formatter f = new Formatter();
		try {
			f.format("%s\n", this);

			if (depositoryGroup != null && depositoryName != null) {
				f.format("\n[Depository]\n");
				f.format("Group: %s\n", depositoryGroup);
				f.format("Depository: %s\n", depositoryName);
				f.format("Email: %s\n", email);
				f.format("Writable: %s %s\n", canwrite, (email == null ? "(no email set, see 'bnd settings email=...')"
						: ""));
				f.format("Public key: %s…\n", Hex.toHexString(settings.getPublicKey()).substring(0, 16));
			}

			f.format("\n[Files]\nCache location %s\n", options.location());
			f.format("Index file     %s\n", options.index());
			f.format("Number of bsns %s\n", index.getBsns().size());
			f.format("Number of revs %s\n", index.getRevisionRefs().size());
			f.format("Dirty          %s\n", index.isDirty());

			return f.toString();
		}
		finally {
			f.close();
		}
	}

	private String programTooltip(String bsn) throws Exception {
		Program p = getProgram(bsn, false);
		if (p != null) {
			Formatter sb = new Formatter();
			try {
				if (p.wiki != null && p.wiki.text != null)
					sb.format("%s\n", p.wiki.text.replaceAll("#\\s?", ""));
				else if (p.last.description != null)
					sb.format("%s\n", p.last.description);
				else
					sb.format("No description\n");
				j.wrap((StringBuilder) sb.out());
				return sb.toString();
			}
			finally {
				sb.close();
			}
		}
		return null;
	}

	private String revisionTooltip(String bsn, Version version) throws Exception {
		RevisionRef r = getRevisionRef(bsn, version);

		if (r == null)
			return null;

		Formatter sb = new Formatter();
		try {
			sb.format("[%s:%s", r.groupId, r.artifactId);

			if (r.classifier != null) {
				sb.format(":%s", r.classifier);
			}
			sb.format("@%s] %s\n\n", r.version, r.phase);

			if (r.releaseSummary != null)
				sb.format("%s\n\n", r.releaseSummary);

			if (r.description != null)
				sb.format("%s\n\n", r.description.replaceAll("#\\s*", ""));
			
			sb.format("Size: %s\n", size(r.size, 0));
			sb.format("SHA-1: %s\n", Hex.toHexString(r.revision));
			sb.format("Age: %s\n", age(r.created));
			sb.format("URL: %s\n", r.urls);

			File f = cache.getPath(bsn, version.toString(), r.revision);
			if (f.isFile() && f.length() == r.size)
				sb.format("Cached %s\n", f);
			else
				sb.format("Not downloaded\n");

			Program p = getProgram(bsn, false);
			if (p != null) {

				Runnable update = getUpdateAction(p, r);
				if (update != null) {
					sb.format(DOWN_ARROW + " This version can be updated to " + update);
				}
			}

			File sources = cache.getPath(bsn, version.toString(), r.revision, true);
			if (sources.isFile())
				sb.format("Has sources: %s\n", sources.getAbsolutePath());
			else
				sb.format("No sources");

			j.wrap((StringBuilder) sb.out());
			return sb.toString();
		}
		finally {
			sb.close();
		}
	}

	private List<RevisionRef> getRevisionRefs(String bsn) throws Exception {
		String classifier = null;
		String parts[] = bsn.split("__");
		if (parts.length == 3) {
			bsn = parts[0] + "__" + parts[1];
			classifier = parts[2];
		}

		Program program = getProgram(bsn, false);
		if (program != null) {
			List<RevisionRef> refs = new ArrayList<Library.RevisionRef>();
			for (RevisionRef r : program.revisions) {
				if (eq(classifier, r.classifier))
					refs.add(r);
			}
			return refs;
		}
		return Collections.emptyList();
	}

	/**
	 * Find a revisionref for a bsn/version
	 * 
	 * @param bsn
	 * @param version
	 * @return
	 * @throws Exception
	 */
	private RevisionRef getRevisionRef(String bsn, Version version) throws Exception {
		// Handle when we have a sha reference

		String id = bsn + "-" + version;
		if (notfoundref.contains(id))
			return null;

		if (isSha(bsn) && version.equals(Version.LOWEST)) {
			Revision r = getRevision(new Coordinate(bsn));
			if (r == null)
				return null;

			return new RevisionRef(r);
		}
		reporter.trace("Looking for %s-%s", bsn, version);
		for (RevisionRef r : getRevisionRefs(bsn)) {
			Version v = toVersion(r.baseline, r.qualifier);
			if (v.equals(version))
				return r;
		}
		notfoundref.add(id);
		return null;
	}

	private boolean eq(String a, String b) {
		if (a == null)
			a = "";
		if (b == null)
			b = "";
		return a.equals(b);
	}

	private String age(long created) {
		if (created == 0)
			return "unknown";

		long diff = (System.currentTimeMillis() - created) / (1000 * 60 * 60);
		if (diff < 48)
			return diff + " hours";

		diff /= 24;
		if (diff < 14)
			return diff + " days";

		diff /= 7;
		if (diff < 8)
			return diff + " weeks";

		diff /= 4;
		if (diff < 24)
			return diff + " months";

		diff /= 12;
		return diff + " years";
	}

	String[]	sizes	= {
			"bytes", "Kb", "Mb", "Gb", "Tb", "Pb", "Showing off?"
						};

	private String size(long size, int power) {
		if (power >= sizes.length)
			return size + " Pb";

		if (size < 1000)
			return size + sizes[power];
		return size(size / 1000, power + 1);
	}

	/**
	 * Update all bsns
	 * 
	 * @throws Exception
	 */

	void updateAll() throws Exception {
		for (String bsn : index.getBsns()) {
			update(bsn);
		}
	}

	/**
	 * Update all baselines for a bsn
	 * 
	 * @param bsn
	 * @throws Exception
	 */
	void update(String bsn) throws Exception {
		Program program = getProgram(bsn, false);
		Runnable updateAction = getUpdateAction(program, bsn);
		if (updateAction == null)
			return;

		reporter.trace("update bsn %s", updateAction);
		updateAction.run();
	}

	/**
	 * Update a bsn
	 * 
	 * @throws Exception
	 */
	Runnable getUpdateAction(Program program, String bsn) throws Exception {
		final Set<Runnable> update = new TreeSet<Runnable>();
		for (Version v : index.getVersions(bsn)) {
			RevisionRef resource = index.getRevisionRef(bsn, v);
			Runnable updateAction = getUpdateAction(program, resource);
			if (updateAction != null)
				update.add(updateAction);
		}
		if (update.isEmpty())
			return null;

		return new Runnable() {

			@Override
			public void run() {
				for (Runnable r : update) {
					r.run();
				}
			}

			@Override
			public String toString() {
				return update.toString();
			}
		};
	}

	/**
	 * Find a RevisionRef from the Program. We are looking for a version with
	 * the same baseline but a higher qualifier or different phase.
	 * 
	 * @param p
	 * @param currentVersion
	 * @return
	 * @throws Exception
	 */

	private Runnable getUpdateAction(Program program, final RevisionRef current) throws Exception {
		RevisionRef candidateRef = null;
		Version candidate = toVersion(current.baseline, current.qualifier);

		for (RevisionRef r : program.revisions) {
			Version refVersion = toVersion(r.baseline, r.qualifier);
			if (eq(r.classifier, current.classifier)) {
				if (refVersion.compareTo(candidate) >= 0) {
					candidate = refVersion;
					candidateRef = r;
				}
			}
		}
		if (candidateRef == null)
			//
			// We're not present anymore, should never happen ...
			//
			return new Runnable() {

				@Override
				public void run() {
					try {
						index.delete(current.bsn, toVersion(current.baseline, current.qualifier));
					}
					catch (Exception e) {
						throw new RuntimeException(e);
					}
				}

				public String toString() {
					return "[delete]";
				}
			};

		//
		// Check if we are not same revision
		//
		if (!candidateRef.version.equals(current.version)) {
			final RevisionRef toAdd = candidateRef;
			return new Runnable() {
				//
				// Replace the current version
				//
				public void run() {
					try {
						index.delete(current.bsn, toVersion(current.baseline, current.qualifier));
						index.addRevision(toAdd);
						index.save();
					}
					catch (Exception e) {
						throw new RuntimeException(e);
					}
				}

				public String toString() {
					return toAdd.version;
				}
			};
		}

		//
		// So now we are the same, check if the phase has changed
		//
		if (candidateRef.phase != current.phase) {
			final RevisionRef toChange = candidateRef;
			return new Runnable() {

				@Override
				public void run() {
					try {
						index.delete(current.bsn, toVersion(current.baseline, current.qualifier));
						index.addRevision(toChange);
						index.save();
					}
					catch (Exception e) {
						throw new RuntimeException(e);
					}
				}

				public String toString() {
					return "-> " + toChange.phase;
				}
			};
		}
		return null;
	}

	public void setIndex(File index) {
		indexFile = index;
	}

	void success(DownloadListener[] downloadListeners, File f) {
		for (DownloadListener l : downloadListeners) {
			try {
				l.success(f);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	void failure(DownloadListener[] listeners, File f, String reason) {
		for (DownloadListener l : listeners) {
			try {
				l.failure(f, reason);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}


	@Override
	public String title(Object... target) throws Exception {
		init();
		if (target == null || target.length == 0)
			return getName();

		if (target.length == 1 && target[0] instanceof String) {
			String bsn = (String) target[0];
			String title = bsn;
			return title;
		}

		if (target.length == 2 && target[0] instanceof String && target[1] instanceof Version) {
			String bsn = (String) target[0];
			Version version = (Version) target[1];

			Library.RevisionRef resource = index.getRevisionRef(bsn, version);
			if (resource == null)
				return "[deleted " + version + "]";

			String title = getPhase(resource.phase.toString()) + " " + version.toString();

			File path = cache.getPath(bsn, version.toString(), resource.revision);
			if (path.isFile() && path.length() == resource.size) {
				title += DOWN_ARROW;
			}
			if (cache.getPath(bsn, version.toString(), resource.revision, true).isFile())
				title += "+";
			return title;
		}

		return null;
	}

	// Temp until we fixed bnd in bndtools
	enum Phase {
		STAGING(false, false, false, "[s]"), LOCKED(true, false, false, "[l]"), MASTER(true, true, true, "[m]"), RETIRED(
				true, false, true, "[r]"), WITHDRAWN(true, false, true, "[x]"), UNKNOWN(true, false, false, "[?]");

		boolean			locked;
		boolean			listable;
		boolean			permanent;
		final String	symbol;

		private Phase(boolean locked, boolean listable, boolean permanent, String symbol) {
			this.locked = locked;
			this.listable = listable;
			this.permanent = permanent;
			this.symbol = symbol;
		}

		public boolean isLocked() {
			return locked;
		}

		public boolean isListable() {
			return listable;
		}

		public boolean isPermanent() {
			return permanent;
		}

		public String getSymbol() {
			return symbol;
		}
	}

	private String getPhase(String phase) {
		try {
			return Phase.valueOf(phase).getSymbol();
		}
		catch (Exception e) {
			return "?";
		}
	}

	@Override
	public File getRoot() {
		return cache.getRoot();
	}

	@Override
	public void close() throws IOException {}

	@Override
	public String getLocation() {
		return options.location();
	}

	protected void fireBundleAdded(File file) throws IOException {
		if (registry == null)
			return;
		List<RepositoryListenerPlugin> listeners = registry.getPlugins(RepositoryListenerPlugin.class);
		if (listeners.isEmpty())
			return;

		Jar jar = new Jar(file);
		try {
			for (RepositoryListenerPlugin listener : listeners) {
				try {
					listener.bundleAdded(this, jar, file);
				}
				catch (Exception e) {
					reporter.error("Repository listener threw an unexpected exception: %s", e, e);
				}
				finally {}
			}
		}
		finally {
			jar.close();
		}
	}

	@Override
	public void setRegistry(Registry registry) {
		this.registry = registry;
	}

	private void init() {
		if (index == null) {
			reporter.trace("init " + indexFile);
			index = new Index(indexFile);
			index.setReporter(reporter);
		}
	}

	public void add(String bsn, Version version) throws Exception {
		reporter.trace("Add %s %s", bsn, version);
		RevisionRef ref = getRevisionRef(bsn, version);
		add(ref);
	}

	void add(RevisionRef ref) throws Exception {
		// Cleanup existing versions
		// We remove everything between [mask(v), v)

		Version newVersion = toVersion(ref.baseline, ref.qualifier);
		reporter.trace("New version " + ref.bsn + " " + newVersion);
		Version newMask = mask(newVersion);
		List<Version> toBeDeleted = new ArrayList<Version>();

		for (Version existingVersion : index.getVersions(ref.bsn)) {
			Version existingMask = mask(existingVersion);
			if (newMask.equals(existingMask)) {
				reporter.trace("delete %s-%s", ref.bsn, existingVersion);
				toBeDeleted.add(existingVersion);
			}
		}

		for (Version v : toBeDeleted)
			index.delete(ref.bsn, v);

		reporter.trace("add %s-%s", ref.bsn, newVersion);
		index.addRevision(ref);

		getLocal(ref, null, new LocalDownloadListener());

		if (index.isRecurse()) {
			Iterable<RevisionRef> refs = getClosure(ref);
			for (RevisionRef r : refs) {
				index.addRevision(r);
				getLocal(ref, null, new LocalDownloadListener());
			}
		}
		index.save(indexFile);
	}

	/**
	 * @param ref
	 * @return
	 * @throws Exception
	 */
	private Iterable<RevisionRef> getClosure(RevisionRef ref) throws Exception {
		return library.getClosure(ref.revision, false);
	}

	public void delete(String bsn, Version version, boolean immediate) throws Exception {
		reporter.trace("Delete %s %s", bsn, version);

		Library.RevisionRef resource = index.getRevisionRef(bsn, version);
		if (resource != null) {
			boolean removed = index.delete(bsn, version);
			reporter.trace("Was present " + removed);
			index.save();
		} else
			reporter.trace("No such resource");
	}

	public void delete(String bsn) throws Exception {
		reporter.trace("Delete %s", bsn);
		Set<Version> set = new HashSet<Version>(index.getVersions(bsn));
		reporter.trace("Versions %s", set);
		for (Version version : set) {
			delete(bsn, version, true);
		}
	}

	public boolean dropTarget(URI uri) throws Exception {
		try {
			init();
			reporter.trace("dropTarget " + uri);
			Matcher m = JPM_REVISION_URL_PATTERN.matcher(uri.toString());
			if (!m.matches()) {
				reporter.trace("not a proper url to drop " + uri);
				return false;
			}

			Revision revision = getRevision(new Coordinate(m.group(1), m.group(2), m.group(3), m.group(4)));
			if (revision == null) {
				reporter.error("no revision found for %s", uri);
				return false;
			}

			Library.RevisionRef resource = index.getRevisionRef(revision._id);
			if (resource != null) {
				reporter.trace("resource already loaded " + uri);
				return true;
			}

			RevisionRef ref = new RevisionRef(revision);
			reporter.trace("adding revision " + ref);
			add(ref);
			return true;
		}
		catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	/*
	 * A utility to open a URL on different OS's browsers
	 * @param url the url to open
	 * @throws IOException
	 */
	void open(String url) {
		try {
			try {
				Desktop desktop = Desktop.getDesktop();
				desktop.browse(new URI(url));
				return;
			}
			catch (Throwable e) {

			}
			String os = System.getProperty("os.name").toLowerCase();
			Runtime rt = Runtime.getRuntime();

			if (os.indexOf("mac") >= 0 || os.indexOf("darwin") >= 0) {
				rt.exec("open " + url);
			} else if (os.indexOf("win") >= 0) {
				// this doesn't support showing urls in the form of
				// "page.html#nameLink"
				rt.exec("rundll32 url.dll,FileProtocolHandler " + url);

			} else if (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0) {

				// Do a best guess on unix until we get a platform independent
				// way
				// Build a list of browsers to try, in this order.
				String[] browsers = {
						"epiphany", "firefox", "mozilla", "konqueror", "netscape", "opera", "links", "lynx"
				};

				// Build a command string which looks like
				// "browser1 "url" || browser2 "url" ||..."
				StringBuffer cmd = new StringBuffer();
				for (int i = 0; i < browsers.length; i++)
					cmd.append((i == 0 ? "" : " || ") + browsers[i] + " \"" + url + "\" ");

				rt.exec(new String[] {
						"sh", "-c", cmd.toString()
				});

			} else
				reporter.trace("Open " + url);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Answer the resource descriptors from a URL
	 */
	// @Override
	public Set<ResourceDescriptor> getResources(URI url, boolean includeDependencies) throws Exception {
		try {
			Matcher m = JPM_REVISION_URL_PATTERN.matcher(url.toString());
			if (!m.matches()) {
				return null;
			}

			Set<ResourceDescriptor> resources = new HashSet<ResourceDescriptor>();
			Revision revision = getRevision(new Coordinate(m.group(1), m.group(2), m.group(3), m.group(4)));

			if (revision != null) {
				ResourceDescriptor rd = createResourceDescriptor(new RevisionRef(revision));
				resources.add(rd);
				if (includeDependencies) {
					for (RevisionRef dependency : library.getClosure(revision._id, false)) {
						ResourceDescriptor dep = createResourceDescriptor(dependency);
						dep.dependency = true;
						resources.add(dep);
					}
				}
			}

			return resources;
		}
		catch (Exception e) {
			e.printStackTrace();
			return Collections.emptySet();
		}
	}

	private ResourceDescriptor createResourceDescriptor(RevisionRef ref) throws Exception {
		ResourceDescriptorImpl rd = new ResourceDescriptorImpl(ref);
		rd.bsn = ref.bsn;
		rd.version = toVersion(ref.baseline, ref.qualifier);
		rd.description = ref.description;
		rd.id = ref.revision;
		rd.included = getIndex().getRevisionRef(rd.id) != null;
		rd.phase = toPhase(ref.phase);
		return rd;
	}

	private Index getIndex() {
		init();
		return index;
	}

	private aQute.bnd.service.repository.Phase toPhase(aQute.service.library.Library.Phase phase) {
		switch (phase) {
			case STAGING :
				return aQute.bnd.service.repository.Phase.STAGING;
			case LOCKED :
				return aQute.bnd.service.repository.Phase.LOCKED;
			case MASTER :
				return aQute.bnd.service.repository.Phase.MASTER;
			case RETIRED :
				return aQute.bnd.service.repository.Phase.RETIRED;
			case WITHDRAWN :
				return aQute.bnd.service.repository.Phase.WITHDRAWN;
			default :
				return null;
		}
	}

	// @Override
	public Set<ResourceDescriptor> query(String query) throws Exception {
		Set<ResourceDescriptor> resources = new HashSet<ResourceDescriptor>();
		RevisionRef master = null;
		RevisionRef staging = null;

		for (Program p : library.getQueryPrograms(query, 0, 100)) {
			for (RevisionRef ref : p.revisions) {
				if (master == null && ref.phase == Library.Phase.MASTER) {
					master = ref;
				} else if (staging != null && ref.phase == Library.Phase.STAGING) {
					staging = ref;
				}
			}
			if (master != null)
				resources.add(createResourceDescriptor(master));
			if (staging != null)
				resources.add(createResourceDescriptor(staging));
		}
		return resources;
	}

	// @Override
	public boolean addResource(ResourceDescriptor resource) throws Exception {
		if (resource instanceof ResourceDescriptorImpl) {
			RevisionRef ref = ((ResourceDescriptorImpl) resource).revision;
			if (index.addRevision(ref)) {
				index.save();
				return true;
			}
		}
		return false;
	}

	// @Override
	public Set<ResourceDescriptor> findResources(org.osgi.resource.Requirement requirement, boolean includeDependencies)
			throws Exception {
		FilterParser fp = new FilterParser();
		aQute.bnd.osgi.resource.FilterParser.Expression expression = fp.parse(requirement.getDirectives().get("filter"));
		String query = expression.query();
		
		if ( query == null) {
			return Collections.emptySet();
		}
		
		return query(query);
	}

	/**
	 * Check if there is at least one network interface up and running so we
	 * have internet access.
	 */
	private boolean isConnected() throws SocketException {
		if (offline)
			return false;

		try {
			for (Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces(); e.hasMoreElements();) {
				NetworkInterface interf = e.nextElement();
				if (!interf.isLoopback() && interf.isUp())
					return true;
			}
		}
		catch (SocketException e) {
			// ignore, we assume we're offline
		}
		return false;
	}

	/**
	 * @param bsn
	 * @return
	 * @throws Exception
	 */
	private Program getProgram(final String bsn, boolean force) throws Exception {
		Program p = cache.getProgram(bsn);
		if (p == null || force) {
			p = library.getProgram(Library.OSGI_GROUP, bsn);
			if (p != null)
				cache.putProgram(bsn, p);
		}
		return p;
	}

	/**
	 * @param sha
	 * @return
	 * @throws Exception
	 */
	private Revision getRevision(Coordinate c) throws Exception {
		return library.getRevisionByCoordinate(c);
	}

	public byte[] getDigest() throws Exception {
		init();
		return index.getRevisions()._id;
	}

	/**
	 * Ensure that the revisions is updated
	 * 
	 * @throws Exception
	 */
	byte[] sync() throws Exception {
		Revisions revisions = index.getRevisions();

		if (!index.isSynced()) {
			reporter.trace("Syncing repo indexes");
			library.createRevisions(revisions);
			index.setSynced(revisions._id);
		}
		return revisions._id;
	}

	/**
	 * Compare a list of versions against the available versions and return the
	 * desired list. This will remove all staged version that are 'below' a
	 * master.
	 */

	public SortedSet<Version> update(SortedSet<Version> input, Program p) throws Exception {

		Map<Version,Version> mapped = new HashMap<Version,Version>();
		for (RevisionRef ref : p.revisions) {
			Version a = toVersion(ref.baseline, ref.qualifier);
			Version mask = mask(a);
			Version highest = mapped.get(mask);
			if (highest == null || a.compareTo(highest) > 0 || ref.phase == Library.Phase.MASTER)
				mapped.put(mask, a);
		}

		HashSet<Version> output = new HashSet<Version>();
		for (Version i : input) {
			Version mask = mask(i);
			Version found = mapped.get(mask);
			if (found != null)
				output.add(found);
			else
				reporter.error("[update] Missing version %s for bsn %s", mask, p.last.bsn);
		}
		return new SortedList<Version>(output);
	}

	private static Version mask(Version in) {
		return new Version(in.getMajor(), in.getMinor());
	}

	/**
	 * Remove any unused entries in this repository
	 * 
	 * @throws Exception
	 */
	void cleanUp() throws Exception {
		Workspace workspace = registry.getPlugin(Workspace.class);
		Set<Container> set = new HashSet<Container>();
		for (Project project : workspace.getAllProjects()) {
			set.addAll(project.getBuildpath());
			set.addAll(project.getRunbundles());
			set.addAll(project.getRunpath());
			set.addAll(project.getTestpath());
			set.addAll(project.getBootclasspath());
			set.addAll(project.getClasspath());

			//
			// This should be replaced with project.getRunfw()
			//
			String s = project.getProperty(Constants.RUNFW);
			List<Container> bundles = project.getBundles(Strategy.HIGHEST, s, Constants.RUNFW);
			set.addAll(bundles);

			File base = project.getBase();
			for (File sub : base.listFiles()) {
				if (sub.getName().endsWith(".bndrun")) {
					Project bndrun = new Project(workspace, base, sub);
					try {
						set.addAll(bndrun.getRunbundles());
						set.addAll(bndrun.getRunpath());
						set.addAll(bndrun.getTestpath());
						set.addAll(bndrun.getBootclasspath());
						set.addAll(bndrun.getClasspath());
					}
					finally {
						bndrun.close();
					}
				}
			}
		}

		Set<RevisionRef> refs = new HashSet<RevisionRef>(index.getRevisionRefs());
		Set<RevisionRef> keep = new HashSet<RevisionRef>();

		for (Container libOrRev : set) {
			for (Container c : libOrRev.getMembers()) {
				reporter.trace("Dependency " + c);

				if (!Verifier.isVersion(c.getVersion()))
					continue;

				RevisionRef ref = index.getRevisionRef(c.getBundleSymbolicName(), new Version(c.getVersion()));
				if (ref != null)
					refs.remove(ref);
				else {
					// missing!
					reporter.trace("Missing " + c.getBundleSymbolicName());
					Coordinate coord = new Coordinate(c.getBundleSymbolicName());
					Revision rev = library.getRevisionByCoordinate(coord);
					if (rev != null) {
						index.addRevision(new RevisionRef(rev));
					} else
						System.out.printf("not found %s\n", c);
				}
				keep.add(ref);
			}
		}

		for (RevisionRef ref : refs) {
			index.delete(ref.bsn, Index.toVersion(ref));
		}
		index.save();
	}

	/**
	 * Get a Resource Descriptor for a given bsn/version
	 * 
	 * @param bsn
	 * @param version
	 * @return
	 * @throws Exception
	 */
	public ResourceDescriptor getDescriptor(String bsn, Version version) throws Exception {
		RevisionRef revisionRef = index.getRevisionRef(bsn, version);
		if (revisionRef == null)
			return null;

		return createResourceDescriptor(revisionRef);
	}

	/**
	 * Copy a string to the clipboard
	 */

	void toClipboard(String bsn, Version base) {
		Version nextMajor = new Version(base.getMajor() + 1, 0, 0);
		toClipboard(bsn + ";version='[" + base.getWithoutQualifier() + "," + nextMajor + ")'");
	}

	void toClipboard(String s) {
		if (s == null)
			return;

		StringSelection stringSelection = new StringSelection(s);
		Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
		clpbrd.setContents(stringSelection, null);
	}


	@Override
	public String toString() {
		byte[] digest;
		try {
			digest = getDigest();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		return "JpmRepository [writable="
				+ canWrite()
				+ ", "
				+ (getName() != null ? "name=" + getName() + ", " : "")
				+ (getLocation() != null ? "location=" + getLocation() + ", " : "")
				+ (digest != null ? "digest="
						+ Hex.toHexString(digest) : "") + "]";
	}


}
