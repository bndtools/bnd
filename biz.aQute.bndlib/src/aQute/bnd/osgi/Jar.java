package aQute.bnd.osgi;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipOutputStream;

import aQute.bnd.classfile.ClassFile;
import aQute.bnd.classfile.ModuleAttribute;
import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.stream.MapStream;
import aQute.bnd.version.Version;
import aQute.lib.base64.Base64;
import aQute.lib.io.ByteBufferDataInput;
import aQute.lib.io.IO;
import aQute.lib.manifest.ManifestUtil;
import aQute.lib.zip.ZipUtil;
import aQute.libg.cryptography.Digester;
import aQute.libg.cryptography.SHA256;
import aQute.libg.glob.PathSet;

public class Jar extends Zip {

	public enum Compression {
		DEFLATE,
		STORE
	}

	public static final Object[]								EMPTY_ARRAY				= new Jar[0];
	private Optional<Manifest>									manifest;
	private Optional<ModuleAttribute>							moduleAttribute;
	private boolean												manifestFirst;
	private String												manifestName			= JarFile.MANIFEST_NAME;
	private boolean												doNotTouchManifest;
	private boolean												nomanifest;
	private String[]											algorithms;
	private SHA256												sha256;
	private boolean												calculateFileDigest;
	public static final Pattern									METAINF_SIGNING_P		= Pattern
		.compile("META-INF/([^/]+\\.(?:DSA|RSA|EC|SF)|SIG-[^/]+)", Pattern.CASE_INSENSITIVE);

	public Jar(String name) {
		super(name);
	}

	public Jar(String name, File dirOrFile, Pattern doNotCopy) throws IOException {
		super(name, dirOrFile, doNotCopy);
	}

	public Jar(String name, InputStream in, long lastModified) throws IOException {
		super(name, in);
	}

	public Jar(String name, String path) throws IOException {
		super(name, new File(path));
	}

	public Jar(String name, InputStream in) throws IOException {
		super(name, in);
	}

	public Jar(String string, File file) throws IOException {
		super(string, file);
	}

	public Jar(File f) throws IOException {
		super(f);
	}

	@SuppressWarnings("resource")
	public static Jar fromResource(String name, Resource resource) throws Exception {
		if (resource instanceof JarResource) {
			return ((JarResource) resource).getJar();
		} else if (resource instanceof FileResource) {
			return new Jar(name, ((FileResource) resource).getFile());
		}
		Jar jar = new Jar(name);
		jar.buildFromResource(resource);
		return jar;
	}

	public static Stream<Resource> getResources(Resource jarResource, Predicate<String> filter) throws Exception {
		requireNonNull(jarResource);
		requireNonNull(filter);
		if (jarResource instanceof JarResource) {
			Jar jar = ((JarResource) jarResource).getJar();
			return jar.getResources(filter);
		}
		ZipResourceSpliterator spliterator = new ZipResourceSpliterator(jarResource, filter);
		return StreamSupport.stream(spliterator, false)
			.onClose(spliterator);
	}

	@Override
	public void setCompression(Compression compression) {
		super.setCompression(compression);
	}


	@Override
	public String toString() {
		return "Jar:" + getName();
	}

	public Manifest getManifest() throws Exception {
		return manifest().orElse(null);
	}

	@Override
	public boolean putResource(String path, Resource resource, boolean overwrite) {
		path = ZipUtil.cleanPath(path);

		if (path.equals(manifestName)) {
			manifest = null;
			if (resources.isEmpty())
				manifestFirst = true;
		} else if (path.equals(Constants.MODULE_INFO_CLASS)) {
			moduleAttribute = null;
		}
		return super.putResource(path, resource, overwrite);
	}

	Optional<Manifest> manifest() {
		check();
		Optional<Manifest> optional = manifest;
		if (optional != null) {
			return optional;
		}
		try {
			Resource manifestResource = getResource(manifestName);
			if (manifestResource == null) {
				return manifest = Optional.empty();
			}
			try (InputStream in = manifestResource.openInputStream()) {
				return manifest = Optional.of(new Manifest(in));
			}
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	Optional<ModuleAttribute> moduleAttribute() throws Exception {
		check();
		Optional<ModuleAttribute> optional = moduleAttribute;
		if (optional != null) {
			return optional;
		}
		Resource module_info_resource = getResource(Constants.MODULE_INFO_CLASS);
		if (module_info_resource == null) {
			return moduleAttribute = Optional.empty();
		}
		ClassFile module_info;
		ByteBuffer bb = module_info_resource.buffer();
		if (bb != null) {
			module_info = ClassFile.parseClassFile(ByteBufferDataInput.wrap(bb));
		} else {
			try (DataInputStream din = new DataInputStream(module_info_resource.openInputStream())) {
				module_info = ClassFile.parseClassFile(din);
			}
		}
		return moduleAttribute = Arrays.stream(module_info.attributes)
			.filter(ModuleAttribute.class::isInstance)
			.map(ModuleAttribute.class::cast)
			.findFirst();
	}

	public String getModuleName() throws Exception {
		return moduleAttribute().map(a -> a.module_name)
			.orElseGet(this::automaticModuleName);
	}

	String automaticModuleName() {
		return manifest().map(m -> m.getMainAttributes()
			.getValue(Constants.AUTOMATIC_MODULE_NAME))
			.orElse(null);
	}

	public String getModuleVersion() throws Exception {
		return moduleAttribute().map(a -> a.module_version)
			.orElse(null);
	}

	public void setManifest(Manifest manifest) {
		check();
		manifestFirst = true;
		this.manifest = Optional.ofNullable(manifest);
	}

	public void setManifest(File file) throws IOException {
		check();
		try (InputStream fin = IO.stream(file)) {
			Manifest m = new Manifest(fin);
			setManifest(m);
		}
	}

	public String getManifestName() {
		return manifestName;
	}

	public void setManifestName(String manifestName) {
		check();
		manifestName = ZipUtil.cleanPath(manifestName);
		if (manifestName.isEmpty())
			throw new IllegalArgumentException("Manifest name must not be empty");
		this.manifestName = manifestName;
	}

	public void write(File file) throws Exception {
		check();
		try (OutputStream out = IO.outputStream(file)) {
			write(out);
		} catch (Exception t) {
			IO.delete(file);
			throw t;
		}
		file.setLastModified(lastModified());
	}

	public void write(String file) throws Exception {
		check();
		write(new File(file));
	}

	public void write(OutputStream to) throws Exception {
		check();

		if (!doNotTouchManifest && !nomanifest && algorithms != null) {
			doChecksums(to);
			return;
		}

		OutputStream out = to;
		Digester<SHA256> digester = null;
		sha256 = null;
		fileLength = -1;

		if (calculateFileDigest) {
			out = digester = SHA256.getDigester(out);
		}

		ZipOutputStream jout = nomanifest || doNotTouchManifest ? new ZipOutputStream(out) : new JarOutputStream(out);

		switch (compression) {
			case STORE :
				jout.setMethod(ZipOutputStream.STORED);
				break;

			default :
				// default is DEFLATED
		}

		Set<String> done = new HashSet<>();

		Set<String> directories = new HashSet<>();

		// Write manifest first
		if (doNotTouchManifest) {
			Resource r = getResource(manifestName);
			if (r != null) {
				writeResource(jout, directories, manifestName, r);
				done.add(manifestName);
			}
		} else if (!nomanifest) {
			doManifest(jout, directories, manifestName);
			done.add(manifestName);
		}

		// Then write any signature info next since JarInputStream really cares!
		Map<String, Resource> metainf = getDirectory("META-INF");
		if (metainf != null) {
			List<String> signing = metainf.keySet()
				.stream()
				.filter(path -> METAINF_SIGNING_P.matcher(path)
					.matches())
				.collect(toList());
			for (String path : signing) {
				if (done.add(path)) {
					writeResource(jout, directories, path, metainf.get(path));
				}
			}
		}

		// Write all remaining entries
		for (Map.Entry<String, Resource> entry : getResources().entrySet()) {
			// Skip metainf contents
			if (!done.contains(entry.getKey()))
				writeResource(jout, directories, entry.getKey(), entry.getValue());
		}
		jout.finish();

		if (digester != null) {
			this.sha256 = digester.digest();
			this.fileLength = digester.getLength();
		}
	}

	/**
	 * Expand the JAR file to a directory.
	 *
	 * @param dir the dst directory, is not required to exist
	 * @throws Exception if anything does not work as expected.
	 */
	public void expand(File dir) throws Exception {
		writeFolder(dir);
	}

	public void writeFolder(File dir) throws Exception {
		IO.mkdirs(dir);

		if (!dir.exists())
			throw new IllegalArgumentException(
				"The directory " + dir + " to write the JAR " + this + " could not be created");

		if (!dir.isDirectory())
			throw new IllegalArgumentException(
				"The directory " + dir + " to write the JAR " + this + " to is not a directory");

		check();

		Set<String> done = new HashSet<>();

		if (doNotTouchManifest) {
			Resource r = getResource(manifestName);
			if (r != null) {
				copyResource(dir, manifestName, r);
				done.add(manifestName);
			}
		} else {
			File file = IO.getBasedFile(dir, manifestName);
			IO.mkdirs(file.getParentFile());
			try (OutputStream fout = IO.outputStream(file)) {
				writeManifest(fout);
				done.add(manifestName);
			}
		}

		for (Map.Entry<String, Resource> entry : getResources().entrySet()) {
			String path = entry.getKey();
			if (done.contains(path))
				continue;

			Resource resource = entry.getValue();
			copyResource(dir, path, resource);
		}
	}

	public void doChecksums(OutputStream out) throws Exception {
		// ok, we have a request to create digests
		// of the resources. Since we have to output
		// the manifest first, we have a slight problem.
		// We can also not make multiple passes over the resource
		// because some resources are not idempotent and/or can
		// take significant time. So we just copy the jar
		// to a temporary file, read it in again, calculate
		// the checksums and save.

		String[] algs = algorithms;
		algorithms = null;
		try {
			File f = File.createTempFile(padString(getName(), 3, '_'), ".jar");
			write(f);
			try (Jar tmp = new Jar(f)) {
				tmp.setCompression(compression);
				tmp.calcChecksums(algs);
				tmp.write(out);
			} finally {
				IO.delete(f);
			}
		} finally {
			algorithms = algs;
		}
	}



	private void doManifest(ZipOutputStream jout, Set<String> directories, String manifestName) throws Exception {
		check();
		createDirectories(directories, jout, manifestName);
		JarEntry ze = new JarEntry(manifestName);
		ZipUtil.setModifiedTime(ze, isReproducible() ? zipEntryConstantTime : lastModified());
		Resource r = new WriteResource() {

			@Override
			public void write(OutputStream out) throws Exception {
				writeManifest(out);
			}

			@Override
			public long lastModified() {
				return 0; // a manifest should not change the date
			}
		};
		putEntry(jout, ze, r);
	}


	@Override
	public void close() {
		super.close();
		manifest = null;
	}
	/**
	 * Cleanup the manifest for writing. Cleaning up consists of adding a space
	 * after any \n to prevent the manifest to see this newline as a delimiter.
	 *
	 * @param out Output
	 * @throws IOException
	 */

	public void writeManifest(OutputStream out) throws Exception {
		check();
		stripSignatures();
		writeManifest(getManifest(), out);
	}

	public static void writeManifest(Manifest manifest, OutputStream out) throws IOException {
		if (manifest == null)
			return;

		manifest = clean(manifest);
		outputManifest(manifest, out);
	}

	/**
	 * Main function to output a manifest properly in UTF-8.
	 *
	 * @param manifest The manifest to output
	 * @param out The output stream
	 * @throws IOException when something fails
	 */
	public static void outputManifest(Manifest manifest, OutputStream out) throws IOException {
		ManifestUtil.write(manifest, out);
	}

	private static Manifest clean(Manifest org) {
		Manifest result = new Manifest();
		Attributes mainAttributes = result.getMainAttributes();
		for (Map.Entry<?, ?> entry : org.getMainAttributes()
			.entrySet()) {
			String nice = clean((String) entry.getValue());
			mainAttributes.put(entry.getKey(), nice);
		}
		mainAttributes.putIfAbsent(Attributes.Name.MANIFEST_VERSION, "1.0");
		for (String name : org.getEntries()
			.keySet()) {
			Attributes attrs = result.getAttributes(name);
			if (attrs == null) {
				attrs = new Attributes();
				result.getEntries()
					.put(name, attrs);
			}

			for (Map.Entry<?, ?> entry : org.getAttributes(name)
				.entrySet()) {
				String nice = clean((String) entry.getValue());
				attrs.put(entry.getKey(), nice);
			}
		}
		return result;
	}


	/**
	 * Add all the resources in the given jar that match the given filter.
	 *
	 * @param sub the jar
	 * @param filter a pattern that should match the resoures in sub to be added
	 */
	public boolean addAll(Jar sub, Instruction filter) {
		return addAll(sub, filter, "");
	}

	/**
	 * Add all the resources in the given jar that match the given filter.
	 *
	 * @param sub the jar
	 * @param filter a pattern that should match the resoures in sub to be added
	 */
	public boolean addAll(Jar sub, Instruction filter, String destination) {
		check();
		boolean dupl = false;
		for (String name : sub.getResources()
			.keySet()) {
			if (manifestName.equals(name))
				continue;

			if (filter == null || filter.matches(name) ^ filter.isNegated())
				dupl |= putResource(Processor.appendPath(destination, name), sub.getResource(name), true);
		}
		return dupl;
	}

	public List<String> getPackages() {
		check();
		return MapStream.of(directories)
			.filterValue(mdir -> Objects.nonNull(mdir) && !mdir.isEmpty())
			.keys()
			.map(k -> k.replace('/', '.'))
			.collect(toList());
	}

	public boolean addAll(Jar src) {
		check();
		return addAll(src, null);
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

	public void calcChecksums(String[] algorithms) throws Exception {
		check();
		if (algorithms == null)
			algorithms = new String[] {
				"SHA1", "MD5"
			};

		Manifest m = getManifest();
		if (m == null) {
			m = new Manifest();
			setManifest(m);
		}

		MessageDigest[] digests = new MessageDigest[algorithms.length];
		int n = 0;
		for (String algorithm : algorithms)
			digests[n++] = MessageDigest.getInstance(algorithm);

		byte[] buffer = new byte[BUFFER_SIZE];

		for (Map.Entry<String, Resource> entry : resources.entrySet()) {
			String path = entry.getKey();
			// Skip the manifest
			if (path.equals(manifestName))
				continue;

			Attributes attributes = m.getAttributes(path);
			if (attributes == null) {
				attributes = new Attributes();
				getManifest().getEntries()
					.put(path, attributes);
			}
			Resource r = entry.getValue();
			ByteBuffer bb = r.buffer();
			if ((bb != null) && bb.hasArray()) {
				for (MessageDigest d : digests) {
					d.update(bb);
					bb.flip();
				}
			} else {
				try (InputStream in = r.openInputStream()) {
					for (int size; (size = in.read(buffer, 0, buffer.length)) > 0;) {
						for (MessageDigest d : digests) {
							d.update(buffer, 0, size);
						}
					}
				}
			}
			for (MessageDigest d : digests) {
				attributes.putValue(d.getAlgorithm() + "-Digest", Base64.encodeBase64(d.digest()));
				d.reset();
			}
		}
	}

	private final static Pattern BSN = Pattern.compile("\\s*([-.\\w]+)\\s*;?.*");

	/**
	 * Get the jar bsn from the {@link Constants#BUNDLE_SYMBOLICNAME} manifest
	 * header.
	 *
	 * @return null when the jar has no manifest, when the manifest has no
	 *         {@link Constants#BUNDLE_SYMBOLICNAME} header, or when the value
	 *         of the header is not a valid bsn according to {@link #BSN}.
	 * @throws Exception when the jar is closed or when the manifest could not
	 *             be retrieved.
	 */
	public String getBsn() throws Exception {
		return manifest().map(m -> m.getMainAttributes()
			.getValue(Constants.BUNDLE_SYMBOLICNAME))
			.map(s -> {
				Matcher matcher = BSN.matcher(s);
				return matcher.matches() ? matcher.group(1) : null;
			})
			.orElse(null);
	}

	/**
	 * Get the jar version from the {@link Constants#BUNDLE_VERSION} manifest
	 * header.
	 *
	 * @return null when the jar has no manifest or when the manifest has no
	 *         {@link Constants#BUNDLE_VERSION} header
	 * @throws Exception when the jar is closed or when the manifest could not
	 *             be retrieved.
	 */
	public String getVersion() throws Exception {
		return manifest().map(m -> m.getMainAttributes()
			.getValue(Constants.BUNDLE_VERSION))
			.map(String::trim)
			.orElse(null);
	}

	/**
	 * Make sure we have a manifest
	 *
	 * @throws Exception
	 */
	public void ensureManifest() throws Exception {
		if (!manifest().isPresent()) {
			manifest = Optional.of(new Manifest());
		}
	}

	/**
	 * Answer if the manifest was the first entry
	 */

	public boolean isManifestFirst() {
		return manifestFirst;
	}


	/**
	 * Return a data uri from the JAR. The data must be less than 32k
	 *
	 * @param path the path in the jar
	 * @param mime the mime type
	 * @return a URI or null if conversion could not take place
	 */

	public URI getDataURI(String path, String mime, int max) throws Exception {
		Resource r = getResource(path);

		if (r.size() >= max || r.size() <= 0)
			return null;

		byte[] data = new byte[(int) r.size()];
		try (DataInputStream din = new DataInputStream(r.openInputStream())) {
			din.readFully(data);
			String encoded = Base64.encodeBase64(data);
			return new URI("data:" + mime + ";base64," + encoded);
		}
	}

	public void setDigestAlgorithms(String[] algorithms) {
		this.algorithms = algorithms;
	}

	public byte[] getTimelessDigest() throws Exception {
		check();

		MessageDigest md = MessageDigest.getInstance("SHA1");
		OutputStream dout = new DigestOutputStream(IO.nullStream, md);
		// dout = System.out;

		Manifest m = getManifest();

		if (m != null) {
			Manifest m2 = new Manifest(m);
			Attributes main = m2.getMainAttributes();
			String lastmodified = (String) main.remove(new Attributes.Name(Constants.BND_LASTMODIFIED));
			String version = main.getValue(new Attributes.Name(Constants.BUNDLE_VERSION));
			if (version != null && Verifier.isVersion(version)) {
				Version v = new Version(version);
				main.putValue(Constants.BUNDLE_VERSION, v.toStringWithoutQualifier());
			}
			writeManifest(m2, dout);

			for (Map.Entry<String, Resource> entry : getResources().entrySet()) {
				String path = entry.getKey();
				if (path.equals(manifestName))
					continue;
				Resource resource = entry.getValue();
				dout.write(path.getBytes(UTF_8));
				resource.write(dout);
			}
		}
		return md.digest();
	}

	public void stripSignatures() {
		Map<String, Resource> metainf = getDirectory("META-INF");
		if (metainf != null) {
			List<String> signing = metainf.keySet()
				.stream()
				.filter(path -> METAINF_SIGNING_P.matcher(path)
					.matches())
				.collect(toList());
			for (String path : signing) {
				remove(path);
			}
		}
	}

	private static final Predicate<String> pomXmlFilter = new PathSet("META-INF/maven/*/*/pom.xml").matches();

	public Stream<Resource> getPomXmlResources() {
		return getResources(pomXmlFilter);
	}

	/**
	 * Make this jar calculate the SHA256 when it is saved as a file. When this
	 * JAR is written, the digest is always cleared. If this flag is on, it will
	 * be calculated and set when the file is successfully saved.
	 *
	 * @param onOrOff state of calculating the digest when writing this jar.
	 *            true is on, otherwise off
	 */

	public Jar setCalculateFileDigest(boolean onOrOff) {
		this.calculateFileDigest = onOrOff;
		return this;
	}

	/**
	 * Get the SHA256 digest of the last write operation when
	 * {@link #setCalculateFileDigest(boolean)} was on.
	 *
	 * @return the SHA 256 digest or empty
	 */

	public Optional<byte[]> getSHA256() {
		return Optional.ofNullable(sha256)
			.map(SHA256::digest);
	}

}
