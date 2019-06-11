package biz.aQute.bnd.reporter.plugins.transformer;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import aQute.bnd.service.reporter.ReportTransformerPlugin;

public class XsltTransformerPlugin implements ReportTransformerPlugin {
	static private final String[]		_extT				= {
		"xslt", "xsl"
	};
	static private final String[]		_extI				= {
		"xml"
	};

	private final TransformerFactory	_transformerFactory	= TransformerFactory.newInstance();

	@Override
	public String[] getHandledTemplateExtensions() {
		return _extT;
	}

	@Override
	public String[] getHandledModelExtensions() {
		return _extI;
	}

	@Override
	public void transform(final InputStream data, final InputStream template, final OutputStream output,
		final Map<String, String> parameters) throws Exception {

		final Templates templates = _transformerFactory.newTemplates(new StreamSource(template));
		final Source xmlSource = new StreamSource(data);
		final Result result = new StreamResult(output);
		final Transformer t = templates.newTransformer();

		parameters.forEach(t::setParameter);

		t.transform(xmlSource, result);
		output.flush();
	}
}
