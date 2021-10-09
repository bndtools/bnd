package test;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import aQute.bnd.osgi.Verifier;

public class FilterTest {

	@Test
	public void testFilter() throws Exception {
		String s = Verifier.validateFilter("(org.osgi.framework.windowing.system=xyz)");
		assertNull(s);
		s = Verifier.validateFilter("(&     (   org.osgi.framework.windowing.system   =xyz)     )");
		assertNull(s);
	}

}
