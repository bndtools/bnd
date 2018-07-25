package biz.aQute.bnd.exporters.subsystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import aQute.bnd.exporter.subsystem.EsaArchivType;

public class EsaArchivTypeTest {

	@Test
	public void testSubSystemDefaults() throws Exception {

		assertEquals(EsaArchivType.NONE, EsaArchivType.byParameter("none"));
		assertEquals(EsaArchivType.ALL, EsaArchivType.byParameter("all"));
		assertEquals(EsaArchivType.CONTENT, EsaArchivType.byParameter("content"));
		assertEquals(EsaArchivType.CONTENT, EsaArchivType.byParameter(null));

		assertNull(EsaArchivType.byParameter("foo"));
	}

}