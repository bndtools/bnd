package org.bndtools.maven;

import junit.framework.TestCase;

public class MavenVersionTest extends TestCase {

	public void testDefaultVersion() {
		assertEquals("0.0.0", new MavenVersion(null).toBndVersion());
	}
	
	public void testVersionsWithQualifier() {
		assertEquals("1.2.0.beta1", new MavenVersion("1.2-beta1").toBndVersion());
		assertEquals("1.2.3.beta1", new MavenVersion("1.2.3-beta1").toBndVersion());
		assertEquals("1.2.3.beta1-0", new MavenVersion("1.2.3-beta1-0").toBndVersion());
	}
	
	public void testInvalidVersions() {
		try {
			new MavenVersion("1.2.3.4");
			fail("invalid version");
		} catch (IllegalArgumentException e) {
		}
	}
	
	public void testSnapshot() {
		assertEquals("1.2.0.${tstamp}", new MavenVersion("1.2-SNAPSHOT").toBndVersion());
		assertEquals("1.2.0.beta-${tstamp}", new MavenVersion("1.2-beta-SNAPSHOT").toBndVersion());
		assertEquals("1.2.0.${tstamp}_56", new MavenVersion("1.2-SNAPSHOT_56").toBndVersion());
		assertEquals("1.2.0.SNAPCHAT", new MavenVersion("1.2-SNAPCHAT").toBndVersion());
	}
	
}
