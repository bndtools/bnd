package biz.aQute.bnd.reporter;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;
import aQute.lib.io.IO;
import biz.aQute.bnd.reporter.lib.ReportPostBuildPlugin;

public class ReporterTest {

	@Test
	public void testReporterSimple() throws Exception {
		Builder b = new Builder();
		b.addBasicPlugin(new ReportPostBuildPlugin());
		b.setProperty("-report.generate", "OSGI-INF/report/report.xml");
		
		b.addClasspath( IO.getFile("bin_test"));
		Jar[] jar = b.builds();

		assertNotNull( jar[0].getResource("OSGI-INF/report/report.xml"));
		
	}
}
