package test.simple;

import junit.framework.TestCase;

public class ProjectNameTest extends TestCase {

	public void testProjectName() {
		String projectNameSysProp = System.getProperty("testprojectname");
		assertEquals("test.simple", projectNameSysProp);
	}
}
