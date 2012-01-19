package aQute.bnd.differ;

import static aQute.bnd.service.diff.Delta.*;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import aQute.bnd.service.diff.*;
import aQute.bnd.service.diff.Tree.Data;
import aQute.lib.hex.*;
import aQute.lib.io.*;
import aQute.lib.osgi.*;
import aQute.libg.cryptography.*;
import aQute.libg.header.*;


/**
 * This Diff Plugin Implementation will compare JARs for their API (based on the
 * Bundle Class Path and exported packages), the Manifest, and the resources.
 * The Differences are represented in a {@link Diff} tree.
 */
public class DiffPluginImpl implements Differ {
	
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
	 * @see aQute.bnd.service.diff.Differ#diff(aQute.lib.resource.Jar,
	 *      aQute.lib.resource.Jar)
	 */
	public Tree tree(File newer) throws Exception {
		Jar jnewer = new Jar(newer);
		try {
			return tree(jnewer);
		} finally {
			jnewer.close();
		}
	}

	/**
	 * 
	 * @see aQute.bnd.service.diff.Differ#diff(aQute.lib.resource.Jar,
	 *      aQute.lib.resource.Jar)
	 */
	public Tree tree(Jar newer) throws Exception {
		Analyzer anewer = new Analyzer();
		try {
				anewer.setJar(newer);
				return tree(anewer);
		} finally {
			anewer.setJar((Jar) null);
			anewer.close();
		}
	}

	public Tree tree(Analyzer newer) throws Exception {
		return bundleElement(newer);
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
	private Element bundleElement(Analyzer analyzer) throws Exception {
		List<Element> result = new ArrayList<Element>();

		Manifest manifest = analyzer.getJar().getManifest();

		if (manifest != null) {
			result.add(JavaElement.getAPI(analyzer));
			result.add(manifestElement(manifest));
		}
		result.add(resourcesElement(analyzer.getJar()));
		return new Element(Type.BUNDLE, analyzer.getJar().getName(), result, CHANGED, CHANGED,
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
						.add(new Element(Type.RESOURCE, entry.getKey()+"="+value, null, CHANGED, CHANGED, null));
			} finally {
				in.close();
			}
		}
		return new Element(Type.RESOURCES, "<resources>", resources, CHANGED, CHANGED, null);
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
				Parameters clauses = OSGiHeader.parseHeader(value);
				Collection<Element> clausesDef = new ArrayList<Element>();
				for (Map.Entry<String, Attrs> clause : clauses.entrySet()) {
					Collection<Element> parameterDef = new ArrayList<Element>();
					for (Map.Entry<String, String> parameter : clause.getValue().entrySet()) {
						parameterDef.add(new Element(Type.PARAMETER, parameter.getKey() + ":" + parameter
								.getValue(), null, CHANGED, CHANGED, null));
					}
					clausesDef.add(new Element(Type.CLAUSE, clause.getKey(), parameterDef,
							CHANGED, CHANGED, null));
				}
				result.add(new Element(Type.HEADER, header, clausesDef, CHANGED, CHANGED, null));
			} else {
				result.add(new Element(Type.HEADER, header +":"+ value, null,CHANGED, CHANGED, null));
			}
		}
		return new Element(Type.MANIFEST, "<manifest>", result, CHANGED, CHANGED, null);
	}

	public Tree deserialize(Data data) throws Exception {
		return new Element(data);
	}

}
