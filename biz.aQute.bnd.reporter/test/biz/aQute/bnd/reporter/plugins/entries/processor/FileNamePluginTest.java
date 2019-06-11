package biz.aQute.bnd.reporter.plugins.entries.processor;

import java.io.File;
import java.util.Locale;

import aQute.bnd.osgi.Processor;
import junit.framework.TestCase;

public class FileNamePluginTest extends TestCase {

	public void testWorkspaceSettingsEntry() throws Exception {

		final Processor p = new Processor();
		final FileNamePlugin s = new FileNamePlugin();
		s.setReporter(p);

		final File file = File.createTempFile("test", "test");
		file.deleteOnExit();

		p.setBase(file);

		assertEquals(s.extract(p, Locale.forLanguageTag("und")), file.getName());
		assertTrue(p.isOk());
	}
}
