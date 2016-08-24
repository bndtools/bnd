package aQute.libg.xml;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;

public class DocumentBuilderFactory {

	static final String				_FEATURES_DISALLOW_DOCTYPE_DECL			= "http://apache.org/xml/features/disallow-doctype-decl";
	static final String				_FEATURES_LOAD_EXTERNAL_DTD				= "http://apache.org/xml/features/nonvalidating/load-external-dtd";
	static final String				_FEATURES_EXTERNAL_GENERAL_ENTITIES		= "http://xml.org/sax/features/external-general-entities";
	static final String				_FEATURES_EXTERNAL_PARAMETER_ENTITIES	= "http://xml.org/sax/features/external-parameter-entities";
	static final Pattern							nbsp									= Pattern.compile("nbsp",
			Pattern.LITERAL);
	static EntityResolver							nullResolver							= new NullEntityResolver();
	static javax.xml.parsers.DocumentBuilderFactory	dbf										= javax.xml.parsers.DocumentBuilderFactory
			.newInstance();

	static {
		dbf.setNamespaceAware(false);
		dbf.setValidating(false);
		dbf.setXIncludeAware(false);
		try {
			dbf.setFeature(_FEATURES_DISALLOW_DOCTYPE_DECL, true);
			dbf.setFeature(_FEATURES_EXTERNAL_GENERAL_ENTITIES, false);
			dbf.setFeature(_FEATURES_EXTERNAL_PARAMETER_ENTITIES, false);
			dbf.setFeature(_FEATURES_LOAD_EXTERNAL_DTD, false);
		} catch (Exception e) {
			Exceptions.duck(e);
		}
	}

	public static DocumentBuilder safeInstance() {
		try {
			DocumentBuilder builder = dbf.newDocumentBuilder();
			builder.setEntityResolver(nullResolver);
			return new SafeDocumentBuilder(builder);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	static class SafeDocumentBuilder extends DocumentBuilder {

		private final DocumentBuilder builder;

		public SafeDocumentBuilder(DocumentBuilder builder) {
			this.builder = builder;
		}

		@Override
		public DOMImplementation getDOMImplementation() {
			return builder.getDOMImplementation();
		}

		@Override
		public boolean isNamespaceAware() {
			return false;
		}

		@Override
		public boolean isValidating() {
			return false;
		}

		@Override
		public Document newDocument() {
			return builder.newDocument();
		}

		@Override
		public Document parse(File file) throws IOException, SAXException {
			InputSource inputSource = new InputSource(new FileInputStream(file));
			return parse(inputSource);
		}

		@Override
		public Document parse(InputStream is) throws IOException, SAXException {
			InputSource inputSource = new InputSource(is);
			return parse(inputSource);
		}

		@Override
		public Document parse(InputStream is, String uri) throws IOException, SAXException {
			InputSource inputSource = new InputSource(is);
			inputSource.setSystemId(uri);
			return parse(inputSource);
		}

		@Override
		public Document parse(String uri) throws IOException, SAXException {
			InputSource inputSource = new InputSource(uri);
			return parse(inputSource);
		}

		@Override
		public Document parse(InputSource inputSource) throws IOException, SAXException {
			BufferedInputStream bis;

			if (inputSource.getByteStream() != null)
				bis = new BufferedInputStream(inputSource.getByteStream());
			else if (inputSource.getCharacterStream() != null) {
				Reader reader = inputSource.getCharacterStream();
				String encoding = inputSource.getEncoding();
				if (encoding == null) {
					encoding = "UTF-8";
				}
				ByteArrayInputStream bais = new ByteArrayInputStream(IO.collect(reader).getBytes(encoding));
				bis = new BufferedInputStream(bais);
			} else if (inputSource.getSystemId() != null) {
				URI uri = URI.create(inputSource.getSystemId());
				bis = new BufferedInputStream(uri.toURL().openStream());
			}
			else
				throw new IllegalStateException("Not properly defined inputSource.");

			bis.mark(0);

			try {
				return builder.parse(bis);
			} catch (SAXParseException spe) {
				if (spe.getMessage().contains("nbsp")) {
					bis.reset();
					String content = IO.collect(bis);
					content = content.replaceAll("nbsp", "#160");
					return builder.parse(new ByteArrayInputStream(content.getBytes("UTF-8")));
				}

				throw spe;
			}
		}

		@Override
		public void setEntityResolver(EntityResolver resolver) {
			builder.setEntityResolver(resolver);
		}

		@Override
		public void setErrorHandler(ErrorHandler handler) {
			builder.setErrorHandler(handler);
		}

	}

	static class NullEntityResolver implements EntityResolver {

		@Override
		public InputSource resolveEntity(String publicId, String systemId) {
			return new InputSource();
		}

	}

}
