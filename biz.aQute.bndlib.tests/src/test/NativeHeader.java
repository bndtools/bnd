package test;

import java.util.*;

import junit.framework.*;
import aQute.lib.osgi.*;

public class NativeHeader extends TestCase {
	static Builder	b	= new Builder();
	static {
		try {

			b.setProperty(
					"Include-Resource",
					"x.so;literal='x',y.so;literal='y',native/libclib_jiio.so;literal='',native/libmlib_jai.so;literal='', org/osgi/test/cases/framework/fragments/tb8/linux_x86/libNative.so;literal=''");
			b.build();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void testFunnyHeader() throws Exception {
		Verifier v = new Verifier(b);
		v.doNative("org/osgi/test/cases/framework/fragments/tb8/linux_x86/libNative.so; osname=Linux; processor=x86; osversion=\"(1000,10000]\",");
		assertBad(v, "name");
	}

	public void testWildcardNotAtEnd() throws Exception {
		Verifier v = new Verifier(b);
		v.doNative("x.so;osname=win32,*,x.dll");
		assertBad(v, "may only END in wildcard");
	}

	public void testWildcard() throws Exception {
		Verifier v = new Verifier(b);
		v.doNative("x.so ;y.so;osname=Linux;processor=amd64,*");
		assertOk(v);
	}

	public void testSimple() throws Exception {
		Verifier v = new Verifier(b);
		v.doNative("\rnative/libclib_jiio.so ;\r" + "native/libmlib_jai.so;\r" + "osname=Linux ;\r"
				+ "processor=amd64\r");
		assertOk(v);
	}

	void assertOk(Processor v) {
		System.err.println(v.getWarnings());
		System.err.println(v.getErrors());
		assertEquals(0, v.getErrors().size());
		assertEquals(0, v.getWarnings().size());
	}

	void assertBad(Processor v, String ok) {
		assertEmptyAfterRemove(v.getErrors(), ok);
		assertEmptyAfterRemove(v.getWarnings(), ok);
	}

	private void assertEmptyAfterRemove(List<String> errors, String ok) {
		for (String s : errors) {
			if (s.indexOf(ok) < 0)
				fail("Found error/warning that can not be removed: " + s + " : " + ok);
		}
	}

}
