package aQute.bnd.osgi;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.apiguardian.APIGuardianAnnotations;
import aQute.bnd.cdi.CDIAnnotations;
import aQute.bnd.component.DSAnnotations;
import aQute.bnd.differ.DiffPluginImpl;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.help.instructions.BuilderInstructions;
import aQute.bnd.make.Make;
import aQute.bnd.make.MakeBnd;
import aQute.bnd.make.MakeCopy;
import aQute.bnd.make.component.ServiceComponent;
import aQute.bnd.maven.PomPropertiesResource;
import aQute.bnd.maven.PomResource;
import aQute.bnd.metatype.MetatypeAnnotations;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.plugin.jpms.JPMSAnnotations;
import aQute.bnd.plugin.jpms.JPMSModuleInfoPlugin;
import aQute.bnd.plugin.spi.SPIDescriptorGenerator;
import aQute.bnd.service.SignerPlugin;
import aQute.bnd.service.diff.Delta;
import aQute.bnd.service.diff.Diff;
import aQute.bnd.service.diff.Tree;
import aQute.bnd.service.diff.Type;
import aQute.bnd.service.specifications.BuilderSpecification;
import aQute.bnd.stream.MapStream;
import aQute.bnd.version.Version;
import aQute.lib.collections.MultiMap;
import aQute.lib.hex.Hex;
import aQute.lib.io.IO;
import aQute.lib.regex.PatternConstants;
import aQute.lib.strings.Strings;
import aQute.libg.generics.Create;

/**
 * Include-Resource: ( [name '=' ] file )+ Private-Package: package-decl ( ','
 * package-decl )* Export-Package: package-decl ( ',' package-decl )*
 * Import-Package: package-decl ( ',' package-decl )* @version $Revision: 1.27 $
 */
public class Builder extends Analyzer {
	private final static Logger		logger						= LoggerFactory.getLogger(Builder.class);
	private final static Pattern	IR_PATTERN					= Pattern.compile("[{]?-?@?(?:[^=]+=)?\\s*([^}!]+).*");
	private final DiffPluginImpl	differ						= new DiffPluginImpl();
	private Pattern					xdoNotCopy					= null;
	private static final int		SPLIT_MERGE_LAST			= 1;
	private static final int		SPLIT_MERGE_FIRST			= 2;
	private static final int		SPLIT_ERROR					= 3;
	private static final int		SPLIT_FIRST					= 4;
	private static final int		SPLIT_DEFAULT				= 0;
	private final List<File>		sourcePath					= new ArrayList<>();
	private final Make				make						= new Make(this);
	private Instructions			defaultPreProcessMatcher	= null;
	private BuilderInstructions		buildInstrs					= getInstructions(BuilderInstructions.class);

	public Builder(Processor parent) {
		super(parent);
	}

	public Builder() {}

	public Jar build() throws Exception {
		logger.debug("build");
		init();
		if (isTrue(getProperty(NOBUNDLES)))
			return null;

		if (getProperty(CONDUIT) != null)
			error("Specified " + CONDUIT + " but calls build() instead of builds() (might be a programmer error");

		Jar dot = getBuildJar();

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
				updateModified(mff.lastModified(), "Manifest " + mff);
				try (InputStream in = IO.stream(mff)) {
					manifest = new Manifest(in);
				} catch (Exception e) {
					exception(e, "%s: exception while reading manifest file", MANIFEST);
				}
			} else {
				error("%s: no such file %s", MANIFEST, mf);
			}
		}

		if (!isTrue(getProperty(NOMANIFEST))) {
			dot.setManifest(manifest);
			String manifestName = getProperty(MANIFEST_NAME);
			if (manifestName != null)
				dot.setManifestName(manifestName);
		} else {
			dot.setDoNotTouchManifest();
		}

		// This must happen after we analyzed so
		// we know what it is on the classpath
		addSources(dot);

		doPom(dot);

		if (!isNoBundle())
			doVerify(dot);

		Map<String, Resource> resources = dot.getResources();
		if (resources.isEmpty() || ((resources.size() == 1) && resources.get("module-info.class") != null))
			warning(
				"The JAR is empty: The instructions for the JAR named %s did not cause any content to be included, this is likely wrong",
				getBsn());

		dot.updateModified(lastModified(), "Last Modified Processor");
		dot.setName(getBsn());

		doDigests(dot);

		sign(dot);
		doSaveManifest(dot);

		doDiff(dot); // check if need to diff this bundle
		doBaseline(dot); // check for a baseline

		String expand = getProperty("-expand");
		if (expand != null) {
			File out = getFile(expand);
			IO.mkdirs(out);
			dot.expand(out);
		}
		return dot;
	}

	private Jar getBuildJar() {
		Jar dot = getJar();
		if (dot == null) {
			dot = new Jar("dot");

			buildInstrs.compression()
				.ifPresent(dot::setCompression);

			dot.setReproducible(is(REPRODUCIBLE));
			setJar(dot);
		}
		try {
			long modified = Long.parseLong(getProperty("base.modified"));
			dot.updateModified(modified, "Base modified");
		} catch (Exception e) {
			// Ignore
		}
		return dot;
	}

	void doPom(Jar dot) throws Exception, IOException {
		try (Processor scoped = new Processor(this)) {
			String bsn = getBsn();
			if (bsn != null)
				scoped.setProperty("@bsn", bsn);
			String version = getBundleVersion();
			if (version != null)
				scoped.setProperty("@version", version);
			String pom = scoped.getProperty(POM);
			if (isTrue(pom)) {
				dot.removeSubDirs("META-INF/maven/");
				scoped.addProperties(OSGiHeader.parseProperties(pom));
				PomResource pomXml = new PomResource(scoped, dot.getManifest());
				String v = pomXml.validate();
				if (v != null) {
					error("Invalid pom for %s: %s", getBundleSymbolicName(), v);
				}
				PomPropertiesResource pomProperties = new PomPropertiesResource(pomXml);
				dot.putResource(pomXml.getWhere(), pomXml);
				if (!pomProperties.getWhere()
					.equals(pomXml.getWhere())) {
					dot.putResource(pomProperties.getWhere(), pomProperties);
				}
			}
		}
	}

	/**
	 * Check if we need to calculate any checksums.
	 *
	 * @param dot
	 * @throws Exception
	 */
	private void doDigests(Jar dot) throws Exception {
		Parameters ps = OSGiHeader.parseHeader(getProperty(DIGESTS));
		if (ps.isEmpty())
			return;
		logger.debug("digests {}", ps);
		String[] digests = ps.keySet()
			.toArray(new String[0]);
		dot.setDigestAlgorithms(digests);
	}

	/**
	 * Allow any local initialization by subclasses before we build.
	 */
	public void init() throws Exception {
		begin();
		doRequireBnd();

		// Check if we have sensible setup

		if (getClasspath().isEmpty() && (getProperty(EXPORT_PACKAGE) != null || getProperty(PRIVATE_PACKAGE) != null
			|| getProperty(PRIVATEPACKAGE) != null))
			warning("Classpath is empty. " + Constants.PRIVATE_PACKAGE + ", " + Constants.PRIVATEPACKAGE + ", and "
				+ EXPORT_PACKAGE + " can only expand from the classpath when there is one");

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

		logger.debug("wab {} {}", wab, wablib);
		setBundleClasspath(append("WEB-INF/classes", getProperty(BUNDLE_CLASSPATH)));

		dot.getResources()
			.keySet()
			.stream()
			.filter(path -> !pathStartsWith(path, "WEB-INF") && Arrays.stream(Constants.METAPACKAGES)
				.noneMatch(meta -> pathStartsWith(path, meta)))
			// we collect since we need to mutate the source set
			.collect(toList())
			.forEach(path -> {
				logger.debug("wab: moving: {}", path);
				dot.rename(path, "WEB-INF/classes/" + path);
			});

		Parameters clauses = parseHeader(getProperty(WABLIB));
		for (Map.Entry<String, Attrs> entry : clauses.entrySet()) {
			File f = getFile(entry.getKey());
			addWabLib(dot, f, entry.getKey(), entry.getValue());
		}
		doIncludeResource(dot, wab);
		return dot;
	}

	private static boolean pathStartsWith(String path, String prefix) {
		return path.startsWith(prefix) && ((path.length() == prefix.length()) || (path.charAt(prefix.length()) == '/'));
	}

	/**
	 * Add a wab lib to the jar.
	 *
	 * @param f
	 */
	private void addWabLib(Jar dot, File f, String name, Map<String, String> attrs) throws Exception {
		if (f.exists()) {
			Jar jar = new Jar(f);
			jar.setDoNotTouchManifest();
			buildInstrs.compression()
				.ifPresent(jar::setCompression);

			addClose(jar);
			String path = "WEB-INF/lib/" + f.getName();
			dot.putResource(path, new JarResource(jar));
			setProperty(BUNDLE_CLASSPATH, append(getProperty(BUNDLE_CLASSPATH), path));

			Manifest m = jar.getManifest();
			if (m != null) {
				String cp = m.getMainAttributes()
					.getValue("Class-Path");
				if (cp != null) {
					Collection<String> parts = split(cp);
					for (String part : parts) {
						File sub = getFile(f.getParentFile(), part);
						if (!sub.exists() || !sub.getParentFile()
							.equals(f.getParentFile())) {
							warning("Invalid Class-Path entry %s in %s, must exist and must reside in same directory",
								sub, f);
						} else {
							addWabLib(dot, sub, part, Collections.emptyMap());
						}
					}
				}
			}
		} else {
			doIncludeResource(dot, name, attrs);
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
		if (!f.exists() || f.lastModified() < dot.lastModified()) {
			IO.delete(f);
			File fp = f.getParentFile();
			IO.mkdirs(fp);
			try (OutputStream out = IO.outputStream(f)) {
				Jar.writeManifest(dot.getManifest(), out);
			}
			changedFile(f);
		}
	}

	protected void changedFile(@SuppressWarnings("unused") File f) {}

	/**
	 * Sign the jar file. -sign : <alias> [ ';' 'password:=' <password> ] [ ';'
	 * 'keystore:=' <keystore> ] [ ';' 'sign-password:=' <pw> ] ( ',' ... )*
	 */

	void sign(@SuppressWarnings("unused") Jar jar) throws Exception {
		String signing = getProperty(SIGN);
		if (signing == null)
			return;

		logger.debug("Signing {}, with {}", getBsn(), signing);
		List<SignerPlugin> signers = getPlugins(SignerPlugin.class);

		Parameters infos = parseHeader(signing);
		for (String alias : infos.keySet()) {
			for (SignerPlugin signer : signers) {
				signer.sign(this, alias);
			}
		}
	}

	public boolean hasSources() {
		return isTrue(getProperty(SOURCES));
	}

	/**
	 * Answer extra packages. In this case we implement conditional package. Any
	 */
	@Override
	protected Jar getExtra() throws Exception {
		Parameters conditionals = getMergedParameters(CONDITIONAL_PACKAGE);
		conditionals.putAll(getMergedParameters(CONDITIONALPACKAGE));
		if (conditionals.isEmpty())
			return null;
		logger.debug("do Conditional Package {}", conditionals);
		Instructions instructions = new Instructions(conditionals);

		Collection<PackageRef> referred = instructions.select(getReferred().keySet(), false);
		referred.removeAll(getContained().keySet());
		if (referred.isEmpty()) {
			logger.debug("no additional conditional packages to add");
			return null;
		}

		Jar jar = new Jar(CONDITIONALPACKAGE);
		addClose(jar);
		for (PackageRef pref : referred) {
			for (Jar cpe : getClasspath()) {
				Map<String, Resource> map = cpe.getDirectory(pref.getPath());
				if (map != null) {
					copy(jar, cpe, pref.getPath(), false);
					break;
				}
			}
		}
		if (jar.getDirectories()
			.isEmpty()) {
			logger.debug("extra dirs {}", jar.getDirectories());
			return null;
		}
		return jar;
	}

	/**
	 * Intercept the call to analyze and cleanup versions after we have analyzed
	 * the setup. We do not want to cleanup if we are going to verify.
	 */

	@Override
	public void analyze() throws Exception {
		super.analyze();
		cleanupVersion(getImports(), null, Constants.IMPORT_PACKAGE);
		cleanupVersion(getExports(), getVersion(), Constants.EXPORT_PACKAGE);
		String version = getProperty(BUNDLE_VERSION);
		if (version != null) {
			version = cleanupVersion(version);
			version = doSnapshot(version);
			setProperty(BUNDLE_VERSION, version);
		}
	}

	private String doSnapshot(String version) {
		String snapshot = getProperty(SNAPSHOT);
		if (snapshot == null) {
			return version;
		}
		if (snapshot.isEmpty()) {
			snapshot = null;
		}
		Version v = Version.parseVersion(version);
		String q = v.getQualifier();
		if (q == null) {
			return version;
		}
		if (q.equals("SNAPSHOT")) {
			q = snapshot;
		} else if (q.endsWith("-SNAPSHOT")) {
			int end = q.length() - "SNAPSHOT".length();
			if (snapshot == null) {
				q = q.substring(0, end - 1);
			} else {
				q = q.substring(0, end) + snapshot;
			}
		} else {
			return version;
		}
		return new Version(v.getMajor(), v.getMinor(), v.getMicro(), q).toString();
	}

	public void cleanupVersion(Packages packages, String defaultVersion) {
		cleanupVersion(packages, defaultVersion, "external");
	}

	public void cleanupVersion(Packages packages, String defaultVersion, String what) {
		if (defaultVersion != null) {
			Matcher m = Verifier.VERSION.matcher(defaultVersion);
			if (m.matches()) {
				// Strip qualifier from default package version
				defaultVersion = Version.parseVersion(defaultVersion)
					.toStringWithoutQualifier();
			}
		}
		Set<String> visited = new HashSet<>();

		for (Map.Entry<PackageRef, Attrs> entry : packages.entrySet()) {

			String packageName = Processor.removeDuplicateMarker(entry.getKey().fqn);

			Attrs attributes = entry.getValue();
			String v = attributes.get(Constants.VERSION_ATTRIBUTE);
			if (v == null && defaultVersion != null) {

				if (visited.contains(packageName)) {

					SetLocation warning = warning(
						"%s duplicate package name (%s) that uses the default version because no version is specified (%s). Remove duplicate package or add an explicit version to it.",
						what, packageName, defaultVersion);
					try {
						getHeader(Constants.EXPORT_PACKAGE, entry.getKey().fqn);
					} catch (Exception e) {
						// not so important
					}
				}

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
			visited.add(packageName);
		}
	}

	/**
	 * @throws IOException
	 */
	private void addSources(Jar dot) throws Exception {
		if (!hasSources())
			return;

		Set<PackageRef> packages = Create.set();

		for (TypeRef typeRef : getClassspace().keySet()) {
			PackageRef packageRef = typeRef.getPackageRef();
			String sourcePath = typeRef.getSourcePath();
			String packagePath = packageRef.getPath();

			boolean found = false;
			String[] fixed = {
				"packageinfo", "package.html", "module-info.java", "package-info.java"
			};

			for (File root : getSourcePath()) {
				File f = getFile(root, sourcePath);
				if (f.exists()) {
					found = true;
					if (!packages.contains(packageRef)) {
						packages.add(packageRef);
						for (int j = 0; j < fixed.length; j++) {
							for (File sp : getSourcePath()) {
								File bdir = getFile(sp, packagePath);
								File ff = getFile(bdir, fixed[j]);
								if (ff.isFile()) {
									String name = "OSGI-OPT/src/" + packagePath + "/" + fixed[j];
									dot.putResource(name, new FileResource(ff));
									break;
								}
							}
						}
					}
					if (packageRef.isDefaultPackage())
						logger.debug("Package reference is default package");
					dot.putResource("OSGI-OPT/src/" + sourcePath, new FileResource(f));
				}
			}
			if (getSourcePath().isEmpty())
				warning("Including sources but " + SOURCEPATH + " does not contain any source directories ");
			// TODO copy from the jars where they came from
		}
	}

	boolean			firstUse	= true;
	private Tree	tree;

	public Collection<File> getSourcePath() {
		if (firstUse) {
			firstUse = false;
			String sp = mergeProperties(SOURCEPATH);
			if (sp != null) {
				Parameters map = parseHeader(sp);
				for (Iterator<String> i = map.keySet()
					.iterator(); i.hasNext();) {
					String file = i.next();
					if (!isDuplicate(file)) {
						File f = getFile(file);
						if (!f.isDirectory()) {
							error("Adding a sourcepath that is not a directory: %s", f).header(SOURCEPATH)
								.context(file);
						} else {
							sourcePath.add(f);
						}
					}
				}
			}
		}
		return sourcePath;
	}

	private void doVerify(@SuppressWarnings("unused") Jar dot) throws Exception {

		// Give the verifier the benefit of our analysis
		// prevents parsing the files twice

		try (Verifier verifier = new Verifier(this)) {
			verifier.setFrombuilder(true);

			verifier.verify();
			getInfo(verifier);
		}
	}

	private void doExpand(Jar dot) throws Exception {

		// Build an index of the class path that we can then
		// use destructively
		MultiMap<String, Jar> packages = new MultiMap<>();
		for (Jar srce : getClasspath()) {
			dot.updateModified(srce.lastModified(), srce + " (" + srce.lastModifiedReason() + ")");
			MapStream.of(srce.getDirectories())
				.filterValue(Objects::nonNull)
				.keys()
				.forEachOrdered(path -> packages.add(path, srce));
		}

		Parameters includedPackages = getPrivatePackage();
		if (buildInstrs.undertest()) {
			String h = mergeProperties(Constants.TESTPACKAGES, "test;presence:=optional");
			includedPackages.putAll(parseHeader(h));
		}

		Parameters limboPackages = buildInstrs.includepackage();
		includedPackages.putAll(limboPackages);

		if (!includedPackages.isEmpty()) {
			Instructions privateFilter = new Instructions(includedPackages);
			Set<Instruction> unused = doExpand(dot, packages, privateFilter);

			if (!unused.isEmpty()) {
				warning(
					"Unused " + Constants.PRIVATE_PACKAGE + " instructions, no such package(s) on the class path: %s",
					unused).header(Constants.PRIVATE_PACKAGE)
						.context(unused.iterator()
							.next()
							.getInput());
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
	 * @throws Exception
	 */
	private Set<Instruction> doExpand(Jar jar, MultiMap<String, Jar> index, Instructions filter) throws Exception {
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

			for (Iterator<Entry<String, List<Jar>>> entry = index.entrySet()
				.iterator(); entry.hasNext();) {
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
				if (instruction.isNegated()) {
					used = true;
					continue;
				}

				// Do the from: directive, filters on the JAR type
				List<Jar> providers = filterFrom(from, p.getValue());
				if (providers.isEmpty())
					continue;

				int splitStrategy = getSplitStrategy(directives.get(SPLIT_PACKAGE_DIRECTIVE));
				copyPackage(jar, providers, directory, splitStrategy);
				Attrs contained = getContained().put(packageRef);

				contained.put(INTERNAL_SOURCE_DIRECTIVE, getName(providers.get(0)));
				used = true;
			}

			if (!used && !isTrue(directives.get("optional:")))
				unused.add(instruction);
		}
		return unused;
	}

	/**
	 * @param from
	 */
	private List<Jar> filterFrom(Instruction from, List<Jar> providers) {
		if (from.isAny())
			return providers;

		List<Jar> np = new ArrayList<>();
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
			case SPLIT_MERGE_LAST :
				for (Jar srce : providers) {
					copy(dest, srce, path, true);
				}
				break;

			case SPLIT_MERGE_FIRST :
				for (Jar srce : providers) {
					copy(dest, srce, path, false);
				}
				break;

			case SPLIT_ERROR :
				error("%s", diagnostic(path, providers));
				break;

			case SPLIT_FIRST :
				copy(dest, providers.get(0), path, false);
				break;

			default :
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
		logger.debug("copy d={} s={} p={}", dest, srce, path);
		dest.copy(srce, path, overwrite);
		if (hasSources()) {
			dest.copy(srce, appendPath("OSGI-OPT/src", path), overwrite);
		}

		// bnd.info sources must be preprocessed
		String bndInfoPath = appendPath(path, "bnd.info");
		Resource r = dest.getResource(bndInfoPath);
		if (r != null && !(r instanceof PreprocessResource)) {
			logger.debug("preprocessing bnd.info");
			PreprocessResource pp = new PreprocessResource(this, r);
			dest.putResource(bndInfoPath, pp);
		}

		if (hasSources()) {
			String srcPath = appendPath("OSGI-OPT/src", path);
			Map<String, Resource> srcContents = srce.getDirectory(srcPath);
			if (srcContents != null) {
				dest.addDirectory(srcContents, overwrite);
			}
		}
	}

	/**
	 * Analyze the classpath for a split package
	 *
	 */
	private String diagnostic(String pack, List<Jar> culprits) {
		// Default is like merge-first, but with a warning
		return "Split package, multiple jars provide the same package:" + pack
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

		error("Invalid strategy for split-package: %s", type);
		return SPLIT_DEFAULT;
	}

	/**
	 * Matches the instructions against a package.
	 *
	 * @param instructions The list of instructions
	 * @param pack The name of the package
	 * @param unused The total list of patterns, matched patterns are removed
	 * @param source The name of the source container, can be filtered upon with
	 *            the from: directive.
	 */
	private Instruction matches(Instructions instructions, String pack, Set<Instruction> unused, String source) {
		for (Entry<Instruction, Attrs> entry : instructions.entrySet()) {
			Instruction pattern = entry.getKey();

			// It is possible to filter on the source of the
			// package with the from: directive. This is an
			// instruction that must match the name of the
			// source class path entry.

			String from = entry.getValue()
				.get(FROM_DIRECTIVE);
			if (from != null) {
				Instruction f = new Instruction(from);
				if (!(f.matches(source) ^ f.isNegated()))
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
			includes = mergeProperties(INCLUDERESOURCE);
			if (includes == null || includes.length() == 0)
				includes = mergeProperties(Constants.INCLUDE_RESOURCE);
		} else
			warning("Please use -includeresource instead of Bundle-Includes");

		doIncludeResource(jar, includes);

	}

	private void doIncludeResource(Jar jar, String includes) throws Exception {
		Parameters clauses = parseHeader(includes);
		doIncludeResource(jar, clauses);
	}

	private void doIncludeResource(Jar jar, Parameters clauses) throws ZipException, IOException, Exception {
		for (Entry<String, Attrs> entry : clauses.entrySet()) {

			String key = removeDuplicateMarker(entry.getKey());
			doIncludeResource(jar, key, entry.getValue());
		}
	}

	private void doIncludeResource(Jar jar, String name, Map<String, String> extra)
		throws ZipException, IOException, Exception {

		Instructions preprocess = null;
		boolean absentIsOk = false;

		if (name.startsWith("{") && name.endsWith("}")) {
			preprocess = getPreProcessMatcher(extra);
			name = name.substring(1, name.length() - 1)
				.trim();
		}

		String parts[] = name.split("\\s*=\\s*");
		String source = parts[0];
		String destination = parts[0];
		if (parts.length == 2)
			source = parts[1];

		if (source.startsWith("-")) {
			source = source.substring(1);
			absentIsOk = true;
		}

		if (source.startsWith("@")) {
			extractFromJar(jar, source.substring(1), parts.length == 1 ? "" : destination, absentIsOk);
		} else if (extra.containsKey("cmd")) {
			doCommand(jar, source, destination, extra, preprocess, absentIsOk);
		} else if (extra.containsKey(LITERAL_ATTRIBUTE)) {
			String literal = extra.get(LITERAL_ATTRIBUTE);
			Resource r = new EmbeddedResource(literal, 0L);
			String x = extra.get("extra");
			if (x != null)
				r.setExtra(x);
			copy(jar, name, r, extra);
			if (preprocess != null) {
				warning("Preprocessing does not work for literals: %s", name);
			}
		} else if (extra.containsKey(Constants.CLASS_ATTRIBUTE)) {
			doClassAttribute(jar, name, extra, preprocess, absentIsOk);
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
				destinationPath = doResourceDirectory(jar, extra, preprocess, sourceFile, destinationPath);
				return;
			}

			// destinationPath = checkDestinationPath(destinationPath);

			if (!sourceFile.exists()) {
				if (absentIsOk)
					return;

				noSuchFile(jar, name, extra, source, destinationPath);
			} else
				copy(jar, destinationPath, sourceFile, preprocess, extra);
		}
	}

	private void doClassAttribute(Jar jar, String name, Map<String, String> extra, Instructions preprocess,
		boolean absentIsOk) throws Exception {
		FileLine header = getHeader(Constants.INCLUDE_RESOURCE, Constants.CLASS_ATTRIBUTE);
		String fqn = extra.get(Constants.CLASS_ATTRIBUTE);
		TypeRef typeRef = getTypeRefFromFQN(fqn);
		if (typeRef == null) {
			header.set(warning(
				"-includeresource entry uses 'class' attribute to refer to classpath but the reference '%s' is not a value type ref (fqn)",
				fqn));
		} else {
			Clazz clazz = findClass(typeRef);
			if (clazz == null) {
				if (!absentIsOk) {
					header.set(warning(
						"-includeresource entry uses 'class' attribute to refer to classpath but the reference '%s' could not be found",
						typeRef));
				}
			} else {
				Resource r = clazz.getResource();
				String x = extra.get("extra");
				if (x != null)
					r.setExtra(x);
				copy(jar, name, r, extra);
				if (preprocess != null) {
					warning("Preprocessing does not work for class references: %s", name);
				}
			}
		}
	}

	private Instructions getPreProcessMatcher(Map<String, String> extra) {
		if (defaultPreProcessMatcher == null) {
			String preprocessmatchers = mergeProperties(PREPROCESSMATCHERS);
			if (preprocessmatchers == null || preprocessmatchers.trim()
				.length() == 0)
				preprocessmatchers = Constants.DEFAULT_PREPROCESSS_MATCHERS;

			defaultPreProcessMatcher = new Instructions(preprocessmatchers);
		}
		if (extra == null)
			return defaultPreProcessMatcher;

		String additionalMatchers = extra.get(PREPROCESSMATCHERS);
		if (additionalMatchers == null)
			return defaultPreProcessMatcher;

		Instructions specialMatcher = new Instructions(additionalMatchers);
		specialMatcher.putAll(defaultPreProcessMatcher);
		return specialMatcher;
	}

	/**
	 * It is possible in Include-Resource to use a system command that generates
	 * the contents, this is indicated with {@code cmd} attribute. The command
	 * can be repeated for a number of source files with the {@code for}
	 * attribute which indicates a list of repetitions, often down with the
	 * {@link Macro#_lsa(String[])} or {@link Macro#_lsb(String[])} macro. The
	 * repetition will repeat the given command for each item. The @} macro can
	 * be used to replace the current item. If no {@code for} is given, the
	 * source is used as the only item. If the destination contains a macro,
	 * each iteration will create a new file, otherwise the destination name is
	 * used.
	 *
	 * @param jar
	 * @param source
	 * @param destination
	 * @param extra
	 * @param preprocess
	 * @param absentIsOk
	 * @throws Exception
	 */
	private void doCommand(Jar jar, String source, String destination, Map<String, String> extra,
		Instructions preprocess, boolean absentIsOk) throws Exception {
		String repeat = extra.get("for"); // TODO constant
		if (repeat == null)
			repeat = source;

		Collection<String> requires = split(extra.get("requires"));
		long lastModified = 0;
		for (String required : requires) {
			File file = getFile(required);
			if (!file.exists()) {
				error(Constants.INCLUDE_RESOURCE + ".cmd for %s, requires %s, but no such file %s", source, required,
					file.getAbsoluteFile()).header(INCLUDERESOURCE + "|" + INCLUDE_RESOURCE);
			} else
				lastModified = findLastModifiedWhileOlder(file, lastModified());
		}

		String cmd = extra.get("cmd");

		List<String> paths = new ArrayList<>();

		for (String item : Processor.split(repeat)) {
			File f = IO.getFile(item);
			traverse(paths, f);
		}

		CombinedResource cr = null;

		if (!destination.contains("${@}")) {
			cr = new CombinedResource();
			cr.lastModified = lastModified;
		}

		setProperty("@requires", join(requires, " "));
		try {
			for (String item : paths) {
				setProperty("@", item);
				try {
					String path = getReplacer().process(destination);
					String command = getReplacer().process(cmd);
					File file = getFile(item);
					if (file.exists())
						lastModified = Math.max(lastModified, file.lastModified());

					CommandResource cmdresource = new CommandResource(command, this, lastModified, getBase());

					Resource r = cmdresource;

					// Turn this resource into a file resource
					// so we execute the command now and catch its
					// errors
					FileResource fr = new FileResource(r);

					addClose(fr);
					r = fr;

					if (preprocess != null && preprocess.matches(path))
						r = new PreprocessResource(this, r);

					if (cr == null)
						jar.putResource(path, r);
					else
						cr.addResource(r);
				} finally {
					unsetProperty("@");
				}
			}
		} finally {
			unsetProperty("@requires");
		}

		// Add last so the correct modification date is used
		// to update the modified time.
		if (cr != null)
			jar.putResource(destination, cr);

		updateModified(lastModified, Constants.INCLUDE_RESOURCE + ": cmd");
	}

	private void traverse(List<String> paths, File item) {

		if (item.isDirectory()) {
			for (File sub : item.listFiles()) {
				traverse(paths, sub);
			}
		} else if (item.isFile())
			paths.add(IO.absolutePath(item));
		else
			paths.add(item.getName());
	}

	/**
	 * Check if a file or directory is older than the given time.
	 *
	 * @param file
	 * @param lastModified
	 */
	private long findLastModifiedWhileOlder(File file, long lastModified) {
		if (file.isDirectory()) {
			File children[] = file.listFiles();
			for (File child : children) {
				if (child.lastModified() > lastModified)
					return child.lastModified();

				long lm = findLastModifiedWhileOlder(child, lastModified);
				if (lm > lastModified)
					return lm;
			}
		}
		return file.lastModified();
	}

	private String doResourceDirectory(Jar jar, Map<String, String> extra, Instructions preprocess, File sourceFile,
		String destinationPath) throws Exception {
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

	private void resolveFiles(File dir, FileFilter filter, boolean recursive, String path, Map<String, File> files,
		boolean flatten) {

		if (doNotCopy(dir)) {
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
					warning(Constants.INCLUDE_RESOURCE + " overwrites entry %s from file %s", p, file);
				files.put(p, file);
			}
		}
		if (fs.length == 0) {
			File empty = new File(dir, Constants.EMPTY_HEADER);
			files.put(appendPath(path, empty.getName()), empty);
		}
	}

	private void noSuchFile(Jar jar, String clause, Map<String, String> extra, String source, String destinationPath)
		throws Exception {
		List<Jar> src = getJarsFromName(source, Constants.INCLUDE_RESOURCE + " " + source);
		if (!src.isEmpty()) {
			for (Jar j : src) {
				File sourceFile = j.getSource();
				String quoted = (sourceFile != null) ? sourceFile.getName() : j.getName();
				Resource resource;
				if ((sourceFile != null) && sourceFile.isFile()) {
					resource = new FileResource(sourceFile);
				} else {
					// Do not touch the manifest so this also
					// works for signed files.
					j.setDoNotTouchManifest();
					resource = new JarResource(j);
				}
				String path = destinationPath.replace(source, quoted);
				logger.debug("copy d={} s={} path={}", jar, j, path);
				copy(jar, path, resource, extra);
			}
		} else {
			Resource lastChance = make.process(source);
			if (lastChance != null) {
				String x = extra.get("extra");
				if (x != null)
					lastChance.setExtra(x);
				copy(jar, destinationPath, lastChance, extra);
			} else
				error("Input file does not exist: %s", source).header(source)
					.context(clause);
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
	private void extractFromJar(Jar jar, String source, String destination, boolean absentIsOk)
		throws ZipException, IOException {
		// Inline all resources and classes from another jar
		// optionally appended with a modified regular expression
		// like @zip.jar!/META-INF/MANIFEST.MF
		int n = source.lastIndexOf("!/");
		Instruction instr = null;
		if (n > 0) {
			instr = new Instruction(source.substring(n + 2));
			source = source.substring(0, n);
		}

		List<Jar> sub = getJarsFromName(source, "extract from jar");
		if (sub.isEmpty()) {
			if (absentIsOk)
				return;

			error("Can not find JAR file '%s'", source);
		} else {
			for (Jar j : sub)
				addAll(jar, j, instr, destination);
		}
	}

	/**
	 * Add all the resources in the given jar that match the given filter.
	 *
	 * @param sub the jar
	 * @param filter a pattern that should match the resoures in sub to be added
	 */
	public boolean addAll(Jar to, Jar sub, Instruction filter) {
		return addAll(to, sub, filter, "");
	}

	/**
	 * Add all the resources in the given jar that match the given filter.
	 *
	 * @param sub the jar
	 * @param filter a pattern that should match the resoures in sub to be added
	 */
	public boolean addAll(Jar to, Jar sub, Instruction filter, String destination) {
		boolean dupl = false;
		for (String name : sub.getResources()
			.keySet()) {
			if ("META-INF/MANIFEST.MF".equals(name))
				continue;

			if (doNotCopy(Strings.getLastSegment(name, '/')))
				continue;

			if (filter == null || filter.matches(name) ^ filter.isNegated())
				dupl |= to.putResource(Processor.appendPath(destination, name), sub.getResource(name), true);
		}
		return dupl;
	}

	private void copy(Jar jar, String path, File from, Instructions preprocess, Map<String, String> extra)
		throws Exception {
		if (doNotCopy(from))
			return;

		logger.debug("copy d={} s={} path={}", jar, from, path);
		if (from.isDirectory()) {

			File files[] = from.listFiles();
			for (int i = 0; i < files.length; i++) {
				copy(jar, appendPath(path, files[i].getName()), files[i], preprocess, extra);
			}
		} else {
			if (from.exists()) {
				Resource resource = new FileResource(from);
				if (preprocess != null && preprocess.matches(path)) {
					resource = new PreprocessResource(this, resource);
				}
				String x = extra.get("extra");
				if (x != null)
					resource.setExtra(x);
				if (path.endsWith("/"))
					path = path + from.getName();
				copy(jar, path, resource, extra);
			} else if (from.getName()
				.equals(Constants.EMPTY_HEADER)) {
				jar.putResource(path, new EmbeddedResource(new byte[0], 0L));
			} else {
				error("Input file does not exist: %s", from).header(INCLUDERESOURCE + "|" + INCLUDE_RESOURCE);
			}
		}
	}

	private void copy(Jar jar, String path, Resource resource, Map<String, String> extra) {
		jar.putResource(path, resource);
		if (isTrue(extra.get(LIB_DIRECTIVE))) {
			setProperty(BUNDLE_CLASSPATH, append(getProperty(BUNDLE_CLASSPATH, "."), path));
		}
	}

	public void setSourcepath(File[] files) {
		for (int i = 0; i < files.length; i++)
			addSourcepath(files[i]);
	}

	public void addSourcepath(File cp) {
		if (!cp.exists())
			warning("File on sourcepath that does not exist: %s", cp);

		sourcePath.add(cp);
	}

	/**
	 * Build Multiple jars. If the -sub command is set, we filter the file with
	 * the given patterns.
	 *
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

				buildInstrs.compression()
					.ifPresent(c::setCompression);

				addClose(c);
				String name = map.get(file)
					.get("name");
				if (name != null)
					c.setName(name);

				result[n++] = c;
			}
			return result;
		}

		List<Jar> result = new ArrayList<>();
		List<Builder> builders;

		builders = getSubBuilders();

		for (Builder builder : builders) {
			try {
				startBuild(builder);
				Jar jar = builder.build();
				jar.setName(builder.getBsn());

				result.add(jar);
				doneBuild(builder);
			} catch (Exception e) {
				builder.exception(e, "Exception Building %s", builder.getBsn());
			}
			if (builder != this)
				getInfo(builder, builder.getBsn() + ": ");
		}
		return result.toArray(new Jar[0]);
	}

	/**
	 * Called when we start to build a builder
	 */
	protected void startBuild(Builder builder) throws Exception {}

	/**
	 * Called when we're done with a builder
	 */
	protected void doneBuild(Builder builder) throws Exception {}

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
		List<Builder> builders = new ArrayList<>();
		String sub = getProperty(SUB);
		if (sub == null || sub.trim()
			.length() == 0 || EMPTY_HEADER.equals(sub)) {
			builders.add(this);
			return builders;
		}

		if (isTrue(getProperty(NOBUNDLES)))
			return builders;

		Parameters subsMap = parseHeader(sub);
		for (Iterator<String> i = subsMap.keySet()
			.iterator(); i.hasNext();) {
			File file = getFile(i.next());
			if (file.isFile() && !file.getName()
				.startsWith(".")) {
				builders.add(getSubBuilder(file));
				i.remove();
			}
		}

		Instructions instructions = new Instructions(subsMap);

		List<File> members = new ArrayList<>(Arrays.asList(getBase().listFiles()));

		nextFile: while (members.size() > 0) {

			File file = members.remove(0);

			// Check if the file is one of our parents
			@SuppressWarnings("resource")
			Processor p = this;
			while (p != null) {
				if (file.equals(p.getPropertiesFile()))
					continue nextFile;
				p = p.getParent();
			}

			for (Iterator<Instruction> i = instructions.keySet()
				.iterator(); i.hasNext();) {

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
		builder.use(this);

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
			error("${maven_version} macro receives too many arguments %s", Arrays.toString(args));
		else if (args.length < 2)
			error("${maven_version} macro has no arguments, use ${maven_version;1.2.3-SNAPSHOT}");
		else {
			return cleanupVersion(args[1]);
		}
		return null;
	}

	public String _permissions(String args[]) {
		return new PermissionGenerator(this, args).generate();
	}

	/**
	 *
	 */
	public void removeBundleSpecificHeaders() {
		Set<String> set = new HashSet<>(Arrays.asList(BUNDLE_SPECIFIC_HEADERS));
		setForceLocal(set);
	}

	/**
	 * Check if the given resource is in scope of this bundle. That is, it
	 * checks if the Include-Resource includes this resource or if it is a class
	 * file it is on the class path and the Export-Package or Private-Package
	 * include this resource.
	 */
	public boolean isInScope(Collection<File> resources) throws Exception {
		Parameters clauses = parseHeader(mergeProperties(Constants.EXPORT_PACKAGE));
		clauses.putAll(parseHeader(mergeProperties(Constants.PRIVATE_PACKAGE)));
		clauses.putAll(parseHeader(mergeProperties(Constants.PRIVATEPACKAGE)));
		if (isTrue(getProperty(Constants.UNDERTEST))) {
			clauses.putAll(parseHeader(mergeProperties(Constants.TESTPACKAGES, "test;presence:=optional")));
		}

		Stream<String> ir = getIncludedResourcePrefixes();

		Instructions instructions = new Instructions(clauses);

		for (File r : resources) {
			String cpEntry = getClasspathEntrySuffix(r);

			if (cpEntry != null) {

				if (cpEntry.equals("")) // Meaning we actually have a CPE
					return true;

				String pack = Descriptors.getPackage(cpEntry);
				Instruction i = matches(instructions, pack, null, r.getName());
				if (i != null)
					return !i.isNegated();
			}

			// Check if this resource starts with one of the I-C header
			// paths.
			String path = IO.absolutePath(r);
			return ir.anyMatch(path::startsWith);
		}
		return false;
	}

	/**
	 * Extra the paths for the directories and files that are used in the
	 * Include-Resource header.
	 */
	private Stream<String> getIncludedResourcePrefixes() {
		Stream<String> prefixes = getIncludeResource().stream()
			.filterValue(attrs -> !attrs.containsKey("literal"))
			.keys()
			.map(IR_PATTERN::matcher)
			.filter(Matcher::matches)
			.map(m -> m.group(1))
			.map(this::getFile)
			.map(IO::absolutePath);
		return prefixes;
	}

	/**
	 * Answer the string of the resource that it has in the container. It is
	 * possible that the resource is a classpath entry. In that case an empty
	 * string is returned.
	 *
	 * @param resource The resource to look for
	 * @return A suffix on the classpath or "" if the resource is a class path
	 *         entry
	 * @throws Exception
	 */
	public String getClasspathEntrySuffix(File resource) throws Exception {
		for (Jar jar : getClasspath()) {
			File source = jar.getSource();
			if (source != null) {

				String sourcePath = IO.absolutePath(source);
				String resourcePath = IO.absolutePath(resource);
				if (sourcePath.equals(resourcePath))
					return ""; // Matches a classpath entry

				if (resourcePath.startsWith(sourcePath)) {
					// Make sure that the path name is translated correctly
					// i.e. on Windows the \ must be translated to /
					String filePath = resourcePath.substring(sourcePath.length() + 1);

					return filePath;
				}
			}
		}
		return null;
	}

	/**
	 * doNotCopy The doNotCopy variable maintains a patter for files that should
	 * not be copied. There is a default {@link #DEFAULT_DO_NOT_COPY} but this
	 * ca be overridden with the {@link Constants#DONOTCOPY} property.
	 */

	public boolean doNotCopy(String v) {
		return getDoNotCopy().matcher(v)
			.matches();
	}

	public boolean doNotCopy(File from) {
		if (doNotCopy(from.getName())) {
			return true;
		}

		if (!since(About._3_1)) {
			return false;
		}

		URI uri = getBaseURI().relativize(from.toURI());

		return doNotCopy(uri.getPath());
	}

	public Pattern getDoNotCopy() {
		if (xdoNotCopy == null) {
			String string = null;
			try {
				string = mergeProperties(DONOTCOPY);
				if (string == null || string.isEmpty())
					string = DEFAULT_DO_NOT_COPY;
				xdoNotCopy = Pattern.compile(string);
			} catch (Exception e) {
				error("Invalid value for %s, value is %s", DONOTCOPY, string).header(DONOTCOPY);
				xdoNotCopy = Pattern.compile(DEFAULT_DO_NOT_COPY);
			}
		}
		return xdoNotCopy;
	}

	/**
	 */

	static MakeBnd					makeBnd					= new MakeBnd();
	static MakeCopy					makeCopy				= new MakeCopy();
	static ServiceComponent			serviceComponent		= new ServiceComponent();
	static CDIAnnotations			cdiAnnotations			= new CDIAnnotations();
	static DSAnnotations			dsAnnotations			= new DSAnnotations();
	static MetatypeAnnotations		metatypeAnnotations		= new MetatypeAnnotations();
	static JPMSAnnotations			moduleAnnotations		= new JPMSAnnotations();
	static JPMSModuleInfoPlugin		moduleInfoPlugin		= new JPMSModuleInfoPlugin();
	static SPIDescriptorGenerator	spiDescriptorGenerator	= new SPIDescriptorGenerator();
	static APIGuardianAnnotations	apiGuardianAnnotations	= new APIGuardianAnnotations();

	@Override
	protected void setTypeSpecificPlugins(Set<Object> list) {
		list.add(makeBnd);
		list.add(makeCopy);
		list.add(serviceComponent);
		list.add(cdiAnnotations);
		list.add(dsAnnotations);
		list.add(metatypeAnnotations);
		list.add(moduleAnnotations);
		list.add(moduleInfoPlugin);
		list.add(spiDescriptorGenerator);
		list.add(apiGuardianAnnotations);
		super.setTypeSpecificPlugins(list);
	}

	/**
	 * Diff this bundle to another bundle for the given packages.
	 *
	 * @throws Exception
	 */

	public void doDiff(@SuppressWarnings("unused") Jar dot) throws Exception {
		Parameters diffs = parseHeader(getProperty("-diff"));
		if (diffs.isEmpty())
			return;

		logger.debug("diff {}", diffs);

		if (tree == null)
			tree = differ.tree(this);

		for (Entry<String, Attrs> entry : diffs.entrySet()) {
			String path = entry.getKey();
			File file = getFile(path);
			if (!file.isFile()) {
				error("Diffing against %s that is not a file", file).header("-diff")
					.context(path);
				continue;
			}

			boolean full = entry.getValue()
				.get("--full") != null;
			boolean warning = entry.getValue()
				.get("--warning") != null;

			Tree other = differ.tree(file);
			Diff api = tree.diff(other)
				.get("<api>");
			Instructions instructions = new Instructions(entry.getValue()
				.get("--pack"));

			logger.debug("diff against {} --full={} --pack={} --warning={}", file, full, instructions);
			for (Diff p : api.getChildren()) {
				String pname = p.getName();
				if (p.getType() == Type.PACKAGE && instructions.matches(pname)) {
					if (p.getDelta() != Delta.UNCHANGED) {

						if (!full)
							if (warning)
								warning("Differ %s", p).header("-diff")
									.context(path);
							else
								error("Differ %s", p).header("-diff")
									.context(path);
						else {
							if (warning)
								warning("Diff found a difference in %s for packages %s", file, instructions)
									.header("-diff")
									.context(path);
							else
								error("Diff found a difference in %s for packages %s", file, instructions)
									.header("-diff")
									.context(path);
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
			warning("%s%s", indent, p).header("-diff");
		else
			error("%s%s", indent, p).header("-diff");

		indent = indent + " ";
		switch (d) {
			case CHANGED :
			case MAJOR :
			case MINOR :
			case MICRO :
				break;

			default :
				return;
		}
		for (Diff c : p.getChildren())
			show(c, indent, warning);
	}

	public void addSourcepath(Collection<File> sourcepath) {
		for (File f : sourcepath) {
			addSourcepath(f);
		}
	}

	/**
	 * Base line against a previous version. Should be overridden in the
	 * ProjectBuilder where we have access to the repos
	 *
	 * @throws Exception
	 */

	protected void doBaseline(Jar dot) throws Exception {}

	/**
	 * #388 Manifest header to get GIT head Get the head commit number. Look for
	 * a .git/HEAD file, going up in the file hierarchy. Then get this file, and
	 * resolve any symbolic reference.
	 */
	private final static Pattern	GITREF_P		= Pattern.compile("ref:\\s*(refs/(heads|tags|remotes)/(\\S+))\\s*");

	final static String				_githeadHelp	= "${githead}, provide the SHA for the current git head";

	public String _githead(String[] args) throws IOException {
		Macro.verifyCommand(args, _githeadHelp, null, 1, 1);

		//
		// Locate the .git directory
		//

		File rover = getBase();
		while (rover != null && rover.isDirectory()) {
			File headFile = IO.getFile(rover, ".git/HEAD");
			if (headFile.isFile()) {
				//
				// The head is either a symref (ref:
				// refs/(heads|tags|remotes)/<name>)
				//
				String head = IO.collect(headFile)
					.trim();
				if (!Hex.isHex(head)) {
					//
					// Should be a symref
					//
					Matcher m = GITREF_P.matcher(head);
					if (m.matches()) {
						String reference = m.group(1);
						// so the commit is in the following path
						File file = IO.getFile(rover, ".git/" + reference);
						if (!file.isFile()) {
							// sigh, gc'd. Is in .git/packed-refs
							file = IO.getFile(rover, ".git/packed-refs");
							if (file.isFile()) {
								String refs = IO.collect(file);
								Pattern packedReferenceLinePattern = Pattern
									.compile("(" + PatternConstants.SHA1 + ")\\s+" + reference + "\\s*\n");
								Matcher packedReferenceMatcher = packedReferenceLinePattern.matcher(refs);
								if (packedReferenceMatcher.find()) {
									head = packedReferenceMatcher.group(1);
								} else
									return ""; // give up
							} else
								return ""; // give up
						} else {
							head = IO.collect(file);
						}
					} else {
						error(
							"Git repo seems corrupt. It exists, find the HEAD but the content is neither hex nor a sym-ref: %s",
							head);
					}
				}
				return head.trim()
					.toUpperCase();
			}
			rover = rover.getParentFile();
		}
		// Cannot find git directory
		return "";
	}

	/**
	 * Create a report of the settings
	 *
	 * @throws Exception
	 */

	@Override
	public void report(Map<String, Object> table) throws Exception {
		build();
		super.report(table);
		table.put("Do Not Copy", getDoNotCopy());
		table.put("Git head", _githead(new String[] {
			"githead"
		}));
	}

	/**
	 * Collect the information from the {@link BuilderSpecification}
	 *
	 * @throws IOException
	 */

	public Builder from(BuilderSpecification spec) throws IOException {
		if (spec.bundleActivator != null)
			setBundleActivator(spec.bundleActivator);

		setFailOk(spec.failOk);
		setSources(spec.sources);
		setProperty(Constants.RESOURCEONLY, spec.resourceOnly + "");

		if (!spec.bundleNativeCode.isEmpty())
			setProperty(Constants.BUNDLE_NATIVECODE, new Parameters(spec.bundleNativeCode).toString());

		if (!spec.bundleSymbolicName.isEmpty()) {
			setBundleSymbolicName(new Parameters(spec.bundleSymbolicName).toString());
		}
		if (!spec.fragmentHost.isEmpty()) {
			setProperty(Constants.FRAGMENT_HOST, new Parameters(spec.fragmentHost).toString());
		}

		if (spec.bundleVersion != null) {
			setBundleVersion(spec.bundleVersion);
		}

		for (String path : spec.classpath) {
			this.addClasspath(new File(path));
		}

		if (!spec.exportContents.isEmpty()) {
			setProperty(Constants.EXPORT_CONTENTS, new Parameters(spec.exportContents).toString());
		}

		if (!spec.exportPackage.isEmpty()) {
			setProperty(Constants.EXPORT_PACKAGE, new Parameters(spec.exportPackage).toString());
		}

		if (!spec.importPackage.isEmpty()) {
			setProperty(Constants.IMPORT_PACKAGE, new Parameters(spec.importPackage).toString());
		}

		if (!spec.includeresource.isEmpty()) {
			setProperty(Constants.INCLUDE_RESOURCE, new Parameters(spec.includeresource).toString());
		}

		if (!spec.privatePackage.isEmpty()) {
			setProperty(Constants.PRIVATEPACKAGE, new Parameters(spec.privatePackage).toString());
		}

		if (!spec.provideCapability.isEmpty()) {
			setProperty(Constants.PROVIDE_CAPABILITY, new Parameters(spec.provideCapability).toString());
		}

		if (!spec.requireBundle.isEmpty()) {
			setProperty(Constants.REQUIRE_BUNDLE, new Parameters(spec.requireBundle).toString());
		}

		if (!spec.requireCapability.isEmpty()) {
			setProperty(Constants.REQUIRE_CAPABILITY, new Parameters(spec.requireCapability).toString());
		}

		MapStream.of(spec.other)
			.forEach(this::setProperty);

		return this;

	}

}
