package aQute.bnd.deployer.repository.providers;

import junit.framework.TestCase;

public class TestAttributeTypeParsing extends TestCase {

	public static void testScalarTypeNames() {
		assertEquals(AttributeType.STRING, AttributeType.parseTypeName(null));
		assertEquals(AttributeType.STRING, AttributeType.parseTypeName("String"));
		assertEquals(AttributeType.LONG, AttributeType.parseTypeName("Long"));
		assertEquals(AttributeType.DOUBLE, AttributeType.parseTypeName("Double"));
		assertEquals(AttributeType.VERSION, AttributeType.parseTypeName("Version"));
	}

	public static void testListTypeNames() {
		assertEquals(AttributeType.STRINGLIST, AttributeType.parseTypeName("List<String>"));
		assertEquals(AttributeType.LONGLIST, AttributeType.parseTypeName("List<Long>"));
		assertEquals(AttributeType.DOUBLELIST, AttributeType.parseTypeName("List<Double>"));
		assertEquals(AttributeType.VERSIONLIST, AttributeType.parseTypeName("List<Version>"));
	}

}
