package aQute.bnd.differ;

import static aQute.bnd.service.diff.Delta.CHANGED;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.About;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.diff.Differ;
import aQute.bnd.service.diff.Tree;
import aQute.bnd.service.diff.Tree.Data;
import aQute.bnd.service.diff.Type;
import aQute.bnd.version.Version;
import aQute.lib.collections.ExtList;
import aQute.lib.hex.Hex;
import aQute.lib.io.IO;
import aQute.libg.cryptography.Digester;
import aQute.libg.cryptography.SHA1;

/**
 * This Diff Plugin Implementation will compare JARs for their API (based on the
 * Bundle Class Path and exported packages), the Manifest, and the resources.
 * The differences are represented in a {@link aQute.bnd.service.diff.Diff Diff}
 * tree.
 */
public class DiffPluginImpl implements Differ {

	/**
	 * Headers that are considered major enough to parse according to spec and
	 * compare their constituents
	 */
	final static Set<String>	MAJOR_HEADERS	= new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

	/**
	 * Headers that are considered not major enough to be considered
	 */
	final static Set<String>	IGNORE_HEADERS	= new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

	/**
	 * Headers that have values that should be sorted
	 */
	final static Set<String>	ORDERED_HEADERS	= new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

	static {
		MAJOR_HEADERS.add(Constants.EXPORT_PACKAGE);
		MAJOR_HEADERS.add(Constants.IMPORT_PACKAGE);
		MAJOR_HEADERS.add(Constants.REQUIRE_BUNDLE);
		MAJOR_HEADERS.add(Constants.FRAGMENT_HOST);
		MAJOR_HEADERS.add(Constants.BUNDLE_SYMBOLICNAME);
		MAJOR_HEADERS.add(Constants.BUNDLE_LICENSE);
		MAJOR_HEADERS.add(Constants.BUNDLE_NATIVECODE);
		MAJOR_HEADERS.add(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
		MAJOR_HEADERS.add(Constants.DYNAMICIMPORT_PACKAGE);
		MAJOR_HEADERS.add(Constants.BUNDLE_VERSION);

		IGNORE_HEADERS.add(Constants.TOOL);
		IGNORE_HEADERS.add(Constants.BND_LASTMODIFIED);
		IGNORE_HEADERS.add(Constants.CREATED_BY);

		ORDERED_HEADERS.add(Constants.SERVICE_COMPONENT);
		ORDERED_HEADERS.add(Constants.TESTCASES);
	}

	Instructions localIgnore = null;

	/**
	 * @see aQute.bnd.service.diff.Differ#tree(aQute.bnd.osgi.Jar)
	 */
	public Tree tree(File newer) throws Exception {
		try (Jar jnewer = new Jar(newer)) {
			return tree(jnewer);
		}
	}

	/**
	 * @see aQute.bnd.service.diff.Differ#tree(aQute.bnd.osgi.Jar)
	 */
	@Override
	public Tree tree(Jar newer) throws Exception {
		try (Analyzer anewer = new Analyzer(newer)) {
			return tree(anewer);
		}
	}

	@Override
	public Tree tree(Analyzer newer) throws Exception {
		return bundleElement(newer);
	}

	/**
	 * Create an element representing a bundle from the Jar.
	 *
	 * @param infos
	 * @param jar The Jar to be analyzed
	 * @return the elements that should be compared
	 * @throws Exception
	 */
	private Element bundleElement(Analyzer analyzer) throws Exception {
		List<Element> result = new ArrayList<>();

		Manifest manifest = analyzer.getJar()
			.getManifest();

		if (manifest != null) {
			result.add(JavaElement.getAPI(analyzer));
			result.add(manifestElement(manifest));
		}
		result.add(resourcesElement(analyzer));
		return new Element(Type.BUNDLE, analyzer.getJar()
			.getName(), result, CHANGED, CHANGED, null);
	}

	/**
	 * Create an element representing all resources in the JAR
	 */
	private final static Pattern META_INF_P = Pattern.compile("META-INF/([^/]+\\.(MF|SF|DSA|RSA))|(SIG-.*)");

	private Element resourcesElement(Analyzer analyzer) throws Exception {
		Jar jar = analyzer.getJar();

		List<Element> resources = new ArrayList<>();

		for (Map.Entry<String, Resource> entry : jar.getResources()
			.entrySet()) {

			//
			// The manifest and other (signer) files are ignored
			// since they are extremely sensitive to time
			//

			if (META_INF_P.matcher(entry.getKey())
				.matches())
				continue;

			if (localIgnore != null && localIgnore.matches(entry.getKey()))
				continue;

			//
			// #794 Use sources for shas of classes in baselining
			// Since the compilers generate different bytecodes the
			// resource comparison by sha is very awkward for classes.
			// This code will not create an element for classes if a
			// directory with source code can be found.
			//

			String path = entry.getKey();

			if (path.endsWith(Constants.EMPTY_HEADER))
				continue;

			if (analyzer.since(About._3_0)) {

				//
				// Skip resources that have a source component in the same
				// Jar. Changes will be reported on that component
				//

				if (hasSource(analyzer, path))
					continue;

			}

			Resource resource = entry.getValue();

			try (InputStream in = resource.openInputStream(); Digester<SHA1> digester = SHA1.getDigester()) {
				IO.copy(in, digester);
				String value = Hex.toHexString(digester.digest()
					.digest());
				resources.add(new Element(Type.RESOURCE, entry.getKey(), Arrays.asList(new Element(Type.SHA, value)),
					CHANGED, CHANGED, null));
			}
		}
		return new Element(Type.RESOURCES, "<resources>", resources, CHANGED, CHANGED, null);
	}

	private boolean hasSource(Analyzer analyzer, String path) throws Exception {

		if (!path.endsWith(".class"))
			return false;

		TypeRef type = analyzer.getTypeRefFromPath(path);
		PackageRef packageRef = type.getPackageRef();
		Clazz clazz = analyzer.findClass(type);
		if (clazz == null)
			return false;

		String sourceFile = clazz.getSourceFile();
		if (sourceFile == null)
			return false;

		String source = "OSGI-OPT/src/" + packageRef.getBinary() + "/" + sourceFile;
		Resource sourceResource = analyzer.getJar()
			.getResource(source);
		if (sourceResource == null)
			return false;

		return true;
	}

	/**
	 * Create an element for each manifest header. There are
	 * {@link #IGNORE_HEADERS} and {@link #MAJOR_HEADERS} that will be treated
	 * differently.
	 *
	 * @param manifest
	 * @return the created {@code Element}
	 */

	private Element manifestElement(Manifest manifest) {
		List<Element> result = new ArrayList<>();

		for (Object key : manifest.getMainAttributes()
			.keySet()) {
			String header = key.toString();
			String value = manifest.getMainAttributes()
				.getValue(header);

			if (IGNORE_HEADERS.contains(header))
				continue;

			if (localIgnore != null && localIgnore.matches(header)) {
				continue;
			}

			if (MAJOR_HEADERS.contains(header)) {
				if (header.equalsIgnoreCase(Constants.BUNDLE_VERSION)) {
					String v = new Version(value).toStringWithoutQualifier();
					result.add(new Element(Type.HEADER, header + ":" + v, null, CHANGED, CHANGED, null));
				} else {
					Parameters clauses = OSGiHeader.parseHeader(value);
					Collection<Element> clausesDef = new ArrayList<>();
					for (Map.Entry<String, Attrs> clause : clauses.entrySet()) {
						Collection<Element> parameterDef = new ArrayList<>();
						for (Map.Entry<String, String> parameter : clause.getValue()
							.entrySet()) {
							String paramValue = parameter.getValue();
							if (Constants.EXPORT_PACKAGE.equals(header)
								&& Constants.USES_DIRECTIVE.equals(parameter.getKey())) {
								ExtList<String> uses = ExtList.from(parameter.getValue());
								Collections.sort(uses);
								paramValue = uses.join();
							}
							parameterDef.add(new Element(Type.PARAMETER, parameter.getKey() + ":" + paramValue, null,
								CHANGED, CHANGED, null));
						}
						clausesDef.add(new Element(Type.CLAUSE, clause.getKey(), parameterDef, CHANGED, CHANGED, null));
					}
					result.add(new Element(Type.HEADER, header, clausesDef, CHANGED, CHANGED, null));
				}
			} else if (ORDERED_HEADERS.contains(header)) {
				ExtList<String> values = ExtList.from(value);
				Collections.sort(values);
				result.add(new Element(Type.HEADER, header + ":" + values.join(), null, CHANGED, CHANGED, null));
			} else {
				result.add(new Element(Type.HEADER, header + ":" + value, null, CHANGED, CHANGED, null));
			}
		}
		return new Element(Type.MANIFEST, "<manifest>", result, CHANGED, CHANGED, null);
	}

	@Override
	public Tree deserialize(Data data) throws Exception {
		return new Element(data);
	}

	public void setIgnore(String diffignore) {
		setIgnore(new Parameters(diffignore));
	}

	public void setIgnore(Parameters diffignore) {
		if ((diffignore == null) || diffignore.isEmpty()) {
			localIgnore = null;
			return;
		}

		localIgnore = new Instructions(diffignore);
	}

}
