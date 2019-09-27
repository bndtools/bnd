package aQute.bnd.osgi.repository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.osgi.resource.Resource;
import org.osgi.service.repository.ContentNamespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.lib.strings.Strings;
import aQute.libg.gzip.GZipUtils;

public class XMLResourceParser extends Processor {
	private final static Logger		logger			= LoggerFactory.getLogger(XMLResourceParser.class);
	final static XMLInputFactory	inputFactory	= XMLInputFactory.newInstance();

	static {
		inputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
		inputFactory.setProperty(XMLInputFactory.IS_VALIDATING, false);
		inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
	}

	private static final String		NS_URI						= "http://www.osgi.org/xmlns/repository/v1.0.0";
	private static final String		TAG_REPOSITORY				= "repository";
	private static final String		TAG_REFERRAL				= "referral";
	private static final String		TAG_RESOURCE				= "resource";
	private static final String		TAG_CAPABILITY				= "capability";
	private static final String		TAG_REQUIREMENT				= "requirement";
	private static final String		TAG_ATTRIBUTE				= "attribute";
	private static final String		TAG_DIRECTIVE				= "directive";

	private static final String		ATTR_REFERRAL_URL			= "url";
	private static final String		ATTR_REFERRAL_DEPTH			= "depth";

	private static final String		ATTR_NAMESPACE				= "namespace";

	private static final String		ATTR_REPOSITORY_NAME		= "name";
	private static final String		ATTR_REPOSITORY_INCREMENT	= "increment";
	private static final String		ATTR_NAME					= "name";
	private static final String		ATTR_VALUE					= "value";
	private static final String		ATTR_TYPE					= "type";

	final private List<Resource>	resources					= new ArrayList<>();
	final private XMLStreamReader	reader;
	final private Set<URI>			traversed;
	final private String			what;
	final private URI				url;

	private int						depth;
	private String					name;
	private long					increment;

	public static List<Resource> getResources(URI uri) throws Exception {
		try (XMLResourceParser parser = new XMLResourceParser(uri)) {
			return parser.parse();
		}
	}

	public static List<Resource> getResources(File file) throws Exception {
		try (XMLResourceParser parser = new XMLResourceParser(file)) {
			return parser.parse();
		}
	}

	public static List<Resource> getResources(InputStream in, URI base) throws Exception {
		try (XMLResourceParser parser = new XMLResourceParser(in, "parse", base)) {
			return parser.parse();
		}
	}

	public XMLResourceParser(URI url) throws Exception {
		this(url.toURL()
			.openStream(), url.toString(), url);
	}

	public XMLResourceParser(InputStream in, String what, URI uri) throws Exception {
		this(in, what, 100, new HashSet<>(), uri);
	}

	public void setDepth(int n) {
		this.depth = n;
	}

	public XMLResourceParser(InputStream in, String what, int depth, Set<URI> traversed, URI url) throws Exception {
		this.what = what;
		this.depth = depth;
		this.traversed = traversed;
		this.url = url;
		in = GZipUtils.detectCompression(in);
		addClose(in);
		this.reader = inputFactory.createXMLStreamReader(in);
	}

	public XMLResourceParser(File location) throws Exception {
		this(location.toURI());
	}

	@Override
	public void close() throws IOException {
		try {
			reader.close();
		} catch (XMLStreamException e) {
			throw new IOException(e);
		} finally {
			super.close();
		}
	}

	public String name() {
		return name;
	}

	public long increment() {
		return increment;
	}

	List<Resource> getResources() {
		if (!isOk())
			return null;

		return resources;
	}

	public List<Resource> parse() throws Exception {
		if (!check(reader.hasNext(), "No content found"))
			return null;

		next();

		if (!check(reader.isStartElement(), "Expected a start element at the root, is %s", reader.getEventType()))
			return null;

		String localName = reader.getLocalName();
		if (!check(TAG_REPOSITORY.equals(localName), "Invalid tag name of top element, expected %s, got %s",
			TAG_REPOSITORY, localName))
			return null;

		String nsUri = reader.getNamespaceURI();
		if (nsUri != null) {
			check(NS_URI.equals(nsUri), "Incorrect namespace. Expected %s, got %s", NS_URI, nsUri);
		}

		name = reader.getAttributeValue(null, ATTR_REPOSITORY_NAME);
		String incrementString = reader.getAttributeValue(null, ATTR_REPOSITORY_INCREMENT);
		if (incrementString != null) {
			increment = Long.parseLong(incrementString);
		}

		next(); // either start resource/referral or end

		while (reader.isStartElement()) {
			localName = reader.getLocalName();
			if (localName.equals(TAG_REFERRAL))
				parseReferral();
			else if (localName.equals(TAG_RESOURCE))
				parseResource(resources);
			else {
				check(false, "Unexpected element %s", localName);
				next();
			}
		}

		check(reader.isEndElement() && reader.getLocalName()
			.equals(TAG_REPOSITORY), "Expected to be at the end but are on %s", reader.getLocalName());
		return getResources();
	}

	public void next() throws XMLStreamException {
		report();
		reader.nextTag();
	}

	private void report() {
		int type = reader.getEventType();
		switch (type) {
			case XMLStreamConstants.START_DOCUMENT :
			case XMLStreamConstants.START_ELEMENT :
			case XMLStreamConstants.END_ELEMENT :
				break;
			default :
				logger.debug("** unknown element, event type {}", type);
				break;
		}
	}

	private void parseReferral() throws Exception {
		if (--depth < 0)
			error("Too deep, traversed %s", traversed);
		else {
			String depthString = reader.getAttributeValue(null, ATTR_REFERRAL_DEPTH);
			String urlString = reader.getAttributeValue(null, ATTR_REFERRAL_URL);

			if (check(urlString != null, "Expected URL in referral")) {
				// TODO resolve url
				URI url = this.url.resolve(urlString);
				traversed.add(url);

				int depth = 100;
				if (depthString != null) {
					depth = Integer.parseInt(depthString);
				}
				InputStream in = url.toURL()
					.openStream();
				try (XMLResourceParser referralParser = new XMLResourceParser(in, urlString, depth, traversed, url)) {
					referralParser.parse();
					resources.addAll(referralParser.resources);
				}

			}
		}
		next();
		tagEnd(TAG_REFERRAL);
	}

	private void tagEnd(String tag) throws XMLStreamException {
		if (!check(reader.isEndElement(), "Expected end element, got %s for %s (%s)", reader.getEventType(), tag,
			reader.getLocalName())) {
			logger.debug("oops, invalid end {}", tag);
		}
		next();
	}

	private void parseResource(List<Resource> resources) throws Exception {
		ResourceBuilder resourceBuilder = new ResourceBuilder();

		next();
		while (reader.isStartElement()) {
			parseCapabilityOrRequirement(resourceBuilder);
		}
		Resource resource = resourceBuilder.build();
		resources.add(resource);
		tagEnd(TAG_RESOURCE);
	}

	private void parseCapabilityOrRequirement(ResourceBuilder resourceBuilder) throws Exception {
		String name = reader.getLocalName();
		check(TAG_REQUIREMENT.equals(name) || TAG_CAPABILITY.equals(name), "Expected <%s> or <%s> tag, got <%s>",
			TAG_REQUIREMENT, TAG_CAPABILITY, name);

		String namespace = reader.getAttributeValue(null, ATTR_NAMESPACE);

		CapReqBuilder capReqBuilder = new CapReqBuilder(namespace);

		next();
		while (reader.isStartElement()) {
			parseAttributesOrDirectives(capReqBuilder);
		}

		if (TAG_REQUIREMENT.equals(name)) {
			resourceBuilder.addRequirement(capReqBuilder);
		} else {
			resourceBuilder.addCapability(capReqBuilder);
		}

		tagEnd(name);
	}

	private void parseAttributesOrDirectives(CapReqBuilder capReqBuilder) throws Exception {
		String name = reader.getLocalName();
		switch (name) {
			case TAG_ATTRIBUTE :
				parseAttribute(capReqBuilder);
				break;

			case TAG_DIRECTIVE :
				parseDirective(capReqBuilder);
				break;

			default :
				check(false, "Invalid tag, expected either <%s> or <%s>, got <%s>", TAG_ATTRIBUTE, TAG_DIRECTIVE);
		}
		next();
		tagEnd(name);
	}

	private boolean check(boolean check, String format, Object... args) {

		if (check)
			return true;

		String message = Strings.format(format, args);
		error("%s: %s", what, message);

		return false;
	}

	private void parseAttribute(CapReqBuilder capReqBuilder) throws Exception {
		String attributeName = reader.getAttributeValue(null, ATTR_NAME);
		String attributeValue = reader.getAttributeValue(null, ATTR_VALUE);
		String attributeType = reader.getAttributeValue(null, ATTR_TYPE);

		if (isContent(capReqBuilder) && attributeName.equals("url")) {
			attributeValue = url.resolve(attributeValue)
				.toString();
		}

		Object value = Attrs.convert(attributeType, attributeValue);
		capReqBuilder.addAttribute(attributeName, value);
	}

	private boolean isContent(CapReqBuilder capReqBuilder) {
		return ContentNamespace.CONTENT_NAMESPACE.equals(capReqBuilder.getNamespace());
	}

	private void parseDirective(CapReqBuilder capReqBuilder) throws XMLStreamException {
		String attributeName = reader.getAttributeValue(null, ATTR_NAME);
		String attributeValue = reader.getAttributeValue(null, ATTR_VALUE);
		String attributeType = reader.getAttributeValue(null, ATTR_TYPE);

		check(attributeType == null, "Expected a directive to have no type: %s:%s=%s", attributeName, attributeType,
			attributeValue);

		capReqBuilder.addDirective(attributeName, attributeValue);
	}
}
