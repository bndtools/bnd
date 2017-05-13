package aQute.bnd.testing;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleActivator;
import org.osgi.service.log.LogService;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Reference;
import junit.framework.TestCase;

public class DSTestWiringTest extends TestCase {

	private LogService		log;
	private String			string;
	private A				a;
	private I				i;
	private List<Integer>	integers	= new ArrayList<Integer>();
	private int				integer;

	public static class A {

		Map<String,Object> map;

		@Activate
		void activate(Map<String,Object> map) {
			this.map = map;
		}
	}

	static interface I {}

	static class B1 extends A implements I {}

	static class B2 extends A implements I {}

	static class B3 extends A implements I {}

	public void testSimple() throws Exception {

		BundleActivator act = mock(BundleActivator.class);
		TestingLog testlog = new TestingLog().direct().stacktrace();

		DSTestWiring ds = new DSTestWiring();
		ds.add(this); // by instance
		ds.add(act);
		ds.add(testlog).$("filters", Arrays.asList("skip")); // instance
		ds.add(String.class.getName()); // by name
		ds.add(A.class).$("a", 1); // by class

		ds.add(1);
		ds.add(2);
		ds.add(3);
		ds.add(4);

		ds.wire();

		assertNotNull(log);
		assertNotNull(string);
		assertNotNull(a);
		assertNotNull(a.map);
		assertEquals(1, a.map.get("a"));
		assertEquals(Arrays.asList(1, 2, 3, 4), integers);
		assertEquals(1, integer);

		ds.get(BundleActivator.class).start(null);
		verify(act).start(null);
		verifyNoMoreInteractions(act);

		log.log(LogService.LOG_ERROR, "skip");
		log.log(LogService.LOG_ERROR, "include");

		assertEquals(1, testlog.getEntries().size());
		assertFalse(testlog.check("include"));
	}

	@Reference
	void setLog(LogService log) {
		this.log = log;
	}

	@Reference
	void setString(String s) {
		this.string = s;
	}

	@Reference
	void setA(A a) {
		this.a = a;
	}

	@Reference(type = '*')
	void addInteger(Integer i) {
		integers.add(i);
	}

	@Reference
	void setInteger(Integer i) {
		integer = i;
	}

	@Reference
	void setB(I i) {
		this.i = i;
	}
}
