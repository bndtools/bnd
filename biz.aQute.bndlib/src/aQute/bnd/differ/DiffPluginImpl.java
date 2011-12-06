package aQute.bnd.differ;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.jar.*;

import aQute.bnd.annotation.*;
import aQute.bnd.differ.Element.*;
import aQute.bnd.service.diff.*;
import aQute.bnd.service.diff.Type;
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
	final static Set<String>	MAJOR_HEADERS	= new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

	/**
	 * Headers that are considered not major enough to be considered
	 */
	final static Set<String>	IGNORE_HEADERS	= new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

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
	public Diff diff(Jar newer, Jar older) throws Exception {
		return new DiffImpl(bundleElement(newer), bundleElement(older));
	}

	/**
	 * Create an element representing a bundle from the Jar.
	 * 
	 * @param jar
	 *            The Jar to be analyzed
	 * @return the elements that should be compared
	 * @throws Exception
	 */
	private Element bundleElement(Jar jar) throws Exception {
		List<Element> result = new ArrayList<Element>();

		Manifest manifest = jar.getManifest();
		Analyzer analyzer = new Analyzer();

		try {

			analyzer.setJar(jar);

			if (manifest != null) {
				result.add(apiElement(analyzer));
				result.add(manifestElement(manifest));
			}
			result.add(resourcesElement(jar));
		} finally {
			analyzer.setJar((Jar) null); // prevent it from being closed as well
			analyzer.close();
		}
		return new Structured(Type.BUNDLE, jar.getName(), result, false);
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
				resources.add(new Leaf(Type.RESOURCE, entry.getKey(), value, false));
			} finally {
				in.close();
			}
		}
		return new Structured(Type.RESOURCES, "<resources>", resources, false);
	}

	/**
	 * Calculate the class element. This requires parsing the class file and
	 * finding all the methods that were added etc. The parsing will take super
	 * interfaces and super classes into account. For this reason it maintains a
	 * queue of classes/interfaces to parse.
	 * 
	 * @param analyzer
	 * @param clazz
	 * @return
	 * @throws Exception
	 */
	private Element classElement(final Analyzer analyzer, Clazz clazz,
			final Set<String> notAccessible) throws Exception {

		final List<Element> members = Create.list();
		final List<Clazz> queue = Create.list();
		final List<Clazz.MethodDef> methods = Create.list();
		final List<Clazz.FieldDef> fields = Create.list();
		final MultiMap<Clazz.Def, Element> annotations = new MultiMap<Clazz.Def, Element>();
		final AtomicBoolean consumer = new AtomicBoolean(true);

		queue.add(clazz);

		while (!queue.isEmpty()) {
			final Clazz rover = queue.remove(0);
			rover.parseClassFileWithCollector(new ClassDataCollector() {
				boolean			memberEnd;
				Clazz.FieldDef	last;

				@Override public void version(int minor, int major) {

					members.add(new Leaf(Type.CLASS_VERSION, "<bytecode>", major + "." + minor,
							true));
				}

				@Override public void method(Clazz.MethodDef defined) {
					if (Modifier.isProtected(defined.access) || Modifier.isPublic(defined.access)) {
						last = defined;
						methods.add(defined);
					} else {
						last = null;
					}
				}

				@Override public void field(Clazz.FieldDef defined) {
					if (Modifier.isProtected(defined.access) || Modifier.isPublic(defined.access)) {
						last = defined;
						fields.add(defined);
					} else
						last = null;
				}

				@Override public void constant(Object o) {
					if (last != null) {
						// Must be accessible now
						last.constant = o;
					}
				}

				@Override public void extendsClass(String name) {
					try {
						if (!rover.isInterface()) {
							Clazz c = analyzer.findClass(name);
							if (c != null)
								queue.add(c);
						}
					} catch (Exception e) {
						// too bad
					}
					String fqn = Clazz.binaryToFQN(name);
					members.add(new Leaf(Type.EXTENDS, fqn, null, false));
				}

				@Override public void implementsInterfaces(String names[]) {
					// TODO is interface reordering important for binary
					// compatibility??

					try {
						if (rover.isInterface()) {
							for (String name : names) {
								Clazz c = analyzer.findClass(name);
								if (c != null)
									queue.add(c);
							}
						}
					} catch (Exception e) {
						// too bad
					}

					for (String name : names) {
						String fqn = Clazz.binaryToFQN(name);
						members.add(new Leaf(Type.IMPLEMENTS, "<implements>." + fqn, null, false));
					}
				}

				@Override public void annotation(Annotation annotation) {
					Collection<Element> properties = Create.set();

					for (String key : annotation.keySet()) {
						properties.add(new Leaf(Type.PROPERTY, key, "" + annotation.get(key), false));
					}

					if (memberEnd) {
						members.add(new Structured(Type.ANNOTATION, annotation.getName(),
								properties, false));
						String name = Clazz.binaryToFQN(annotation.getName());
						if (ProviderType.class.getName().equals(name)) {
							consumer.set(false);
						}
					} else if (last != null)
						annotations.add(last, new Structured(Type.ANNOTATION, annotation.getName(),
								properties, false));
				}

				@Override public void innerClass(String innerClass, String outerClass,
						String innerName, int innerClassAccessFlags) {
					if (Modifier.isProtected(innerClassAccessFlags)
							|| Modifier.isPublic(innerClassAccessFlags))
						return;

					notAccessible.add(Clazz.binaryToFQN(innerClass));
				}

				@Override public void memberEnd() {
					memberEnd = true;
				}
			});

			for (Clazz.MethodDef m : methods) {

				Element member = new Structured(Type.METHOD, Clazz.descriptorToJava(m.name,
						m.descriptor), annotations.get(m), consumer.get() == true
						&& clazz.isInterface());

				members.add(member);
			}

			for (Clazz.FieldDef f : fields) {
				Set<Element> children = annotations.get(f);

				// Fields can have a constant value, this is a new element
				if (f.constant != null) {
					if (children == null)
						children = Create.set();
					children.add(new Leaf(Type.CONSTANT, "<constant>", f.constant.toString(), true));
				}

				Element member = new Structured(Type.FIELD, Clazz.descriptorToJava(f.name,
						f.descriptor), children, false);

				members.add(member);
			}
		}

		// And make the result
		return new Structured(clazz.isInterface() ? Type.INTERFACE : Type.CLASS, clazz.getFQN(),
				members, false);
	}

	/**
	 * Create an element for the API. We take the exported packages and traverse
	 * those for their classes. If there is no manifest or it does not describe
	 * a bundle we assume the whole contents is exported.
	 */
	private Element apiElement(Analyzer analyzer) throws Exception {
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

		for (Clazz c : analyzer.getClassspace().values()) {
			if (c.isPublic() || c.isProtected()) {
				String packageName = c.getPackage();

				if (exports.containsKey(packageName)) {
					Element cdef = classElement(analyzer, c, notAccessible);
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
			Element pd = new Structured(Type.PACKAGE, entry.getKey(), set, false);
			result.add(pd);
		}
		return new Structured(Type.API, analyzer.getBsn(), result, false);
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
						parameterDef.add(new Leaf(Type.PARAMETER, parameter.getKey(), parameter
								.getValue(), false));
					}
					clausesDef
							.add(new Structured(Type.CLAUSE, clause.getKey(), parameterDef, false));
				}
				result.add(new Structured(Type.HEADER, header, clausesDef, false));
			} else {
				result.add(new Leaf(Type.HEADER, header, value, false));
			}
		}
		return new Structured(Type.MANIFEST, "<manifest>", result, false);
	}

}
