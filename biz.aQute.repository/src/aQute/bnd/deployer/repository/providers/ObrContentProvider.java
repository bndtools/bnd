package aQute.bnd.deployer.repository.providers;

import static aQute.bnd.deployer.repository.api.Decision.*;
import static javax.xml.stream.XMLStreamConstants.*;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.xml.stream.*;
import javax.xml.transform.stream.*;

import org.osgi.framework.*;
import org.osgi.framework.namespace.*;
import org.osgi.namespace.service.*;
import org.osgi.resource.*;
import org.osgi.service.bindex.*;
import org.osgi.service.log.*;
import org.osgi.service.repository.*;

import aQute.bnd.deployer.repository.api.*;
import aQute.bnd.osgi.resource.*;
import aQute.bnd.service.*;
import aQute.lib.io.*;

public class ObrContentProvider implements IRepositoryContentProvider {

	public static final String	NAME						= "OBR";

	private static final String	INDEX_NAME					= "repository.xml";
	private static final String	EMPTY_REPO_TEMPLATE			= "<?xml version='1.0' encoding='UTF-8'?>%n<repository name='%s' lastmodified='0' xmlns='http://www.osgi.org/xmlns/obr/v1.0.0'/>";

	private static final String	NS_URI						= "http://www.osgi.org/xmlns/obr/v1.0.0";
	private static final String	PI_DATA_STYLESHEET			= "type='text/xsl' href='http://www2.osgi.org/www/obr2html.xsl'";
	private static final String	PI_TARGET_STYLESHEET		= "xml-stylesheet";

	private static final String	TAG_REPOSITORY				= "repository";

	private static final String	TAG_RESOURCE				= "resource";
	private static final String	ATTR_RESOURCE_SYMBOLIC_NAME	= "symbolicname";
	private static final String	ATTR_RESOURCE_URI			= "uri";
	private static final String	ATTR_RESOURCE_VERSION		= "version";

	private static final String	TAG_REFERRAL				= "referral";
	private static final String	ATTR_REFERRAL_URL			= "url";
	private static final String	ATTR_REFERRAL_DEPTH			= "depth";

	private static final String	TAG_CAPABILITY				= "capability";
	private static final String TAG_REQUIRE 				= "require";
	private static final String ATTR_NAME					= "name";
	private static final String ATTR_EXTEND					= "extend";
	private static final String ATTR_OPTIONAL				= "optional";
	private static final String ATTR_FILTER					= "filter";
	
	private static final String	TAG_PROPERTY				= "p";
	private static final String ATTR_PROPERTY_NAME			= "n";
	private static final String ATTR_PROPERTY_TYPE			= "t";
	private static final String ATTR_PROPERTY_VALUE			= "v";
	
	private static final String PROPERTY_USES				= "uses";
	private static final String TYPE_VERSION				= "version";

	private BundleIndexer		indexer;

	private static enum ParserState {
		beforeRoot, inRoot, inResource, inCapability
	}
	
	public ObrContentProvider(BundleIndexer indexer) {
		this.indexer = indexer;
	}

	public String getName() {
		return NAME;
	}

	public String getDefaultIndexName(boolean pretty) {
		return INDEX_NAME;
	}

	public void parseIndex(InputStream stream, URI baseUri, IRepositoryIndexProcessor listener, LogService log)
			throws Exception {
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		inputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
		inputFactory.setProperty(XMLInputFactory.IS_VALIDATING, false);
		inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

		StreamSource source = new StreamSource(stream, baseUri.toString());
		XMLStreamReader reader = inputFactory.createXMLStreamReader(source);
		
		ResourceBuilder resourceBuilder = null;
		CapReqBuilder capReqBuilder = null;

		while (reader.hasNext()) {
			int type = reader.next();
			String localName;

			switch (type) {
				case START_ELEMENT :
					localName = reader.getLocalName();
					if (TAG_REFERRAL.equals(localName)) {
						Referral referral = new Referral(reader.getAttributeValue(null, ATTR_REFERRAL_URL),
								parseInt(reader.getAttributeValue(null, ATTR_REFERRAL_DEPTH)));
						listener.processReferral(baseUri, referral, referral.getDepth(), 1);
					} else if (TAG_RESOURCE.equals(localName)) {
						resourceBuilder = new ResourceBuilder();
						
						
						String bsn = reader.getAttributeValue(null, ATTR_RESOURCE_SYMBOLIC_NAME);
						String versionStr = reader.getAttributeValue(null, ATTR_RESOURCE_VERSION);
						Version version = Version.parseVersion(versionStr);
						String uri = reader.getAttributeValue(null, ATTR_RESOURCE_URI);
						URI resolvedUri = resolveUri(uri, baseUri);
						addBasicCapabilities(resourceBuilder, bsn, version, resolvedUri);
						addUnresolvableRequirement(resourceBuilder);
					} else if (TAG_CAPABILITY.equals(localName)) {
						String obrName = reader.getAttributeValue(null, ATTR_NAME);
						String namespace = mapObrNameToR5Namespace(obrName, false);
						capReqBuilder = new CapReqBuilder(namespace);
					} else if (TAG_REQUIRE.equals(localName)) {
						String obrName = reader.getAttributeValue(null, ATTR_NAME);
						boolean extend = "true".equalsIgnoreCase(reader.getAttributeValue(null,  ATTR_EXTEND));
						String namespace = mapObrNameToR5Namespace(obrName, extend);
						boolean optional = "true".equalsIgnoreCase(reader.getAttributeValue(null, ATTR_OPTIONAL));
						
						capReqBuilder = new CapReqBuilder(namespace);
						if (optional)
							capReqBuilder.addDirective(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE, Namespace.RESOLUTION_OPTIONAL);
						String filter = translateObrFilter(namespace, reader.getAttributeValue(null, ATTR_FILTER));
						capReqBuilder.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter);
					} else if (TAG_PROPERTY.equals(localName)) {
						String name = reader.getAttributeValue(null, ATTR_PROPERTY_NAME);
						String typeStr = reader.getAttributeValue(null, ATTR_PROPERTY_TYPE);
						String valueStr = reader.getAttributeValue(null, ATTR_PROPERTY_VALUE);
						if (capReqBuilder != null) {
							name = mapObrPropertyToR5(capReqBuilder.getNamespace(), name);
							if (PROPERTY_USES.equals(name))
								capReqBuilder.addDirective(PROPERTY_USES, valueStr);
							else {
								Object value = convertProperty(valueStr, typeStr);
								capReqBuilder.addAttribute(name, value);
							}
						}
					}
					break;
				case END_ELEMENT:
					localName = reader.getLocalName();
					if (TAG_RESOURCE.equals(localName)) {
						if (resourceBuilder != null) {
							Resource resource = resourceBuilder.build();
							listener.processResource(resource);
						}
					} else if (TAG_CAPABILITY.equals(localName)) {
						if (resourceBuilder != null && capReqBuilder != null)
							resourceBuilder.addCapability(capReqBuilder);
						capReqBuilder = null;
					} else if (TAG_REQUIRE.equals(localName)) {
						if (resourceBuilder != null && capReqBuilder != null)
							resourceBuilder.addRequirement(capReqBuilder);
						capReqBuilder = null;
					}
			}
		}
	}
	
	private Object convertProperty(String value, String typeName) {
		final Object result;
		if (TYPE_VERSION.equals(typeName))
			result = Version.parseVersion(value);
		else
			result = value;
		return result;
	}
	
	private String mapObrNameToR5Namespace(String obrName, boolean extend) {
		if ("bundle".equals(obrName))
			return extend ? HostNamespace.HOST_NAMESPACE : BundleNamespace.BUNDLE_NAMESPACE;
		if ("package".equals(obrName))
			return PackageNamespace.PACKAGE_NAMESPACE;
		if ("service".equals(obrName))
			return ServiceNamespace.SERVICE_NAMESPACE;
		if ("ee".equals(obrName))
			return ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE;
		
		return obrName;
	}
	
	private String translateObrFilter(String namespace, String filter) {
		if (PackageNamespace.PACKAGE_NAMESPACE.equals(namespace))
			return filter.replaceAll("\\(package", "(" + PackageNamespace.PACKAGE_NAMESPACE);
		
		if (ServiceNamespace.SERVICE_NAMESPACE.equals(namespace))
			return filter.replaceAll("\\(service", "(" + ServiceNamespace.SERVICE_NAMESPACE);
		
		if (BundleNamespace.BUNDLE_NAMESPACE.equals(namespace)) {
			filter = filter.replaceAll("\\(symbolicname", "(" + BundleNamespace.BUNDLE_NAMESPACE);
			return filter.replaceAll("\\(version", "(" + BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);
		}
		
		if (ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE.equals(namespace))
			return filter.replaceAll("\\(ee", "(" + ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);
		
		return filter;
	}
	
	private String mapObrPropertyToR5(String namespace, String propName) {
		if (BundleNamespace.BUNDLE_NAMESPACE.equals(namespace)) {
			if ("symbolicname".equals(propName))
				return BundleNamespace.BUNDLE_NAMESPACE;
			if ("version".equals(propName))
				return BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE;
		}
		
		if (PackageNamespace.PACKAGE_NAMESPACE.equals(namespace)) {
			if ("package".equals(propName))
				return PackageNamespace.PACKAGE_NAMESPACE;
		}
		
		if (ServiceNamespace.SERVICE_NAMESPACE.equals(namespace)) {
			if ("service".equals(propName))
				return ServiceNamespace.SERVICE_NAMESPACE;
		}
		
		return propName;
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

	private void addBasicCapabilities(ResourceBuilder builder, String bsn, Version version, URI resolvedUri) {
		CapReqBuilder identity = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE)
			.addAttribute(IdentityNamespace.IDENTITY_NAMESPACE, bsn)
			.addAttribute(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, IdentityNamespace.TYPE_BUNDLE)
			.addAttribute(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, version);
		
		CapReqBuilder content = new CapReqBuilder(ContentNamespace.CONTENT_NAMESPACE)
			.addAttribute(ContentNamespace.CONTENT_NAMESPACE, "DUMMY")
			.addAttribute(ContentNamespace.CAPABILITY_URL_ATTRIBUTE, resolvedUri);
		
		CapReqBuilder host = new CapReqBuilder(HostNamespace.HOST_NAMESPACE)
			.addAttribute(HostNamespace.HOST_NAMESPACE, bsn)
			.addAttribute(HostNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, version);
		
		builder.addCapability(identity)
			.addCapability(content)
			.addCapability(host);
	}
	
	private void addUnresolvableRequirement(ResourceBuilder builder) {
		CapReqBuilder noresolve = new CapReqBuilder("bnd.noresolve").
				addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, "(bnd.noresolve=true)");
		builder.addRequirement(noresolve);
	}

	private static int parseInt(String value) {
		if (value == null || "".equals(value))
			return 0;
		return Integer.parseInt(value);
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
					case PROCESSING_INSTRUCTION :
						if (PI_TARGET_STYLESHEET.equals(reader.getPITarget())
								&& PI_DATA_STYLESHEET.equals(reader.getPIData()))
							return new CheckResult(accept, "Recognised stylesheet", null);
						break;
					case START_ELEMENT :
						localName = reader.getLocalName();
						switch (state) {
							case beforeRoot :
								String nsUri = reader.getNamespaceURI();
								if (nsUri != null)
									return CheckResult.fromBool(NS_URI.equals(nsUri),
											"Correct namespace on root element",
											"Incorrect namespace on root element: " + nsUri, null);
								if (!TAG_REPOSITORY.equals(localName))
									return new CheckResult(reject, "Incorrect root element name", null);
								state = ParserState.inRoot;
								break;
							case inRoot :
								if (TAG_RESOURCE.equals(localName)) {
									state = ParserState.inResource;
								} else if (!TAG_REFERRAL.equals(localName)) {
									return new CheckResult(reject, String.format(
											"Incorrect element '%s', expected '%s' or '%s'.", localName, TAG_RESOURCE,
											TAG_REFERRAL), null);
								}
								break;
							case inResource :
								if (TAG_CAPABILITY.equals(localName)) {
									state = ParserState.inCapability;
								}
								break;
							case inCapability :
								return CheckResult.fromBool(TAG_PROPERTY.equals(localName),
										"Found 'p' tag inside 'capability'", String.format(
												"Incorrect element '%s' inside '%s'; expected '%s'.", localName,
												TAG_CAPABILITY, TAG_PROPERTY), null);
						}
						break;
					case END_ELEMENT :
						localName = reader.getLocalName();
						if (state == ParserState.inResource && TAG_RESOURCE.equals(localName))
							state = ParserState.inRoot;
						if (state == ParserState.inCapability && TAG_CAPABILITY.equals(localName))
							state = ParserState.inResource;
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

	public boolean supportsGeneration() {
		return true;
	}

	public void generateIndex(Set<File> files, OutputStream output, String repoName, URI rootUri, boolean pretty, Registry registry, LogService log) throws Exception {
		if (!files.isEmpty()) {
			if (indexer == null)
				throw new IllegalStateException("Cannot index repository: no Bundle Indexer provided.");

			Map<String,String> config = new HashMap<String,String>();

			config.put(BundleIndexer.REPOSITORY_NAME, repoName);
			config.put(BundleIndexer.ROOT_URL, rootUri.toString());

			indexer.index(files, output, config);
		} else {
			String content = String.format(EMPTY_REPO_TEMPLATE, repoName);
			IO.copy(IO.stream(content), output);
		}
	}

}
