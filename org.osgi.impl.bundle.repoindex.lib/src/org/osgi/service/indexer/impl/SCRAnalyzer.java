package org.osgi.service.indexer.impl;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.indexer.Builder;
import org.osgi.service.indexer.Capability;
import org.osgi.service.indexer.Namespaces;
import org.osgi.service.indexer.Requirement;
import org.osgi.service.indexer.Resource;
import org.osgi.service.indexer.ResourceAnalyzer;
import org.osgi.service.indexer.impl.types.ScalarType;
import org.osgi.service.indexer.impl.types.TypedValue;
import org.osgi.service.indexer.impl.types.VersionKey;
import org.osgi.service.indexer.impl.types.VersionRange;
import org.osgi.service.log.LogService;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SCRAnalyzer implements ResourceAnalyzer {
	static final Pattern		URI_VERSION_P		= Pattern.compile("/scr/v(\\d+\\.\\d+\\.\\d+)$");
	public static final String	NS_1_0				= Namespaces.NS_OSGI + "/scr/v1.0.0";
	public static final String	NS_1_1				= Namespaces.NS_OSGI + "/scr/v1.1.0";
	public static final String	NS_1_2				= Namespaces.NS_OSGI + "/scr/v1.2.0";
	public static final String	NS_1_2_1			= Namespaces.NS_OSGI + "/scr/v1.2.1";
	public static final String	NS_1_3				= Namespaces.NS_OSGI + "/scr/v1.3.0";

	public static final String	ELEMENT_COMPONENT	= "component";
	public static final String	ELEMENT_SERVICE		= "service";
	public static final String	ELEMENT_PROVIDE		= "provide";
	public static final String	ELEMENT_REFERENCE	= "reference";
	public static final String	ELEMENT_PROPERTY	= "property";

	public static final String	ATTRIB_INTERFACE	= "interface";
	public static final String	ATTRIB_CARDINALITY	= "cardinality";
	public static final String	ATTRIB_NAME			= "name";
	public static final String	ATTRIB_TYPE			= "type";
	public static final String	ATTRIB_VALUE		= "value";
	public static final String	ATTRIB_TARGET		= "target";

	private LogService			log;

	public SCRAnalyzer(LogService log) {
		this.log = log;
	}

	public void analyzeResource(Resource resource, List<Capability> caps, List<Requirement> reqs) throws Exception {
		String header = null;
		Manifest manifest = resource.getManifest();
		if (manifest != null)
			header = manifest.getMainAttributes().getValue(ComponentConstants.SERVICE_COMPONENT);
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
					if (version == null)
						continue;
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

	private Version processScrXml(Resource resource, String path, List<Capability> caps, List<Requirement> reqs)
			throws IOException {
		Resource childResource = resource.getChild(path);
		if (childResource == null) {
			if (log != null)
				log.log(LogService.LOG_WARNING,
						MessageFormat.format(
								"Cannot analyse SCR requirement version: resource {0} does not contain path {1} referred from Service-Component header.",
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
				log.log(LogService.LOG_ERROR,
						MessageFormat.format("Processing error: failed to parse child resource {0} in resource {1}.",
								path, resource.getLocation()),
						e);
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

		builder.addDirective(Namespaces.DIRECTIVE_FILTER, filter.toString())
				.addDirective(Namespaces.DIRECTIVE_EFFECTIVE, Namespaces.EFFECTIVE_ACTIVE);
		Requirement requirement = builder.buildRequirement();
		return requirement;
	}

	private static class SCRContentHandler extends DefaultHandler {
		private List<Capability>	caps;
		private List<Requirement>	reqs;

		Version						highest					= null;

		private List<String>		provides				= null;
		private List<Requirement>	references				= null;

		private Map<String,Object>	properties				= null;
		private String				currentPropertyName		= null;
		private ScalarType			currentPropertyType		= null;
		private String				currentPropertyValue	= null;
		private StringBuilder		currentPropertyText		= null;

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
				provides = new LinkedList<>();
				properties = new LinkedHashMap<>();
				references = new LinkedList<>();

				if (uri == null || "".equals(uri)) {
					setVersion(new Version(1, 0, 0));
				} else {
					//
					// Actually, we do not care that match
					// since we just create a dependency on that
					// version. So lets parse the version out of the
					// URI assuming the URI will look similar in the future,
					// which is a realistic expectation. If the syntax
					// does not match, too bad.
					//

					Matcher m = URI_VERSION_P.matcher(uri);

					if (m.find()) {
						String v = m.group(1);
						setVersion(new Version(v));
					} else
						throw new SAXException("Unknown namespace " + uri);
				}
			}

			else if (ELEMENT_PROVIDE.equals(localNameLowerCase)) {
				String objectClass = attribs.getValue(ATTRIB_INTERFACE);
				provides.add(objectClass);
			}

			else if (ELEMENT_PROPERTY.equals(localNameLowerCase)) {
				currentPropertyName = attribs.getValue(ATTRIB_NAME);
				if (currentPropertyName == null)
					throw new SAXException("Missing required attribute '" + ATTRIB_NAME + "'.");
				currentPropertyType = typeOf(attribs.getValue(ATTRIB_TYPE));
				String value = attribs.getValue(ATTRIB_VALUE);
				if (value != null) {
					currentPropertyValue = value;
				} else {
					currentPropertyText = new StringBuilder();
				}
			}

			else if (ELEMENT_REFERENCE.equals(localNameLowerCase)) {
				references.add(createServiceRequirement(attribs));
			}
		}

		@Override
		public void characters(char[] chars, int start, int length) throws SAXException {
			if (currentPropertyName != null && currentPropertyText != null)
				currentPropertyText.append(chars, start, length);
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			String localNameLowerCase = localName.toLowerCase();

			if (ELEMENT_PROPERTY.equals(localNameLowerCase)) {
				if (currentPropertyValue != null) {
					Object value = readTyped(currentPropertyType, currentPropertyValue);
					properties.put(currentPropertyName, new TypedValue(currentPropertyType, value));
				} else if (currentPropertyText != null) {
					String[] lines = currentPropertyText.toString().split("\n");
					List<Object> values = new ArrayList<>(lines.length);
					for (int i = 0; i < lines.length; i++) {
						String line = lines[i].trim();
						if (line.length() > 0) {
							Object value = readTyped(currentPropertyType, line);
							values.add(value);
						}
					}
					properties.put(currentPropertyName, new TypedValue(currentPropertyType, values));
				}
				currentPropertyName = null;
				currentPropertyType = null;
				currentPropertyValue = null;
				currentPropertyText = null;
			}

			if (ELEMENT_COMPONENT.equals(localNameLowerCase)) {
				if (provides != null && !provides.isEmpty()) {
					Builder builder = new Builder().setNamespace(Namespaces.NS_SERVICE);
					builder.addAttribute(Constants.OBJECTCLASS, provides);
					for (Entry<String,Object> entry : properties.entrySet()) {
						builder.addAttribute(entry.getKey(), entry.getValue());
					}

					StringBuilder uses = new StringBuilder();
					boolean first = true;
					for (String objectClass : provides) {
						if (!first)
							uses.append(',');
						first = false;

						int dotindex = objectClass.lastIndexOf('.');
						if (dotindex < 0)
							throw new SAXException("Service interface in default package.");
						String pkgName = objectClass.substring(0, dotindex);
						uses.append(pkgName);
					}
					builder.addDirective(Constants.USES_DIRECTIVE, uses.toString());

					caps.add(builder.buildCapability());
				}

				if (references != null && !references.isEmpty()) {
					reqs.addAll(references);
				}
			}

			super.endElement(uri, localName, qName);
		}

		private void setVersion(Version version) {
			if (highest == null || (version.compareTo(highest) > 0))
				highest = version;
		}

		private static Requirement createServiceRequirement(Attributes attribs) throws SAXException {
			String interfaceClass = attribs.getValue(ATTRIB_INTERFACE);
			if (interfaceClass == null || interfaceClass.length() == 0) {
				throw new SAXException("Missing required " + ATTRIB_INTERFACE + " attribute");
			}

			Builder builder = new Builder().setNamespace(Namespaces.NS_SERVICE);

			String filter;

			// #770 https://github.com/bndtools/bnd/issues/770
			//
			// We should not include any target properties since this is more a
			// runtime
			// filter that is often overridden by config admin. Since this is
			// handled in
			// runtime anyway, we should be able to ignore it easily.
			// @formatter:off
			//			String target = attribs.getValue(ATTRIB_TARGET);
			//			if (target != null)
			//				filter = String.format("(&(%s=%s)%s)", Constants.OBJECTCLASS, interfaceClass, target);
			//			else
			// @formatter:on

			filter = String.format("(%s=%s)", Constants.OBJECTCLASS, interfaceClass);
			builder.addDirective(Namespaces.DIRECTIVE_FILTER, filter);

			String cardinality = attribs.getValue(ATTRIB_CARDINALITY);
			if (cardinality != null) {
				cardinality = cardinality.trim().toLowerCase();
				if (cardinality.length() > 0) {
					if ('0' == cardinality.charAt(0))
						builder.addDirective(Namespaces.DIRECTIVE_RESOLUTION, Namespaces.RESOLUTION_OPTIONAL);
					if ('n' == cardinality.charAt(cardinality.length() - 1))
						builder.addDirective(Namespaces.DIRECTIVE_CARDINALITY, Namespaces.CARDINALITY_MULTIPLE);
				}
			}

			builder.addDirective(Namespaces.DIRECTIVE_EFFECTIVE, Namespaces.EFFECTIVE_ACTIVE);
			return builder.buildRequirement();
		}

		private static ScalarType typeOf(String name) {
			if (name == null)
				return ScalarType.String;
			if (Long.class.getSimpleName().equals(name) || Integer.class.getSimpleName().equals(name)
					|| Short.class.getSimpleName().equals(name) || Byte.class.getSimpleName().equals(name))
				return ScalarType.Long;
			if (Float.class.getSimpleName().equals(name) || Double.class.getSimpleName().equals(name))
				return ScalarType.Double;
			return ScalarType.String;
		}

		private static Object readTyped(ScalarType type, String string) {
			switch (type) {
				case Long :
					return Long.parseLong(string);
				case Double :
					return Double.parseDouble(string);
				default :
					return string;
			}
		}
	}
}
