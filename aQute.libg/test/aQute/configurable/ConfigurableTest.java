package aQute.configurable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

public class ConfigurableTest extends TestCase {

	interface C {
		@Config(deflt = "abc\\,def,ghi")
		List<String> foo();

		@Config(deflt = "abc\\,def|ghi")
		List<String> bar();
	}

	public void testDefault() {
		C c = Configurable.createConfigurable(C.class, Collections.emptyMap());
		assertEquals(Arrays.asList("abc,def", "ghi"), c.foo());
		assertEquals(Arrays.asList("abc,def", "ghi"), c.foo());

	}

	public void testSimple() {
		assertEquals(Arrays.asList("abc|def"), Configurable.unescape("abc\\|def"));
		assertEquals(Arrays.asList("abc", "def"), Configurable.unescape("abc,def"));
		assertEquals(Arrays.asList("abc,def"), Configurable.unescape("abc\\,def"));
		assertEquals(Arrays.asList("abc", "def"), Configurable.unescape("abc  ,   def"));
		assertEquals(Arrays.asList("abc", "def"), Configurable.unescape("   abc  ,   def    "));
		assertEquals(Arrays.asList(" abc", "def"), Configurable.unescape("  \\ abc  ,   def    "));
		assertEquals(Arrays.asList("a b c", "def"), Configurable.unescape("  a b c  ,   def    "));
		assertEquals(Arrays.asList("\\ a b c", "def"), Configurable.unescape(" \\\\ a b c  ,   def    "));
	}
}
