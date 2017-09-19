package biz.aQute.bnd.reporter.generator;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.jar.Manifest;

import org.junit.Test;

import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;

public class ReporterGeneratorTest {

	@Test
	public void testReporterFull() throws Exception {
		final Processor b = new Processor();
		b.setProperty("-plugin.Test",
				"biz.aQute.bnd.reporter.plugin.ReporterPlugin,"
						+ "biz.aQute.bnd.reporter.plugin.MainHeadersGeneratorPlugin,"
						+ "biz.aQute.bnd.reporter.plugin.DSGeneratorPlugin,"
						+ "biz.aQute.bnd.reporter.plugin.MetatypesGeneratorPlugin");

		final Jar jar = new Jar("jar");
		final Manifest manifest = new Manifest();

		manifest.getMainAttributes().putValue("Bundle-Description", "%des");

		PropResource r = new PropResource();
		r.add("des", "valueUN");
		jar.putResource("OSGI-INF/l10n/bundle.properties", r);

		r = new PropResource();
		r.add("des", "valueEN");
		jar.putResource("OSGI-INF/l10n/bundle_en.properties", r);

		jar.setManifest(manifest);

		final ReportGenerator rg = new ReportGenerator(b);
		final ByteArrayOutputStream output1 = new ByteArrayOutputStream();
		final ByteArrayOutputStream output2 = new ByteArrayOutputStream();

		rg.generate(ReportConfig.builder(jar).addTemplates("xslt/simpleXml.xslt").addTemplates("xslt/simple.xslt")
				.setOutput(output1).build());
		rg.generate(
				ReportConfig.builder(jar).setLocale("en").addTemplates("xslt/simple.xslt").setOutput(output2).build());

		assertTrue(rg.isOk());

		assertTrue(output1.toByteArray().length > 0);

		final String res = new String(output2.toByteArray());
		assertTrue(res.contains("valueEN"));
		assertTrue(!res.contains("valueUN"));

		rg.close();
		b.close();
	}
}
