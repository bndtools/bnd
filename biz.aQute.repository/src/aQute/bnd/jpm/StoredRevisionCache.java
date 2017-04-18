package aQute.bnd.jpm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import aQute.bnd.http.HttpClient;
import aQute.lib.hex.Hex;
import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;
import aQute.lib.settings.Settings;
import aQute.service.library.Library.Program;
import aQute.service.library.Library.RevisionRef;

public class StoredRevisionCache {
	private final File				root;
	final File						tmpdir;
	final File						repodir;
	final File						programdir;
	final Settings					settings;
	boolean							sign;
	final Map<File,Exception>		errors		= new HashMap<File,Exception>();
	private Map<String,Program>		programs	= new HashMap<String,Program>();
	private static final JSONCodec	codec		= new JSONCodec();
	File							refresh;
	private HttpClient				httpc		= new HttpClient();

	public StoredRevisionCache(File root, Settings settings, HttpClient client) throws Exception {
		if (client != null)
			this.httpc = client;

		this.root = root;
		this.settings = settings;
		IO.mkdirs(this.getRoot());
		this.tmpdir = new File(root, "tmp");
		IO.mkdirs(this.tmpdir);
		this.repodir = new File(root, "shas");
		IO.mkdirs(this.repodir);
		this.programdir = new File(root, "programs");
		IO.mkdirs(this.programdir);
		this.refresh = new File(root, ".refreshed");
		if (!this.refresh.isFile())
			this.refresh.createNewFile();
	}

	/*
	 * We create a path that ends in a readable name since Eclipse shows the
	 * last segement. The name is superfluous
	 */

	public File getPath(String bsn, String version, byte[] sha) {
		return getPath(bsn, version, sha, false);
	}

	public File getPath(String bsn, String version, byte[] sha, boolean withsource) {
		if (withsource)
			return IO.getFile(repodir, Hex.toHexString(sha) + "/+" + bsn + "-" + version + ".jar");
		else
			return IO.getFile(repodir, Hex.toHexString(sha) + "/" + bsn + "-" + version + ".jar");
	}

	/*
	 * Delete a revision
	 */
	public void delete(byte[] sha) {
		File f = IO.getFile(getRoot(), Hex.toHexString(sha));
		IO.delete(f);
	}

	static class Download {
		File			tmp;
		byte[]			md5;
		byte[]			sha;
		URI				uri;
	}

	/*
	 * Actually download a file. First in a temp directory, then it is renamed
	 */
	public void download(final File file, final Set<URI> urls, byte[] sha) throws Exception {
		if (errors.containsKey(file))
			throw errors.get(file);

		if (urls.isEmpty())
			throw new Exception("No URLs to download " + file);

		for (URI url : urls) {

			// Fixup for older bug that put the file url in the urls :-(
			Download d = null;
			try {
				d = doDownload(url);
				if (d == null)
					continue;

				if (!Arrays.equals(sha, d.sha))
					throw new Exception("Shas did not match (expected)" + Hex.toHexString(sha) + " (downloaded)" + d.tmp
							+ " (" + Hex.toHexString(d.sha) + ")");

				IO.mkdirs(file.getParentFile());
				IO.rename(d.tmp, file);
				return;
			} catch (Exception e) {
				if (d != null)
					IO.delete(d.tmp);
				errors.put(file, e);
				throw e;
			} finally {
				if (d != null)
					IO.delete(d.tmp);
			}
		}
		throw new FileNotFoundException(urls.toString());
	}

	/*
	 * Download an URI into a temporary file while calculating SHA & MD5. The
	 * connection uses the normal protections
	 */
	Download doDownload(URI url) throws Exception {
		InputStream connect = httpc.connect(url.toURL());
		if (connect == null)
			return null;

		Download d = new Download();
		d.tmp = IO.createTempFile(tmpdir, "tmp", ".tmp");

		MessageDigest sha = MessageDigest.getInstance("SHA1");
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		DigestInputStream shaIn = new DigestInputStream(connect, sha);
		DigestInputStream md5In = new DigestInputStream(shaIn, md5);
		IO.copy(md5In, d.tmp);

		d.sha = sha.digest();
		d.md5 = md5.digest();
		return d;
	}

	public void add(RevisionRef d, File file) throws IOException {
		File path = getPath(d.bsn, d.version, d.revision);
		IO.mkdirs(path.getParentFile());
		IO.copy(file, path);
		long modified = file.lastModified();
		if (modified > 0)
			path.setLastModified(modified);
	}

	public File getRoot() {
		return root;
	}

	public void refresh() {
		errors.clear();
		refresh.setLastModified(System.currentTimeMillis());
	}

	public void deleteAll() throws IOException {
		IO.delete(root);
		IO.mkdirs(root);
	}

	public Program getProgram(String bsn) {
		Program p = programs.get(bsn);
		if (p != null)
			return p;

		File pf = IO.getFile(programdir, bsn + ".json");
		if (pf != null && pf.isFile() && pf.lastModified() >= refresh.lastModified()) {
			try {
				p = codec.dec().from(pf).get(Program.class);
				programs.put(bsn, p);
				return p;
			} catch (Exception e) {
				//
				return null;
			}
		} else
			return null;
	}

	public void putProgram(String bsn, Program p) throws IOException, Exception {
		programs.put(bsn, p);
		File pf = IO.getFile(root, bsn + ".json");
		codec.enc().to(pf).put(p);
	}

	/**
	 * Check if a revision has sources ...
	 * 
	 */
	public boolean hasSources(String bsn, String version, byte[] sha) {
		return getPath(bsn, version, sha, true).isFile();
	}

	/**
	 * Remove the sources from the cache.
	 * 
	 * @param bsn
	 * @param version
	 * @param sha
	 */
	public void removeSources(String bsn, String version, byte[] sha) {
		IO.delete(getPath(bsn, version, sha, true));
	}

	/*
	 * After we used download, we need to create the file in the cache area
	 */
	public void makePermanent(RevisionRef ref, Download d) throws Exception {
		File f = getPath(ref.bsn, ref.version, ref.revision);
		IO.mkdirs(f.getParentFile());
		IO.rename(d.tmp, f);
	}

}
