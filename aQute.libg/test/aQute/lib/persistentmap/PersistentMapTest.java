package aQute.lib.persistentmap;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import aQute.lib.io.IO;
import junit.framework.TestCase;

public class PersistentMapTest extends TestCase {

	private static final String	UNIX_KEY	= "abc/../../";
	private static final String	WINDOWS_KEY	= "abc\\..\\..\\";

	public void testSimple() throws Exception {
		File tmp = new File("tmp");
		PersistentMap<String> pm = new PersistentMap<>(new File(tmp, "simple"), String.class);
		try {

			assertNull(pm.put("abc", "def"));
			assertEquals("def", pm.get("abc"));

			pm.close();

			PersistentMap<String> pm2 = new PersistentMap<>(new File(tmp, "simple"), String.class);
			assertEquals("def", pm2.get("abc"));

			assertEquals(Arrays.asList("abc"), new ArrayList<>(pm2.keySet()));

			for (Map.Entry<String, String> e : pm2.entrySet()) {
				e.setValue("XXX");
			}
			assertEquals("XXX", pm2.get("abc"));
			pm2.close();
		} finally {
			pm.close();
			IO.delete(tmp);
		}
	}

	public void testZipSlipnames() throws Exception {
		File tmp = new File("tmp");
		IO.delete(tmp);
		PersistentMap<String> pm = new PersistentMap<>(new File(tmp, "simple"), String.class);
		try {

			assertNull(pm.put(UNIX_KEY, "def"));
			assertEquals("def", pm.get(UNIX_KEY));

			pm.close();

			PersistentMap<String> pm2 = new PersistentMap<>(new File(tmp, "simple"), String.class);
			assertEquals("def", pm2.get(UNIX_KEY));

			assertEquals(Arrays.asList(UNIX_KEY), new ArrayList<>(pm2.keySet()));

			for (Map.Entry<String, String> e : pm2.entrySet()) {
				e.setValue("XXX");
			}
			assertEquals("XXX", pm2.get(UNIX_KEY));
			pm2.clear();
			pm2.close();
		} finally {
			pm.close();
			IO.delete(tmp);
		}
	}

	public void testZipSlipnamesWindows() throws Exception {
		File tmp = new File("tmp");
		IO.delete(tmp);
		PersistentMap<String> pm = new PersistentMap<>(new File(tmp, "simple"), String.class);
		try {

			assertNull(pm.put(WINDOWS_KEY, "def"));
			assertEquals("def", pm.get(WINDOWS_KEY));

			pm.close();

			PersistentMap<String> pm2 = new PersistentMap<>(new File(tmp, "simple"), String.class);
			assertEquals("def", pm2.get(WINDOWS_KEY));

			assertEquals(Arrays.asList(WINDOWS_KEY), new ArrayList<>(pm2.keySet()));

			for (Map.Entry<String, String> e : pm2.entrySet()) {
				e.setValue("XXX");
			}
			assertEquals("XXX", pm2.get(WINDOWS_KEY));
			pm2.close();
		} finally {
			pm.close();
			IO.delete(tmp);
		}
	}

	public static class X {
		public String		abc;
		public int			def;
		public List<String>	list	= new ArrayList<>();
	}

	public void testStructs() throws Exception {
		File tmp = new File("tmp");
		PersistentMap<X> pm = new PersistentMap<>(new File(tmp, "simple"), X.class);
		try {
			X x = new X();
			x.abc = "def";
			x.def = 5;
			x.list.add("abc");
			assertNull(pm.put("abc", x));
			pm.close();

			PersistentMap<X> pm2 = new PersistentMap<>(new File(tmp, "simple"), X.class);

			X x2 = pm2.get("abc");
			assertEquals("def", x2.abc);
			assertEquals(5, x2.def);

			pm2.remove("abc");

			assertEquals(0, pm2.size());

			pm2.close();
		} finally {
			pm.close();
			IO.delete(tmp);
		}
	}
}
