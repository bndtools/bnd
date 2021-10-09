package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Verifier;

public class NativeHeader {
	Builder b;

	@BeforeEach
	protected void setUp() throws Exception {
		b = new Builder();
		b.setProperty("Include-Resource",
			"x.so;literal='x',y.so;literal='y',native/libclib_jiio.so;literal='',native/libmlib_jai.so;literal='', org/osgi/test/cases/framework/fragments/tb8/linux_x86/libNative.so;literal=''");
		b.build();
	}

	@AfterEach
	protected void tearDown() throws Exception {
		b.close();
	}

	@Test
	public void testFunnyHeader() throws Exception {
		try (Verifier v = new Verifier(b)) {
			v.doNative(
				"org/osgi/test/cases/framework/fragments/tb8/linux_x86/libNative.so; osname=Linux; processor=x86; osversion=\"(1000,10000]\",");
			assertBad(v, "name");
		}
	}

	@Test
	public void testWildcardNotAtEnd() throws Exception {
		try (Verifier v = new Verifier(b)) {
			v.doNative("x.so;osname=win32,*,x.dll");
			assertBad(v, "may only END in wildcard");
		}
	}

	@Test
	public void testWildcard() throws Exception {
		try (Verifier v = new Verifier(b)) {
			v.doNative("x.so ;y.so;osname=Linux;processor=amd64,*");
			assertOk(v);
		}
	}

	@Test
	public void testSimple() throws Exception {
		try (Verifier v = new Verifier(b)) {
			v.doNative(
				"\rnative/libclib_jiio.so ;\r" + "native/libmlib_jai.so;\r" + "osname=Linux ;\r" + "processor=amd64\r");
			assertOk(v);
		}
	}

	static void assertOk(Processor v) {
		System.err.println(v.getWarnings());
		System.err.println(v.getErrors());
		assertEquals(0, v.getErrors()
			.size());
		assertEquals(0, v.getWarnings()
			.size());
	}

	static void assertBad(Processor v, String ok) {
		assertEmptyAfterRemove(v.getErrors(), ok);
		assertEmptyAfterRemove(v.getWarnings(), ok);
	}

	private static void assertEmptyAfterRemove(List<String> errors, String ok) {
		for (String s : errors) {
			if (!s.contains(ok))
				fail("Found error/warning that can not be removed: " + s + " : " + ok);
		}
	}

}
