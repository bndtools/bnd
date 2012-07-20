package aQute.bnd.deployer.repository.providers;

import static aQute.bnd.deployer.repository.api.Decision.*;
import static javax.xml.stream.XMLStreamConstants.*;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.xml.stream.*;

import org.osgi.framework.*;
import org.osgi.resource.Resource;
import org.osgi.service.indexer.*;
import org.osgi.service.indexer.impl.*;
import org.osgi.service.log.*;
import org.osgi.service.repository.*;

import aQute.bnd.deployer.repository.api.*;
import aQute.bnd.osgi.resource.*;
import aQute.bnd.service.*;

public class R5RepoContentProvider implements IRepositoryContentProvider {

	public static final String	NAME					= "R5";

	private static final String	NS_URI					= "http://www.osgi.org/xmlns/repository/v1.0.0";

	private static final String	INDEX_NAME_COMPRESSED	= "index.xml.gz";
	private static final String	INDEX_NAME_PRETTY		= "index.xml";

	private static final String	TAG_REPOSITORY			= "repository";
	private static final String	TAG_REFERRAL			= "referral";
	private static final String	TAG_RESOURCE			= "resource";
	private static final String	TAG_CAPABILITY			= "capability";
	private static final String	TAG_REQUIREMENT			= "requirement";
	private static final String	TAG_ATTRIBUTE			= "attribute";
	private static final String	TAG_DIRECTIVE			= "directive";

	private static final String	ATTR_REFERRAL_URL		= "url";
	private static final String	ATTR_REFERRAL_DEPTH		= "depth";

	private static final String	ATTR_NAMESPACE			= "namespace";

	private static final String	ATTR_NAME				= "name";
	private static final String	ATTR_VALUE				= "value";
	private static final String	ATTR_TYPE				= "type";
	
	public String getName() {
		return NAME;
	}

	public String getDefaultIndexName(boolean pretty) {
		return pretty ? INDEX_NAME_PRETTY : INDEX_NAME_COMPRESSED;
	}

	private static enum ParserState {
		beforeRoot, inRoot, inResource, inCapability
	}

	public CheckResult checkStream(String name, InputStream stream) throws IOException {
		XMLStreamReader reader = null;
		try {
			XMLInputFactory inputFactory = XMLInputFactory.newInstance();

			inputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
			inputFactory.setProperty(XMLInputFactory.IS_VALIDATING, false);
			inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

			reader = inputFactory.createXMLStreamReader(stream);
			ParserState state = ParserState.beforeRoot;

			while (reader.hasNext()) {
				int type = reader.next();
				String localName;

				switch (type) {
					case START_ELEMENT :
						localName = reader.getLocalName();
						switch (state) {
							case beforeRoot :
								String nsUri = reader.getNamespaceURI();
								if (nsUri != null)
									return CheckResult.fromBool(NS_URI.equals(nsUri), "Corrent namespace",
											"Incorrect namespace: " + nsUri, null);
								if (!TAG_REPOSITORY.equals(localName))
									return new CheckResult(reject, "Incorrect root element name", null);
								state = ParserState.inRoot;
								break;
							case inRoot :
								if (TAG_RESOURCE.equals(localName)) {
									state = ParserState.inResource;
								}
								break;
							case inResource :
								if (TAG_REQUIREMENT.equals(localName))
									return new CheckResult(accept, "Recognised element 'requirement' in 'resource'",
											null);
								if (TAG_CAPABILITY.equals(localName))
									state = ParserState.inCapability;
								break;
							case inCapability :
								if (TAG_ATTRIBUTE.equals(localName) || TAG_DIRECTIVE.equals(localName)) {
									return new CheckResult(accept, "Recognised element '%s' in 'capability'", null);
								}
								break;
						}
						break;
					case END_ELEMENT :
						localName = reader.getLocalName();
						if (state == ParserState.inResource && TAG_RESOURCE.equals(localName))
							state = ParserState.inRoot;
						if (state == ParserState.inCapability && TAG_CAPABILITY.equals(localName))
							state = ParserState.inResource;
						break;
					default :
						break;
				}

			}
			return new CheckResult(undecided, "Reached end of stream", null);
		}
		catch (XMLStreamException e) {
			return new CheckResult(reject, "Invalid XML", e);
		}
		finally {
			if (reader != null)
				try {
					reader.close();
				}
				catch (XMLStreamException e) {}
		}
	}

	public void parseIndex(InputStream stream, URI baseUri, IRepositoryIndexProcessor listener, LogService log)
			throws Exception {
		XMLStreamReader reader = null;
		try {
			XMLInputFactory inputFactory = XMLInputFactory.newInstance();

			inputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
			inputFactory.setProperty(XMLInputFactory.IS_VALIDATING, false);
			inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

			reader = inputFactory.createXMLStreamReader(stream);

			ResourceBuilder resourceBuilder = null;
			CapReqBuilder capReqBuilder = null;

			while (reader.hasNext()) {
				int type = reader.next();
				String localName;

				switch (type) {
					case START_ELEMENT :
						localName = reader.getLocalName();
						if (TAG_REFERRAL.equals(localName)) {
							String url = reader.getAttributeValue(null, ATTR_REFERRAL_URL);
							String depth = reader.getAttributeValue(null, ATTR_REFERRAL_DEPTH);
							Referral referral = new Referral(url, parseInt(depth));
							listener.processReferral(baseUri, referral, 0, 0);
						} else if (TAG_RESOURCE.equals(localName)) {
							resourceBuilder = new ResourceBuilder();
						} else if (TAG_CAPABILITY.equals(localName) || TAG_REQUIREMENT.equals(localName)) {
							String namespace = reader.getAttributeValue(null, ATTR_NAMESPACE);
							capReqBuilder = new CapReqBuilder(namespace);
						} else if (TAG_ATTRIBUTE.equals(localName)) {
							String name = reader.getAttributeValue(null, ATTR_NAME);
							String valueStr = reader.getAttributeValue(null, ATTR_VALUE);
							String typeAttr = reader.getAttributeValue(null, ATTR_TYPE);
							if (capReqBuilder != null) {
								// If the attribute is 'url' on the osgi.content namespace then resolve it relative to the base URI.
								if (ContentNamespace.CONTENT_NAMESPACE.equals(capReqBuilder.getNamespace()) && ContentNamespace.CAPABILITY_URL_ATTRIBUTE.equals(name)) {
									URI resolvedUri = resolveUri(valueStr, baseUri);
									capReqBuilder.addAttribute(name, resolvedUri);
								} else {
									Object convertedAttr = convertAttribute(valueStr, typeAttr);
									capReqBuilder.addAttribute(name, convertedAttr);
								}
							}
						} else if (TAG_DIRECTIVE.equals(localName)) {
							String name = reader.getAttributeValue(null, ATTR_NAME);
							String valueStr = reader.getAttributeValue(null, ATTR_VALUE);
							if (capReqBuilder != null)
								capReqBuilder.addDirective(name, valueStr);
						}
						break;
					case END_ELEMENT :
						localName = reader.getLocalName();
						if (TAG_CAPABILITY.equals(localName)) {
							if (resourceBuilder != null && capReqBuilder != null)
								resourceBuilder.addCapability(capReqBuilder);
							capReqBuilder = null;
						} else if (TAG_REQUIREMENT.equals(localName)) {
							if (resourceBuilder != null && capReqBuilder != null)
								resourceBuilder.addRequirement(capReqBuilder);
							capReqBuilder = null;
						} else if (TAG_RESOURCE.equals(localName)) {
							if (resourceBuilder != null) {
								Resource resource = resourceBuilder.build();
								listener.processResource(resource);
								resourceBuilder = null;
							}
						}
						break;
				}
			}
		}
		finally {
			if (reader != null) {
				try {
					reader.close();
				}
				catch (Exception e) {}
			}
		}
	}

	private URI resolveUri(String uriStr, URI baseUri) throws URISyntaxException {
		URI resolved;
		
		URI resourceUri = new URI(uriStr);
		if (resourceUri.isAbsolute())
			resolved = resourceUri;
		else
			resolved = baseUri.resolve(resourceUri);
		
		return resolved;
	}

	private static int parseInt(String value) {
		if (value == null || "".equals(value))
			return 0;
		return Integer.parseInt(value);
	}

	private Object convertAttribute(String value, String type) {
		AttributeType attType = AttributeType.parseTypeName(type);
		return attType.parseString(value);
	}

	public boolean supportsGeneration() {
		return true;
	}

	public void generateIndex(Set<File> files, OutputStream output, String repoName, URI baseUri, boolean pretty, Registry registry, LogService log) throws Exception {
		BIndex2 indexer;
		if (log != null)
			indexer = new BIndex2(log);
		else
			indexer = new BIndex2();
		indexer.addAnalyzer(new KnownBundleAnalyzer(), FrameworkUtil.createFilter("(name=*)"));

		if (registry != null) {
			List<ResourceAnalyzer> analyzers = registry.getPlugins(ResourceAnalyzer.class);
			for (ResourceAnalyzer analyzer : analyzers) {
				// TODO: where to get the filter property??
				indexer.addAnalyzer(analyzer, null);
			}
		}

		Map<String,String> config = new HashMap<String,String>();
		config.put(ResourceIndexer.REPOSITORY_NAME, repoName);
		config.put(ResourceIndexer.ROOT_URL, baseUri.toString());
		config.put(ResourceIndexer.PRETTY, Boolean.toString(pretty));

		indexer.index(files, output, config);
	}
}
