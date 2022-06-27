package aQute.bnd.differ;

import static aQute.bnd.osgi.Jar.METAINF_SIGNING_P;
import static aQute.bnd.service.diff.Delta.CHANGED;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

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
import aQute.bnd.unmodifiable.Sets;
import aQute.bnd.version.Version;
import aQute.lib.hex.Hex;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
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
	final static Set<String>	MAJOR_HEADERS	= Sets.of(		//
		Constants.EXPORT_PACKAGE,								//
		Constants.IMPORT_PACKAGE,																							//
		Constants.REQUIRE_BUNDLE,																							//
		Constants.FRAGMENT_HOST,																							//
		Constants.BUNDLE_SYMBOLICNAME,																						//
		Constants.BUNDLE_LICENSE,																							//
		Constants.BUNDLE_NATIVECODE,																						//
		Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT,																		//
		Constants.DYNAMICIMPORT_PACKAGE,																					//
		Constants.BUNDLE_VERSION);

	/**
	 * Headers that are considered not major enough to be considered
	 */
	final static Set<String>	IGNORED_HEADERS	= Sets.of(		//
		Constants.TOOL,											//
		Constants.BND_LASTMODIFIED,																			//
		Constants.CREATED_BY);

	/**
	 * Headers that have values that should be sorted
	 */
	final static Set<String>	ORDERED_HEADERS	= Sets.of(		//
		Constants.SERVICE_COMPONENT,							//
		Constants.TESTCASES);

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
	private Element resourcesElement(Analyzer analyzer) throws Exception {
		Jar jar = analyzer.getJar();

		List<Element> resources = new ArrayList<>();

		for (Map.Entry<String, Resource> entry : jar.getResources()
			.entrySet()) {

			String path = entry.getKey();
			//
			// The manifest and other (signer) files are ignored
			// since they are extremely sensitive to time
			//

			if (jar.getManifestName()
				.equals(path)
				|| METAINF_SIGNING_P.matcher(path)
				.matches()) {
				continue;
			}

			if (localIgnore != null && localIgnore.matches(path))
				continue;

			//
			// #794 Use sources for shas of classes in baselining
			// Since the compilers generate different bytecodes the
			// resource comparison by sha is very awkward for classes.
			// This code will not create an element for classes if a
			// directory with source code can be found.
			//

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
	 * {@link #IGNORED_HEADERS} and {@link #MAJOR_HEADERS} that will be treated
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
				.getValue((Name) key);

			if (IGNORED_HEADERS.contains(header))
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
								paramValue = Strings.splitAsStream(paramValue)
									.sorted()
									.collect(Strings.joining());
							}
							parameterDef.add(new Element(Type.PARAMETER, parameter.getKey() + ":" + paramValue, null,
								CHANGED, CHANGED, null));
						}
						clausesDef.add(new Element(Type.CLAUSE, clause.getKey(), parameterDef, CHANGED, CHANGED, null));
					}
					result.add(new Element(Type.HEADER, header, clausesDef, CHANGED, CHANGED, null));
				}
			} else if (ORDERED_HEADERS.contains(header)) {
				String sorted = Strings.splitAsStream(value)
					.sorted()
					.collect(Strings.joining());
				result.add(new Element(Type.HEADER, header + ":" + sorted, null, CHANGED, CHANGED, null));
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
