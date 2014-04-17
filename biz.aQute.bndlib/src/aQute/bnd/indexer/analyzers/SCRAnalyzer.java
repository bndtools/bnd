package aQute.bnd.indexer.analyzers;

import javax.xml.parsers.*;

import org.osgi.resource.*;
import org.osgi.service.component.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

import aQute.bnd.header.*;
import aQute.bnd.indexer.*;
import aQute.bnd.osgi.*;
import aQute.bnd.osgi.Resource;
import aQute.bnd.osgi.resource.*;
import aQute.bnd.version.*;
import aQute.libg.glob.*;

public class SCRAnalyzer implements ResourceAnalyzer {
	public static final String	NS_1_0	= Namespaces.NS_OSGI + "/scr/v1.0.0";
	public static final String	NS_1_1	= Namespaces.NS_OSGI + "/scr/v1.1.0";
	public static final String	NS_1_2	= Namespaces.NS_OSGI + "/scr/v1.2.0";

	public void analyzeResource(Jar resource, ResourceBuilder rb) throws Exception {
		String header = resource.getManifest().getMainAttributes().getValue(ComponentConstants.SERVICE_COMPONENT);
		if (header == null)
			return;

		Parameters parameters = new Parameters(header);

		Version highest = null;
		for (String pattern : parameters.keySet()) {
			Glob glob = new Glob(pattern);
			for (String path : resource.getResources().keySet()) {
				if (glob.matcher(path).matches()) {
					Version version = processScrXml(resource.getResource(path), path);
					if (version != null && (highest == null || (version.compareTo(highest) > 0)))
						highest = version;
				}
			}
		}

		if (highest != null) {
			Version lower = new Version(highest.getMajor(), highest.getMinor(), 0);
			Version upper = new Version(highest.getMajor() + 1, 0, 0);
			Requirement requirement = createRequirement(new VersionRange(true, lower, upper, false));
			rb.addRequirement(requirement);
		}
	}

	private Version processScrXml(Resource resource, String path) throws Exception {
		if (resource == null) {
			return null;
		}

		SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setNamespaceAware(true);

		SAXParser parser = spf.newSAXParser();
		SCRContentHandler handler = new SCRContentHandler();
		parser.parse(resource.openInputStream(), handler);

		return handler.highest;
	}

	private static Requirement createRequirement(VersionRange range) {
		CapReqBuilder builder = new CapReqBuilder(Namespaces.NS_EXTENDER);

		StringBuilder filter = new StringBuilder();
		filter.append('(').append(Namespaces.NS_EXTENDER).append('=').append(Namespaces.EXTENDER_SCR).append(')');

		filter.insert(0, "(&");
		filter.append(range.toFilter());
		filter.append(')');

		builder.addDirective(Namespaces.DIRECTIVE_FILTER, filter.toString()).addDirective(
				Namespaces.DIRECTIVE_EFFECTIVE, Namespaces.EFFECTIVE_ACTIVE);
		Requirement requirement = builder.buildRequirement();
		return requirement;
	}

	private static class SCRContentHandler extends DefaultHandler {

		Version			highest		= null;
		private boolean	beforeRoot	= true;

		public SCRContentHandler() {}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attribs) throws SAXException {
			if (uri == null || "".equals(uri)) {
				if (beforeRoot) {
					beforeRoot = false;
					setVersion(new Version(1, 0, 0));
				}
			} else {
				if (NS_1_2.equals(uri))
					setVersion(new Version(1, 2, 0));
				else if (NS_1_1.equals(uri))
					setVersion(new Version(1, 1, 0));
				else if (NS_1_0.equals(uri))
					setVersion(new Version(1, 0, 0));
			}
		}

		private void setVersion(Version version) {
			if (highest == null || (version.compareTo(highest) > 0))
				highest = version;
		}
	}

}
