package aQute.bnd.differ;

import static aQute.bnd.service.diff.Delta.*;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import aQute.bnd.differ.Element.Structured;
import aQute.bnd.service.diff.*;
import aQute.lib.collections.*;
import aQute.lib.hex.*;
import aQute.lib.io.*;
import aQute.lib.osgi.*;
import aQute.libg.cryptography.*;
import aQute.libg.generics.*;
import aQute.libg.header.*;


/**
 * This Diff Plugin Implementation will compare JARs for their API (based on the
 * Bundle Class Path and exported packages), the Manifest, and the resources.
 * The Differences are represented in a {@link Diff} tree.
 */
public class DiffPluginImpl implements DiffPlugin {
	
	/**
	 * Headers that are considered major enough to parse according to spec and
	 * compare their constituents
	 */
	final static Set<String>				MAJOR_HEADERS	= new TreeSet<String>(
																	String.CASE_INSENSITIVE_ORDER);

	/**
	 * Headers that are considered not major enough to be considered
	 */
	final static Set<String>				IGNORE_HEADERS	= new TreeSet<String>(
																	String.CASE_INSENSITIVE_ORDER);

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

		IGNORE_HEADERS.add(Constants.TOOL);
		IGNORE_HEADERS.add(Constants.BND_LASTMODIFIED);
		IGNORE_HEADERS.add(Constants.CREATED_BY);
	}

	/**
	 * 
	 * @see aQute.bnd.service.diff.DiffPlugin#diff(aQute.lib.osgi.Jar,
	 *      aQute.lib.osgi.Jar)
	 */
	public Diff diff(Jar newer, Jar older, Info ...infos ) throws Exception {
		Analyzer anewer = new Analyzer();
		try {
			Analyzer aolder = new Analyzer();
			try {
				anewer.setJar(newer);
				aolder.setJar(older);
				return diff(anewer, aolder, infos);
			} finally {
				aolder.setJar((Jar) null);
				aolder.close();
			}
		} finally {
			anewer.setJar((Jar) null);
			anewer.close();
		}
	}

	public Diff diff(Analyzer newer, Analyzer older, Info ...infos ) throws Exception {
		return new DiffImpl(bundleElement(newer, infos), bundleElement(older,infos));
	}

	/**
	 * Create an element representing a bundle from the Jar.
	 * 
	 * @param infos 
	 * @param jar
	 *            The Jar to be analyzed
	 * @return the elements that should be compared
	 * @throws Exception
	 */
	private Element bundleElement(Analyzer analyzer, Info ... infos) throws Exception {
		List<Element> result = new ArrayList<Element>();

		Manifest manifest = analyzer.getJar().getManifest();

		if (manifest != null) {
			result.add(apiElement(analyzer, infos));
			result.add(manifestElement(manifest));
		}
		result.add(resourcesElement(analyzer.getJar()));
		return new Structured(Type.BUNDLE, "<name>", analyzer.getJar().getName(), result, CHANGED, CHANGED,
				null);
	}

	/**
	 * Create an element representing all resources in the JAR
	 * 
	 * @param jar
	 * @return
	 * @throws Exception
	 */
	private Element resourcesElement(Jar jar) throws Exception {
		List<Element> resources = new ArrayList<Element>();
		for (Map.Entry<String, Resource> entry : jar.getResources().entrySet()) {

			InputStream in = entry.getValue().openInputStream();
			try {
				Digester<SHA1> digester = SHA1.getDigester();
				IO.copy(in, digester);
				String value = Hex.toHexString(digester.digest().digest());
				resources
						.add(new Element(Type.RESOURCE, entry.getKey(), value, CHANGED, CHANGED, null));
			} finally {
				in.close();
			}
		}
		return new Structured(Type.RESOURCES, "<resources>", null, resources, CHANGED, CHANGED, null);
	}



	/**
	 * Create an element for the API. We take the exported packages and traverse
	 * those for their classes. If there is no manifest or it does not describe
	 * a bundle we assume the whole contents is exported.
	 * @param infos 
	 */
	private Element apiElement(Analyzer analyzer, Info ... infos) throws Exception {
		analyzer.analyze();

		Map<String, Map<String, String>> exports;

		Manifest manifest = analyzer.getJar().getManifest();
		if (manifest != null
				&& manifest.getMainAttributes().getValue(Constants.BUNDLE_MANIFESTVERSION) != null)
			exports = OSGiHeader.parseHeader(manifest.getMainAttributes().getValue(
					Constants.EXPORT_PACKAGE));
		else
			exports = analyzer.getContained();

		// we now need to gather all the packages but without
		// creating the packages yet because we do not yet know
		// which classes are accessible

		MultiMap<String, Element> packages = new MultiMap<String, Element>();
		Set<String> notAccessible = Create.set();
		Map<Clazz, Structured> cache = Create.map();

		for (Clazz c : analyzer.getClassspace().values()) {
			if (c.isPublic() || c.isProtected()) {
				String packageName = c.getPackage();

				if (exports.containsKey(packageName)) {
					Element cdef = TypedElement.classElement(analyzer, c, notAccessible, cache, infos);
					packages.add(packageName, cdef);
				}
			}
		}

		List<Element> result = new ArrayList<Element>();

		for (Map.Entry<String, Set<Element>> entry : packages.entrySet()) {
			Set<Element> set = entry.getValue();
			for (Iterator<Element> i = set.iterator(); i.hasNext();) {
				if (notAccessible.contains(i.next().getName()))
					i.remove();
			}
			Element pd = new Structured(Type.PACKAGE, entry.getKey(), null,set, MINOR, MAJOR, null);
			result.add(pd);
		}
		return new Structured(Type.API, "<api>", null, result, CHANGED, CHANGED, null);
	}

	/**
	 * Create an element for each manifest header. There are
	 * {@link #IGNORE_HEADERS} and {@link #MAJOR_HEADERS} that will be treated
	 * differently.
	 * 
	 * @param manifest
	 * @return
	 */

	private Element manifestElement(Manifest manifest) {
		List<Element> result = new ArrayList<Element>();

		for (Object key : manifest.getMainAttributes().keySet()) {
			String header = key.toString();
			String value = manifest.getMainAttributes().getValue(header);
			if (IGNORE_HEADERS.contains(header))
				continue;

			if (MAJOR_HEADERS.contains(header)) {
				Map<String, Map<String, String>> clauses = OSGiHeader.parseHeader(value);
				Collection<Element> clausesDef = new ArrayList<Element>();
				for (Map.Entry<String, Map<String, String>> clause : clauses.entrySet()) {
					Collection<Element> parameterDef = new ArrayList<Element>();
					for (Map.Entry<String, String> parameter : clause.getValue().entrySet()) {
						parameterDef.add(new Element(Type.PARAMETER, parameter.getKey(), parameter
								.getValue(), CHANGED, CHANGED, null));
					}
					clausesDef.add(new Structured(Type.CLAUSE, clause.getKey(), null, parameterDef,
							CHANGED, CHANGED, null));
				}
				result.add(new Structured(Type.HEADER, header, null, clausesDef, CHANGED, CHANGED, null));
			} else {
				result.add(new Element(Type.HEADER, header, value, CHANGED, CHANGED, null));
			}
		}
		return new Structured(Type.MANIFEST, "<manifest>", null, result, CHANGED, CHANGED, null);
	}

}
