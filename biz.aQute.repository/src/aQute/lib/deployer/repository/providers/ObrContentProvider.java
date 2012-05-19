package aQute.lib.deployer.repository.providers;

import java.io.File;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.osgi.service.bindex.BundleIndexer;
import org.osgi.service.log.LogService;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import aQute.bnd.service.Registry;
import aQute.lib.deployer.repository.api.BaseResource;
import aQute.lib.deployer.repository.api.IRepositoryContentProvider;
import aQute.lib.deployer.repository.api.IRepositoryListener;
import aQute.lib.deployer.repository.api.Referral;
import aQute.lib.deployer.repository.api.StopParseException;
import aQute.lib.io.IO;

public class ObrContentProvider implements IRepositoryContentProvider {
	
	public static final String NAME = "OBR";
	
	private static final String INDEX_NAME = "repository.xml";
	private static final String EMPTY_REPO_TEMPLATE = "<?xml version='1.0' encoding='UTF-8'?>%n<repository name='%s' lastmodified='0'/>";

	public String getName() {
		return NAME;
	}
	
	public String getDefaultIndexName(boolean pretty) {
		return INDEX_NAME;
	}
	
	public ContentHandler createContentHandler(String baseUrl, IRepositoryListener listener) {
		return new ObrSaxHandler(baseUrl, listener);
	}
	
	public void generateIndex(Set<File> files, OutputStream output, String repoName, String rootUrl, boolean pretty, Registry registry, LogService log) throws Exception {
		if (!files.isEmpty()) {
			BundleIndexer indexer = (registry != null) ? registry.getPlugin(BundleIndexer.class) : null;
			if (indexer == null)
				throw new IllegalStateException("Cannot index repository: no Bundle Indexer service or plugin found.");
	
			Map<String, String> config = new HashMap<String, String>();
			
			config.put(BundleIndexer.REPOSITORY_NAME, repoName);
			config.put(BundleIndexer.ROOT_URL, rootUrl);
			
			indexer.index(files, output, config);
		} else {
			String content = String.format(EMPTY_REPO_TEMPLATE, repoName);
			IO.copy(IO.stream(content), output);
		}
	}

}

class ObrSaxHandler extends DefaultHandler {
	
	private static final String TAG_RESOURCE = "resource";
	private static final String ATTR_RESOURCE_SYMBOLIC_NAME = "symbolicname";
	private static final String ATTR_RESOURCE_URI = "uri";
	private static final String ATTR_RESOURCE_VERSION = "version";
	
	private static final String TAG_REFERRAL = "referral";
	private static final String ATTR_REFERRAL_URL = "url";
	private static final String ATTR_REFERRAL_DEPTH = "depth";
	
	private final String baseUrl;
	
	private final IRepositoryListener resourceListener;
	
	private int currentDepth;
	private int maxDepth;

	public ObrSaxHandler(String baseUrl, IRepositoryListener listener) {
		this.baseUrl = baseUrl;
		this.resourceListener = listener;
		this.currentDepth = 0;
	}
	
	public ObrSaxHandler(String baseUrl, IRepositoryListener listener, int maxDepth, int currentDepth) {
		this.baseUrl = baseUrl;
		this.resourceListener = listener;
		this.currentDepth = currentDepth;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		if (TAG_REFERRAL.equals(qName)) {
			Referral referral = new Referral(atts.getValue(ATTR_REFERRAL_URL), parseInt(atts.getValue(ATTR_REFERRAL_DEPTH)));
			if (maxDepth == 0) {
				maxDepth = referral.getDepth();
			}
			boolean cont = resourceListener.processReferral(baseUrl, referral, maxDepth, currentDepth + 1);
			if (!cont) throw new StopParseException();
		} else if (TAG_RESOURCE.equals(qName)) {
			ObrResource resource = new ObrResource(baseUrl, atts.getValue(ATTR_RESOURCE_SYMBOLIC_NAME), atts.getValue(ATTR_RESOURCE_VERSION), atts.getValue(ATTR_RESOURCE_URI));
			boolean cont = resourceListener.processResource(resource);
			if (!cont) throw new StopParseException();
		}
	}

	private static int parseInt(String value) {
		if (value == null || "".equals(value))
			return 0;
		return Integer.parseInt(value);
	}

}

class ObrResource extends BaseResource {
	
	private final String bsn;
	private final String version;
	private final String contentUrl;

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
