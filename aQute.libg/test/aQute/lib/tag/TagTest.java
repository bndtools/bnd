package aQute.lib.tag;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;

import junit.framework.TestCase;

public class TagTest extends TestCase {

	public void testDtoToTag() {
		Tag t;
		ListIterator<Object> l;
		Tag c;
		Map<String,Object> map;
		Map<String,Object> map2;

		// Test Scalar
		t = getTag(null);
		assertTrue(t.content.size() == 0);

		t = getTag("");
		assertTrue(t.content.size() == 1);
		assertTrue(((Tag) t.content.listIterator().next()).content.size() == 1);
		assertTrue(((Tag) t.content.listIterator().next()).content.listIterator().next() == "");

		t = getTag("simple");
		assertTrue(t.content.size() == 1);
		assertTrue(((Tag) t.content.listIterator().next()).content.size() == 1);
		assertTrue(((Tag) t.content.listIterator().next()).content.listIterator().next() == "simple");

		// Test Collection of Scalar
		t = getTag(new LinkedList<>());
		assertTrue(t.content.size() == 0);

		t = getTag(Arrays.asList(""));
		assertTrue(t.content.size() == 1);
		assertTrue(((Tag) t.content.listIterator().next()).content.size() == 1);
		assertTrue(((Tag) t.content.listIterator().next()).content.listIterator().next() == "");

		t = getTag(Arrays.asList("oneElement"));
		assertTrue(t.content.size() == 1);
		assertTrue(((Tag) t.content.listIterator().next()).content.size() == 1);
		assertTrue(((Tag) t.content.listIterator().next()).content.listIterator().next() == "oneElement");

		t = getTag(Arrays.asList("oneElement", "andMore"));
		assertTrue(t.content.size() == 2);
		l = t.content.listIterator();
		c = (Tag) l.next();
		assertTrue(c.content.size() == 1);
		assertTrue(c.content.listIterator().next() == "oneElement");
		c = (Tag) l.next();
		assertTrue(c.content.size() == 1);
		assertTrue(c.content.listIterator().next() == "andMore");

		// Test Collection of Collection
		t = getTag(Arrays.asList(Arrays.asList()));
		assertTrue(t.content.size() == 0);

		t = getTag(Arrays.asList(Arrays.asList("")));
		assertTrue(t.content.size() == 1);
		assertTrue(((Tag) t.content.listIterator().next()).content.size() == 1);
		assertTrue(((Tag) t.content.listIterator().next()).content.listIterator().next() == "");

		t = getTag(Arrays.asList(Arrays.asList("oneElement")));
		assertTrue(t.content.size() == 1);
		assertTrue(((Tag) t.content.listIterator().next()).content.size() == 1);
		assertTrue(((Tag) t.content.listIterator().next()).content.listIterator().next() == "oneElement");

		t = getTag(Arrays.asList(Arrays.asList("oneElement", null)));
		assertTrue(t.content.size() == 1);
		assertTrue(((Tag) t.content.listIterator().next()).content.size() == 1);
		assertTrue(((Tag) t.content.listIterator().next()).content.listIterator().next() == "oneElement");

		t = getTag(Arrays.asList(Arrays.asList("oneElement", "andMore"), Arrays.asList("oneElement2")));
		assertTrue(t.content.size() == 3);
		l = t.content.listIterator();
		c = (Tag) l.next();
		assertTrue(c.content.size() == 1);
		assertTrue(c.content.listIterator().next() == "oneElement");
		c = (Tag) l.next();
		assertTrue(c.content.size() == 1);
		assertTrue(c.content.listIterator().next() == "andMore");
		c = (Tag) l.next();
		assertTrue(c.content.size() == 1);
		assertTrue(c.content.listIterator().next() == "oneElement2");

		// Test Array (treated as a collection)
		t = getTag(new String[] {
				"oneElement", "andMore"
		});
		assertTrue(t.content.size() == 2);
		l = t.content.listIterator();
		c = (Tag) l.next();
		assertTrue(c.content.size() == 1);
		assertTrue(c.content.listIterator().next() == "oneElement");
		c = (Tag) l.next();
		assertTrue(c.content.size() == 1);
		assertTrue(c.content.listIterator().next() == "andMore");

		// Test Map of scalar
		t = getTag(new LinkedHashMap<>());
		map = new LinkedHashMap<>();
		map.put("oneElement", null);
		t = getTag(map);

		assertTrue(t.content.size() == 0);

		map = new LinkedHashMap<>();
		map.put(null, "oneElement");
		t = getTag(map);
		assertTrue(((Tag) t.content.listIterator().next()).content.size() == 1);
		assertTrue(((Tag) ((Tag) t.content.listIterator().next()).content.listIterator().next()).name == "null");

		map = new LinkedHashMap<>();
		map.put("oneElement", "onValue");
		t = getTag(map);
		l = t.content.listIterator();
		c = (Tag) l.next();
		assertTrue(c.content.size() == 1);
		l = c.content.listIterator();
		c = (Tag) l.next();
		assertTrue(c.content.listIterator().next() == "onValue");
		assertTrue(c.getName() == "oneElement");

		map = new LinkedHashMap<>();
		map.put("oneElement", "onValue");
		map.put("andMore", "moreValue");
		t = getTag(map);
		l = t.content.listIterator();
		c = (Tag) l.next();
		assertTrue(c.content.size() == 2);
		l = c.content.listIterator();
		c = (Tag) l.next();
		assertTrue(c.content.listIterator().next() == "onValue");
		assertTrue(c.getName() == "oneElement");
		c = (Tag) l.next();
		assertTrue(c.content.listIterator().next() == "moreValue");
		assertTrue(c.getName() == "andMore");

		// Test Map of Collection
		map = new LinkedHashMap<>();
		map.put("oneElement", new LinkedList<>());
		map.put("andMore", "moreValue");
		t = getTag(map);
		l = t.content.listIterator();
		c = (Tag) l.next();
		assertTrue(c.content.size() == 1);
		l = c.content.listIterator();
		c = (Tag) l.next();
		assertTrue(c.content.listIterator().next() == "moreValue");
		assertTrue(c.getName() == "andMore");

		map = new LinkedHashMap<>();
		map.put("oneElement", Arrays.asList(""));
		map.put("andMore", "moreValue");
		t = getTag(map);
		l = t.content.listIterator();
		c = (Tag) l.next();
		assertTrue(c.content.size() == 2);
		l = c.content.listIterator();
		c = (Tag) l.next();
		assertTrue(c.content.listIterator().next() == "");
		assertTrue(c.getName() == "oneElement");
		c = (Tag) l.next();
		assertTrue(c.content.listIterator().next() == "moreValue");
		assertTrue(c.getName() == "andMore");

		map = new LinkedHashMap<>();
		map.put("oneElement", Arrays.asList("onValue"));
		map.put("andMore", "moreValue");
		t = getTag(map);
		l = t.content.listIterator();
		c = (Tag) l.next();
		assertTrue(c.content.size() == 2);
		l = c.content.listIterator();
		c = (Tag) l.next();
		assertTrue(c.content.listIterator().next() == "onValue");
		assertTrue(c.getName() == "oneElement");
		c = (Tag) l.next();
		assertTrue(c.content.listIterator().next() == "moreValue");
		assertTrue(c.getName() == "andMore");

		map = new LinkedHashMap<>();
		map.put("oneElement", Arrays.asList("onValue", "otherValue"));
		map.put("andMore", "moreValue");
		t = getTag(map);
		l = t.content.listIterator();
		c = (Tag) l.next();
		assertTrue(c.content.size() == 3);
		l = c.content.listIterator();
		c = (Tag) l.next();
		assertTrue(c.content.listIterator().next() == "onValue");
		assertTrue(c.getName() == "oneElement");
		c = (Tag) l.next();
		assertTrue(c.content.listIterator().next() == "otherValue");
		assertTrue(c.getName() == "oneElement");
		c = (Tag) l.next();
		assertTrue(c.content.listIterator().next() == "moreValue");
		assertTrue(c.getName() == "andMore");

		// Test Map of Map
		map2 = new LinkedHashMap<>();
		map2.put("complexElement", map);
		map2.put("andMore", "moreValue");
		t = getTag(map2);
		l = t.content.listIterator();
		c = (Tag) l.next();
		assertTrue(c.content.size() == 2);
		l = c.content.listIterator();
		c = (Tag) l.next();
		assertTrue(c.getName() == "complexElement");
		t = c;
		c = (Tag) l.next();
		assertTrue(c.content.listIterator().next() == "moreValue");
		assertTrue(c.getName() == "andMore");
		c = t;
		assertTrue(c.content.size() == 3);
		l = c.content.listIterator();
		c = (Tag) l.next();
		assertTrue(c.content.listIterator().next() == "onValue");
		assertTrue(c.getName() == "oneElement");
		c = (Tag) l.next();
		assertTrue(c.content.listIterator().next() == "otherValue");
		assertTrue(c.getName() == "oneElement");
		c = (Tag) l.next();
		assertTrue(c.content.listIterator().next() == "moreValue");
		assertTrue(c.getName() == "andMore");

		// Test mixed Map Collection Scalar
		map = new LinkedHashMap<>();
		map.put("complexElement", Arrays.asList("valueToTag", map2));
		map.put("andMore", "moreValue");
		t = getTag(map);
		l = t.content.listIterator();
		c = (Tag) l.next();
		assertTrue(c.content.size() == 3);
		l = c.content.listIterator();
		c = (Tag) l.next();
		assertTrue(c.content.listIterator().next() == "valueToTag");
		assertTrue(c.getName() == "complexElement");
		c = (Tag) l.next();
		assertTrue(c.getName() == "complexElement");
		t = c;
		c = (Tag) l.next();
		assertTrue(c.content.listIterator().next() == "moreValue");
		assertTrue(c.getName() == "andMore");
		assertTrue(t.content.size() == 2);
		l = t.content.listIterator();
		c = (Tag) l.next();
		assertTrue(c.content.size() == 3);
		assertTrue(c.getName() == "complexElement");
		t = c;
		c = (Tag) l.next();
		assertTrue(c.content.listIterator().next() == "moreValue");
		assertTrue(c.getName() == "andMore");
		c = t;
		assertTrue(c.content.size() == 3);
		l = c.content.listIterator();
		c = (Tag) l.next();
		assertTrue(c.content.listIterator().next() == "onValue");
		assertTrue(c.getName() == "oneElement");
		c = (Tag) l.next();
		assertTrue(c.content.listIterator().next() == "otherValue");
		assertTrue(c.getName() == "oneElement");
		c = (Tag) l.next();
		assertTrue(c.content.listIterator().next() == "moreValue");
		assertTrue(c.getName() == "andMore");

		// Test DTO
		OtherDTO dd = new OtherDTO();
		t = getTag(dd);
		l = t.content.listIterator();
		c = (Tag) l.next();
		assertTrue(c.content.size() == 1);
		l = c.content.listIterator();
		c = (Tag) l.next();
		assertTrue(c.content.listIterator().next() == "test");
		assertTrue(c.getName() == "oneElement");
	}

	private Tag getTag(Object toInster) {
		Tag res = new Tag("test");
		res.addContent("root", toInster);
		return res;
	}
}
