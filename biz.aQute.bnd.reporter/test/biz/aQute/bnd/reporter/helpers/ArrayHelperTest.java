package biz.aQute.bnd.reporter.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ArrayHelperTest {

	@Test
	public void testContainsIgnoreCase() {
		assertFalse(ArrayHelper.containsIgnoreCase(null, null));
		assertFalse(ArrayHelper.containsIgnoreCase(new String[0], null));
		assertFalse(ArrayHelper.containsIgnoreCase(new String[] {}, ""));
		assertFalse(ArrayHelper.containsIgnoreCase(null, "a"));
		assertTrue(ArrayHelper.containsIgnoreCase(new String[] {
			""
		}, ""));
		assertTrue(ArrayHelper.containsIgnoreCase(new String[] {
			"a"
		}, "a"));
		assertTrue(ArrayHelper.containsIgnoreCase(new String[] {
			"a"
		}, "A"));
		assertTrue(ArrayHelper.containsIgnoreCase(new String[] {
			"a", "b"
		}, "A"));
		assertFalse(ArrayHelper.containsIgnoreCase(new String[] {
			"a", "b"
		}, "c"));
	}

	@Test
	public void testOneInBoth() {
		assertEquals(null, ArrayHelper.oneInBoth(null, null));
		assertEquals(null, ArrayHelper.oneInBoth(null, new String[] {}));
		assertEquals(null, ArrayHelper.oneInBoth(new String[] {}, null));
		assertEquals(null, ArrayHelper.oneInBoth(null, new String[] {
			"a"
		}));
		assertEquals(null, ArrayHelper.oneInBoth(new String[] {
			"a"
		}, null));
		assertEquals("a", ArrayHelper.oneInBoth(new String[] {
			"a"
		}, new String[] {
			"a"
		}));
		assertEquals("a", ArrayHelper.oneInBoth(new String[] {
			"a", "b"
		}, new String[] {
			"a", "c"
		}));
		assertEquals(null, ArrayHelper.oneInBoth(new String[] {
			"a", "b"
		}, new String[] {
			"d", "c"
		}));
		assertEquals("a", ArrayHelper.oneInBoth(new String[] {
			"a", "b"
		}, new String[] {
			"A", "c"
		}));
		assertEquals("A", ArrayHelper.oneInBoth(new String[] {
			"A", "b"
		}, new String[] {
			"a", "c"
		}));
		assertEquals(null, ArrayHelper.oneInBoth(new String[] {
			null, "b"
		}, new String[] {
			null, "c"
		}));
		assertEquals("b", ArrayHelper.oneInBoth(new String[] {
			null, "b"
		}, new String[] {
			null, "b"
		}));
		final String e = ArrayHelper.oneInBoth(new String[] {
			"a", "b"
		}, new String[] {
			"a", "b"
		});
		assertTrue(e.equals("a") || e.equals("b"));
	}
}
