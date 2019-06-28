package bndtools.release;

import aQute.bnd.version.Version;
import junit.framework.TestCase;

public class TestRelease extends TestCase {

	public void testUpdateTemplateVersion() {

		String version = ReleaseHelper.updateTemplateVersion("1.0.2.${tstamp}",
			Version.parseVersion("1.0.3.20121212-1212"));
		assertEquals("1.0.3.${tstamp}", version);

		version = ReleaseHelper.updateTemplateVersion("1.0.3.20121212-1212",
			Version.parseVersion("1.0.4.20121213-1313"));
		assertEquals("1.0.4.20121213-1313", version);

		version = ReleaseHelper.updateTemplateVersion("1.0", Version.parseVersion("2"));
		assertEquals("2.0.0", version);

		version = ReleaseHelper.updateTemplateVersion("1.0.2.${tstamp}", Version.parseVersion("2"));
		assertEquals("2.0.0.${tstamp}", version);

	}

}
