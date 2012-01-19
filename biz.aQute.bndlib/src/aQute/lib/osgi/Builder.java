package aQute.lib.osgi;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.*;
import java.util.regex.*;
import java.util.zip.*;

import aQute.bnd.component.*;
import aQute.bnd.differ.*;
import aQute.bnd.differ.Baseline.Info;
import aQute.bnd.make.*;
import aQute.bnd.make.component.*;
import aQute.bnd.make.metatype.*;
import aQute.bnd.maven.*;
import aQute.bnd.service.*;
import aQute.bnd.service.diff.*;
import aQute.lib.collections.*;
import aQute.lib.osgi.Descriptors.PackageRef;
import aQute.lib.osgi.Descriptors.TypeRef;
import aQute.libg.generics.*;
import aQute.libg.header.*;

/**
 * Include-Resource: ( [name '=' ] file )+
 * 
 * Private-Package: package-decl ( ',' package-decl )*
 * 
 * Export-Package: package-decl ( ',' package-decl )*
 * 
 * Import-Package: package-decl ( ',' package-decl )*
 * 
 * @version $Revision: 1.27 $
 */
public class Builder extends Analyzer {
	private final DiffPluginImpl	differ				= new DiffPluginImpl();
	private Pattern					xdoNotCopy			= null;
	private static final int		SPLIT_MERGE_LAST	= 1;
	private static final int		SPLIT_MERGE_FIRST	= 2;
	private static final int		SPLIT_ERROR			= 3;
	private static final int		SPLIT_FIRST			= 4;
	private static final int		SPLIT_DEFAULT		= 0;
	private final List<File>		sourcePath			= new ArrayList<File>();
	private final Make				make				= new Make(this);

	public Builder(Processor parent) {
		super(parent);
	}

	public Builder() {
	}

	public Jar build() throws Exception {
		init();
		if (isTrue(getProperty(NOBUNDLES)))
			return null;

		if (getProperty(CONDUIT) != null)
			error("Specified " + CONDUIT
					+ " but calls build() instead of builds() (might be a programmer error");

		Jar dot = new Jar("dot");
		try {
			long modified = Long.parseLong(getProperty("base.modified"));
			dot.updateModified(modified, "Base modified");
		} catch (Exception e) {
			// Ignore
		}
		setJar(dot);

		doExpand(dot);
		doIncludeResources(dot);
		doWab(dot);

		// Check if we override the calculation of the
		// manifest. We still need to calculated it because
		// we need to have analyzed the classpath.

		Manifest manifest = calcManifest();

		String mf = getProperty(MANIFEST);
		if (mf != null) {
			File mff = getFile(mf);
			if (mff.isFile()) {
				try {
					InputStream in = new FileInputStream(mff);
					manifest = new Manifest(in);
					in.close();
				} catch (Exception e) {
					error(MANIFEST + " while reading manifest file", e);
				}
			} else {
				error(MANIFEST + ", no such file " + mf);
			}
		}

		if (getProperty(NOMANIFEST) == null)
			dot.setManifest(manifest);
		else
			dot.setDoNotTouchManifest();

		// This must happen after we analyzed so
		// we know what it is on the classpath
		addSources(dot);

		if (getProperty(POM) != null)
			dot.putResource("pom.xml", new PomResource(dot.getManifest()));

		if (!isNoBundle())
			doVerify(dot);

		if (dot.getResources().isEmpty())
			error("The JAR is empty: " + dot.getName());

		dot.updateModified(lastModified(), "Last Modified Processor");
		dot.setName(getBsn());

		sign(dot);

		doSaveManifest(dot);

		doDiff(dot); // check if need to diff this bundle
		doBaseline(dot); // check for a baseline
		return dot;
	}

	/**
	 * Allow any local initialization by subclasses before we build.
	 */
	public void init() throws Exception {
		begin();
		doRequireBnd();

		// Check if we have sensible setup

		if (getClasspath().size() == 0
				&& (getProperty(EXPORT_PACKAGE) != null || getProperty(EXPORT_PACKAGE) != null || getProperty(PRIVATE_PACKAGE) != null))
			warning("Classpath is empty. Private-Package and Export-Package can only expand from the classpath when there is one");

	}

	/**
	 * Turn this normal bundle in a web and add any resources.
	 * 
	 * @throws Exception
	 */
	private Jar doWab(Jar dot) throws Exception {
		String wab = getProperty(WAB);
		String wablib = getProperty(WABLIB);
		if (wab == null && wablib == null)
			return dot;

		setProperty(BUNDLE_CLASSPATH, append("WEB-INF/classes", getProperty(BUNDLE_CLASSPATH)));

		Set<String> paths = new HashSet<String>(dot.getResources().keySet());

		for (String path : paths) {
			if (path.indexOf('/') > 0 && !Character.isUpperCase(path.charAt(0))) {
				trace("wab: moving: %s", path);
				dot.rename(path, "WEB-INF/classes/" + path);
			}
		}

		Parameters clauses = parseHeader(getProperty(WABLIB));
		for (String key : clauses.keySet()) {
			File f = getFile(key);
			addWabLib(dot, f);
		}
		doIncludeResource(dot, wab);
		return dot;
	}

	/**
	 * Add a wab lib to the jar.
	 * 
	 * @param f
	 */
	private void addWabLib(Jar dot, File f) throws Exception {
		if (f.exists()) {
			Jar jar = new Jar(f);
			jar.setDoNotTouchManifest();
			addClose(jar);
			String path = "WEB-INF/lib/" + f.getName();
			dot.putResource(path, new JarResource(jar));
			setProperty(BUNDLE_CLASSPATH, append(getProperty(BUNDLE_CLASSPATH), path));

			Manifest m = jar.getManifest();
			String cp = m.getMainAttributes().getValue("Class-Path");
			if (cp != null) {
				Collection<String> parts = split(cp, ",");
				for (String part : parts) {
					File sub = getFile(f.getParentFile(), part);
					if (!sub.exists() || !sub.getParentFile().equals(f.getParentFile())) {
						warning("Invalid Class-Path entry %s in %s, must exist and must reside in same directory",
								sub, f);
					} else {
						addWabLib(dot, sub);
					}
				}
			}
		} else {
			error("WAB lib does not exist %s", f);
		}
	}

	/**
	 * Get the manifest and write it out separately if -savemanifest is set
	 * 
	 * @param dot
	 */
	private void doSaveManifest(Jar dot) throws Exception {
		String output = getProperty(SAVEMANIFEST);
		if (output == null)
			return;

		File f = getFile(output);
		if (f.isDirectory()) {
			f = new File(f, "MANIFEST.MF");
		}
		f.delete();
		f.getParentFile().mkdirs();
		OutputStream out = new FileOutputStream(f);
		try {
			Jar.writeManifest(dot.getManifest(), out);
		} finally {
			out.close();
		}
		changedFile(f);
	}

	protected void changedFile(File f) {
	}

	/**
	 * Sign the jar file.
	 * 
	 * -sign : <alias> [ ';' 'password:=' <password> ] [ ';' 'keystore:='
	 * <keystore> ] [ ';' 'sign-password:=' <pw> ] ( ',' ... )*
	 * 
	 * @return
	 */

	void sign(Jar jar) throws Exception {
		String signing = getProperty("-sign");
		if (signing == null)
			return;

		trace("Signing %s, with %s", getBsn(), signing);
		List<SignerPlugin> signers = getPlugins(SignerPlugin.class);

		Parameters infos = parseHeader(signing);
		for (Entry<String, Attrs> entry : infos.entrySet()) {
			for (SignerPlugin signer : signers) {
				signer.sign(this, entry.getKey());
			}
		}
	}

	public boolean hasSources() {
		return isTrue(getProperty(SOURCES));
	}

	/**
	 * Answer extra packages. In this case we implement conditional package. Any
	 */
	protected Jar getExtra() throws Exception {
		Parameters conditionals = getParameters(CONDITIONAL_PACKAGE);
		if (conditionals.isEmpty())
			return null;
		Instructions instructions = new Instructions(conditionals);

		Collection<PackageRef> referred = instructions.select(getReferred().keySet());
		referred.removeAll(getContained().keySet());

		Jar jar = new Jar("conditional-import");
		addClose(jar);
		for (PackageRef pref : referred) {
			for (Jar cpe : getClasspath()) {
				Map<String, Resource> map = cpe.getDirectories().get(pref.getPath());
				if (map != null) {
					jar.addDirectory(map, false);
					break;
				}
			}
		}
		if (jar.getDirectories().size() == 0)
			return null;
		return jar;
	}

	/**
	 * Intercept the call to analyze and cleanup versions after we have analyzed
	 * the setup. We do not want to cleanup if we are going to verify.
	 */

	public void analyze() throws Exception {
		super.analyze();
		cleanupVersion(getImports(), null);
		cleanupVersion(getExports(), getVersion());
		String version = getProperty(BUNDLE_VERSION);
		if (version != null) {
			version = cleanupVersion(version);
			if (version.endsWith(".SNAPSHOT")) {
				version = version.replaceAll("SNAPSHOT$", getProperty(SNAPSHOT, "SNAPSHOT"));
			}
			setProperty(BUNDLE_VERSION, version);
		}
	}

	public void cleanupVersion(Packages packages, String defaultVersion) {
		for (Map.Entry<PackageRef, Attrs> entry : packages.entrySet()) {
			Attrs attributes = entry.getValue();
			String v = attributes.get(Constants.VERSION_ATTRIBUTE);
			if (v == null && defaultVersion != null) {
				if (!isTrue(getProperty(Constants.NODEFAULTVERSION))) {
					v = defaultVersion;
					if (isPedantic())
						warning("Used bundle version %s for exported package %s", v, entry.getKey());
				} else {
					if (isPedantic())
						warning("No export version for exported package %s", entry.getKey());
				}
			}
			if (v != null)
				attributes.put(Constants.VERSION_ATTRIBUTE, cleanupVersion(v));
		}
	}

	/**
     * 
     */
	private void addSources(Jar dot) {
		if (!hasSources())
			return;

		Set<PackageRef> packages = Create.set();

		for (TypeRef typeRef : getClassspace().keySet()) {
			PackageRef packageRef = typeRef.getPackageRef();
			String sourcePath = typeRef.getSourcePath();
			String packagePath = packageRef.getPath();

			boolean found = false;
			String[] fixed = { "packageinfo", "package.html", "module-info.java",
					"package-info.java" };

			for (Iterator<File> i = getSourcePath().iterator(); i.hasNext();) {
				File root = i.next();

				// TODO should use bcp?

				File f = getFile(root, sourcePath);
				if (f.exists()) {
					found = true;
					if (!packages.contains(packageRef)) {
						packages.add(packageRef);
						File bdir = getFile(root, packagePath);
						for (int j = 0; j < fixed.length; j++) {
							File ff = getFile(bdir, fixed[j]);
							if (ff.isFile()) {
								String name = "OSGI-OPT/src/" + packagePath + fixed[j];
								dot.putResource(name, new FileResource(ff));
							}
						}
					}
					if (packageRef.isDefaultPackage())
						System.out.println("Duh?");
					dot.putResource("OSGI-OPT/src/" + sourcePath, new FileResource(f));
				}
			}
			if (!found) {
				for (Jar jar : getClasspath()) {
					Resource resource = jar.getResource(sourcePath);
					if (resource != null) {
						dot.putResource("OSGI-OPT/src/" + sourcePath, resource);
					} else {
						resource = jar.getResource("OSGI-OPT/src/" + sourcePath);
						if (resource != null) {
							dot.putResource("OSGI-OPT/src/" + sourcePath, resource);
						}
					}
				}
			}
			if (getSourcePath().isEmpty())
				warning("Including sources but " + SOURCEPATH
						+ " does not contain any source directories ");
			// TODO copy from the jars where they came from
		}
	}

	boolean			firstUse	= true;
	private Tree	tree;

	public Collection<File> getSourcePath() {
		if (firstUse) {
			firstUse = false;
			String sp = getProperty(SOURCEPATH);
			if (sp != null) {
				Parameters map = parseHeader(sp);
				for (Iterator<String> i = map.keySet().iterator(); i.hasNext();) {
					String file = i.next();
					if (!isDuplicate(file)) {
						File f = getFile(file);
						if (!f.isDirectory()) {
							error("Adding a sourcepath that is not a directory: " + f);
						} else {
							sourcePath.add(f);
						}
					}
				}
			}
		}
		return sourcePath;
	}

	private void doVerify(Jar dot) throws Exception {
		Verifier verifier = new Verifier(this);
		// Give the verifier the benefit of our analysis
		// prevents parsing the files twice
		verifier.verify();
		getInfo(verifier);
	}

	private void doExpand(Jar dot) throws IOException {

		// Build an index of the class path that we can then
		// use destructively
		MultiMap<String, Jar> packages = new MultiMap<String, Jar>();
		for (Jar srce : getClasspath()) {
			for (Entry<String, Map<String, Resource>> e : srce.getDirectories().entrySet()) {
				if (e.getValue() != null)
					packages.add(e.getKey(), srce);
			}
		}

		Parameters privatePackages = getPrivatePackage();
		if (isTrue(getProperty(Constants.UNDERTEST))) {
			String h = getProperty(Constants.TESTPACKAGES, "test;presence:=optional");
			privatePackages.putAll(parseHeader(h));
		}

		if (!privatePackages.isEmpty()) {
			Instructions privateFilter = new Instructions(privatePackages);
			Set<Instruction> unused = doExpand(dot, packages, privateFilter);

			if (!unused.isEmpty()) {
				warning("Unused Private-Package instructions, no such package(s) on the class path: %s",
						unused);
			}
		}

		Parameters exportedPackage = getExportPackage();
		if (!exportedPackage.isEmpty()) {
			Instructions exportedFilter = new Instructions(exportedPackage);

			// We ignore unused instructions for exports, they should show
			// up as errors during analysis. Otherwise any overlapping
			// packages with the private packages should show up as
			// unused

			doExpand(dot, packages, exportedFilter);
		}
	}

	/**
	 * Destructively filter the packages from the build up index. This index is
	 * used by the Export Package as well as the Private Package
	 * 
	 * @param jar
	 * @param name
	 * @param instructions
	 */
	private Set<Instruction> doExpand(Jar jar, MultiMap<String, Jar> index, Instructions filter) {
		Set<Instruction> unused = Create.set();

		for (Entry<Instruction, Attrs> e : filter.entrySet()) {
			Instruction instruction = e.getKey();
			if (instruction.isDuplicate())
				continue;

			Attrs directives = e.getValue();

			// We can optionally filter on the
			// source of the package. We assume
			// they all match but this can be overridden
			// on the instruction
			Instruction from = new Instruction(directives.get(FROM_DIRECTIVE, "*"));

			boolean used = false;

			for (Iterator<Entry<String, List<Jar>>> entry = index.entrySet().iterator(); entry
					.hasNext();) {
				Entry<String, List<Jar>> p = entry.next();

				String directory = p.getKey();
				PackageRef packageRef = getPackageRef(directory);

				// Skip * and meta data, we're talking packages!
				if (packageRef.isMetaData() && instruction.isAny())
					continue;

				if (!instruction.matches(packageRef.getFQN()))
					continue;

				// Ensure it is never matched again
				entry.remove();

				// ! effectively removes it from consideration by others (this
				// includes exports)
				if (instruction.isNegated())
					continue;

				// Do the from: directive, filters on the JAR type
				List<Jar> providers = filterFrom(from, p.getValue());
				if (providers.isEmpty())
					continue;

				int splitStrategy = getSplitStrategy(directives.get(SPLIT_PACKAGE_DIRECTIVE));
				copyPackage(jar, providers, directory, splitStrategy);

				used = true;
			}

			if (!used && !isTrue(directives.get("optional:")))
				unused.add(instruction);
		}
		return unused;
	}

	/**
	 * @param from
	 * @return
	 */
	private List<Jar> filterFrom(Instruction from, List<Jar> providers) {
		if (from.isAny())
			return providers;

		List<Jar> np = new ArrayList<Jar>();
		for (Iterator<Jar> i = providers.iterator(); i.hasNext();) {
			Jar j = i.next();
			if (from.matches(j.getName())) {
				np.add(j);
			}
		}
		return np;
	}

	/**
	 * Copy the package from the providers based on the split package strategy.
	 * 
	 * @param dest
	 * @param providers
	 * @param directory
	 * @param splitStrategy
	 */
	private void copyPackage(Jar dest, List<Jar> providers, String path, int splitStrategy) {
		switch (splitStrategy) {
		case SPLIT_MERGE_LAST:
			for (Jar srce : providers) {
				copy(dest, srce, path, true);
			}
			break;

		case SPLIT_MERGE_FIRST:
			for (Jar srce : providers) {
				copy(dest, srce, path, false);
			}
			break;

		case SPLIT_ERROR:
			error(diagnostic(path, providers));
			break;

		case SPLIT_FIRST:
			copy(dest, providers.get(0), path, false);
			break;

		default:
			if (providers.size() > 1)
				warning("%s", diagnostic(path, providers));
			for (Jar srce : providers) {
				copy(dest, srce, path, false);
			}
			break;
		}
	}

	/**
	 * Cop
	 * 
	 * @param dest
	 * @param srce
	 * @param path
	 * @param overwriteResource
	 */
	private void copy(Jar dest, Jar srce, String path, boolean overwrite) {
		dest.copy(srce, path, overwrite);

		String key = path + "/bnd.info";
		Resource r = dest.getResource(key);
		if (r != null)
			dest.putResource(key, new PreprocessResource(this, r));

		if (hasSources()) {
			String srcPath = "OSGI-OPT/src/" + path;
			Map<String, Resource> srcContents = srce.getDirectories().get(srcPath);
			if (srcContents != null) {
				dest.addDirectory(srcContents, overwrite);
			}
		}
	}

	/**
	 * Analyze the classpath for a split package
	 * 
	 * @param pack
	 * @param classpath
	 * @param source
	 * @return
	 */
	private String diagnostic(String pack, List<Jar> culprits) {
		// Default is like merge-first, but with a warning
		return "Split package, multiple jars provide the same package:"
				+ pack
				+ "\nUse Import/Export Package directive -split-package:=(merge-first|merge-last|error|first) to get rid of this warning\n"
				+ "Package found in   " + culprits + "\n" //
				+ "Class path         " + getClasspath();
	}

	private int getSplitStrategy(String type) {
		if (type == null)
			return SPLIT_DEFAULT;

		if (type.equals("merge-last"))
			return SPLIT_MERGE_LAST;

		if (type.equals("merge-first"))
			return SPLIT_MERGE_FIRST;

		if (type.equals("error"))
			return SPLIT_ERROR;

		if (type.equals("first"))
			return SPLIT_FIRST;

		error("Invalid strategy for split-package: " + type);
		return SPLIT_DEFAULT;
	}

	/**
	 * Matches the instructions against a package.
	 * 
	 * @param instructions
	 *            The list of instructions
	 * @param pack
	 *            The name of the package
	 * @param unused
	 *            The total list of patterns, matched patterns are removed
	 * @param source
	 *            The name of the source container, can be filtered upon with
	 *            the from: directive.
	 * @return
	 */
	private Instruction matches(Instructions instructions, String pack, Set<Instruction> unused,
			String source) {
		for (Entry<Instruction, Attrs> entry : instructions.entrySet()) {
			Instruction pattern = entry.getKey();

			// It is possible to filter on the source of the
			// package with the from: directive. This is an
			// instruction that must match the name of the
			// source class path entry.

			String from = entry.getValue().get(FROM_DIRECTIVE);
			if (from != null) {
				Instruction f = new Instruction(from);
				if (!f.matches(source) || f.isNegated())
					continue;
			}

			// Now do the normal
			// matching
			if (pattern.matches(pack)) {
				if (unused != null)
					unused.remove(pattern);
				return pattern;
			}
		}
		return null;
	}

	/**
	 * Parse the Bundle-Includes header. Files in the bundles Include header are
	 * included in the jar. The source can be a directory or a file.
	 * 
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private void doIncludeResources(Jar jar) throws Exception {
		String includes = getProperty("Bundle-Includes");
		if (includes == null) {
			includes = getProperty(INCLUDERESOURCE);
			if (includes == null || includes.length() == 0)
				includes = getProperty("Include-Resource");
		} else
			warning("Please use -includeresource instead of Bundle-Includes");

		doIncludeResource(jar, includes);

	}

	private void doIncludeResource(Jar jar, String includes) throws Exception {
		Parameters clauses = parseHeader(includes);
		doIncludeResource(jar, clauses);
	}

	private void doIncludeResource(Jar jar, Parameters clauses) throws ZipException, IOException,
			Exception {
		for (Entry<String, Attrs> entry : clauses.entrySet()) {
			doIncludeResource(jar, entry.getKey(), entry.getValue());
		}
	}

	private void doIncludeResource(Jar jar, String name, Map<String, String> extra)
			throws ZipException, IOException, Exception {
		boolean preprocess = false;
		if (name.startsWith("{") && name.endsWith("}")) {
			preprocess = true;
			name = name.substring(1, name.length() - 1).trim();
		}

		String parts[] = name.split("\\s*=\\s*");
		String source = parts[0];
		String destination = parts[0];
		if (parts.length == 2)
			source = parts[1];

		if (source.startsWith("@")) {
			extractFromJar(jar, source.substring(1), parts.length == 1 ? "" : destination);
		} else if (extra.containsKey("literal")) {
			String literal = (String) extra.get("literal");
			Resource r = new EmbeddedResource(literal.getBytes("UTF-8"), 0);
			String x = (String) extra.get("extra");
			if (x != null)
				r.setExtra(x);
			jar.putResource(name, r);
		} else {
			File sourceFile;
			String destinationPath;

			sourceFile = getFile(source);
			if (parts.length == 1) {
				// Directories should be copied to the root
				// but files to their file name ...
				if (sourceFile.isDirectory())
					destinationPath = "";
				else
					destinationPath = sourceFile.getName();
			} else {
				destinationPath = parts[0];
			}
			// Handle directories
			if (sourceFile.isDirectory()) {
				destinationPath = doResourceDirectory(jar, extra, preprocess, sourceFile,
						destinationPath);
				return;
			}

			// destinationPath = checkDestinationPath(destinationPath);

			if (!sourceFile.exists()) {
				noSuchFile(jar, name, extra, source, destinationPath);
			} else
				copy(jar, destinationPath, sourceFile, preprocess, extra);
		}
	}

	private String doResourceDirectory(Jar jar, Map<String, String> extra, boolean preprocess,
			File sourceFile, String destinationPath) throws Exception {
		String filter = extra.get("filter:");
		boolean flatten = isTrue(extra.get("flatten:"));
		boolean recursive = true;
		String directive = extra.get("recursive:");
		if (directive != null) {
			recursive = isTrue(directive);
		}

		Instruction.Filter iFilter = null;
		if (filter != null) {
			iFilter = new Instruction.Filter(new Instruction(filter), recursive, getDoNotCopy());
		} else {
			iFilter = new Instruction.Filter(null, recursive, getDoNotCopy());
		}

		Map<String, File> files = newMap();
		resolveFiles(sourceFile, iFilter, recursive, destinationPath, files, flatten);

		for (Map.Entry<String, File> entry : files.entrySet()) {
			copy(jar, entry.getKey(), entry.getValue(), preprocess, extra);
		}
		return destinationPath;
	}

	private void resolveFiles(File dir, FileFilter filter, boolean recursive, String path,
			Map<String, File> files, boolean flatten) {

		if (doNotCopy(dir.getName())) {
			return;
		}

		File[] fs = dir.listFiles(filter);
		for (File file : fs) {
			if (file.isDirectory()) {
				if (recursive) {
					String nextPath;
					if (flatten)
						nextPath = path;
					else
						nextPath = appendPath(path, file.getName());

					resolveFiles(file, filter, recursive, nextPath, files, flatten);
				}
				// Directories are ignored otherwise
			} else {
				String p = appendPath(path, file.getName());
				if (files.containsKey(p))
					warning("Include-Resource overwrites entry %s from file %s", p, file);
				files.put(p, file);
			}
		}
	}

	private void noSuchFile(Jar jar, String clause, Map<String, String> extra, String source,
			String destinationPath) throws Exception {
		Jar src = getJarFromName(source, "Include-Resource " + source);
		if (src != null) {
			JarResource jarResource = new JarResource(src);
			jar.putResource(destinationPath, jarResource);
		} else {
			Resource lastChance = make.process(source);
			if (lastChance != null) {
				String x = extra.get("extra");
				if (x != null)
					lastChance.setExtra(x);
				jar.putResource(destinationPath, lastChance);
			} else
				error("Input file does not exist: " + source);
		}
	}

	/**
	 * Extra resources from a Jar and add them to the given jar. The clause is
	 * the
	 * 
	 * @param jar
	 * @param clauses
	 * @param i
	 * @throws ZipException
	 * @throws IOException
	 */
	private void extractFromJar(Jar jar, String source, String destination) throws ZipException,
			IOException {
		// Inline all resources and classes from another jar
		// optionally appended with a modified regular expression
		// like @zip.jar!/META-INF/MANIFEST.MF
		int n = source.lastIndexOf("!/");
		Instruction instr = null;
		if (n > 0) {
			instr = new Instruction(source.substring(n + 2));
			source = source.substring(0, n);
		}

		// Pattern filter = null;
		// if (n > 0) {
		// String fstring = source.substring(n + 2);
		// source = source.substring(0, n);
		// filter = wildcard(fstring);
		// }
		Jar sub = getJarFromName(source, "extract from jar");
		if (sub == null)
			error("Can not find JAR file " + source);
		else {
			addAll(jar, sub, instr, destination);
		}
	}

	/**
	 * Add all the resources in the given jar that match the given filter.
	 * 
	 * @param sub
	 *            the jar
	 * @param filter
	 *            a pattern that should match the resoures in sub to be added
	 */
	public boolean addAll(Jar to, Jar sub, Instruction filter) {
		return addAll(to, sub, filter, "");
	}

	/**
	 * Add all the resources in the given jar that match the given filter.
	 * 
	 * @param sub
	 *            the jar
	 * @param filter
	 *            a pattern that should match the resoures in sub to be added
	 */
	public boolean addAll(Jar to, Jar sub, Instruction filter, String destination) {
		boolean dupl = false;
		for (String name : sub.getResources().keySet()) {
			if ("META-INF/MANIFEST.MF".equals(name))
				continue;

			if (filter == null || filter.matches(name) != filter.isNegated())
				dupl |= to.putResource(Processor.appendPath(destination, name),
						sub.getResource(name), true);
		}
		return dupl;
	}

	private void copy(Jar jar, String path, File from, boolean preprocess, Map<String, String> extra)
			throws Exception {
		if (doNotCopy(from.getName()))
			return;

		if (from.isDirectory()) {

			File files[] = from.listFiles();
			for (int i = 0; i < files.length; i++) {
				copy(jar, appendPath(path, files[i].getName()), files[i], preprocess, extra);
			}
		} else {
			if (from.exists()) {
				Resource resource = new FileResource(from);
				if (preprocess) {
					resource = new PreprocessResource(this, resource);
				}
				String x = extra.get("extra");
				if (x != null)
					resource.setExtra(x);
				if (path.endsWith("/"))
					path = path + from.getName();
				jar.putResource(path, resource);

				if (isTrue(extra.get(LIB_DIRECTIVE))) {
					setProperty(BUNDLE_CLASSPATH, append(getProperty(BUNDLE_CLASSPATH), path));
				}
			} else {
				error("Input file does not exist: " + from);
			}
		}
	}

	public void setSourcepath(File[] files) {
		for (int i = 0; i < files.length; i++)
			addSourcepath(files[i]);
	}

	public void addSourcepath(File cp) {
		if (!cp.exists())
			warning("File on sourcepath that does not exist: " + cp);

		sourcePath.add(cp);
	}

	public void close() {
		super.close();
	}

	/**
	 * Build Multiple jars. If the -sub command is set, we filter the file with
	 * the given patterns.
	 * 
	 * @return
	 * @throws Exception
	 */
	public Jar[] builds() throws Exception {
		begin();

		// Are we acting as a conduit for another JAR?
		String conduit = getProperty(CONDUIT);
		if (conduit != null) {
			Parameters map = parseHeader(conduit);
			Jar[] result = new Jar[map.size()];
			int n = 0;
			for (String file : map.keySet()) {
				Jar c = new Jar(getFile(file));
				addClose(c);
				String name = map.get(file).get("name");
				if (name != null)
					c.setName(name);

				result[n++] = c;
			}
			return result;
		}

		List<Jar> result = new ArrayList<Jar>();
		List<Builder> builders;

		builders = getSubBuilders();

		for (Builder builder : builders) {
			try {
				Jar jar = builder.build();
				jar.setName(builder.getBsn());
				result.add(jar);
			} catch (Exception e) {
				e.printStackTrace();
				error("Sub Building " + builder.getBsn(), e);
			}
			if (builder != this)
				getInfo(builder, builder.getBsn() + ": ");
		}
		return result.toArray(new Jar[result.size()]);
	}

	/**
	 * Answer a list of builders that represent this file or a list of files
	 * specified in -sub. This list can be empty. These builders represents to
	 * be created artifacts and are each scoped to such an artifacts. The
	 * builders can be used to build the bundles or they can be used to find out
	 * information about the to be generated bundles.
	 * 
	 * @return List of 0..n builders representing artifacts.
	 * @throws Exception
	 */
	public List<Builder> getSubBuilders() throws Exception {
		String sub = (String) getProperty(SUB);
		if (sub == null || sub.trim().length() == 0 || EMPTY_HEADER.equals(sub))
			return Arrays.asList(this);

		List<Builder> builders = new ArrayList<Builder>();
		if (isTrue(getProperty(NOBUNDLES)))
			return builders;

		Parameters subsMap = parseHeader(sub);
		for (Iterator<String> i = subsMap.keySet().iterator(); i.hasNext();) {
			File file = getFile(i.next());
			if (file.isFile()) {
				builders.add(getSubBuilder(file));
				i.remove();
			}
		}

		Instructions instructions = new Instructions(subsMap);

		List<File> members = new ArrayList<File>(Arrays.asList(getBase().listFiles()));

		nextFile: while (members.size() > 0) {

			File file = members.remove(0);

			// Check if the file is one of our parents
			Processor p = this;
			while (p != null) {
				if (file.equals(p.getPropertiesFile()))
					continue nextFile;
				p = p.getParent();
			}

			for (Iterator<Instruction> i = instructions.keySet().iterator(); i.hasNext();) {

				Instruction instruction = i.next();
				if (instruction.matches(file.getName())) {

					if (!instruction.isNegated()) {
						builders.add(getSubBuilder(file));
					}

					// Because we matched (even though we could be negated)
					// we skip any remaining searches
					continue nextFile;
				}
			}
		}
		return builders;
	}

	public Builder getSubBuilder(File file) throws Exception {
		Builder builder = getSubBuilder();
		if (builder != null) {
			builder.setProperties(file);
			addClose(builder);
		}
		return builder;
	}

	public Builder getSubBuilder() throws Exception {
		Builder builder = new Builder(this);
		builder.setBase(getBase());

		for (Jar file : getClasspath()) {
			builder.addClasspath(file);
		}

		return builder;
	}

	/**
	 * A macro to convert a maven version to an OSGi version
	 */

	public String _maven_version(String args[]) {
		if (args.length > 2)
			error("${maven_version} macro receives too many arguments " + Arrays.toString(args));
		else if (args.length < 2)
			error("${maven_version} macro has no arguments, use ${maven_version;1.2.3-SNAPSHOT}");
		else {
			return cleanupVersion(args[1]);
		}
		return null;
	}

	public String _permissions(String args[]) throws IOException {
		StringBuilder sb = new StringBuilder();

		for (String arg : args) {
			if ("packages".equals(arg) || "all".equals(arg)) {
				for (PackageRef imp : getImports().keySet()) {
					if (!imp.isJava()) {
						sb.append("(org.osgi.framework.PackagePermission \"");
						sb.append(imp);
						sb.append("\" \"import\")\r\n");
					}
				}
				for (PackageRef exp : getExports().keySet()) {
					sb.append("(org.osgi.framework.PackagePermission \"");
					sb.append(exp);
					sb.append("\" \"export\")\r\n");
				}
			} else if ("admin".equals(arg) || "all".equals(arg)) {
				sb.append("(org.osgi.framework.AdminPermission)");
			} else if ("permissions".equals(arg))
				;
			else
				error("Invalid option in ${permissions}: %s", arg);
		}
		return sb.toString();
	}

	/**
     * 
     */
	public void removeBundleSpecificHeaders() {
		Set<String> set = new HashSet<String>(Arrays.asList(BUNDLE_SPECIFIC_HEADERS));
		setForceLocal(set);
	}

	/**
	 * Check if the given resource is in scope of this bundle. That is, it
	 * checks if the Include-Resource includes this resource or if it is a class
	 * file it is on the class path and the Export-Pacakge or Private-Package
	 * include this resource.
	 * 
	 * For now, include resources are skipped.
	 * 
	 * @param f
	 * @return
	 */
	public boolean isInScope(Collection<File> resources) throws Exception {
		Parameters clauses = parseHeader(getProperty(Constants.EXPORT_PACKAGE));
		clauses.putAll(parseHeader(getProperty(Constants.PRIVATE_PACKAGE)));
		if (isTrue(getProperty(Constants.UNDERTEST))) {
			clauses.putAll(parseHeader(getProperty(Constants.TESTPACKAGES,
					"test;presence:=optional")));
		}
		Instructions instructions = new Instructions(clauses);

		for (File r : resources) {
			String cpEntry = getClasspathEntrySuffix(r);
			if (cpEntry != null) {
				String pack = Descriptors.getPackage(cpEntry);
				Instruction i = matches(instructions, pack, null, r.getName());
				if (i != null)
					return !i.isNegated();
			}
		}
		return false;
	}

	/**
	 * Answer the string of the resource that it has in the container.
	 * 
	 * @param resource
	 *            The resource to look for
	 * @return
	 * @throws Exception
	 */
	public String getClasspathEntrySuffix(File resource) throws Exception {
		for (Jar jar : getClasspath()) {
			File source = jar.getSource();
			if (source != null) {
				source = source.getCanonicalFile();
				String sourcePath = source.getAbsolutePath();
				String resourcePath = resource.getAbsolutePath();

				if (resourcePath.startsWith(sourcePath)) {
					// Make sure that the path name is translated correctly
					// i.e. on Windows the \ must be translated to /
					String filePath = resourcePath.substring(sourcePath.length() + 1);

					return filePath.replace(File.separatorChar, '/');
				}
			}
		}
		return null;
	}

	/**
	 * doNotCopy
	 * 
	 * The doNotCopy variable maintains a patter for files that should not be
	 * copied. There is a default {@link #DEFAULT_DO_NOT_COPY} but this ca be
	 * overridden with the {@link Constants#DONOTCOPY} property.
	 */

	public boolean doNotCopy(String v) {
		return getDoNotCopy().matcher(v).matches();
	}

	public Pattern getDoNotCopy() {
		if (xdoNotCopy == null) {
			String string = null;
			try {
				string = getProperty(DONOTCOPY, DEFAULT_DO_NOT_COPY);
				xdoNotCopy = Pattern.compile(string);
			} catch (Exception e) {
				error("Invalid value for %s, value is %s", DONOTCOPY, string);
				xdoNotCopy = Pattern.compile(DEFAULT_DO_NOT_COPY);
			}
		}
		return xdoNotCopy;
	}

	/**
	 */

	static MakeBnd			makeBnd				= new MakeBnd();
	static MakeCopy			makeCopy			= new MakeCopy();
	static ServiceComponent	serviceComponent	= new ServiceComponent();
	static DSAnnotations	dsAnnotations		= new DSAnnotations();
	static MetatypePlugin	metatypePlugin		= new MetatypePlugin();

	@Override protected void setTypeSpecificPlugins(Set<Object> list) {
		list.add(makeBnd);
		list.add(makeCopy);
		list.add(serviceComponent);
		list.add(dsAnnotations);
		list.add(metatypePlugin);
		super.setTypeSpecificPlugins(list);
	}

	/**
	 * Diff this bundle to another bundle for the given packages.
	 * 
	 * @throws Exception
	 */

	public void doDiff(Jar dot) throws Exception {
		Parameters diffs = parseHeader(getProperty("-diff"));
		if (diffs.isEmpty())
			return;

		trace("diff %s", diffs);

		if (tree == null)
			tree = differ.tree(this);

		for (Entry<String, Attrs> entry : diffs.entrySet()) {
			String path = entry.getKey();
			File file = getFile(path);
			if (!file.isFile()) {
				error("Diffing against %s that is not a file", file);
				continue;
			}

			boolean full = entry.getValue().get("--full") != null;
			boolean warning = entry.getValue().get("--warning") != null;

			Tree other = differ.tree(file);
			Diff api = tree.diff(other).get("<api>");
			Instructions instructions = new Instructions(entry.getValue().get("--pack"));

			trace("diff against %s --full=%s --pack=%s --warning=%s", file, full, instructions);
			for (Diff p : api.getChildren()) {
				String pname = p.getName();
				if (p.getType() == Type.PACKAGE && instructions.matches(pname)) {
					if (p.getDelta() != Delta.UNCHANGED) {

						if (!full)
							if (warning)
								warning("Differ %s", p);
							else
								error("Differ %s", p);
						else {
							if (warning)
								warning("Diff found a difference in %s for packages %s", file,
										instructions);
							else
								error("Diff found a difference in %s for packages %s", file,
										instructions);
							show(p, "", warning);
						}
					}
				}
			}
		}
	}

	/**
	 * Show the diff recursively
	 * 
	 * @param p
	 * @param i
	 */
	private void show(Diff p, String indent, boolean warning) {
		Delta d = p.getDelta();
		if (d == Delta.UNCHANGED)
			return;

		if (warning)
			warning("%s%s", indent, p);
		else
			error("%s%s", indent, p);

		indent = indent + " ";
		switch (d) {
		case CHANGED:
		case MAJOR:
		case MINOR:
		case MICRO:
			break;

		default:
			return;
		}
		for (Diff c : p.getChildren())
			show(c, indent, warning);
	}

	/**
	 * Base line against a previous version
	 * 
	 * @throws Exception
	 */

	private void doBaseline(Jar dot) throws Exception {
		Parameters diffs = parseHeader(getProperty("-baseline"));
		if (diffs.isEmpty())
			return;

		System.out.printf("baseline %s\n", diffs);

		Baseline baseline = new Baseline(this, differ);

		for (Entry<String, Attrs> entry : diffs.entrySet()) {
			String path = entry.getKey();
			File file = getFile(path);
			if (!file.isFile()) {
				error("Diffing against %s that is not a file", file);
				continue;
			}
			Jar other = new Jar(file);
			Set<Info> infos = baseline.baseline(dot, other, null);
			for (Info info : infos) {
				if (info.mismatch) {
					error("%s %-50s %-10s %-10s %-10s %-10s %-10s\n", info.mismatch ? '*' : ' ',
							info.packageName, info.packageDiff.getDelta(), info.newerVersion,
							info.olderVersion, info.suggestedVersion,
							info.suggestedIfProviders == null ? "-" : info.suggestedIfProviders);
				}
			}
		}
	}

	public void addSourcepath(Collection<File> sourcepath) {
		for (File f : sourcepath) {
			addSourcepath(f);
		}
	}

}
