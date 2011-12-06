package aQute.lib.osgi;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.regex.*;
import java.util.zip.*;

import aQute.bnd.component.*;
import aQute.bnd.make.*;
import aQute.bnd.make.component.*;
import aQute.bnd.make.metatype.*;
import aQute.bnd.maven.*;
import aQute.bnd.service.*;

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
	Pattern						xdoNotCopy			= null;
	private static final int	SPLIT_MERGE_LAST	= 1;
	private static final int	SPLIT_MERGE_FIRST	= 2;
	private static final int	SPLIT_ERROR			= 3;
	private static final int	SPLIT_FIRST			= 4;
	private static final int	SPLIT_DEFAULT		= 0;

	List<File>					sourcePath			= new ArrayList<File>();

	Make						make				= new Make(this);

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

		dot = new Jar("dot");
		addClose(dot);
		try {
			long modified = Long.parseLong(getProperty("base.modified"));
			dot.updateModified(modified, "Base modified");
		} catch (Exception e) {
		}

		doExpand(dot);
		doIncludeResources(dot);
		doConditional(dot);
		dot = doWab(dot);

		// NEW!
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
		return dot;
	}

	/**
	 * Allow any local initialization by subclasses before we build.
	 */
	public void init() throws Exception {
		begin();
		doRequireBnd();
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

		Jar next = new Jar(dot.getName());
		addClose(next);

		for (Map.Entry<String, Resource> entry : dot.getResources().entrySet()) {
			String path = entry.getKey();
			if (path.indexOf('/') > 0 && !Character.isUpperCase(path.charAt(0))) {
				trace("wab: moving: %s", path);
				next.putResource("WEB-INF/classes/" + path, entry.getValue());
			} else {
				trace("wab: not moving: %s", path);
				next.putResource(path, entry.getValue());
			}
		}

		Map<String, Map<String, String>> clauses = parseHeader(getProperty(WABLIB));
		for (String key : clauses.keySet()) {
			File f = getFile(key);
			addWabLib(next, f);
		}
		doIncludeResource(next, wab);
		return next;
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

		Map<String, Map<String, String>> infos = parseHeader(signing);
		for (Map.Entry<String, Map<String, String>> entry : infos.entrySet()) {
			for (SignerPlugin signer : signers) {
				signer.sign(this, entry.getKey());
			}
		}
	}

	public boolean hasSources() {
		return isTrue(getProperty(SOURCES));
	}

	protected String getImportPackages() {
		String ip = super.getImportPackages();
		if (ip != null)
			return ip;

		return "*";
	}

	private void doConditional(Jar dot) throws Exception {
		Map<String, Map<String, String>> conditionals = getHeader(CONDITIONAL_PACKAGE);
		if (conditionals.isEmpty())
			return;

		while (true) {
			analyze();
			Map<String, Map<String, String>> imports = getImports();

			// Match the packages specified in conditionals
			// against the imports. Any match must become a
			// Private-Package
			Map<String, Map<String, String>> filtered = merge(CONDITIONAL_PACKAGE, conditionals,
					imports, new HashSet<String>(), null);

			// Imports can also specify a private import. These
			// packages must also be copied to the bundle
			for (Map.Entry<String, Map<String, String>> entry : getImports().entrySet()) {
				String type = entry.getValue().get(IMPORT_DIRECTIVE);
				if (type != null && type.equals(PRIVATE_DIRECTIVE))
					filtered.put(entry.getKey(), entry.getValue());
			}

			// remove existing packages to prevent merge errors
			filtered.keySet().removeAll(dot.getPackages());
			if (filtered.size() == 0)
				break;

			int size = dot.getResources().size();
			doExpand(dot, CONDITIONAL_PACKAGE + " Private imports",
					Instruction.replaceWithInstruction(filtered), false);

			// Were there any expansions?
			if (size == dot.getResources().size())
				break;

			analyzed = false;
		}
	}

	/**
	 * Intercept the call to analyze and cleanup versions after we have analyzed
	 * the setup. We do not want to cleanup if we are going to verify.
	 */

	public void analyze() throws Exception {
		super.analyze();
		cleanupVersion(imports, null);
		cleanupVersion(exports, getVersion());
		String version = getProperty(BUNDLE_VERSION);
		if (version != null) {
			version = cleanupVersion(version);
			if (version.endsWith(".SNAPSHOT")) {
				version = version.replaceAll("SNAPSHOT$", getProperty(SNAPSHOT, "SNAPSHOT"));
			}
			setProperty(BUNDLE_VERSION, version);
		}
	}

	public void cleanupVersion(Map<String, Map<String, String>> mapOfMap, String defaultVersion) {
		for (Iterator<Map.Entry<String, Map<String, String>>> e = mapOfMap.entrySet().iterator(); e
				.hasNext();) {
			Map.Entry<String, Map<String, String>> entry = e.next();
			Map<String, String> attributes = entry.getValue();
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

		Set<String> packages = new HashSet<String>();

		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			getProperties().store(out, "Generated by BND, at " + new Date());
			dot.putResource("OSGI-OPT/bnd.bnd", new EmbeddedResource(out.toByteArray(), 0));
			out.close();
		} catch (Exception e) {
			error("Can not embed bnd file in JAR: " + e);
		}

		for (Iterator<String> cpe = classspace.keySet().iterator(); cpe.hasNext();) {
			String path = cpe.next();
			path = path.substring(0, path.length() - ".class".length()) + ".java";
			String pack = getPackage(path).replace('.', '/');
			if (pack.length() > 1)
				pack = pack + "/";
			boolean found = false;
			String[] fixed = { "packageinfo", "package.html", "module-info.java",
					"package-info.java" };
			for (Iterator<File> i = getSourcePath().iterator(); i.hasNext();) {
				File root = i.next();
				File f = getFile(root, path);
				if (f.exists()) {
					found = true;
					if (!packages.contains(pack)) {
						packages.add(pack);
						File bdir = getFile(root, pack);
						for (int j = 0; j < fixed.length; j++) {
							File ff = getFile(bdir, fixed[j]);
							if (ff.isFile()) {
								String name = "OSGI-OPT/src/" + pack + fixed[j];
								dot.putResource(name,
										new FileResource(ff));
							}
						}
					}
					if ( path.trim().length() == 0)
						System.out.println("Duh?");
					dot.putResource("OSGI-OPT/src/" + path, new FileResource(f));
				}
			}
			if (!found) {
				for (Jar jar : classpath) {
					Resource resource = jar.getResource(path);
					if (resource != null) {
						dot.putResource("OSGI-OPT/src/"+path, resource);
					} else {
						resource = jar.getResource("OSGI-OPT/src/" + path);
						if (resource != null) {
							dot.putResource("OSGI-OPT/src/"+path, resource);
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

	boolean	firstUse	= true;

	public Collection<File> getSourcePath() {
		if (firstUse) {
			firstUse = false;
			String sp = getProperty(SOURCEPATH);
			if (sp != null) {
				Map<String, Map<String, String>> map = parseHeader(sp);
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
		Verifier verifier = new Verifier(dot, getProperties());
		verifier.setPedantic(isPedantic());

		// Give the verifier the benefit of our analysis
		// prevents parsing the files twice
		verifier.setClassSpace(classspace, contained, referred, uses);
		verifier.verify();
		getInfo(verifier);
	}

	private void doExpand(Jar jar) throws IOException {
		if (getClasspath().size() == 0
				&& (getProperty(EXPORT_PACKAGE) != null || getProperty(EXPORT_PACKAGE) != null || getProperty(PRIVATE_PACKAGE) != null))
			warning("Classpath is empty. Private-Package and Export-Package can only expand from the classpath when there is one");

		Map<Instruction, Map<String, String>> privateMap = Instruction
				.replaceWithInstruction(getHeader(PRIVATE_PACKAGE));
		Map<Instruction, Map<String, String>> exportMap = Instruction
				.replaceWithInstruction(getHeader(EXPORT_PACKAGE));

		if (isTrue(getProperty(Constants.UNDERTEST))) {
			privateMap.putAll(Instruction.replaceWithInstruction(parseHeader(getProperty(
					Constants.TESTPACKAGES, "test;presence:=optional"))));
		}
		if (!privateMap.isEmpty())
			doExpand(jar, "Private-Package, or -testpackages", privateMap, true);

		if (!exportMap.isEmpty()) {
			Jar exports = new Jar("exports");
			doExpand(exports, EXPORT_PACKAGE, exportMap, true);
			jar.addAll(exports);
			exports.close();
		}

		if (!isNoBundle()) {
			if (privateMap.isEmpty() && exportMap.isEmpty() && !isResourceOnly()
					&& getProperty(EXPORT_CONTENTS) == null) {
				warning("None of Export-Package, Provide-Package, Private-Package, -testpackages, or -exportcontents is set, therefore no packages will be included");
			}
		}
	}

	/**
	 * 
	 * @param jar
	 * @param name
	 * @param instructions
	 */
	private void doExpand(Jar jar, String name, Map<Instruction, Map<String, String>> instructions,
			boolean mandatory) {
		Set<Instruction> superfluous = removeMarkedDuplicates(instructions.keySet());

		for (Iterator<Jar> c = getClasspath().iterator(); c.hasNext();) {
			Jar now = c.next();
			doExpand(jar, instructions, now, superfluous);
		}

		if (mandatory && superfluous.size() > 0) {
			StringBuilder sb = new StringBuilder();
			String del = "Instructions in " + name + " that are never used: ";
			for (Iterator<Instruction> i = superfluous.iterator(); i.hasNext();) {
				Instruction p = i.next();
				sb.append(del);
				sb.append(p.toString());
				del = "\n                ";
			}
			sb.append("\nClasspath: ");
			sb.append(Processor.join(getClasspath()));
			sb.append("\n");

			warning("%s",sb.toString());
			if (isPedantic())
				diagnostics = true;
		}
	}

	/**
	 * Iterate over each directory in the class path entry and check if that
	 * directory is a desired package.
	 * 
	 * @param included
	 * @param classpathEntry
	 */
	private void doExpand(Jar jar, Map<Instruction, Map<String, String>> included,
			Jar classpathEntry, Set<Instruction> superfluous) {

		loop: for (Map.Entry<String, Map<String, Resource>> directory : classpathEntry
				.getDirectories().entrySet()) {
			String path = directory.getKey();

			if (doNotCopy(getName(path)))
				continue;

			if (directory.getValue() == null)
				continue;

			String pack = path.replace('/', '.');
			Instruction instr = matches(included, pack, superfluous, classpathEntry.getName());
			if (instr != null) {
				// System.out.println("Pattern match: " + pack + " " +
				// instr.getPattern() + " " + instr.isNegated());
				if (!instr.isNegated()) {
					Map<String, Resource> contents = directory.getValue();

					// What to do with split packages? Well if this
					// directory already exists, we will check the strategy
					// and react accordingly.
					boolean overwriteResource = true;
					if (jar.hasDirectory(path)) {
						Map<String, String> directives = included.get(instr);

						switch (getSplitStrategy((String) directives.get(SPLIT_PACKAGE_DIRECTIVE))) {
						case SPLIT_MERGE_LAST:
							overwriteResource = true;
							break;

						case SPLIT_MERGE_FIRST:
							overwriteResource = false;
							break;

						case SPLIT_ERROR:
							error(diagnostic(pack, getClasspath(), classpathEntry.source));
							continue loop;

						case SPLIT_FIRST:
							continue loop;

						default:
							warning("%s", diagnostic(pack, getClasspath(), classpathEntry.source));
							overwriteResource = false;
							break;
						}
					}

					jar.addDirectory(contents, overwriteResource);

					String key = path + "/bnd.info";
					Resource r = jar.getResource(key);
					if (r != null)
						jar.putResource(key, new PreprocessResource(this, r));

					if (hasSources()) {
						String srcPath = "OSGI-OPT/src/" + path;
						Map<String, Resource> srcContents = classpathEntry.getDirectories().get(
								srcPath);
						if (srcContents != null) {
							jar.addDirectory(srcContents, overwriteResource);
						}
					}
				}
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
	private String diagnostic(String pack, List<Jar> classpath, File source) {
		// Default is like merge-first, but with a warning
		// Find the culprits
		pack = pack.replace('.', '/');
		List<Jar> culprits = new ArrayList<Jar>();
		for (Iterator<Jar> i = classpath.iterator(); i.hasNext();) {
			Jar culprit = (Jar) i.next();
			if (culprit.getDirectories().containsKey(pack)) {
				culprits.add(culprit);
			}
		}
		return "Split package "
				+ pack
				+ "\nUse directive -split-package:=(merge-first|merge-last|error|first) on Export/Private Package instruction to get rid of this warning\n"
				+ "Package found in   " + culprits + "\n" + "Reference from     " + source + "\n"
				+ "Classpath          " + classpath;
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
	 * @param superfluousPatterns
	 *            The total list of patterns, matched patterns are removed
	 * @param source
	 *            The name of the source container, can be filtered upon with
	 *            the from: directive.
	 * @return
	 */
	private Instruction matches(Map<Instruction, Map<String, String>> instructions, String pack,
			Set<Instruction> superfluousPatterns, String source) {
		for (Map.Entry<Instruction, Map<String, String>> entry : instructions.entrySet()) {
			Instruction pattern = entry.getKey();

			// It is possible to filter on the source of the
			// package with the from: directive. This is an
			// instruction that must match the name of the
			// source class path entry.

			String from = entry.getValue().get(FROM_DIRECTIVE);
			if (from != null) {
				Instruction f = Instruction.getPattern(from);
				if (!f.matches(source) || f.isNegated())
					return null;
			}

			// Now do the normal
			// matching
			if (pattern.matches(pack)) {
				if (superfluousPatterns != null)
					superfluousPatterns.remove(pattern);
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
		Map<String, Map<String, String>> clauses = parseHeader(includes);
		doIncludeResource(jar, clauses);
	}

	private void doIncludeResource(Jar jar, Map<String, Map<String, String>> clauses)
			throws ZipException, IOException, Exception {
		for (Map.Entry<String, Map<String, String>> entry : clauses.entrySet()) {
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

		InstructionFilter iFilter = null;
		if (filter != null) {
			iFilter = new InstructionFilter(Instruction.getPattern(filter), recursive,
					getDoNotCopy());
		} else {
			iFilter = new InstructionFilter(null, recursive, getDoNotCopy());
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
			instr = Instruction.getPattern(source.substring(n + 2));
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
			jar.addAll(sub, instr, destination);
		}
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

	private String getName(String where) {
		int n = where.lastIndexOf('/');
		if (n < 0)
			return where;

		return where.substring(n + 1);
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
			Map<String, Map<String, String>> map = parseHeader(conduit);
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

		Map<String, Map<String, String>> subsMap = parseHeader(sub);
		for (Iterator<String> i = subsMap.keySet().iterator(); i.hasNext();) {
			File file = getFile(i.next());
			if (file.isFile()) {
				builders.add(getSubBuilder(file));
				i.remove();
			}
		}

		Set<Instruction> subs = Instruction.replaceWithInstruction(subsMap).keySet();

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

			for (Iterator<Instruction> i = subs.iterator(); i.hasNext();) {

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
				for (String imp : getImports().keySet()) {
					if (!imp.startsWith("java.")) {
						sb.append("(org.osgi.framework.PackagePermission \"");
						sb.append(imp);
						sb.append("\" \"import\")\r\n");
					}
				}
				for (String exp : getExports().keySet()) {
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
		Map<String, Map<String, String>> clauses = parseHeader(getProperty(Constants.EXPORT_PACKAGE));
		clauses.putAll(parseHeader(getProperty(Constants.PRIVATE_PACKAGE)));
		if (isTrue(getProperty(Constants.UNDERTEST))) {
			clauses.putAll(parseHeader(getProperty(Constants.TESTPACKAGES,
					"test;presence:=optional")));
		}
		Map<Instruction, Map<String, String>> instructions = Instruction
				.replaceWithInstruction(clauses);

		for (File r : resources) {
			String cpEntry = getClasspathEntrySuffix(r);
			if (cpEntry != null) {
				String pack = Clazz.getPackage(cpEntry);
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

}
