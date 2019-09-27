package aQute.lib.tag;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import junit.framework.TestCase;

public class TagTest extends TestCase {

	public void testScalar() {
		depthCompare(null, "root", "");
		depthCompare("", "root", "");
		depthCompare("simple", "root", "simple");
	}

	public void testCollectionOfScalar() {
		depthCompare(new LinkedList<>(), "root", "");
		depthCompare(Arrays.asList(""), "root", "rootElement", "");
		depthCompare(Arrays.asList("oneElement"), "root", "rootElement", "oneElement");
		depthCompare(Arrays.asList("oneElement", "andMore"), "root", "rootElement", "oneElement", "rootElement",
			"andMore");
	}

	public void testCollectionOfCollection() {
		depthCompare(Arrays.asList(Arrays.asList()), "root", "rootElement", "");
		depthCompare(Arrays.asList(Arrays.asList("")), "root", "rootElement", "element", "");
		depthCompare(Arrays.asList(Arrays.asList("oneElement")), "root", "rootElement", "element", "oneElement");
		depthCompare(Arrays.asList(Arrays.asList("oneElement", null)), "root", "rootElement", "element", "oneElement");
		depthCompare(Arrays.asList(Arrays.asList("oneElement", "andMore"), Arrays.asList("oneElement2")), "root",
			"rootElement", "element", "oneElement", "element", "andMore", "rootElement", "element", "oneElement2");
	}

	public void testArray() {
		depthCompare(new String[] {
			"oneElement", "andMore"
		}, "root", "rootElement", "oneElement", "rootElement", "andMore");

	}

	public void testMapOfScalar() {
		Map<String, Object> map;

		depthCompare(new LinkedHashMap<>(), "root", "");

		map = new LinkedHashMap<>();
		map.put("oneElement", null);
		depthCompare(map, "root", "");

		map = new LinkedHashMap<>();
		map.put(null, "oneElement");
		depthCompare(map, "root", "");

		map = new LinkedHashMap<>();
		map.put("oneElement", "onValue");
		depthCompare(map, "root", "oneElement", "onValue");

		map = new LinkedHashMap<>();
		map.put("oneElement", "onValue");
		map.put("andMore", "moreValue");
		depthCompare(map, "root", "oneElement", "onValue", "andMore", "moreValue");
	}

	public void testMapOfCollection() {
		Map<String, Object> map;

		map = new LinkedHashMap<>();
		map.put("oneElement", new LinkedList<>());
		map.put("andMore", "moreValue");
		depthCompare(map, "root", "oneElement", "", "andMore", "moreValue");

		map = new LinkedHashMap<>();
		map.put("oneElement", Arrays.asList(""));
		map.put("andMore", "moreValue");
		depthCompare(map, "root", "oneElement", "oneElementElement", "", "andMore", "moreValue");

		map = new LinkedHashMap<>();
		map.put("oneElement", Arrays.asList("onValue"));
		map.put("andMore", "moreValue");
		depthCompare(map, "root", "oneElement", "oneElementElement", "onValue", "andMore", "moreValue");

		map = new LinkedHashMap<>();
		map.put("oneElement", Arrays.asList("onValue", "otherValue"));
		map.put("andMore", "moreValue");
		depthCompare(map, "root", "oneElement", "oneElementElement", "onValue", "oneElementElement", "otherValue",
			"andMore", "moreValue");
	}

	public void testMapOfMap() {
		Map<String, Object> map;
		Map<String, Object> map2;

		map = new LinkedHashMap<>();
		map.put("oneElement", Arrays.asList("onValue", "otherValue"));
		map.put("andMore", "moreValue");

		map2 = new LinkedHashMap<>();
		map2.put("complexElement", map);
		map2.put("andMore", "moreValue");

		depthCompare(map2, "root", "complexElement", "oneElement", "oneElementElement", "onValue", "oneElementElement",
			"otherValue", "andMore", "moreValue", "andMore", "moreValue");
	}

	public void testMixedMapCollectionScalar() {
		Map<String, Object> map;
		Map<String, Object> map2;

		map = new LinkedHashMap<>();
		map.put("oneElement", Arrays.asList("onValue", "otherValue"));
		map.put("andMore", "moreValue");

		map2 = new LinkedHashMap<>();
		map2.put("complexElement", map);
		map2.put("andMore", "moreValue");

		map = new LinkedHashMap<>();
		map.put("complexElement", Arrays.asList("valueToTag", map2));
		map.put("andMore", "moreValue");

		depthCompare(map, "root", "complexElement", "complexElementElement", "valueToTag", "complexElementElement",
			"complexElement", "oneElement", "oneElementElement", "onValue", "oneElementElement", "otherValue",
			"andMore", "moreValue", "andMore", "moreValue", "andMore", "moreValue");
	}

	public void testDto() {
		depthCompare(new OtherDTO(), "root", "oneElement", "test");
	}

	public void testGenericName() {
		depthCompareTweak("roots", Arrays.asList("test"), "roots", "root", "test");
		depthCompareTweak("ROOTS", Arrays.asList("test"), "ROOTS", "ROOT", "test");
		depthCompareTweak("ROOT", Arrays.asList("test"), "ROOT", "ROOT_ELEMENT", "test");
		depthCompareTweak("root", Arrays.asList("test"), "root", "rootElement", "test");
		depthCompareTweak("root8", Arrays.asList("test"), "root8", "root8_ELEMENT", "test");

		depthCompareTweak("ROOT", "item", Arrays.asList("test"), "ROOT", "ROOT_ITEM", "test");
		depthCompareTweak("root", "item", Arrays.asList("test"), "root", "rootItem", "test");
		depthCompareTweak("root", "_item", Arrays.asList("test"), "root", "root_item", "test");
		depthCompareTweak("root8", "item", Arrays.asList("test"), "root8", "root8_ITEM", "test");
		depthCompareTweak("root8", "", Arrays.asList("test"), "root8", "root8_ELEMENT", "test");
		depthCompareTweak("root8", null, Arrays.asList("test"), "root8", "root8_ELEMENT", "test");
	}

	private static void depthCompareTweak(String rootName, Object dto, String... elements) {
		LinkedList<String> e = new LinkedList<>(Arrays.asList(elements));
		depthCompare(Tag.fromDTO(rootName, dto), e);
		assertEquals(0, e.size());
	}

	private static void depthCompareTweak(String rootName, String genericName, Object dto, String... elements) {
		LinkedList<String> e = new LinkedList<>(Arrays.asList(elements));
		depthCompare(Tag.fromDTO(rootName, genericName, dto), e);
		assertEquals(0, e.size());
	}

	private static void depthCompare(Object dto, String... elements) {
		LinkedList<String> e = new LinkedList<>(Arrays.asList(elements));
		depthCompare(Tag.fromDTO("root", dto), e);
		assertEquals(0, e.size());
	}

	private static void depthCompare(Tag tag, LinkedList<String> elements) {
		assertTrue(elements.size() > 0);
		assertEquals(elements.pop(), tag.getName());

		if (tag.content.isEmpty() && elements.getFirst() == "") {
			elements.pop();
		} else {
			Iterator<Object> it = tag.content.iterator();
			while (it.hasNext()) {
				Object o = it.next();
				if (o instanceof Tag) {
					depthCompare((Tag) o, elements);
				} else {
					assertEquals(elements.pop(), o);
				}
			}
		}
	}

	public void testCDATAEscaped() throws Exception {
		Tag t = new Tag("test", "]]>blah blah bl]]>]]>ah blah]]>");
		t.setCDATA();
		StringWriter w = new StringWriter();
		try (PrintWriter pw = new PrintWriter(w)) {
			t.print(0, pw);
		}
		assertThat(w.toString())
			.contains("<![CDATA[]]]]><![CDATA[>blah blah bl]]]]><![CDATA[>]]]]><![CDATA[>ah blah]]]]><![CDATA[>]]>");
	}

	public void testCDATA() throws Exception {
		Tag t = new Tag("test", "blah blah blah blah");
		t.setCDATA();
		StringWriter w = new StringWriter();
		try (PrintWriter pw = new PrintWriter(w)) {
			t.print(0, pw);
		}
		assertThat(w.toString()).contains("<![CDATA[blah blah blah blah]]>");
	}

	public void testDate() throws Exception {
		Date now = new Date();
		Tag t = new Tag("test");
		t.addAttribute("date", now);
		StringWriter w = new StringWriter();
		try (PrintWriter pw = new PrintWriter(w)) {
			t.print(0, pw);
		}
		assertThat(w.toString()).contains("<test date=\"" + Tag.DATE_TIME_FORMATTER.format(now.toInstant()) + "\"");
	}
}
