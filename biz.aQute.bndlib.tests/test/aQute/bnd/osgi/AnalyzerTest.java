package aQute.bnd.osgi;

import org.junit.jupiter.api.Test;

public class AnalyzerTest {


	@Test
	public void testdoNameSection() throws Exception {
		try (Analyzer a = new Analyzer()) {
			a.doNameSection(null, "@");
			a.doNameSection(null, "@@");
			a.doNameSection(null, "@foo@bar@");
		}
	}

}
