package aQute.configurable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

public class ConfigurableTest {

	interface C {
		@Config(deflt = "abc\\,def,ghi")
		List<String> foo();

		@Config(deflt = "abc\\,def|ghi")
		List<String> bar();
	}

	@Test
	public void testDefault() {
		C c = Configurable.createConfigurable(C.class, Collections.emptyMap());
		assertEquals(Arrays.asList("abc,def", "ghi"), c.foo());
		assertEquals(Arrays.asList("abc,def", "ghi"), c.foo());

	}

	@Test
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
