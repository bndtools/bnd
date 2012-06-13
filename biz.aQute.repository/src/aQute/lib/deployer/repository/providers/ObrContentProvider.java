package aQute.lib.deployer.repository.providers;

import static aQute.lib.deployer.repository.api.Decision.*;
import static javax.xml.stream.XMLStreamConstants.*;

import java.io.*;
import java.util.*;

import javax.xml.stream.*;
import javax.xml.transform.stream.*;

import org.osgi.service.bindex.*;
import org.osgi.service.log.*;

import aQute.bnd.service.*;
import aQute.lib.deployer.repository.api.*;
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
	private static final String	TAG_PROPERTY				= "p";

	private static enum ParserState {
		beforeRoot, inRoot, inResource, inCapability
	}

	public String getName() {
		return NAME;
	}

	public String getDefaultIndexName(boolean pretty) {
		return INDEX_NAME;
	}

	public void parseIndex(InputStream stream, String baseUrl, IRepositoryListener listener, LogService log)
			throws Exception {
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		inputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
		inputFactory.setProperty(XMLInputFactory.IS_VALIDATING, false);
		inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

		StreamSource source = new StreamSource(stream, baseUrl);
		XMLStreamReader reader = inputFactory.createXMLStreamReader(source);

		while (reader.hasNext()) {
			int type = reader.next();
			String localName;

			switch (type) {
				case START_ELEMENT :
					localName = reader.getLocalName();
					if (TAG_REFERRAL.equals(localName)) {
						Referral referral = new Referral(reader.getAttributeValue(null, ATTR_REFERRAL_URL),
								parseInt(reader.getAttributeValue(null, ATTR_REFERRAL_DEPTH)));
						listener.processReferral(baseUrl, referral, referral.getDepth(), 1);
					} else if (TAG_RESOURCE.equals(localName)) {
						String bsn = reader.getAttributeValue(null, ATTR_RESOURCE_SYMBOLIC_NAME);
						String version = reader.getAttributeValue(null, ATTR_RESOURCE_VERSION);
						String uri = reader.getAttributeValue(null, ATTR_RESOURCE_URI);
						listener.processResource(new ObrResource(baseUrl, bsn, version, uri));
					}
					break;
			}
		}
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

	public void generateIndex(Set<File> files, OutputStream output, String repoName, String rootUrl, boolean pretty,
			Registry registry, LogService log) throws Exception {
		if (!files.isEmpty()) {
			BundleIndexer indexer = (registry != null) ? registry.getPlugin(BundleIndexer.class) : null;
			if (indexer == null)
				throw new IllegalStateException("Cannot index repository: no Bundle Indexer service or plugin found.");

			Map<String,String> config = new HashMap<String,String>();

			config.put(BundleIndexer.REPOSITORY_NAME, repoName);
			config.put(BundleIndexer.ROOT_URL, rootUrl);

			indexer.index(files, output, config);
		} else {
			String content = String.format(EMPTY_REPO_TEMPLATE, repoName);
			IO.copy(IO.stream(content), output);
		}
	}

}

class ObrResource extends BaseResource {

	private final String	bsn;
	private final String	version;
	private final String	contentUrl;

	public ObrResource(String baseUrl, String bsn, String version, String contentUrl) {
		super(baseUrl);
		this.bsn = bsn;
		this.version = version;
		this.contentUrl = contentUrl;
	}

	@Override
	public String getIdentity() {
		return bsn;
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public String getContentUrl() {
		return contentUrl;
	}

}
