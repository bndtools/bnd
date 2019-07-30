package biz.aQute.bnd.reporter.plugins.entries.bundle;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.junit.Test;

import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import biz.aQute.bnd.reporter.artifact.dto.ChecksumDTO;

public class ChecksumPluginTest {

	@SuppressWarnings("unchecked")
	@Test
	public void testChecksumPlugin() throws IOException {
		final ChecksumPlugin plugin = new ChecksumPlugin();
		try (final Jar jar = new Jar("jar", new File("testresources/componentsEntry/source.jar"));
			Processor p = new Processor()) {

			plugin.setReporter(p);

			final ChecksumDTO dto = (ChecksumDTO) plugin.extract(jar, Locale.forLanguageTag("und"));

			assertNotNull(dto.md5);
			assertNotNull(dto.sha1);
			assertNotNull(dto.sha256);

			assertTrue(p.isOk());

		}
	}
}
