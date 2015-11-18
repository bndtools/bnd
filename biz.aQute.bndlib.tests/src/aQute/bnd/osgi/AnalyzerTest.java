package aQute.bnd.osgi;

import junit.framework.TestCase;

public class AnalyzerTest extends TestCase {

	public void testdoNameSection() throws Exception {
		Analyzer a = new Analyzer();
		a.doNameSection(null, "@");
		a.doNameSection(null, "@@");
		a.doNameSection(null, "@foo@bar@");
	}

}
