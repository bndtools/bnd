package aQute.bnd.osgi;

import junit.framework.TestCase;

public class AnalyzerTest extends TestCase {

	public void testdoNameSection() throws Exception {
		try (Analyzer a = new Analyzer()) {
			a.doNameSection(null, "@");
			a.doNameSection(null, "@@");
			a.doNameSection(null, "@foo@bar@");
		}
	}

}
