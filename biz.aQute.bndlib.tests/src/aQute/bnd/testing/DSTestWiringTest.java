package aQute.bnd.testing;

import java.util.*;

import junit.framework.*;

import org.osgi.framework.*;
import org.osgi.service.log.*;

import aQute.bnd.annotation.component.*;
import static org.mockito.Mockito.*;

public class DSTestWiringTest extends TestCase {

	private LogService	log;
	private String		string;
	private A			a;
	private List<Integer>	integers = new ArrayList<Integer>();
	private int	integer;
	
	static class A {
		
		Map<String,Object>	map;

		@Activate
		void activate(Map<String,Object> map) {
			this.map = map;
		}
	}

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
		assertEquals(1,a.map.get("a"));
		assertEquals(Arrays.asList(1,2,3,4), integers);
		assertEquals(1, integer);
		
		ds.get(BundleActivator.class).start(null);
		verify(act).start(null);
		verifyNoMoreInteractions(act);
		
		log.log(LogService.LOG_ERROR, "skip");
		log.log(LogService.LOG_ERROR, "include");
		
		assertEquals(1,testlog.getEntries().size());
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
	
	@Reference(type='*')
	void addInteger(Integer i) {
		integers .add(i);
	}

	@Reference
	void setInteger(Integer i) {
		integer = i;
	}

}
