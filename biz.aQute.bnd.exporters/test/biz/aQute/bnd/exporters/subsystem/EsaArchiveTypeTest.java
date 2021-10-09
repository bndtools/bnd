package biz.aQute.bnd.exporters.subsystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import aQute.bnd.exporter.subsystem.EsaArchiveType;

public class EsaArchiveTypeTest {

	@Test
	public void testSubSystemDefaults() throws Exception {

		assertEquals(EsaArchiveType.NONE, EsaArchiveType.byParameter("none"));
		assertEquals(EsaArchiveType.ALL, EsaArchiveType.byParameter("all"));
		assertEquals(EsaArchiveType.CONTENT, EsaArchiveType.byParameter("content"));
		assertEquals(EsaArchiveType.CONTENT, EsaArchiveType.byParameter(null));

		assertNull(EsaArchiveType.byParameter("foo"));
	}
}
