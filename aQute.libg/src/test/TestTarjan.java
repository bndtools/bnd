package test;

import java.util.*;

import junit.framework.*;
import aQute.lib.collections.*;
import aQute.libg.tarjan.*;

public class TestTarjan extends TestCase {

	public void testTarjan() throws Exception {
		MultiMap<String, String> g = mkGraph("A{BC}B{A}C{DE}D{C}E{D}");
		System.out.println(g);
		
		Collection<? extends Collection<String>> scc = Tarjan.tarjan(g);
		
		assertEquals(2, scc.size());
		for ( Collection<String> set : scc) {
			if ( set.size()==3)
				assertEquals( new HashSet<String>(Arrays.asList("E","C", "D")), set);
			else if ( set.size()==2)
				assertEquals( new HashSet<String>(Arrays.asList("B","A")), set);
			else
				fail();
		}
	}

	private MultiMap<String, String> mkGraph(String string) {
		MultiMap<String, String> map = new MultiMap<String, String>();

		String key = null;

		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			switch (c) {
			case '{':
				break;
			case '}':
				key = null;
				break;
			default:
				if (key == null) {
					key = c + "";
					map.put(key, new ArrayList<String>());
				}
				else
					map.add(key, c + "");
			}
		}
		return map;
	}
}
