package org.osgi.service.indexer.impl;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.osgi.framework.Version;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.indexer.Builder;
import org.osgi.service.indexer.Capability;
import org.osgi.service.indexer.Namespaces;
import org.osgi.service.indexer.Requirement;
import org.osgi.service.indexer.Resource;
import org.osgi.service.indexer.ResourceAnalyzer;
import org.osgi.service.indexer.impl.types.VersionKey;
import org.osgi.service.indexer.impl.types.VersionRange;
import org.osgi.service.log.LogService;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SCRAnalyzer implements ResourceAnalyzer {
	public static final String NS_1_0 = Namespaces.NS_OSGI + "/scr/v1.0.0";
	public static final String NS_1_1 = Namespaces.NS_OSGI + "/scr/v1.1.0";
	public static final String NS_1_2 = Namespaces.NS_OSGI + "/scr/v1.2.0";

	public static final String ELEMENT_COMPONENT = "component";
	public static final String ELEMENT_SERVICE = "service";
	public static final String ELEMENT_PROVIDE = "provide";
	public static final String ELEMENT_REFERENCE = "reference";

	public static final String ATTRIB_INTERFACE = "interface";

	public static final String ATTRIB_CARDINALITY = "cardinality";
	public static final String ATTRIB_CARDINALITY_DEFAULT = "1..1";

	private LogService log;

	public SCRAnalyzer(LogService log) {
		this.log = log;
	}

	public void analyzeResource(Resource resource, List<Capability> caps, List<Requirement> reqs) throws Exception {
		String header = resource.getManifest().getMainAttributes().getValue(ComponentConstants.SERVICE_COMPONENT);
		if (header == null)
			return;

		StringTokenizer tokenizer = new StringTokenizer(header, ",");
		Version highest = null;
		while (tokenizer.hasMoreTokens()) {
			String pattern = tokenizer.nextToken().trim();
			List<String> paths = Util.findMatchingPaths(resource, pattern);
			if (paths != null)
				for (String path : paths) {
					Version version = processScrXml(resource, path, caps, reqs);
					if (highest == null || (version.compareTo(highest) > 0))
						highest = version;
				}
		}

		if (highest != null) {
			Version lower = new Version(highest.getMajor(), highest.getMinor(), 0);
			Version upper = new Version(highest.getMajor() + 1, 0, 0);
			Requirement requirement = createRequirement(new VersionRange(true, lower, upper, false));
			reqs.add(requirement);
		}
	}

	private Version processScrXml(Resource resource, String path, List<Capability> caps, List<Requirement> reqs) throws IOException {
		Resource childResource = resource.getChild(path);
		if (childResource == null) {
			if (log != null)
				log.log(LogService.LOG_WARNING,
						MessageFormat.format("Cannot analyse SCR requirement version: resource {0} does not contain path {1} referred from Service-Component header.",
								resource.getLocation(), path));
			return null;
		}

		SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setNamespaceAware(true);

		try {
			SAXParser parser = spf.newSAXParser();
			SCRContentHandler handler = new SCRContentHandler(caps, reqs);
			parser.parse(childResource.getStream(), handler);

			return handler.highest;
		} catch (Exception e) {
			if (log != null)
				log.log(LogService.LOG_ERROR, MessageFormat.format("Processing error: failed to parse child resource {0} in resource {1}.", path, resource.getLocation()), e);
			return null;
		}
	}

	private static Requirement createRequirement(VersionRange range) {
		Builder builder = new Builder().setNamespace(Namespaces.NS_EXTENDER);

		StringBuilder filter = new StringBuilder();
		filter.append('(').append(Namespaces.NS_EXTENDER).append('=').append(Namespaces.EXTENDER_SCR).append(')');

		filter.insert(0, "(&");
		Util.addVersionFilter(filter, range, VersionKey.PackageVersion);
		filter.append(')');

		builder.addDirective(Namespaces.DIRECTIVE_FILTER, filter.toString()).addDirective(Namespaces.DIRECTIVE_EFFECTIVE, Namespaces.EFFECTIVE_ACTIVE);
		Requirement requirement = builder.buildRequirement();
		return requirement;
	}

	private static class SCRContentHandler extends DefaultHandler {
		private List<Capability> caps;
		private List<Requirement> reqs;

		private boolean inComponent = false;
		private boolean inService = false;
		private Version highest = null;

		public SCRContentHandler(List<Capability> caps, List<Requirement> reqs) {
			super();
			this.caps = caps;
			this.reqs = reqs;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attribs) throws SAXException {
			super.startElement(uri, localName, qName, attribs);

			String localNameLowerCase = localName.toLowerCase();
			if (ELEMENT_COMPONENT.equals(localNameLowerCase)) {
				inComponent = true;
			}

			if (inComponent) {
				if (uri == null || "".equals(uri)) {
					setVersion(new Version(1, 0, 0));
				} else {
					if (NS_1_2.equals(uri))
						setVersion(new Version(1, 2, 0));
					else if (NS_1_1.equals(uri))
						setVersion(new Version(1, 1, 0));
					else if (NS_1_0.equals(uri))
						setVersion(new Version(1, 0, 0));
					else
						throw new SAXException("Unknown namespace " + uri);
				}

				if (ELEMENT_SERVICE.equals(localNameLowerCase)) {
					inService = true;
					return;
				}

				if (inService && ELEMENT_PROVIDE.equals(localNameLowerCase)) {
					createServiceCapability(attribs);
					return;
				}

				if (ELEMENT_REFERENCE.equals(localNameLowerCase)) {
					createServiceRequirement(attribs);
					return;
				}
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			String localNameLowerCase = localName.toLowerCase();
			if (ELEMENT_COMPONENT.equals(localNameLowerCase)) {
				inComponent = false;
			}

			if (inComponent) {
				if (ELEMENT_SERVICE.equals(localNameLowerCase)) {
					inService = false;
					return;
				}
			}

			super.endElement(uri, localName, qName);
		}

		private void setVersion(Version version) {
			if (highest == null || (version.compareTo(highest) > 0))
				highest = version;
		}

		private void createServiceCapability(Attributes attribs) throws SAXException {
			String interfaceClass = attribs.getValue(ATTRIB_INTERFACE);
			if (interfaceClass == null || interfaceClass.length() == 0) {
				throw new SAXException("Missing required " + ATTRIB_INTERFACE + " attribute");
			}

			Builder builder = new Builder().setNamespace(Namespaces.NS_SERVICE);
			builder.addAttribute(Namespaces.NS_SERVICE, interfaceClass);
			builder.addDirective(Namespaces.DIRECTIVE_EFFECTIVE, Namespaces.EFFECTIVE_ACTIVE);

			caps.add(builder.buildCapability());
		}

		private void createServiceRequirement(Attributes attribs) throws SAXException {
			String interfaceClass = attribs.getValue(ATTRIB_INTERFACE);
			if (interfaceClass == null || interfaceClass.length() == 0) {
				throw new SAXException("Missing required " + ATTRIB_INTERFACE + " attribute");
			}

			String cardinality = attribs.getValue(ATTRIB_CARDINALITY);
			if (cardinality == null || cardinality.length() == 0) {
				cardinality = ATTRIB_CARDINALITY_DEFAULT;
			}

			Builder builder = new Builder().setNamespace(Namespaces.NS_SERVICE);
			builder.addAttribute(Namespaces.NS_SERVICE, interfaceClass);
			builder.addDirective(ATTRIB_CARDINALITY, cardinality.toLowerCase());
			if (cardinality.startsWith("0")) {
				builder.addDirective(Namespaces.DIRECTIVE_RESOLUTION, Namespaces.RESOLUTION_OPTIONAL);
			}
			builder.addDirective(Namespaces.DIRECTIVE_EFFECTIVE, Namespaces.EFFECTIVE_ACTIVE);

			reqs.add(builder.buildRequirement());
		}
	}
}
