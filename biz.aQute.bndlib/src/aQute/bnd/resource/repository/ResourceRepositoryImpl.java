package aQute.bnd.resource.repository;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.zip.InflaterInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.RepositoryPlugin.DownloadListener;
import aQute.bnd.service.repository.ResourceRepository;
import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;
import aQute.bnd.service.url.URLConnectionHandler;
import aQute.bnd.url.DefaultURLConnectionHandler;
import aQute.bnd.version.VersionRange;
import aQute.lib.collections.MultiMap;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.hex.Hex;
import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;
import aQute.libg.cryptography.SHA1;
import aQute.libg.reporter.ReporterAdapter;
import aQute.service.reporter.Reporter;

/**
 * This class implements a hidden repository. This repo is kept in a text file
 * that is under scm control. Files are fetched on demand. The idea is that bnd
 * will bootstrap from this repo and downloads plugins. These plugins then
 * provide faces on this hidden repository.
 */
public class ResourceRepositoryImpl implements ResourceRepository {
	private final static Logger						logger							= LoggerFactory
		.getLogger(ResourceRepositoryImpl.class);
	private static Comparator<ResourceDescriptor>	RESOURCE_DESCRIPTOR_COMPARATOR	= (o1, o2) -> {
																						if (o1 == o2)
																							return 0;

																						int r = o1.bsn
																							.compareTo(o2.bsn);
																						if (r > 0)
																							return 1;
																						else if (r < 0)
																							return -1;

																						return o1.version
																							.compareTo(o2.version);
																					};
	private static final long						THRESHOLD						= 4 * 3600 * 1000;					// 4
	protected static final DownloadListener[]		EMPTY_LISTENER					= new DownloadListener[0];
	static JSONCodec								codec							= new JSONCodec();
	private final List<Listener>					listeners						= new CopyOnWriteArrayList<>();
	private boolean									dirty;
	private FileLayout								index;
	private Map<URI, Long>							failures						= new HashMap<>();
	private File									cache;
	private File									hosting;
	private Reporter								reporter						= new ReporterAdapter(System.out);
	private Executor								executor;
	private File									indexFile;
	private URLConnectionHandler					connector						= new DefaultURLConnectionHandler();
	final MultiMap<File, DownloadListener>			queues							= new MultiMap<>();
	final Semaphore									limitDownloads					= new Semaphore(5);

	{
		((ReporterAdapter) reporter).setTrace(true);
	}

	/**
	 * Class maintains the info stored in the text file in the cnf directory
	 * that holds our contents.
	 */
	static public class FileLayout {
		public int							version;
		public List<ResourceDescriptorImpl>	descriptors	= new ArrayList<>();
		public int							increment;
		public long							date;

		// Manually print this so that we have 1 line per resource, making it
		// easier on git to compare.

		void write(Formatter format) throws Exception {
			Collections.sort(descriptors);
			date = System.currentTimeMillis();

			format.format(//
				"{\n\"version\"      :%s,\n" //
					+ "\"descriptors\"   : [\n",
				version);
			String del = "";

			for (ResourceDescriptorImpl rd : descriptors) {
				format.format(del);
				format.flush();
				codec.enc()
					.to(format.out())
					.keepOpen()
					.put(rd);
				del = ",\n";
			}
			format.format("\n]}\n");
			format.flush();
		}
	}

	/**
	 * List the resources. We skip the filter for now.
	 */
	@Override
	public List<ResourceDescriptorImpl> filter(String repoId, String filter) throws Exception {
		List<ResourceDescriptorImpl> result = new ArrayList<>();
		for (ResourceDescriptorImpl rdi : getIndex().descriptors) {
			if (repoId == null || rdi.repositories.contains(repoId))
				result.add(rdi);
		}
		return result;
	}

	/**
	 * Delete a resource from the text file (not from the cache)
	 */

	void delete(byte[] id) throws Exception {
		for (Iterator<ResourceDescriptorImpl> i = getIndex().descriptors.iterator(); i.hasNext();) {
			ResourceDescriptorImpl d = i.next();
			if (Arrays.equals(id, d.id)) {
				i.remove();
				logger.debug("removing resource {} from index", d);
				event(TYPE.REMOVE, d, null);
				setDirty();
			}
		}
		save();
	}

	@Override
	public boolean delete(String repoId, byte[] id) throws Exception {
		ResourceDescriptorImpl rd = getResourceDescriptor(id);
		if (rd == null)
			return false;

		if (repoId == null) {
			delete(id);
			return true;
		}

		boolean remove = rd.repositories.remove(repoId);
		if (rd.repositories.isEmpty()) {
			delete(rd.id);
		} else
			save();

		return remove;
	}

	/**
	 * Delete a cache entry
	 */
	@Override
	public boolean deleteCache(byte[] id) throws Exception {
		File dir = IO.getFile(cache, Hex.toHexString(id));
		if (dir.isDirectory()) {
			IO.delete(dir);
			return true;
		}
		return false;
	}

	/**
	 * Add a resource descriptor to the index.
	 */
	@Override
	public boolean add(String repoId, ResourceDescriptor rd) throws Exception {
		ResourceDescriptorImpl rdi = getResourceDescriptor(rd.id);
		boolean add = false;
		if (rdi != null) {
			add = true;
			logger.debug("adding repo {} to resource {} to index", repoId, rdi);
		} else {
			rdi = new ResourceDescriptorImpl(rd);
			getIndex().descriptors.add(rdi);
			logger.debug("adding resource {} to index", rdi);
		}
		rdi.repositories.add(repoId);
		event(TYPE.ADD, rdi, null);
		setDirty();
		save();
		return add;
	}

	/**
	 * Get the file belonging to a Resource Descriptor
	 */
	@Override
	public File getResource(byte[] rd, final RepositoryPlugin.DownloadListener... blockers) throws Exception {
		final ResourceDescriptorImpl rds = getResourceDescriptor(rd);

		// No such descriptor?

		if (rds == null) {
			logger.debug("no such descriptor {}", Hex.toHexString(rd));
			return null;
		}

		//
		// Construct a path
		//
		final File path = IO.getFile(cache, Hex.toHexString(rds.id) + "/" + rds.bsn + "-" + rds.version + ".jar");

		if (path.isFile()) {
			//
			// Ok, it is there, just report
			//
			ok(blockers, path);
			return path;
		}

		//
		// Check if we had an earlier failure
		//

		synchronized (failures) {
			Long l = failures.get(rds.url);
			if (l != null && (System.currentTimeMillis() - l) < THRESHOLD) {
				logger.debug("descriptor {}, had earlier failure not retrying", Hex.toHexString(rd));
				return null;
			}
		}

		//
		// Check if we need to download directly, no blockers
		//
		if (blockers == null || blockers.length == 0) {
			logger.debug("descriptor {}, not found, immediate download", Hex.toHexString(rd));
			download(rds, path);
			return path;
		}

		//
		// We have blockers so we can download in the background.
		//

		logger.debug("descriptor {}, not found, background download", Hex.toHexString(rd));

		//
		// With download listeners we need to be careful to queue them
		// appropriately. Don't want to download n times because
		// requests arrive during downloads.
		//

		synchronized (queues) {
			List<DownloadListener> list = queues.get(path);
			boolean first = list == null || list.isEmpty();
			for (DownloadListener b : blockers) {
				queues.add(path, b);
			}

			if (!first) {
				// return, file is being downloaded by another and that
				// other will signal the download listener.
				logger.debug("someone else is downloading our file {}", queues.get(path));
				return path;
			}
		}
		limitDownloads.acquire();

		executor.execute(() -> {
			try {
				download(rds, path);
				synchronized (queues) {
					ok(queues.get(path)
						.toArray(EMPTY_LISTENER), path);
				}
			} catch (Exception e) {
				synchronized (queues) {
					fail(e, queues.get(path)
						.toArray(EMPTY_LISTENER), path);
				}
			} finally {
				synchronized (queues) {
					queues.remove(path);
				}
				limitDownloads.release();
			}
		});
		return path;
	}

	/**
	 * Add a new event listener
	 */
	@Override
	public void addListener(Listener rrl) {
		listeners.add(rrl);
	}

	/**
	 * Remove an event listener
	 */
	public void removeListener(Listener rrl) {
		listeners.remove(rrl);
	}

	/**
	 * Set dirty for save. Save is a noop if not dirty
	 */
	private void setDirty() {
		dirty = true;
	}

	/**
	 * List the resources. We skip the filter for now.
	 */
	@Override
	public ResourceDescriptorImpl getResourceDescriptor(byte[] rd) throws Exception {
		for (ResourceDescriptorImpl d : getIndex().descriptors) {
			if (Arrays.equals(d.id, rd))
				return d;
		}
		return null;
	}

	/**
	 * Just report success to all download listeners
	 *
	 * @param blockers
	 * @param file
	 */

	void ok(DownloadListener[] blockers, File file) {
		for (DownloadListener dl : blockers) {
			try {
				dl.success(file);
			} catch (Exception e) {
				//
			}
		}
	}

	void fail(Exception e, DownloadListener[] blockers, File file) {
		String reason = Exceptions.toString(e);
		for (DownloadListener dl : blockers) {
			try {
				dl.failure(file, reason);
			} catch (Exception ee) {
				//
			}
		}
	}

	void download(ResourceDescriptor rds, File path) throws Exception {
		logger.debug("starting download {}", path);
		Exception exception = new Exception();
		event(TYPE.START_DOWNLOAD, rds, null);
		for (int i = 0; i < 3; sleep(3000), i++)
			try {
				download0(rds.url, path, rds.id);
				event(TYPE.END_DOWNLOAD, rds, null);
				logger.debug("succesful download {}", path);
				failures.remove(rds.url);
				return;
			} catch (FileNotFoundException e) {
				logger.debug("no such file download {}", path);
				exception = e;
				break; // no use retrying
			} catch (Exception e) {
				logger.debug("exception download {}", path);
				exception = e;
			}

		failures.put(rds.url, System.currentTimeMillis());

		logger.debug("failed download {}", path, exception);
		event(TYPE.ERROR, rds, exception);
		event(TYPE.END_DOWNLOAD, rds, exception);
		throw exception;
	}

	void download0(URI url, File path, byte[] sha) throws Exception {
		IO.mkdirs(path.getParentFile());
		File tmp = IO.createTempFile(path.getParentFile(), "tmp", ".jar");
		URL u = url.toURL();

		URLConnection conn = u.openConnection();
		InputStream in;
		if (conn instanceof HttpURLConnection) {
			HttpURLConnection http = (HttpURLConnection) conn;
			http.setRequestProperty("Accept-Encoding", "deflate");
			http.setInstanceFollowRedirects(true);

			connector.handle(conn);

			int result = http.getResponseCode();
			if (result / 100 != 2) {
				String s = "";
				try (InputStream err = http.getErrorStream()) {
					if (err != null)
						s = IO.collect(err);
					if (result == 404) {
						logger.debug("not found ");
						throw new FileNotFoundException("Cannot find " + url + " : " + s);
					}
					throw new IOException("Failed request " + result + ":" + http.getResponseMessage() + " " + s);
				}
			}

			String deflate = http.getHeaderField("Content-Encoding");
			in = http.getInputStream();
			if (deflate != null && deflate.toLowerCase()
				.contains("deflate")) {
				in = new InflaterInputStream(in);
				logger.debug("inflate");
			}
		} else {
			connector.handle(conn);
			in = conn.getInputStream();
		}

		IO.copy(in, tmp);

		byte[] digest = SHA1.digest(tmp)
			.digest();
		if (Arrays.equals(digest, sha)) {
			IO.rename(tmp, path);
		} else {
			logger.debug("sha's did not match {}, expected {}, got {}", tmp, Hex.toHexString(sha), digest);
			throw new IllegalArgumentException("Invalid sha downloaded");
		}
	}

	/**
	 * Dispatch the events
	 *
	 * @param type
	 * @param rds
	 * @param exception
	 */
	private void event(TYPE type, ResourceDescriptor rds, Exception exception) {
		for (Listener l : listeners) {
			try {
				l.events(new ResourceRepositoryEvent(type, rds, exception));
			} catch (Exception e) {
				logger.debug("listener {} throws exception", l, e);
			}
		}
	}

	/**
	 * Sleep function that does not throw {@link InterruptedException}
	 *
	 * @param i
	 */
	private boolean sleep(int i) {
		try {
			Thread.sleep(i);
			return true;
		} catch (InterruptedException e) {
			return false;
		}
	}

	/**
	 * Save the index file.
	 *
	 * @throws Exception
	 */
	private void save() throws Exception {
		if (!dirty)
			return;

		Path index = indexFile.toPath();
		Path tmp = Files.createTempFile(IO.mkdirs(index.getParent()), "index", null);

		try (PrintWriter ps = IO.writer(tmp); Formatter frm = new Formatter(ps)) {
			getIndex().write(frm);
		}
		IO.rename(tmp, index);
	}

	/**
	 * Get the index, load it if necessary
	 *
	 * @throws Exception
	 */
	private FileLayout getIndex() throws Exception {
		if (index != null)
			return index;

		if (!indexFile.isFile()) {
			return index = new FileLayout();
		} else
			return index = codec.dec()
				.from(indexFile)
				.get(FileLayout.class);
	}

	public void setReporter(Reporter processor) {
		this.reporter = processor;
	}

	public void setIndexFile(File file) {
		this.indexFile = file;
	}

	public void setCache(File cache) {
		this.cache = cache;
		this.hosting = new File(cache, "hosting");
	}

	public void setExecutor(Executor executor) throws Exception {
		this.executor = executor;
	}

	public void setURLConnector(URLConnectionHandler connector) throws Exception {
		this.connector = connector;
	}

	@Override
	public SortedSet<ResourceDescriptor> find(String repoId, String bsn, VersionRange range) throws Exception {
		TreeSet<ResourceDescriptor> result = new TreeSet<>(RESOURCE_DESCRIPTOR_COMPARATOR);

		for (ResourceDescriptorImpl r : filter(repoId, null)) {
			if (!bsn.equals(r.bsn))
				continue;

			if (range != null && !range.includes(r.version))
				continue;

			result.add(r);
		}
		return result;
	}

	@Override
	public File getCacheDir(String name) {
		File dir = new File(hosting, name);
		try {
			IO.mkdirs(dir);
		} catch (IOException e) {
			throw Exceptions.duck(e);
		}
		return dir;
	}

	@Override
	public String toString() {
		return "ResourceRepositoryImpl [" + (cache != null ? "cache=" + cache + ", " : "")
			+ (indexFile != null ? "indexFile=" + indexFile + ", " : "") + "]";
	}

}
