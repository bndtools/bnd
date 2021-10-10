package bndtools.wizards.project;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ProjectNameGroupTest {

	@Test
	public void testConvertToLegalPackageName() {
		assertEquals("bundle_1._2", ProjectNameGroup.toLegalPackageName("bundle-1.2"));
		assertEquals("bundle_1._2.foo", ProjectNameGroup.toLegalPackageName("bundle-1.2..foo"));
		assertEquals("blahblah", ProjectNameGroup.toLegalPackageName("blah blah"));
	}

}
