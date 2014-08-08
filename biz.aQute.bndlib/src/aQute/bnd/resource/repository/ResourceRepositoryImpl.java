package aQute.bnd.resource.repository;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;

import aQute.bnd.service.*;
import aQute.bnd.service.RepositoryPlugin.DownloadListener;
import aQute.bnd.service.repository.*;
import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;
import aQute.bnd.service.url.*;
import aQute.bnd.url.*;
import aQute.bnd.version.*;
import aQute.lib.collections.*;
import aQute.lib.hex.*;
import aQute.lib.io.*;
import aQute.lib.json.*;
import aQute.libg.cryptography.*;
import aQute.libg.reporter.*;
import aQute.service.reporter.*;

/**
 * This class implements a hidden repository. This repo is kept in a text file
 * that is under scm control. Files are fetched on demand. The idea is that bnd
 * will bootstrap from this repo and downloads plugins. These plugins then
 * provide faces on this hidden repository.
 */
public class ResourceRepositoryImpl implements ResourceRepository {
	private static Comparator<ResourceDescriptor>	RESOURCE_DESCRIPTOR_COMPARATOR	= new Comparator<ResourceDescriptor>() {

																						public int compare(
																								ResourceDescriptor o1,
																								ResourceDescriptor o2) {
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
																						}
																					};
	private static final long						THRESHOLD						= 4 * 3600 * 1000;											// 4
	protected static final DownloadListener[]		EMPTY_LISTENER					= new DownloadListener[0];
	static JSONCodec								codec							= new JSONCodec();
	private final List<Listener>					listeners						= new CopyOnWriteArrayList<ResourceRepository.Listener>();
	private boolean									dirty;
	private FileLayout								index;
	private Map<URI,Long>							failures						= new HashMap<URI,Long>();
	private File									cache;
	private File									hosting;
	private Reporter								reporter						= new ReporterAdapter(System.out);
	private Executor								executor;
	private File									indexFile;
	private URLConnectionHandler					connector						= new DefaultURLConnectionHandler();
	final MultiMap<File,DownloadListener>			queues							= new MultiMap<File,RepositoryPlugin.DownloadListener>();
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
		public List<ResourceDescriptorImpl>	descriptors	= new ArrayList<ResourceDescriptorImpl>();
		public int							increment;
		public long							date;

		// Manually print this so that we have 1 line per resource, making it
		// easier on git to compare.

		void write(Formatter format) throws Exception {
			Collections.sort(descriptors);
			date = System.currentTimeMillis();

			format.format(//
					"{\n\"version\"      :%s,\n" //
							+ "\"descriptors\"   : [\n", version, increment, date);
			String del = "";

			for (ResourceDescriptorImpl rd : descriptors) {
				format.format(del);
				format.flush();
				codec.enc().to(format.out()).keepOpen().put(rd);
				del = ",\n";
			}
			format.format("\n]}\n");
			format.flush();
		}
	}

	/**
	 * List the resources. We skip the filter for now.
	 */
	public List<ResourceDescriptorImpl> filter(String repoId, String filter) throws Exception {
		List<ResourceDescriptorImpl> result = new ArrayList<ResourceDescriptorImpl>();
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
				reporter.trace("removing resource %s from index", d);
				event(TYPE.REMOVE, d, null);
				setDirty();
			}
		}
		save();
	}

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
	public boolean add(String repoId, ResourceDescriptor rd) throws Exception {
		ResourceDescriptorImpl rdi = getResourceDescriptor(rd.id);
		boolean add = false;
		if (rdi != null) {
			add = true;
			reporter.trace("adding repo %s to resource %s to index", repoId, rdi);
		} else {
			rdi = new ResourceDescriptorImpl(rd);
			getIndex().descriptors.add(rdi);
			reporter.trace("adding resource %s to index", rdi);
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
	public File getResource(byte[] rd, final RepositoryPlugin.DownloadListener... blockers) throws Exception {
		final ResourceDescriptorImpl rds = getResourceDescriptor(rd);

		// No such descriptor?

		if (rds == null) {
			reporter.trace("no such descriptor %s", Hex.toHexString(rd));
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
				reporter.trace("descriptor %s, had earlier failure not retrying", Hex.toHexString(rd));
				return null;
			}
		}

		//
		// Check if we need to download directly, no blockers
		//
		if (blockers == null || blockers.length == 0) {
			reporter.trace("descriptor %s, not found, immediate download", Hex.toHexString(rd));
			download(rds, path);
			return path;
		}

		//
		// We have blockers so we can download in the background.
		//

		reporter.trace("descriptor %s, not found, background download", Hex.toHexString(rd));

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
				reporter.trace("someone else is downloading our file " + queues.get(path));
				return path;
			}
		}
		limitDownloads.acquire();

		executor.execute(new Runnable() {
			public void run() {
				try {
					download(rds, path);
					synchronized (queues) {
						ok(queues.get(path).toArray(EMPTY_LISTENER), path);
					}
				}
				catch (Exception e) {
					synchronized (queues) {
						fail(e, queues.get(path).toArray(EMPTY_LISTENER), path);
					}
				}
				finally {
					synchronized (queues) {
						queues.remove(path);
					}
					limitDownloads.release();
				}
			}
		});
		return path;
	}

	/**
	 * Add a new event listener
	 */
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
			}
			catch (Exception e) {
				//
			}
		}
	}

	void fail(Exception e, DownloadListener[] blockers, File file) {
		for (DownloadListener dl : blockers) {
			try {
				dl.failure(file, e.toString());
			}
			catch (Exception ee) {
				//
			}
		}
	}

	void download(ResourceDescriptor rds, File path) throws Exception {
		reporter.trace("starting download %s", path);
		Exception exception = new Exception();
		event(TYPE.START_DOWNLOAD, rds, null);
		for (int i = 0; i < 3; sleep(3000), i++)
			try {
				download0(rds.url, path, rds.id);
				event(TYPE.END_DOWNLOAD, rds, null);
				reporter.trace("succesful download %s", path);
				failures.remove(rds.url);
				return;
			}
			catch (FileNotFoundException e) {
				reporter.trace("no such file download %s", path);
				exception = e;
				break; // no use retrying
			}
			catch (Exception e) {
				reporter.trace("exception download %s", path);
				exception = e;
			}

		failures.put(rds.url, System.currentTimeMillis());

		reporter.trace("failed download %s", path);
		event(TYPE.ERROR, rds, exception);
		event(TYPE.END_DOWNLOAD, rds, exception);
		throw exception;
	}

	void download0(URI url, File path, byte[] sha) throws Exception {
		path.getParentFile().mkdirs();
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
				InputStream err = http.getErrorStream();
				try {
					if (err != null)
						s = IO.collect(err);
					if (result == 404) {
						reporter.trace("not found ");
						throw new FileNotFoundException("Cannot find " + url + " : " + s);
					}
					throw new IOException("Failed request " + result + ":" + http.getResponseMessage() + " " + s);
				}
				finally {
					if (err != null)
						err.close();
				}
			}

			String deflate = http.getHeaderField("Content-Encoding");
			in = http.getInputStream();
			if (deflate != null && deflate.toLowerCase().contains("deflate")) {
				in = new InflaterInputStream(in);
				reporter.trace("inflate");
			}
		} else {
			connector.handle(conn);
			in = conn.getInputStream();
		}

		IO.copy(in, tmp);

		byte[] digest = SHA1.digest(tmp).digest();
		if (Arrays.equals(digest, sha)) {
			IO.rename(tmp, path);
		} else {
			reporter.trace("sha's did not match %s, expected %s, got %s", tmp, Hex.toHexString(sha), digest);
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
			}
			catch (Exception e) {
				reporter.trace("listener %s throws exception %s", l, e);
			}
		}
	}

	/**
	 * Sleep function that does not throw {@link InterruptedException}
	 * 
	 * @param i
	 * @return
	 */
	private boolean sleep(int i) {
		try {
			Thread.sleep(i);
			return true;
		}
		catch (InterruptedException e) {
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

		File tmp = new File(indexFile.getAbsolutePath() + ".tmp");
		tmp.getParentFile().mkdirs();

		PrintWriter ps = new PrintWriter(tmp, "UTF-8");
		try {
			Formatter frm = new Formatter(ps);
			getIndex().write(frm);
			frm.close();
		}
		finally {
			ps.close();
		}
		IO.rename(tmp, indexFile);
	}

	/**
	 * Get the index, load it if necessary
	 * 
	 * @return
	 * @throws Exception
	 */
	private FileLayout getIndex() throws Exception {
		if (index != null)
			return index;

		if (!indexFile.isFile()) {
			return index = new FileLayout();
		} else
			return index = codec.dec().from(indexFile).get(FileLayout.class);
	}

	public void setReporter(Reporter processor) {
		this.reporter = processor;
	}

	public void setIndexFile(File file) {
		this.indexFile = file;
	}

	public void setCache(File cache) {
		this.cache = cache;
		this.hosting = new File(cache,"hosting");
	}

	public void setExecutor(Executor executor) throws Exception {
		this.executor = executor;
	}

	public void setURLConnector(URLConnectionHandler connector) throws Exception {
		this.connector = connector;
	}

	public SortedSet<ResourceDescriptor> find(String repoId, String bsn, VersionRange range) throws Exception {
		TreeSet<ResourceDescriptor> result = new TreeSet<ResourceDescriptor>(RESOURCE_DESCRIPTOR_COMPARATOR);

		for (ResourceDescriptorImpl r : filter(repoId, null)) {
			if (!bsn.equals(r.bsn))
				continue;

			if (range != null && !range.includes(r.version))
				continue;

			result.add(r);
		}
		return result;
	}

	public File getCacheDir(String name) {
		File dir = new File(hosting, name);
		dir.mkdirs();
		return dir;
	}

	@Override
	public String toString() {
		return "ResourceRepositoryImpl ["+ (cache != null ? "cache=" + cache + ", " : "")
				+ (indexFile != null ? "indexFile=" + indexFile + ", " : "") + "]";
	}

}
