package aQute.lib.deployer.repository.providers;

import static aQute.lib.deployer.repository.api.Decision.*;
import static javax.xml.stream.XMLStreamConstants.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.osgi.service.indexer.Builder;
import org.osgi.service.indexer.Capability;
import org.osgi.service.indexer.Namespaces;
import org.osgi.service.indexer.Requirement;
import org.osgi.service.indexer.ResourceAnalyzer;
import org.osgi.service.indexer.ResourceIndexer;
import org.osgi.service.indexer.impl.BIndex2;
import org.osgi.service.log.LogService;

import aQute.bnd.service.Registry;
import aQute.lib.collections.MultiMap;
import aQute.lib.deployer.repository.api.BaseResource;
import aQute.lib.deployer.repository.api.CheckResult;
import aQute.lib.deployer.repository.api.IRepositoryContentProvider;
import aQute.lib.deployer.repository.api.IRepositoryListener;
import aQute.lib.deployer.repository.api.Referral;

public class R5RepoContentProvider implements IRepositoryContentProvider {
	
	public static final String NAME = "R5";
	
	private static final String NS_URI = "http://www.osgi.org/xmlns/repository/v1.0.0";
	
	private static final String INDEX_NAME_COMPRESSED = "index.xml.gz";
	private static final String INDEX_NAME_PRETTY = "index.xml";

	private static final String TAG_REPOSITORY = "repository";
	private static final String TAG_REFERRAL = "referral";
	private static final String TAG_RESOURCE = "resource";
	private static final String TAG_CAPABILITY= "capability";
	private static final String TAG_REQUIREMENT = "requirement";
	private static final String TAG_ATTRIBUTE = "attribute";
	private static final String TAG_DIRECTIVE = "directive";

	private static final String ATTR_REFERRAL_URL = "url";
	private static final String ATTR_REFERRAL_DEPTH = "depth";
	
	private static final String ATTR_NAMESPACE = "namespace";
	
	private static final String ATTR_NAME = "name";
	private static final String ATTR_VALUE = "value";
	private static final String ATTR_TYPE = "type";


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
				case START_ELEMENT:
					localName = reader.getLocalName();
					switch (state) {
					case beforeRoot:
						String nsUri = reader.getNamespaceURI();
						if (nsUri != null)
							return CheckResult.fromBool(NS_URI.equals(nsUri), "Corrent namespace", "Incorrect namespace: " + nsUri, null);
						if (!TAG_REPOSITORY.equals(localName))
							return new CheckResult(reject, "Incorrect root element name", null);
						state = ParserState.inRoot;
						break;
					case inRoot:
						if (TAG_RESOURCE.equals(localName)) {
							state = ParserState.inResource;
						}
						break;
					case inResource:
						if (TAG_REQUIREMENT.equals(localName))
							return new CheckResult(accept, "Recognised element 'requirement' in 'resource'", null);
						if (TAG_CAPABILITY.equals(localName))
							state = ParserState.inCapability;
						break;
					case inCapability:
						if (TAG_ATTRIBUTE.equals(localName) || TAG_DIRECTIVE.equals(localName)) {
							return new CheckResult(accept, "Recognised element '%s' in 'capability'", null);
						}
						break;
					}
					break;
				case END_ELEMENT:
					localName = reader.getLocalName();
					if (state == ParserState.inResource && TAG_RESOURCE.equals(localName))
						state = ParserState.inRoot;
					if (state == ParserState.inCapability && TAG_CAPABILITY.equals(localName))
						state = ParserState.inResource;
					break;
				default:
					break;
				}
				
			}
			return new CheckResult(undecided, "Reached end of stream", null);
		} catch (XMLStreamException e) {
			return new CheckResult(reject, "Invalid XML", e);
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (XMLStreamException e) {
				}
		}
	}
	
	public void parseIndex(InputStream stream, String baseUrl, IRepositoryListener listener, LogService log) throws Exception {
		XMLStreamReader reader = null;
		try {
			XMLInputFactory inputFactory = XMLInputFactory.newInstance();
	
			inputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
			inputFactory.setProperty(XMLInputFactory.IS_VALIDATING, false);
			inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
	
			reader = inputFactory.createXMLStreamReader(stream);
			
			R5Resource.Builder resourceBuilder = null;
			Builder capReqBuilder = null;

			while (reader.hasNext()) {
				int type = reader.next();
				String localName;
				
				switch (type) {
				case START_ELEMENT:
					localName = reader.getLocalName();
					if (TAG_REFERRAL.equals(localName)) {
						String url = reader.getAttributeValue(null, ATTR_REFERRAL_URL);
						String depth = reader.getAttributeValue(null, ATTR_REFERRAL_DEPTH);
						Referral referral = new Referral(url, parseInt(depth));
						listener.processReferral(baseUrl, referral, 0, 0);
					} else if (TAG_RESOURCE.equals(localName)) {
						resourceBuilder = new R5Resource.Builder().setBaseUrl(baseUrl);
					} else if (TAG_CAPABILITY.equals(localName) || TAG_REQUIREMENT.equals(localName)) {
						capReqBuilder = new Builder().setNamespace(reader.getAttributeValue(null, ATTR_NAMESPACE));
					} else if (TAG_ATTRIBUTE.equals(localName)) {
						String name = reader.getAttributeValue(null, ATTR_NAME);
						String valueStr = reader.getAttributeValue(null, ATTR_VALUE);
						String typeAttr = reader.getAttributeValue(null, ATTR_TYPE);
						capReqBuilder.addAttribute(name, convertAttribute(valueStr, typeAttr));
					} else if (TAG_DIRECTIVE.equals(localName)) {
						String name = reader.getAttributeValue(null, ATTR_NAME);
						String valueStr = reader.getAttributeValue(null, ATTR_VALUE);
						capReqBuilder.addDirective(name, valueStr);
					}
					break;
				case END_ELEMENT:
					localName = reader.getLocalName();
					if (TAG_CAPABILITY.equals(localName)) {
						resourceBuilder.addCapability(capReqBuilder.buildCapability());
						capReqBuilder = null;
					} else if (TAG_REQUIREMENT.equals(localName)) {
						resourceBuilder.addRequirement(capReqBuilder.buildRequirement());
						capReqBuilder = null;
					} else if (TAG_RESOURCE.equals(localName)) {
						R5Resource resource = resourceBuilder.build();
						listener.processResource(resource);
						resourceBuilder = null;
					}
					break;
				}
			}
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception e) {
				}
			}
		}
	}
	
	private static int parseInt(String value) {
		if (value == null || "".equals(value))
			return 0;
		return Integer.parseInt(value);
	}
	
	private Object convertAttribute(String value, String type) {
		// TODO just treat everything as String for now
		return value;
	}
	
	public boolean supportsGeneration() {
		return true;
	}
	
	public void generateIndex(Set<File> files, OutputStream output, String repoName, String rootUrl, boolean pretty, Registry registry, LogService log) throws Exception {
		BIndex2 indexer = new BIndex2();
		if (log != null) indexer.setLog(log);
		
		if (registry != null) {
			List<ResourceAnalyzer> analyzers = registry.getPlugins(ResourceAnalyzer.class);
			for (ResourceAnalyzer analyzer : analyzers) {
				// TODO: where to get the filter property??
				indexer.addAnalyzer(analyzer, null);
			}
		}
		
		Map<String, String> config = new HashMap<String, String>();
		config.put(ResourceIndexer.REPOSITORY_NAME, repoName);
		config.put(ResourceIndexer.ROOT_URL, rootUrl);
		config.put(ResourceIndexer.PRETTY, Boolean.toString(pretty));
		
		indexer.index(files, output, config);
	}
}

class R5Resource extends BaseResource {
	
	private final MultiMap<String, Capability> capabilities = new MultiMap<String, Capability>();
	private final MultiMap<String, Requirement> requires = new MultiMap<String, Requirement>();

	private R5Resource(String baseUrl, Collection<? extends Capability> capabilities, Collection<? extends Requirement> requires) {
		super(baseUrl);
		for (Capability capability : capabilities) {
			this.capabilities.add(capability.getNamespace(), capability);
		}
		for (Requirement requirement : requires) {
			this.requires.add(requirement.getNamespace(), requirement);
		}
	}
	
	public static class Builder {
		private String baseUrl = null;
		private final List<Capability> capabilities = new LinkedList<Capability>();
		private final List<Requirement> requires = new LinkedList<Requirement>();
		
		public Builder setBaseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
			return this;
		}
		public Builder addCapability(Capability capability) {
			this.capabilities.add(capability);
			return this;
		}
		public Builder addRequirement(Requirement require) {
			this.requires.add(require);
			return this;
		}
		
		public R5Resource build() {
			return new R5Resource(baseUrl, Collections.unmodifiableList(capabilities), Collections.unmodifiableList(requires));
		}
	}
	
	public List<Capability> getCapabilities(String namespace) {
		List<Capability> list = capabilities.get(namespace);
		return list != null ? Collections.unmodifiableList(list) : Collections.<Capability>emptyList();
	}

	public Capability findPackageCapability(String pkgName) {
		List<Capability> list = capabilities.get(Namespaces.NS_WIRING_PACKAGE);
		if (list != null) for (Capability capability : list) {
			if (pkgName.equals(capability.getAttributes().get(Namespaces.NS_WIRING_PACKAGE)))
				return capability;
		}
		return null;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Resource [capabilities=").append(capabilities)
				.append(", requirements=").append(requires)
				.append("]");
		return builder.toString();
	}

	@Override
	public String getIdentity() {
		String bsn = null;
		List<Capability> list = capabilities.get(Namespaces.NS_IDENTITY);
		if (list != null && !list.isEmpty()) {
			bsn = (String) list.get(0).getAttributes().get(Namespaces.NS_IDENTITY);
		}
		return bsn;
	}

	@Override
	public String getVersion() {
		String version = null;
		List<Capability> list = capabilities.get(Namespaces.NS_IDENTITY);
		if (list != null && !list.isEmpty()) {
			version = (String) list.get(0).getAttributes().get(Namespaces.ATTR_VERSION);
		}
		return version;
	}

	@Override
	public String getContentUrl() {
		String url = null;
		List<Capability> list = capabilities.get(Namespaces.NS_CONTENT);
		if (list != null && !list.isEmpty())
			url = (String) list.get(0).getAttributes().get(Namespaces.ATTR_CONTENT_URL);
		return url;
	}

}
