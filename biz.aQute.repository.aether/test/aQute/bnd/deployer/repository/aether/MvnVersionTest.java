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
	}	
	
	public void testQualifierWithDotSeparator() {
		MvnVersion mv = MvnVersion.parseString("1.2.3.beta-1");
		assertEquals(new Version(1, 2, 3, "beta-1"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
	}
	
	public void testMajorMinorWithQualifierWithDotSeparator() {
		MvnVersion mv = MvnVersion.parseString("1.2.beta-1");
		assertEquals(new Version(1, 2, 0, "beta-1"), mv.getOSGiVersion());
		assertFalse(mv.isSnapshot());
	}
	
	public void testInvalid() {
		MvnVersion mv = MvnVersion.parseString("1.2.3.4.5");
		assertNull(mv);
	}
}
