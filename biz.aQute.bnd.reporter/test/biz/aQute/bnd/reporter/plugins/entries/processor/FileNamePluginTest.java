package biz.aQute.bnd.reporter.plugins.entries.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import aQute.bnd.osgi.Processor;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;

public class FileNamePluginTest {

	@Test
	public void testWorkspaceSettingsEntry(@InjectTemporaryDirectory
	File tmp) throws Exception {

		final Processor p = new Processor();
		final FileNamePlugin s = new FileNamePlugin();
		s.setReporter(p);

		final File file = File.createTempFile("test", "test", tmp);

		p.setBase(file);

		assertEquals(s.extract(p, Locale.forLanguageTag("und")), file.getName());
		assertTrue(p.isOk());
	}
}
