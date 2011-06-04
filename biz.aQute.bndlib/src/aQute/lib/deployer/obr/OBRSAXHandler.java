package aQute.lib.deployer.obr;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class OBRSAXHandler extends DefaultHandler {
	
	private static final String TAG_RESOURCE = "resource";
	private static final String ATTR_RESOURCE_ID = "id";
	private static final String ATTR_RESOURCE_PRESENTATION_NAME = "presentationname";
	private static final String ATTR_RESOURCE_SYMBOLIC_NAME = "symbolicname";
	private static final String ATTR_RESOURCE_URI = "uri";
	private static final String ATTR_RESOURCE_VERSION = "version";
	
	private static final String TAG_CAPABILITY = "capability";
	private static final String ATTR_CAPABILITY_NAME = "name";
	
	private static final String TAG_PROPERTY = "p";
	private static final String ATTR_PROPERTY_NAME = "n";
	private static final String ATTR_PROPERTY_TYPE = "t";
	private static final String ATTR_PROPERTY_VALUE = "v";

	private final String baseUrl;
	private final IResourceListener[] resourceListeners;
	
	private Resource.Builder resourceBuilder = null;
	private Capability.Builder capabilityBuilder = null;

	public OBRSAXHandler(String baseUrl, IResourceListener[] listeners) {
		this.baseUrl = baseUrl;
		this.resourceListeners = listeners;
	}
	

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		if (TAG_RESOURCE.equals(qName)) {
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
			boolean cont = true;
			for (IResourceListener listener : resourceListeners) {
				cont &= listener.processResource(resourceBuilder.build());
			}
			if (!cont) throw new StopParseException();
			resourceBuilder = null;
		}
	}

}
