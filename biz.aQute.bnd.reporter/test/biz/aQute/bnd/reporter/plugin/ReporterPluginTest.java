package biz.aQute.bnd.reporter.plugin;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;
import aQute.lib.io.IO;

public class ReporterPluginTest {

	@Test
	public void testReporterPlugin() throws Exception {
		final Builder b = new Builder();
		b.setProperty("-plugin.Test",
				"biz.aQute.bnd.reporter.plugin.ReporterPlugin,"
						+ "biz.aQute.bnd.reporter.plugin.MainHeadersGeneratorPlugin,"
						+ "biz.aQute.bnd.reporter.plugin.DSGeneratorPlugin,"
						+ "biz.aQute.bnd.reporter.plugin.MetatypesGeneratorPlugin");

		b.setProperty("-report",
				"OSGI-INF/report/report.html;locale=en;templates='xslt/simpleXml.xslt,xslt/simple.xslt';includes=xslt/simple.xslt:xml:xslt");

		b.addClasspath(IO.getFile("bin_test"));
		final Jar[] jar = b.builds();

		assertNotNull(jar[0].getResource("OSGI-INF/report/report.html"));

		b.close();
	}
}
