package aQute.bnd.deployer.repository.aether;

import junit.framework.*;
import aQute.bnd.version.*;

public class MvnVersionTest extends TestCase {

	public void testMajorMinorMicro() {
		MvnVersion mv = MvnVersion.parseString("1.2.3");
		assertEquals(new Version(1, 2, 3), mv.getOSGiVersion());
	}

	public void testMajorMinor() {
		MvnVersion mv = MvnVersion.parseString("1.2");
		assertEquals(new Version(1, 2), mv.getOSGiVersion());
	}

	public void testMajor() {
		MvnVersion mv = MvnVersion.parseString("1");
		assertEquals(new Version(1), mv.getOSGiVersion());
	}

	public void testSnapshot() {
		MvnVersion mv = MvnVersion.parseString("1.2.3-SNAPSHOT");
		assertEquals(new Version(1, 2, 3, "SNAPSHOT"), mv.getOSGiVersion());
		assertTrue(mv.isSnapshot());
		mv = MvnVersion.parseString("1.2-SNAPSHOT");
		assertEquals(new Version(1, 2, 0, "SNAPSHOT"), mv.getOSGiVersion());
		assertTrue(mv.isSnapshot());
		mv = MvnVersion.parseString("1-SNAPSHOT");
		assertEquals(new Version(1, 0, 0, "SNAPSHOT"), mv.getOSGiVersion());
		assertTrue(mv.isSnapshot());
		mv = MvnVersion.parseString("1.2.3.SNAPSHOT");
		assertEquals(new Version(1, 2, 3, "SNAPSHOT"), mv.getOSGiVersion());
		assertTrue(mv.isSnapshot());
	}

	public void testNumericQualifier() {
		MvnVersion mv = MvnVersion.parseString("1.2.3-01");
		assertEquals(new Version(1, 2, 3, "01"), mv.getOSGiVersion());
		mv = MvnVersion.parseString("1.2.3.01");
		assertEquals(new Version(1, 2, 3, "01"), mv.getOSGiVersion());
	}

	public void testQualifierWithDashSeparator() {
		MvnVersion mv = MvnVersion.parseString("1.2.3-beta-1");
		assertEquals(new Version(1, 2, 3, "beta-1"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
	}

	public void testQualifierWithoutSeparator() {
		MvnVersion mv = MvnVersion.parseString("1.2.3rc1");
		assertEquals(new Version(1, 2, 3, "rc1"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
		mv = MvnVersion.parseString("1.2rc1");
		assertEquals(new Version(1, 2, 0, "rc1"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
		mv = MvnVersion.parseString("1rc1");
		assertEquals(new Version(1, 0, 0, "rc1"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
	}	
	
	public void testQualifierWithDotSeparator() {
		MvnVersion mv = MvnVersion.parseString("1.2.3.beta-1");
		assertEquals(new Version(1, 2, 3, "beta-1"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
		mv = MvnVersion.parseString("1.2.beta-1");
		assertEquals(new Version(1, 2, 0, "beta-1"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
		mv = MvnVersion.parseString("1.beta-1");
		assertEquals(new Version(1, 0, 0, "beta-1"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
	}
	
	public void testDotsInQualifier() {
		MvnVersion mv = MvnVersion.parseString("1.2.3.4.5");
		assertEquals(new Version(1, 2, 3, "4.5"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
		mv = MvnVersion.parseString("1.2.3-4.5");
		assertEquals(new Version(1, 2, 3, "4.5"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
		mv = MvnVersion.parseString("1.2-4.5");
		assertEquals(new Version(1, 2, 0, "4.5"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
		mv = MvnVersion.parseString("1-4.5");
		assertEquals(new Version(1, 0, 0, "4.5"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
	}

	public void testInvalidVersion() {
		try {
			MvnVersion mv = MvnVersion.parseString("Not a number");
			fail();
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}
}
