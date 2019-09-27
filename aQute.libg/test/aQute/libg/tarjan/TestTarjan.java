package aQute.libg.tarjan;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;

import aQute.lib.collections.MultiMap;
import junit.framework.TestCase;

public class TestTarjan extends TestCase {

	public void testTarjan() throws Exception {
		MultiMap<String, String> g = mkGraph("A{BC}B{A}C{DE}D{C}E{D}");
		System.err.println(g);

		Collection<? extends Collection<String>> scc = Tarjan.tarjan(g);

		assertEquals(2, scc.size());
		for (Collection<String> set : scc) {
			if (set.size() == 3)
				assertThat(set).containsExactlyInAnyOrder("E", "C", "D");
			else if (set.size() == 2)
				assertThat(set).containsExactlyInAnyOrder("B", "A");
			else
				fail();
		}
	}

	private MultiMap<String, String> mkGraph(String string) {
		MultiMap<String, String> map = new MultiMap<>();

		String key = null;

		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			switch (c) {
				case '{' :
					break;
				case '}' :
					key = null;
					break;
				default :
					if (key == null) {
						key = c + "";
						map.put(key, new ArrayList<>());
					} else
						map.add(key, c + "");
			}
		}
		return map;
	}
}
