package aQute.lib.deployer.repository.xml;

import org.osgi.service.indexer.Builder;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class IndexSAXHandler extends DefaultHandler {
	
	private static final String TAG_RESOURCE = "resource";
	
	private static final String TAG_REFERRAL = "referral";
	private static final String ATTR_REFERRAL_URL = "url";
	private static final String ATTR_REFERRAL_DEPTH = "depth";
	
	private static final String TAG_CAPABILITY = "capability";
	private static final String TAG_REQUIREMENT = "requirement";
	private static final String ATTR_NAMESPACE = "namespace";
	
	private static final String TAG_ATTRIBUTE = "attribute";
	private static final String TAG_DIRECTIVE = "directive";
	private static final String ATTR_NAME = "name";
	private static final String ATTR_VALUE = "value";
	private static final String ATTR_TYPE = "type";

	private final String baseUrl;
	private final IRepositoryListener resourceListener;
	
	private Resource.Builder resourceBuilder = null;
	private Builder capReqBuilder = null;
	
	private Referral referral = null;
	private int currentDepth;
	private int maxDepth;

	public IndexSAXHandler(String baseUrl, IRepositoryListener listener) {
		this.baseUrl = baseUrl;
		this.resourceListener = listener;
		this.currentDepth = 0;
	}
	
	public IndexSAXHandler(String baseUrl, IRepositoryListener listener, int maxDepth, int currentDepth) {
		this.baseUrl = baseUrl;
		this.resourceListener = listener;
		this.currentDepth = currentDepth;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		if (TAG_REFERRAL.equals(qName)) {
			referral = new Referral(atts.getValue(ATTR_REFERRAL_URL), parseInt(atts.getValue(ATTR_REFERRAL_DEPTH)));
		} else if (TAG_RESOURCE.equals(qName)) {
			resourceBuilder = new Resource.Builder().setBaseUrl(baseUrl);
		} else if (TAG_CAPABILITY.equals(qName) || TAG_REQUIREMENT.equals(qName)) {
			capReqBuilder = new Builder().setNamespace(atts.getValue(ATTR_NAMESPACE));
		} else if (TAG_ATTRIBUTE.equals(qName)) {
			String name = atts.getValue(ATTR_NAME);
			String valueStr = atts.getValue(ATTR_VALUE);
			String type = atts.getValue(ATTR_TYPE);
			capReqBuilder.addAttribute(name, convertAttribute(valueStr, type));
		} else if (TAG_DIRECTIVE.equals(qName)) {
			String name = atts.getValue(ATTR_NAME);
			String valueStr = atts.getValue(ATTR_VALUE);
			capReqBuilder.addDirective(name, valueStr);
		}
	}

	private Object convertAttribute(String value, String type) {
		// TODO just treat everything as String for now
		return value;
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (TAG_CAPABILITY.equals(qName)) {
			resourceBuilder.addCapability(capReqBuilder.buildCapability());
			capReqBuilder = null;
		} else if (TAG_REQUIREMENT.equals(qName)) {
			resourceBuilder.addRequirement(capReqBuilder.buildRequirement());
			capReqBuilder = null;
		} else if (TAG_RESOURCE.equals(qName)) {
			Resource resource = resourceBuilder.build();
			if (!resourceListener.processResource(resource))
				throw new StopParseException();
			resourceBuilder = null;
		} else if (TAG_REFERRAL.equals(qName)) {
			if (maxDepth == 0) {
				maxDepth = referral.getDepth();
			}
			resourceListener.processReferral(baseUrl, referral, maxDepth, currentDepth + 1);
			referral = null;
		}
	}

	private static int parseInt(String value) {
		if (value == null || "".equals(value))
			return 0;
		return Integer.parseInt(value);
	}

}
