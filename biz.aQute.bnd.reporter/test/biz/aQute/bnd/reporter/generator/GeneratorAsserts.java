package biz.aQute.bnd.reporter.generator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;
import java.util.Set;

public class GeneratorAsserts {

	static public void verify(final ReportGenerator rg, final int metadataSize, final Set<String> expectAvailableReport,
			final int generatedCount, final String... errors) {

		final Map<String, Object> metadata = rg.getMetadata();
		final Set<String> available = rg.getAvailableReports();
		final Set<File> generated = rg.generateReports("*");

		assertEquals(metadataSize, metadata.size());
		assertEquals(expectAvailableReport, available);
		assertEquals(generatedCount, generated.size());

		for (final File f : generated) {
			assertTrue(f.isFile());
		}

		if (errors == null || errors.length == 0) {
			assertTrue(rg.isOk());
		} else {
			assertTrue(!rg.isOk());
			final StringBuilder sb = new StringBuilder();
			rg.getErrors().forEach(sb::append);
			rg.getWarnings().forEach(sb::append);
			final String s = sb.toString();
			for (final String e : errors) {
				assertTrue(s.contains(e));
			}
		}

		assertTrue(metadata == rg.getMetadata());
		rg.refresh();
		assertTrue(metadata != rg.getMetadata());
		rg.refresh();
	}
}
