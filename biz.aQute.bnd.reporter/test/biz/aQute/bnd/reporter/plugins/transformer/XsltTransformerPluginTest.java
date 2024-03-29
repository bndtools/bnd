package biz.aQute.bnd.reporter.plugins.transformer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import aQute.lib.io.IO;
import biz.aQute.bnd.reporter.plugins.serializer.XmlReportSerializerPlugin;

public class XsltTransformerPluginTest {

	@Test
	public void testXsltTransformer() throws Exception {

		final XsltTransformerPlugin t = new XsltTransformerPlugin();
		final Map<String, Object> report = new HashMap<>();
		final Map<String, String> parameters = new HashMap<>();
		report.put("test", "test");
		parameters.put("param1", "param");

		final ByteArrayOutputStream model = new ByteArrayOutputStream();
		new XmlReportSerializerPlugin().serialize(report, model);

		final ByteArrayOutputStream output = new ByteArrayOutputStream();

		t.transform(IO.stream(model.toByteArray()),
			IO.stream(new File("testresources/transformer/xsltTransformer.xslt")), output, parameters);

		assertTrue(new String(output.toByteArray()).contains("test"));

		assertThat(t.getHandledModelExtensions()).containsExactly("xml");
		assertThat(t.getHandledTemplateExtensions()).containsExactly("xslt", "xsl");
	}
}
