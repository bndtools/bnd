package aQute.lib.xml;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

// https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html#java

public final class XML {
	private static final Logger logger = LoggerFactory.getLogger(XML.class);

	private XML() {}

	/**
	 * Create and return a DocumentBuilderFactory instance.
	 * <p>
	 * The returned DocumentBuilderFactory is configured to avoid XML External
	 * Entity (XXE) attacks.
	 *
	 * @return A properly configured DocumentBuilderFactory instance.
	 */
	public static DocumentBuilderFactory newDocumentBuilderFactory() {
		DocumentBuilderFactory instance = DocumentBuilderFactory.newInstance();
		instance.setXIncludeAware(false);
		instance.setExpandEntityReferences(false);
		try {
			instance.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		} catch (ParserConfigurationException e) {
			try { // Xerces 2 only fallback
				instance.setFeature("http://xerces.apache.org/xerces2-j/features.html#disallow-doctype-decl", true);
			} catch (ParserConfigurationException e2) {
				logger.info(
					"Unable to set feature to disallow DTD (doctypes): XML External Entity (XXE) attack risk",
					e);
			}
		}
		try {
			instance.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		} catch (ParserConfigurationException e) {
			logger.info("Unable to set feature to disable load-external-dtd: XML External Entity (XXE) attack risk", e);
		}
		try {
			instance.setFeature("http://xml.org/sax/features/external-general-entities", false);
		} catch (ParserConfigurationException e) {
			try { // Xerces 2 only fallback
				instance.setFeature("http://xerces.apache.org/xerces2-j/features.html#external-general-entities",
					false);
			} catch (ParserConfigurationException e2) {
				logger.info(
					"Unable to set feature to disallow external-general-entities: XML External Entity (XXE) attack risk",
					e);
			}
		}
		try {
			instance.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		} catch (ParserConfigurationException e) {
			try { // Xerces 2 only fallback
				instance.setFeature("http://xerces.apache.org/xerces2-j/features.html#external-parameter-entities",
					false);
			} catch (ParserConfigurationException e2) {
				logger.info(
					"Unable to set feature to disallow external-parameter-entities: XML External Entity (XXE) attack risk",
					e);
			}
		}
		return instance;
	}

	/**
	 * Create and return a SAXParserFactory instance.
	 * <p>
	 * The returned SAXParserFactory is configured to avoid XML External Entity
	 * (XXE) attacks.
	 *
	 * @return A properly configured SAXParserFactory instance.
	 */
	public static SAXParserFactory newSAXParserFactory() {
		SAXParserFactory instance = SAXParserFactory.newInstance();
		instance.setXIncludeAware(false);
		try {
			instance.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		} catch (ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
			try { // Xerces 2 only fallback
				instance.setFeature("http://xerces.apache.org/xerces2-j/features.html#disallow-doctype-decl", true);
			} catch (ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e2) {
				logger.info("Unable to set feature to disallow DTD (doctypes): XML External Entity (XXE) attack risk",
					e);
			}
		}
		try {
			instance.setFeature("http://xml.org/sax/features/external-general-entities", false);
		} catch (ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
			try { // Xerces 2 only fallback
				instance.setFeature("http://xerces.apache.org/xerces2-j/features.html#external-general-entities",
					false);
			} catch (ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e2) {
				logger.info(
					"Unable to set feature to disallow external-general-entities: XML External Entity (XXE) attack risk",
					e);
			}
		}
		try {
			instance.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		} catch (ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
			try { // Xerces 2 only fallback
				instance.setFeature("http://xerces.apache.org/xerces2-j/features.html#external-parameter-entities",
					false);
			} catch (ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e2) {
				logger.info(
					"Unable to set feature to disallow external-parameter-entities: XML External Entity (XXE) attack risk",
					e);
			}
		}
		return instance;
	}

	/**
	 * Create and return a XMLInputFactory instance.
	 * <p>
	 * The returned XMLInputFactory is configured to avoid XML External Entity
	 * (XXE) attacks.
	 *
	 * @return A properly configured XMLInputFactory instance.
	 */
	public static XMLInputFactory newXMLInputFactory() {
		XMLInputFactory instance = XMLInputFactory.newInstance();
		try {
			instance.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		} catch (IllegalArgumentException e) {
			logger.info("Unable to set property {} to false: XML External Entity (XXE) attack risk",
				XMLInputFactory.SUPPORT_DTD, e);
		}
		try {
			instance.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
		} catch (IllegalArgumentException e) {
			logger.info("Unable to set property {} to false: XML External Entity (XXE) attack risk",
				XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, e);
		}
		return instance;
	}

	/**
	 * Create and return a TransformerFactory instance.
	 * <p>
	 * The returned TransformerFactory is configured to avoid XML External
	 * Entity (XXE) attacks.
	 *
	 * @return A properly configured TransformerFactory instance.
	 */
	public static TransformerFactory newTransformerFactory() {
		TransformerFactory instance = TransformerFactory.newInstance();
		try {
			instance.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		} catch (IllegalArgumentException e) {
			logger.info("Unable to set attribute {} to \"\": XML External Entity (XXE) attack risk",
				XMLConstants.ACCESS_EXTERNAL_DTD, e);
		}
		try {
			instance.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
		} catch (IllegalArgumentException e) {
			logger.info("Unable to set attribute {} to \"\": XML External Entity (XXE) attack risk",
				XMLConstants.ACCESS_EXTERNAL_STYLESHEET, e);
		}
		return instance;
	}
}
