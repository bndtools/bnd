package org.bndtools.templating;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Test;

public class CategoryTest {

    @Test
    public void testCategorySort() {
        Category a = new Category("aaa/Foobar");
        Category b = new Category("bbb/Foobar");
        Category c = new Category("bbb/Foobar2");

        assertFalse(a.equals(b));
        assertFalse(a.hashCode() == b.hashCode());
        assertFalse(a.compareTo(b) == 0);

        SortedSet<Category> set = new TreeSet<>();
        set.add(a);
        set.add(b);
        set.add(c);

        Category[] array = set.toArray(new Category[0]);
        assertEquals("aaa", array[0].getPrefix());
        assertEquals("Foobar", array[0].getName());

        assertEquals("bbb", array[1].getPrefix());
        assertEquals("Foobar", array[1].getName());

        assertEquals("bbb", array[2].getPrefix());
        assertEquals("Foobar2", array[2].getName());
    }

}
