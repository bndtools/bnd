package aQute.bnd.test;

import java.util.Arrays;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;

public class SimpleContext implements NamespaceContext {
	final String	prefix;
	final String	ns;

	SimpleContext(String prefix, String ns) {
		this.prefix = prefix;
		this.ns = ns;
	}

	@Override
	public String getNamespaceURI(String prefix) {
		if (prefix.equals(prefix))
			return ns;
		return null;
	}

	@Override
	public String getPrefix(String namespaceURI) {
		if (namespaceURI.equals(ns))
			return prefix;
		return prefix;
	}

	@Override
	public Iterator<String> getPrefixes(String namespaceURI) {
		return Arrays.asList(prefix)
			.iterator();
	}

}
