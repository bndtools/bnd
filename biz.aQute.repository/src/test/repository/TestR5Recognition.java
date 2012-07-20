package test.repository;

import static aQute.bnd.deployer.repository.api.Decision.*;

import java.io.*;

import javax.xml.stream.*;

import junit.framework.*;
import aQute.bnd.deployer.repository.api.*;
import aQute.bnd.deployer.repository.providers.*;

public class TestR5Recognition extends TestCase {

	public void testRejectNamespace() throws Exception {
		String testdata = "<?xml version='1.0' encoding='utf-8'?>"
				+ "<repository increment='0' name='index1' xmlns='http://www2.osgi.org/www/obr2html.xsl'>"
				+ "<resource>";
		ByteArrayInputStream stream = new ByteArrayInputStream(testdata.getBytes());
		assertEquals(reject, new R5RepoContentProvider().checkStream("xxx", stream).getDecision());
	}

	public void testAcceptNamespace() throws Exception {
		String testdata = "<?xml version='1.0'?>" + "<repository xmlns='http://www.osgi.org/xmlns/repository/v1.0.0'>";
		ByteArrayInputStream stream = new ByteArrayInputStream(testdata.getBytes());
		assertEquals(accept, new R5RepoContentProvider().checkStream("xxx", stream).getDecision());
	}

	public void testRejectRootElementName() throws Exception {
		String testdata = "<?xml version='1.0' encoding='utf-8'?>" + "<repo name='index1'/>";
		ByteArrayInputStream stream = new ByteArrayInputStream(testdata.getBytes());
		assertEquals(reject, new R5RepoContentProvider().checkStream("xxx", stream).getDecision());
	}

	public void testUndecidable() throws Exception {
		String testdata;
		ByteArrayInputStream stream;
		CheckResult result;

		testdata = "<?xml version='1.0' encoding='utf-8'?><repository name='index1'/>";
		stream = new ByteArrayInputStream(testdata.getBytes());
		assertEquals(undecided, new R5RepoContentProvider().checkStream("xxx", stream).getDecision());

		testdata = "<repository><resource/></repository>";
		stream = new ByteArrayInputStream(testdata.getBytes());
		result = new R5RepoContentProvider().checkStream("xxx", stream);
		assertEquals(undecided, result.getDecision());

		testdata = "<repository><referral/></repository>";
		stream = new ByteArrayInputStream(testdata.getBytes());
		result = new R5RepoContentProvider().checkStream("xxx", stream);
		assertEquals(undecided, result.getDecision());
	}

	public void testUnparseable() throws Exception {
		String testdata = "<?xml version='1.0' encoding='utf-8'?>" + "<repository name='index1'>";
		ByteArrayInputStream stream = new ByteArrayInputStream(testdata.getBytes());
		CheckResult result = new R5RepoContentProvider().checkStream("xxx", stream);
		assertEquals(reject, result.getDecision());
		assertTrue(result.getException() != null && result.getException() instanceof XMLStreamException);
	}

	public void testAcceptOnCapabilityChildElementNames() throws Exception {
		String testdata;
		ByteArrayInputStream stream;
		CheckResult result;

		// Must be R5
		testdata = "<repository><resource><capability><attribute/>...";
		stream = new ByteArrayInputStream(testdata.getBytes());
		result = new R5RepoContentProvider().checkStream("xxx", stream);
		assertEquals(accept, result.getDecision());

		// Must be R5
		testdata = "<repository><resource><capability><directive/>...";
		stream = new ByteArrayInputStream(testdata.getBytes());
		result = new R5RepoContentProvider().checkStream("xxx", stream);
		assertEquals(accept, result.getDecision());

		// Arbitrary elements under repo, resource and capability are allowed
		testdata = "<repository><XXX/><resource><YYY/><capability><ZZZ/><attribute/>...";
		stream = new ByteArrayInputStream(testdata.getBytes());
		result = new R5RepoContentProvider().checkStream("xxx", stream);
		assertEquals(accept, result.getDecision());
	}

	public void testAcceptExtensionElementOtherNamespace() throws Exception {
		String testdata;
		ByteArrayInputStream stream;
		CheckResult result;

		// Arbitrary elements under resource are allowed
		testdata = "<repository><resource><foo:XXX xmlns:foo='http://org.example/ns'/><YYY/><capability><attribute/></capability><ZZZ/></resource><repo...";
		stream = new ByteArrayInputStream(testdata.getBytes());
		result = new R5RepoContentProvider().checkStream("xxx", stream);
		assertEquals(accept, result.getDecision());
	}
}
