package biz.aQute.bnd.reporter.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ArrayHelperTest {

  @Test
  public void testContainsIgnoreCase() {
    assertEquals(false, ArrayHelper.containsIgnoreCase(null, null));
    assertEquals(false, ArrayHelper.containsIgnoreCase(new String[0], null));
    assertEquals(false, ArrayHelper.containsIgnoreCase(new String[] {}, ""));
    assertEquals(false, ArrayHelper.containsIgnoreCase(null, "a"));
    assertEquals(true, ArrayHelper.containsIgnoreCase(new String[] {""}, ""));
    assertEquals(true, ArrayHelper.containsIgnoreCase(new String[] {"a"}, "a"));
    assertEquals(true, ArrayHelper.containsIgnoreCase(new String[] {"a"}, "A"));
    assertEquals(true, ArrayHelper.containsIgnoreCase(new String[] {"a", "b"}, "A"));
    assertEquals(false, ArrayHelper.containsIgnoreCase(new String[] {"a", "b"}, "c"));
  }

  @Test
  public void testOneInBoth() {
    assertEquals(null, ArrayHelper.oneInBoth(null, null));
    assertEquals(null, ArrayHelper.oneInBoth(null, new String[] {}));
    assertEquals(null, ArrayHelper.oneInBoth(new String[] {}, null));
    assertEquals(null, ArrayHelper.oneInBoth(null, new String[] {"a"}));
    assertEquals(null, ArrayHelper.oneInBoth(new String[] {"a"}, null));
    assertEquals("a", ArrayHelper.oneInBoth(new String[] {"a"}, new String[] {"a"}));
    assertEquals("a", ArrayHelper.oneInBoth(new String[] {"a", "b"}, new String[] {"a", "c"}));
    assertEquals(null, ArrayHelper.oneInBoth(new String[] {"a", "b"}, new String[] {"d", "c"}));
    assertEquals("a", ArrayHelper.oneInBoth(new String[] {"a", "b"}, new String[] {"A", "c"}));
    assertEquals("A", ArrayHelper.oneInBoth(new String[] {"A", "b"}, new String[] {"a", "c"}));
    assertEquals(null, ArrayHelper.oneInBoth(new String[] {null, "b"}, new String[] {null, "c"}));
    assertEquals("b", ArrayHelper.oneInBoth(new String[] {null, "b"}, new String[] {null, "b"}));
    final String e = ArrayHelper.oneInBoth(new String[] {"a", "b"}, new String[] {"a", "b"});
    assertTrue(e.equals("a") || e.equals("b"));
  }
}
