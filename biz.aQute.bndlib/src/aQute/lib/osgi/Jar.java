package aQute.lib.osgi;

import static aQute.lib.io.IO.*;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.jar.*;
import java.util.regex.*;
import java.util.zip.*;

import aQute.lib.base64.*;
import aQute.lib.io.*;
import aQute.libg.reporter.*;

public class Jar implements Closeable {
	public enum Compression {
		DEFLATE, STORE
	}

	public static final Object[]			EMPTY_ARRAY	= new Jar[0];
	final Map<String,Resource>				resources	= new TreeMap<String,Resource>();
	final Map<String,Map<String,Resource>>	directories	= new TreeMap<String,Map<String,Resource>>();
	Manifest								manifest;
	boolean									manifestFirst;
	String									name;
	File									source;
	ZipFile									zipFile;
	long									lastModified;
	String									lastModifiedReason;
	Reporter								reporter;
	boolean									doNotTouchManifest;
	boolean									nomanifest;
	Compression								compression	= Compression.DEFLATE;
	boolean									closed;

	public Jar(String name) {
		this.name = name;
	}

	public Jar(String name, File dirOrFile, Pattern doNotCopy) throws ZipException, IOException {
		this(name);
		source = dirOrFile;
		if (dirOrFile.isDirectory())
			FileResource.build(this, dirOrFile, doNotCopy);
		else if (dirOrFile.isFile()) {
			zipFile = ZipResource.build(this, dirOrFile);
		} else {
			throw new IllegalArgumentException("A Jar can only accept a valid file or directory: " + dirOrFile);
		}
	}

	public Jar(String name, InputStream in, long lastModified) throws IOException {
		this(name);
		EmbeddedResource.build(this, in, lastModified);
	}

	public Jar(String name, String path) throws IOException {
		this(name);
		File f = new File(path);
		InputStream in = new FileInputStream(f);
		EmbeddedResource.build(this, in, f.lastModified());
		in.close();
	}

	public Jar(File f) throws IOException {
		this(getName(f), f, null);
	}

	/**
	 * Make the JAR file name the project name if we get a src or bin directory.
	 * 
	 * @param f
	 * @return
	 */
	private static String getName(File f) {
		f = f.getAbsoluteFile();
		String name = f.getName();
		if (name.equals("bin") || name.equals("src"))
			return f.getParentFile().getName();
		if (name.endsWith(".jar"))
			name = name.substring(0, name.length() - 4);
		return name;
	}

	public Jar(String string, InputStream resourceAsStream) throws IOException {
		this(string, resourceAsStream, 0);
	}

	public Jar(String string, File file) throws ZipException, IOException {
		this(string, file, Pattern.compile(Constants.DEFAULT_DO_NOT_COPY));
	}

	public void setName(String name) {
		this.name = name;
	}

	public String toString() {
		return "Jar:" + name;
	}

	public boolean putResource(String path, Resource resource) {
		check();
		return putResource(path, resource, true);
	}

	public boolean putResource(String path, Resource resource, boolean overwrite) {
		check();
		updateModified(resource.lastModified(), path);
		while (path.startsWith("/"))
			path = path.substring(1);

		if (path.equals("META-INF/MANIFEST.MF")) {
			manifest = null;
			if (resources.isEmpty())
				manifestFirst = true;
		}
		String dir = getDirectory(path);
		Map<String,Resource> s = directories.get(dir);
		if (s == null) {
			s = new TreeMap<String,Resource>();
			directories.put(dir, s);
			int n = dir.lastIndexOf('/');
			while (n > 0) {
				String dd = dir.substring(0, n);
				if (directories.containsKey(dd))
					break;
				directories.put(dd, null);
				n = dd.lastIndexOf('/');
			}
		}
		boolean duplicate = s.containsKey(path);
		if (!duplicate || overwrite) {
			resources.put(path, resource);
			s.put(path, resource);
		}
		return duplicate;
	}

	public Resource getResource(String path) {
		check();
		if (resources == null)
			return null;
		return resources.get(path);
	}

	private String getDirectory(String path) {
		check();
		int n = path.lastIndexOf('/');
		if (n < 0)
			return "";

		return path.substring(0, n);
	}

	public Map<String,Map<String,Resource>> getDirectories() {
		check();
		return directories;
	}

	public Map<String,Resource> getResources() {
		check();
		return resources;
	}

	public boolean addDirectory(Map<String,Resource> directory, boolean overwrite) {
		check();
		boolean duplicates = false;
		if (directory == null)
			return false;

		for (Map.Entry<String,Resource> entry : directory.entrySet()) {
			String key = entry.getKey();
			if (!key.endsWith(".java")) {
				duplicates |= putResource(key, entry.getValue(), overwrite);
			}
		}
		return duplicates;
	}

	public Manifest getManifest() throws Exception {
		check();
		if (manifest == null) {
			Resource manifestResource = getResource("META-INF/MANIFEST.MF");
			if (manifestResource != null) {
				InputStream in = manifestResource.openInputStream();
				manifest = new Manifest(in);
				in.close();
			}
		}
		return manifest;
	}

	public boolean exists(String path) {
		check();
		return resources.containsKey(path);
	}

	public void setManifest(Manifest manifest) {
		check();
		manifestFirst = true;
		this.manifest = manifest;
	}

	public void setManifest(File file) throws IOException {
		check();
		FileInputStream fin = new FileInputStream(file);
		try {
			Manifest m = new Manifest(fin);
			setManifest(m);
		}
		finally {
			fin.close();
		}
	}

	public void write(File file) throws Exception {
		check();
		try {
			OutputStream out = new FileOutputStream(file);
			try {
				write(out);
			}
			finally {
				IO.close(out);
			}
			return;

		}
		catch (Exception t) {
			file.delete();
			throw t;
		}
	}

	public void write(String file) throws Exception {
		check();
		write(new File(file));
	}

	public void write(OutputStream out) throws Exception {
		check();
		ZipOutputStream jout = nomanifest || doNotTouchManifest ? new ZipOutputStream(out) : new JarOutputStream(out);

		switch (compression) {
			case STORE :
				jout.setMethod(ZipOutputStream.DEFLATED);
				break;

			default :
				// default is DEFLATED
		}

		Set<String> done = new HashSet<String>();

		Set<String> directories = new HashSet<String>();
		if (doNotTouchManifest) {
			Resource r = getResource("META-INF/MANIFEST.MF");
			if (r != null) {
				writeResource(jout, directories, "META-INF/MANIFEST.MF", r);
				done.add("META-INF/MANIFEST.MF");
			}
		} else
			doManifest(done, jout);

		for (Map.Entry<String,Resource> entry : getResources().entrySet()) {
			// Skip metainf contents
			if (!done.contains(entry.getKey()))
				writeResource(jout, directories, entry.getKey(), entry.getValue());
		}
		jout.finish();
	}

	private void doManifest(Set<String> done, ZipOutputStream jout) throws Exception {
		check();
		if (nomanifest)
			return;

		JarEntry ze = new JarEntry("META-INF/MANIFEST.MF");
		jout.putNextEntry(ze);
		writeManifest(jout);
		jout.closeEntry();
		done.add(ze.getName());
	}

	/**
	 * Cleanup the manifest for writing. Cleaning up consists of adding a space
	 * after any \n to prevent the manifest to see this newline as a delimiter.
	 * 
	 * @param out
	 *            Output
	 * @throws IOException
	 */

	public void writeManifest(OutputStream out) throws Exception {
		check();
		writeManifest(getManifest(), out);
	}

	public static void writeManifest(Manifest manifest, OutputStream out) throws IOException {
		if (manifest == null)
			return;

		manifest = clean(manifest);
		outputManifest(manifest, out);
	}

	/**
	 * Unfortunately we have to write our own manifest :-( because of a stupid
	 * bug in the manifest code. It tries to handle UTF-8 but the way it does it
	 * it makes the bytes platform dependent. So the following code outputs the
	 * manifest. A Manifest consists of
	 * 
	 * <pre>
	 *   'Manifest-Version: 1.0\r\n'
	 *   main-attributes *
	 *   \r\n
	 *   name-section
	 *   
	 *   main-attributes ::= attributes
	 *   attributes      ::= key ': ' value '\r\n'
	 *   name-section    ::= 'Name: ' name '\r\n' attributes
	 * </pre>
	 * 
	 * Lines in the manifest should not exceed 72 bytes (! this is where the
	 * manifest screwed up as well when 16 bit unicodes were used).
	 * <p>
	 * As a bonus, we can now sort the manifest!
	 */
	static byte[]	CONTINUE	= new byte[] {
			'\r', '\n', ' '
								};

	/**
	 * Main function to output a manifest properly in UTF-8.
	 * 
	 * @param manifest
	 *            The manifest to output
	 * @param out
	 *            The output stream
	 * @throws IOException
	 *             when something fails
	 */
	public static void outputManifest(Manifest manifest, OutputStream out) throws IOException {
		writeEntry(out, "Manifest-Version", "1.0");
		attributes(manifest.getMainAttributes(), out);

		TreeSet<String> keys = new TreeSet<String>();
		for (Object o : manifest.getEntries().keySet())
			keys.add(o.toString());

		for (String key : keys) {
			write(out, 0, "\r\n");
			writeEntry(out, "Name", key);
			attributes(manifest.getAttributes(key), out);
		}
		out.flush();
	}

	/**
	 * Write out an entry, handling proper unicode and line length constraints
	 */
	private static void writeEntry(OutputStream out, String name, String value) throws IOException {
		int n = write(out, 0, name + ": ");
		write(out, n, value);
		write(out, 0, "\r\n");
	}

	/**
	 * Convert a string to bytes with UTF8 and then output in max 72 bytes
	 * 
	 * @param out
	 *            the output string
	 * @param i
	 *            the current width
	 * @param s
	 *            the string to output
	 * @return the new width
	 * @throws IOException
	 *             when something fails
	 */
	private static int write(OutputStream out, int i, String s) throws IOException {
		byte[] bytes = s.getBytes("UTF8");
		return write(out, i, bytes);
	}

	/**
	 * Write the bytes but ensure that the line length does not exceed 72
	 * characters. If it is more than 70 characters, we just put a cr/lf +
	 * space.
	 * 
	 * @param out
	 *            The output stream
	 * @param width
	 *            The nr of characters output in a line before this method
	 *            started
	 * @param bytes
	 *            the bytes to output
	 * @return the nr of characters in the last line
	 * @throws IOException
	 *             if something fails
	 */
	private static int write(OutputStream out, int width, byte[] bytes) throws IOException {
		int w = width;
		for (int i = 0; i < bytes.length; i++) {
			if (w >= 72) { // we need to add the \n\r!
				out.write(CONTINUE);
				w = 1;
			}
			out.write(bytes[i]);
			w++;
		}
		return w;
	}

	/**
	 * Output an Attributes map. We will sort this map before outputing.
	 * 
	 * @param value
	 *            the attrbutes
	 * @param out
	 *            the output stream
	 * @throws IOException
	 *             when something fails
	 */
	private static void attributes(Attributes value, OutputStream out) throws IOException {
		TreeMap<String,String> map = new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);
		for (Map.Entry<Object,Object> entry : value.entrySet()) {
			map.put(entry.getKey().toString(), entry.getValue().toString());
		}

		map.remove("Manifest-Version"); // get rid of
		// manifest
		// version
		for (Map.Entry<String,String> entry : map.entrySet()) {
			writeEntry(out, entry.getKey(), entry.getValue());
		}
	}

	private static Manifest clean(Manifest org) {

		Manifest result = new Manifest();
		for (Map.Entry< ? , ? > entry : org.getMainAttributes().entrySet()) {
			String nice = clean((String) entry.getValue());
			result.getMainAttributes().put(entry.getKey(), nice);
		}
		for (String name : org.getEntries().keySet()) {
			Attributes attrs = result.getAttributes(name);
			if (attrs == null) {
				attrs = new Attributes();
				result.getEntries().put(name, attrs);
			}

			for (Map.Entry< ? , ? > entry : org.getAttributes(name).entrySet()) {
				String nice = clean((String) entry.getValue());
				attrs.put(entry.getKey(), nice);
			}
		}
		return result;
	}

	private static String clean(String s) {
		if (s.indexOf('\n') < 0)
			return s;

		StringBuilder sb = new StringBuilder(s);
		for (int i = 0; i < sb.length(); i++) {
			if (sb.charAt(i) == '\n')
				sb.insert(++i, ' ');
		}
		return sb.toString();
	}

	private void writeResource(ZipOutputStream jout, Set<String> directories, String path, Resource resource)
			throws Exception {
		if (resource == null)
			return;
		try {
			createDirectories(directories, jout, path);
			ZipEntry ze = new ZipEntry(path);
			ze.setMethod(ZipEntry.DEFLATED);
			long lastModified = resource.lastModified();
			if (lastModified == 0L) {
				lastModified = System.currentTimeMillis();
			}
			ze.setTime(lastModified);
			if (resource.getExtra() != null)
				ze.setExtra(resource.getExtra().getBytes("UTF-8"));
			jout.putNextEntry(ze);
			resource.write(jout);
			jout.closeEntry();
		}
		catch (Exception e) {
			throw new Exception("Problem writing resource " + path, e);
		}
	}

	void createDirectories(Set<String> directories, ZipOutputStream zip, String name) throws IOException {
		int index = name.lastIndexOf('/');
		if (index > 0) {
			String path = name.substring(0, index);
			if (directories.contains(path))
				return;
			createDirectories(directories, zip, path);
			ZipEntry ze = new ZipEntry(path + '/');
			zip.putNextEntry(ze);
			zip.closeEntry();
			directories.add(path);
		}
	}

	public String getName() {
		return name;
	}

	/**
	 * Add all the resources in the given jar that match the given filter.
	 * 
	 * @param sub
	 *            the jar
	 * @param filter
	 *            a pattern that should match the resoures in sub to be added
	 */
	public boolean addAll(Jar sub, Instruction filter) {
		return addAll(sub, filter, "");
	}

	/**
	 * Add all the resources in the given jar that match the given filter.
	 * 
	 * @param sub
	 *            the jar
	 * @param filter
	 *            a pattern that should match the resoures in sub to be added
	 */
	public boolean addAll(Jar sub, Instruction filter, String destination) {
		check();
		boolean dupl = false;
		for (String name : sub.getResources().keySet()) {
			if ("META-INF/MANIFEST.MF".equals(name))
				continue;

			if (filter == null || filter.matches(name) != filter.isNegated())
				dupl |= putResource(Processor.appendPath(destination, name), sub.getResource(name), true);
		}
		return dupl;
	}

	public void close() {
		this.closed = true;
		if (zipFile != null)
			try {
				zipFile.close();
			}
			catch (IOException e) {
				// Ignore
			}
		resources.clear();
		directories.clear();
		manifest = null;
		source = null;
	}

	public long lastModified() {
		return lastModified;
	}

	public void updateModified(long time, String reason) {
		if (time > lastModified) {
			lastModified = time;
			lastModifiedReason = reason;
		}
	}

	public void setReporter(Reporter reporter) {
		this.reporter = reporter;
	}

	public boolean hasDirectory(String path) {
		check();
		return directories.get(path) != null;
	}

	public List<String> getPackages() {
		check();
		List<String> list = new ArrayList<String>(directories.size());

		for (Map.Entry<String,Map<String,Resource>> i : directories.entrySet()) {
			if (i.getValue() != null) {
				String path = i.getKey();
				String pack = path.replace('/', '.');
				list.add(pack);
			}
		}
		return list;
	}

	public File getSource() {
		check();
		return source;
	}

	public boolean addAll(Jar src) {
		check();
		return addAll(src, null);
	}

	public boolean rename(String oldPath, String newPath) {
		check();
		Resource resource = remove(oldPath);
		if (resource == null)
			return false;

		return putResource(newPath, resource);
	}

	public Resource remove(String path) {
		check();
		Resource resource = resources.remove(path);
		String dir = getDirectory(path);
		Map<String,Resource> mdir = directories.get(dir);
		// must be != null
		mdir.remove(path);
		return resource;
	}

	/**
	 * Make sure nobody touches the manifest! If the bundle is signed, we do not
	 * want anybody to touch the manifest after the digests have been
	 * calculated.
	 */
	public void setDoNotTouchManifest() {
		doNotTouchManifest = true;
	}

	/**
	 * Calculate the checksums and set them in the manifest.
	 */

	public void calcChecksums(String algorithms[]) throws Exception {
		check();
		if (algorithms == null)
			algorithms = new String[] {
					"SHA", "MD5"
			};

		Manifest m = getManifest();
		if (m == null) {
			m = new Manifest();
			setManifest(m);
		}

		MessageDigest digests[] = new MessageDigest[algorithms.length];
		int n = 0;
		for (String algorithm : algorithms)
			digests[n++] = MessageDigest.getInstance(algorithm);

		byte buffer[] = new byte[30000];

		for (Map.Entry<String,Resource> entry : resources.entrySet()) {

			// Skip the manifest
			if (entry.getKey().equals("META-INF/MANIFEST.MF"))
				continue;

			Resource r = entry.getValue();
			Attributes attributes = m.getAttributes(entry.getKey());
			if (attributes == null) {
				attributes = new Attributes();
				getManifest().getEntries().put(entry.getKey(), attributes);
			}
			InputStream in = r.openInputStream();
			try {
				for (MessageDigest d : digests)
					d.reset();
				int size = in.read(buffer);
				while (size > 0) {
					for (MessageDigest d : digests)
						d.update(buffer, 0, size);
					size = in.read(buffer);
				}
			}
			finally {
				in.close();
			}
			for (MessageDigest d : digests)
				attributes.putValue(d.getAlgorithm() + "-Digest", Base64.encodeBase64(d.digest()));
		}
	}

	Pattern	BSN	= Pattern.compile("\\s*([-\\w\\d\\._]+)\\s*;?.*");

	public String getBsn() throws Exception {
		check();
		Manifest m = getManifest();
		if (m == null)
			return null;

		String s = m.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
		if (s == null)
			return null;

		Matcher matcher = BSN.matcher(s);
		if (matcher.matches()) {
			return matcher.group(1);
		}
		return null;
	}

	public String getVersion() throws Exception {
		check();
		Manifest m = getManifest();
		if (m == null)
			return null;

		String s = m.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
		if (s == null)
			return null;

		return s.trim();
	}

	/**
	 * Expand the JAR file to a directory.
	 * 
	 * @param dir
	 *            the dst directory, is not required to exist
	 * @throws Exception
	 *             if anything does not work as expected.
	 */
	public void expand(File dir) throws Exception {
		check();
		dir = dir.getAbsoluteFile();
		dir.mkdirs();
		if (!dir.isDirectory()) {
			throw new IllegalArgumentException("Not a dir: " + dir.getAbsolutePath());
		}

		for (Map.Entry<String,Resource> entry : getResources().entrySet()) {
			File f = getFile(dir, entry.getKey());
			f.getParentFile().mkdirs();
			IO.copy(entry.getValue().openInputStream(), f);
		}
	}

	/**
	 * Make sure we have a manifest
	 * 
	 * @throws Exception
	 */
	public void ensureManifest() throws Exception {
		if (getManifest() != null)
			return;
		manifest = new Manifest();
	}

	/**
	 * Answer if the manifest was the first entry
	 */

	public boolean isManifestFirst() {
		return manifestFirst;
	}

	public void copy(Jar srce, String path, boolean overwrite) {
		check();
		addDirectory(srce.getDirectories().get(path), overwrite);
	}

	public void setCompression(Compression compression) {
		this.compression = compression;
	}

	public Compression hasCompression() {
		return this.compression;
	}

	void check() {
		if (closed)
			throw new RuntimeException("Already closed " + name);
	}
}
