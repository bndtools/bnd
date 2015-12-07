package bndtools.wizards.project;

import junit.framework.TestCase;

public class ProjectNameGroupTest extends TestCase {

    public void testConvertToLegalPackageName() {
        assertEquals("bundle_1._2", ProjectNameGroup.toLegalPackageName("bundle-1.2"));
        assertEquals("bundle_1._2.foo", ProjectNameGroup.toLegalPackageName("bundle-1.2..foo"));
        assertEquals("blahblah", ProjectNameGroup.toLegalPackageName("blah blah"));
    }

}
