package test;

import java.util.*;

import junit.framework.*;
import aQute.bnd.repo.eclipse.*;
import aQute.libg.generics.*;

public class TestEclipseRepo extends TestCase {

	public static void testSimple() {
		EclipseRepo er = new EclipseRepo();
		Map<String,String> map = Create.map();
		map.put("location", "test/eclipse");
		map.put("name", "eclipse-test");
		er.setProperties(map);

		System.err.println(er.list("*"));
	}
}
