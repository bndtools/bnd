package aQute.bnd.make.calltree;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import aQute.bnd.osgi.ClassDataCollector;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.WriteResource;

/**
 * Create an XML call tree of a set of classes. The structure of the XML is:
 *
 * <pre>
 *  calltree ::= &lt;using&gt; &lt;usedby&gt; using ::= &lt;method&gt; *
 * usedby ::= &lt;method&gt; * method ::= &lt;ref&gt;
 * </pre>
 *
 * The <code>using</code> element contains methods in the set of classes and
 * their references. The <code>usedby</code> element contains the used methods
 * and their references to the set of classes. The <code>ref</code> element
 * contains the class, the method name, the descriptor, and a pretty print
 * version of the method. The XML does not contain an XML processor instruction
 * to make it easier to include in other XML. The encoding is always UTF-8. This
 * class can be used as a resource, just add it to a JAR and the data is
 * generated when the resource is written (saving time when the JAR is up to
 * date and does not have to be generated). However, the actual write method is
 * a static method and can be called as well:
 * {@link #writeCalltree(PrintWriter, Collection)}.
 */
public class CalltreeResource extends WriteResource {
	Collection<Clazz> classes;

	/**
	 * Create a resource for inclusion that will print a call tree.
	 *
	 * @param values the classes for which the call tree is generated.
	 */
	public CalltreeResource(Collection<Clazz> values) {
		this.classes = values;
		System.err.println(values);
	}

	/**
	 * We set the last modified to 0 so this resource does not force a new JAR
	 * if all other resources are up to date.
	 */
	@Override
	public long lastModified() {
		return 0;
	}

	/**
	 * The write method is called to write the resource. We just call the static
	 * method.
	 */
	@Override
	public void write(OutputStream out) throws Exception {
		OutputStreamWriter osw = new OutputStreamWriter(out, Constants.DEFAULT_CHARSET);
		PrintWriter pw = new PrintWriter(osw);
		try {
			writeCalltree(pw, classes);
		} finally {
			pw.flush();
		}
	}

	/**
	 * Print the call tree in XML.
	 *
	 * @param out The output writer
	 * @param classes The set of classes
	 * @throws Exception Any errors
	 */
	public static void writeCalltree(PrintWriter out, Collection<Clazz> classes) throws Exception {

		final Map<Clazz.MethodDef, Set<Clazz.MethodDef>> using = new TreeMap<>(COMPARATOR);
		final Map<Clazz.MethodDef, Set<Clazz.MethodDef>> usedby = new TreeMap<>(COMPARATOR);

		ClassDataCollector cd = new ClassDataCollector() {
			// Clazz.MethodDef source;

			// Before a method is parsed
			@Override
			public void method(Clazz.MethodDef source) {
				// this.source = source;
				xref(using, source, null);
				xref(usedby, source, null);
			}

			// For any reference in the previous method.
			// public void reference(Clazz.MethodDef reference) {
			// xref(using, source, reference);
			// xref(usedby, reference, source);
			// }
		};
		for (Clazz clazz : classes) {
			clazz.parseClassFileWithCollector(cd);
		}

		out.println("<calltree>");
		xref(out, "using", using);
		xref(out, "usedby", usedby);
		out.println("</calltree>");
	}

	/*
	 * Add a new reference
	 */
	static Comparator<Clazz.MethodDef> COMPARATOR = (a, b) -> {
		int r = a.getName()
			.compareTo(b.getName());
		return r != 0 ? r
			: a.descriptor()
				.compareTo(b.descriptor());
	};

	static void xref(Map<Clazz.MethodDef, Set<Clazz.MethodDef>> references, Clazz.MethodDef source,
		Clazz.MethodDef reference) {
		Set<Clazz.MethodDef> set = references.get(source);
		if (set == null)
			references.put(source, set = new TreeSet<>(COMPARATOR));
		if (reference != null)
			set.add(reference);
	}

	/*
	 * Print out either using or usedby sets
	 */
	private static void xref(PrintWriter out, String group, Map<Clazz.MethodDef, Set<Clazz.MethodDef>> references) {
		out.println("  <" + group + ">");
		for (Map.Entry<Clazz.MethodDef, Set<Clazz.MethodDef>> entry : references.entrySet()) {
			Clazz.MethodDef source = entry.getKey();
			Set<Clazz.MethodDef> refs = entry.getValue();
			method(out, "method", source, ">");
			for (Clazz.MethodDef ref : refs) {
				method(out, "ref", ref, "/>");
			}
			out.println("      </method>");
		}
		out.println("  </" + group + ">");
	}

	/*
	 * Print out a method.
	 */
	private static void method(PrintWriter out, String element, Clazz.MethodDef source, String closeElement) {
		out.println("      <" + element + " class='" + source.getContainingClass()
			.getFQN() + "'" + getAccess(source.getAccess())
			+ (source.isConstructor() ? "" : " name='" + source.getName() + "'") + " descriptor='"
			+ source.descriptor() + "' pretty='" + source.toString() + "'" + closeElement);
	}

	private static String getAccess(int access) {
		StringBuilder sb = new StringBuilder();
		if (Modifier.isPublic(access))
			sb.append(" public='true'");
		if (Modifier.isStatic(access))
			sb.append(" static='true'");
		if (Modifier.isProtected(access))
			sb.append(" protected='true'");
		if (Modifier.isInterface(access))
			sb.append(" interface='true'");

		return sb.toString();
	}

}
