package test;

import org.osgi.framework.namespace.NativeNamespace;
import org.osgi.resource.Requirement;

import aQute.bnd.osgi.Verifier;
import aQute.bnd.osgi.resource.FilterParser;
import aQute.bnd.osgi.resource.FilterParser.Expression;
import aQute.bnd.osgi.resource.ResourceBuilder;
import junit.framework.TestCase;

public class NativeCodeHeaderTest extends TestCase {

	public void testBadSelectionFilter() throws Exception {

	}

	public void testNative() throws Exception {
		ResourceBuilder rb = new ResourceBuilder();

		Requirement nativeCode = rb.getNativeCode(//
			"f1;"//
				+ "  osname=Windows95;" //
				+ "  processor=x86;" //
				+ "  selection-filter='(com.acme.windowing=win32)';" //
				+ "  language=en;" //
				+ "  osname=Windows98;" //
				+ "  language=se, " //
				+ "lib/solaris/libhttp.so;" //
				+ "  osname=Solaris;" //
				+ "  osname = SunOS ;" //
				+ "  processor = sparc, "//
				+ "lib/linux/libhttp.so ; " //
				+ "  osname = Linux ; "//
				+ "  osversion = 3.1.4; " //
				+ "  processor = mips; "//
				+ "  selection-filter = '(com.acme.windowing=gtk)',"//
				+ "*")
			.synthetic();

		assertEquals(NativeNamespace.NATIVE_NAMESPACE, nativeCode.getNamespace());
		assertEquals("optional", nativeCode.getDirectives()
			.get("resolution"));
		String filter = nativeCode.getDirectives()
			.get("filter");

		assertEquals(null, Verifier.validateFilter(filter));

		FilterParser p = new FilterParser();
		Expression parse = p.parse(filter);

		assertEquals(
			"(|(&(|(osgi.native.osname~=Windows95)(osgi.native.osname~=Windows98))(osgi.native.processor~=x86)(|(osgi.native.language~=en)(osgi.native.language~=se))(com.acme.windowing=win32))(&(|(osgi.native.osname~=Solaris)(osgi.native.osname~=SunOS))(osgi.native.processor~=sparc))(&(osgi.native.osname~=Linux)(osgi.native.processor~=mips)(osgi.native.osversion=3.1.4)(com.acme.windowing=gtk)))",
			filter);

		System.out.println(nativeCode);
	}
}
