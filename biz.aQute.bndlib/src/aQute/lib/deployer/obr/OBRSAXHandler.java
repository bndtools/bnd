package aQute.lib.deployer.obr;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class OBRSAXHandler extends DefaultHandler {
	
	private static final String TAG_RESOURCE = "resource";
	private static final String ATTR_RESOURCE_ID = "id";
	private static final String ATTR_RESOURCE_PRESENTATION_NAME = "presentationname";
	private static final String ATTR_RESOURCE_SYMBOLIC_NAME = "symbolicname";
	private static final String ATTR_RESOURCE_URI = "uri";
	private static final String ATTR_RESOURCE_VERSION = "version";
	
	private static final String TAG_REFERRAL = "referral";
	private static final String ATTR_REFERRAL_URL = "url";
	private static final String ATTR_REFERRAL_DEPTH = "depth";
	
	private static final String TAG_CAPABILITY = "capability";
	private static final String ATTR_CAPABILITY_NAME = "name";
	
	private static final String TAG_REQUIRE = "require";
	private static final String ATTR_REQUIRE_NAME = "name";
	private static final String ATTR_REQUIRE_FILTER = "filter";
	private static final String ATTR_REQUIRE_OPTIONAL = "optional";
	
	private static final String TAG_PROPERTY = "p";
	private static final String ATTR_PROPERTY_NAME = "n";
	private static final String ATTR_PROPERTY_TYPE = "t";
	private static final String ATTR_PROPERTY_VALUE = "v";

	private final String baseUrl;
	private final IRepositoryListener resourceListener;
	
	private Resource.Builder resourceBuilder = null;
	private Capability.Builder capabilityBuilder = null;
	private Require require = null;
	private Referral referral = null;
	private int currentDepth;
	private int maxDepth;

	public OBRSAXHandler(String baseUrl, IRepositoryListener listener) {
		this.baseUrl = baseUrl;
		this.resourceListener = listener;
		this.currentDepth = 0;
	}
	
	public OBRSAXHandler(String baseUrl, IRepositoryListener listener, int maxDepth, int currentDepth) {
		this.baseUrl = baseUrl;
		this.resourceListener = listener;
		this.currentDepth = currentDepth;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		if (TAG_REFERRAL.equals(qName)) {
			referral = new Referral(atts.getValue(ATTR_REFERRAL_URL), parseInt(atts.getValue(ATTR_REFERRAL_DEPTH)));
		} else if (TAG_RESOURCE.equals(qName)) {
			resourceBuilder = new Resource.Builder()
				.setId(atts.getValue(ATTR_RESOURCE_ID))
				.setPresentationName(atts.getValue(ATTR_RESOURCE_PRESENTATION_NAME))
				.setSymbolicName(atts.getValue(ATTR_RESOURCE_SYMBOLIC_NAME))
				.setUrl(atts.getValue(ATTR_RESOURCE_URI))
				.setVersion(atts.getValue(ATTR_RESOURCE_VERSION))
				.setBaseUrl(baseUrl);
		} else if (TAG_CAPABILITY.equals(qName)) {
			capabilityBuilder = new Capability.Builder()
				.setName(atts.getValue(ATTR_CAPABILITY_NAME));
		} else if (TAG_REQUIRE.equals(qName)) {
			require = new Require(atts.getValue(ATTR_REQUIRE_NAME), atts.getValue(ATTR_REQUIRE_FILTER), "true".equalsIgnoreCase(atts.getValue(ATTR_REQUIRE_OPTIONAL)));
		} else if (TAG_PROPERTY.equals(qName)) {
			Property p = new Property(atts.getValue(ATTR_PROPERTY_NAME), atts.getValue(ATTR_PROPERTY_TYPE), atts.getValue(ATTR_PROPERTY_VALUE));
			if (capabilityBuilder != null)
				capabilityBuilder.addProperty(p);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (TAG_CAPABILITY.equals(qName)) {
			resourceBuilder.addCapability(capabilityBuilder);
			capabilityBuilder = null;
		} else if (TAG_RESOURCE.equals(qName)) {
			if (!resourceListener.processResource(resourceBuilder.build()))
				throw new StopParseException();
			resourceBuilder = null;
		} else if (TAG_REQUIRE.equals(qName)) {
			resourceBuilder.addRequire(require);
			require = null;
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
