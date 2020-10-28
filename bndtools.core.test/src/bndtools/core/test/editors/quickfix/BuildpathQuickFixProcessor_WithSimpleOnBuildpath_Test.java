package bndtools.core.test.editors.quickfix;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class BuildpathQuickFixProcessor_WithSimpleOnBuildpath_Test extends AbstractBuildpathQuickFixProcessorTest {

	@BeforeAll
	static void beforeAll() throws Exception {
		AbstractBuildpathQuickFixProcessorTest.beforeAll();
		addBundlesToBuildpath("bndtools.core.test.fodder.simple");
	}

	@Test
	void withInconsistentHierarchy_forClassDefinition_thatImplementsAnInterfaceFromAnotherBundle_suggestsBundles() {
		String header = "package test; class ";
		String source = header + DEFAULT_CLASS_NAME + " extends simple.pkg.ClassWithInterfaceFromAnotherBundle {}";

		// IsClassPathCorrect occurs at [0, 1]
		assertThatProposals(proposalsFor(0, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyInterface"));
		// HierarchyHasProblems is on the type name
		assertThatProposals(proposalsFor(header.length() + 1, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyInterface"));
	}

	@Test
	void withInconsistentHierarchy_forClassDefinition_thatExtendsClassFromAnotherBundle_suggestsBundles() {
		String header = "package test; class ";
		String source = header + DEFAULT_CLASS_NAME + " extends simple.pkg.ClassExtendingClassFromAnotherBundle {}";

		// IsClassPathCorrect occurs at [0, 1]
		assertThatProposals(proposalsFor(0, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyForeignClass"));
		// HierarchyHasProblems is on the type name
		assertThatProposals(proposalsFor(header.length() + 1, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyForeignClass"));
	}

	@Test
	void withInconsistentHierarchy_forInterfaceDefinition_thatExtendsAnInterfaceFromAnotherBundle_suggestsBundles() {
		String header = "package test; interface ";
		String source = header + DEFAULT_CLASS_NAME
			+ " extends simple.pkg.InterfaceExtendingInterfaceFromAnotherBundle {}";

		// IsClassPathCorrect occurs at [0, 1]
		assertThatProposals(proposalsFor(0, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyInterface"));
		// HierarchyHasProblems is on the type name
		assertThatProposals(proposalsFor(header.length() + 1, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyInterface"));
	}

	@Test
	void withMissingMethod_fromInterfaceFromAnotherBundle_suggestsBundles() {
		String header = "package test; class ";
		String source = header + DEFAULT_CLASS_NAME + "{\n"
			+ "  simple.pkg.InterfaceExtendingInterfaceFromAnotherBundle var;\n" + "  void myMethod() {"
			+ "    var.myInterfaceMethod();" + "  }" + "}";

		// IsClassPathCorrect occurs at [112, 134]
		assertThatProposals(proposalsFor(112, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyInterface"));
		// UnknownMethod is on the method at [116,132]
		assertThatProposals(proposalsFor(117, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyInterface"));
	}

	@Test
	void withMissingMethod_fromClassFromAnotherBundle_suggestsBundles() {
		String header = "package test; class ";
		String source = header + DEFAULT_CLASS_NAME + "{\n" + "  simple.pkg.ClassExtendingClassFromAnotherBundle var;\n"
			+ "  void myMethod() {" + "    var.bMethod();" + "  }" + "}";

		// IsClassPathCorrect occurs at [104, 116]
		assertThatProposals(proposalsFor(104, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyForeignClass"));
		// UnknownMethod is on the method at [108,114]
		assertThatProposals(proposalsFor(108, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyForeignClass"));
	}

	@Test
	void withMissingMethod_fromUnknownSimpleSuperclass_suggestsBundles() {
		String header = "package test; class ";
		String source = header + DEFAULT_CLASS_NAME + " extends MyForeignClass {\n" + "  void myMethod() {"
			+ "    bMethod();" + "  }" + "}";

		// UnknownMethod is on the method at [73,80]
		assertThatProposals(proposalsFor(74, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyForeignClass"));
	}

	@Test
	void withMissingSuperMethod_fromClassFromAnotherBundle_suggestsBundles() {
		String header = "package test; class ";
		// set up various triggers for UnknownMethod that are caused
		// by an incomplete type hierarchy
		// @formatter:off
		String source = header + DEFAULT_CLASS_NAME + " extends simple.pkg.ClassExtendingClassFromAnotherBundle\n"
			+ "  implements simple.pkg.InterfaceExtendingInterfaceFromAnotherBundle {\n"
			+ "  void myMethod() {\n"
			+ "    bMethod();\n"
			+ "    super.bMethod();\n"
			+ "    this.bMethod();\n"
			+ "    simple.pkg.InterfaceExtendingInterfaceFromAnotherBundle.super.myInterfaceMethod();\n"
			+ "  }\n"
			+ "  public void cMethod() {}\n"
			+ "}";
		// @formatter:on

		// bMethod() at [176,183]
		assertThatProposals(proposalsFor(176, 0, source)).haveExactly(1, suggestsBundle(
			"bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyForeignClass, iface.bundle.MyInterface"));
		// super.bMethod() at [197,204]
		assertThatProposals(proposalsFor(197, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyForeignClass"));
		// this.bMethod() at [217,224]
		assertThatProposals(proposalsFor(217, 0, source)).haveExactly(1, suggestsBundle(
			"bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyForeignClass, iface.bundle.MyInterface"));
		// simple.pkg.InterfaceExtendingInterfaceFromAnotherBundle.super.myInterfaceMethod()
		// at [294,311]
		assertThatProposals(proposalsFor(294, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyInterface"));
	}

	@Test
	void withMissingField_fromClassFromAnotherBundle_forQualifiedNameAccess_suggestsBundles() {
		String header = "package test; class ";
		String source = header + DEFAULT_CLASS_NAME + "{\n" + "  simple.pkg.ClassExtendingClassFromAnotherBundle var;\n"
			+ "  void myMethod() {" + "    String s = var.bField;" + "  }" + "}";

		// IsClassPathCorrect occurs at [115, 125]
		assertThatProposals(proposalsFor(116, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyForeignClass"));
		// UnknownField is on the method at [119,125]
		assertThatProposals(proposalsFor(120, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyForeignClass"));
	}

	@Test
	void withMissingField_fromClassFromAnotherBundle_forExpressionAccess_suggestsBundles() {
		String header = "package test; class ";
		String source = header + DEFAULT_CLASS_NAME + "{\n"
			+ "  simple.pkg.ClassExtendingClassFromAnotherBundle var() { return null; };\n" + "  void myMethod() {"
			+ "    String s = var().bField;" + "  }" + "}";

		// IsClassPathCorrect occurs at [134, 146]
		assertThatProposals(proposalsFor(134, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyForeignClass"));
		// UnknownField is on the method at [140,146]
		assertThatProposals(proposalsFor(140, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyForeignClass"));
	}

	@Test
	void withMissingField_fromSuperClassFromAnotherBundle_forExpressionAccess_suggestsBundles() {
		String header = "package test; class ";
		String source = header + DEFAULT_CLASS_NAME + " extends simple.pkg.ClassExtendingClassFromAnotherBundle {\n"
			+ "  void myMethod() {" + "    String s = super.bField;" + "  }" + "}";

		// UnknownField is on the method at [123,129]
		assertThatProposals(proposalsFor(123, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyForeignClass"));
	}

	@Test
	void withInconsistentHierarchy_forComplicatedGenericHierarchy_suggestsBundles() {
		String header = "package test; import java.util.List;" + "import simple.pkg.MyParameterizedClass;"
			+ "import simple.MyClass;" + "import simple.pkg.ClassExtendingAbstractExtendingMyParameterizedClass;"
			+ "class ";
		String source = header + DEFAULT_CLASS_NAME + "{\n" + "  List<MyParameterizedClass<? extends MyClass>> var;\n"
			+ "  void myMethod() {" + "    var.add(new ClassExtendingAbstractExtendingMyParameterizedClass());" + "  }"
			+ "}";

		// ParameterMismatch [259, 261]
		assertThatProposals(proposalsFor(259, 0, source)).haveExactly(1, suggestsBundle(
			"bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.ClassExtendingMyParameterizedClass"));
	}

	@Test
	void withOverloadedMethod_onInconsistentHierarchy_forComplicatedGenericHierarchy_suggestsBundles() {
		String header = "package test; import java.util.List;" + "import simple.pkg.MyParameterizedClass;"
			+ "import simple.MyClass;" + "import simple.pkg.ClassExtendingAbstractExtendingMyParameterizedClass;"
			+ "class ";
		String source = header + DEFAULT_CLASS_NAME + "{\n"
			+ "  ClassExtendingAbstractExtendingMyParameterizedClass var;\n" + "  void myMethod() {"
			+ "    var.myOverloadedMethod(\"something\");" + "  }" + "}";

		// ParameterMismatch [265, 283]
		assertThatProposals(proposalsFor(266, 0, source)).haveExactly(1, suggestsBundle(
			"bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.ClassExtendingMyParameterizedClass"));
	}

	@Test
	void withUnknownMethod_forRecursiveGenericHierarchy_avoidsStackOverflow() {
		String header = "package test; import simple.pkg.RecursiveClass; class " + DEFAULT_CLASS_NAME + " {\n"
			+ "void myMethod() { new RecursiveClass().";
		source = header + "unknownMethod(); } " + "}";

		assertThatProposals(proposalsFor(header.length() + 1, 0, source)).isEmpty();
	}

	@Test
	void withInconsistentHierarchy_forRecursive_suggestsBundles() {
		source = "package test; class Test extends simple.pkg.ClassExtendingForeignRecursiveClass<Test> {\n"
			+ "String myMethod() { \n" + "  field.length();" + "  method();" + "  return field;" + "}" + "}";

		// HierarchyHasProblems on "Test"
		assertThatProposals(proposalsFor(20 + 1, 0, source)).haveExactly(1, suggestsBundle(
			"bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyForeignRecursiveClass"));
		// UnknownName on "field.length()"
		assertThatProposals(proposalsFor(111 + 1, 0, source)).haveExactly(1, suggestsBundle(
			"bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyForeignRecursiveClass"));
		// UnknownMethod on "method()"
		assertThatProposals(proposalsFor(128 + 1, 0, source)).haveExactly(1, suggestsBundle(
			"bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyForeignRecursiveClass"));
		// UnknownField on "return field"
		assertThatProposals(proposalsFor(146 + 1, 0, source)).haveExactly(1, suggestsBundle(
			"bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyForeignRecursiveClass"));
	}

	@Test
	void withInconsistentSuperclassHierarchy_forRecursive_suggestsBundles() {
		source = "package test; class Test extends simple.pkg.GrandchildOfForeignRecursiveClass<Test> {\n"
			+ "String myMethod() { \n" + "  field.length();" + "  method();" + "  return field;" + "}" + "}";

		// HierarchyHasProblems on "Test"
		assertThatProposals(proposalsFor(20 + 1, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyForeignRecursiveClass"));
		// UnknownName on "field.length()"
		assertThatProposals(proposalsFor(109 + 1, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyForeignRecursiveClass"));
		// UnknownMethod on "method()"
		assertThatProposals(proposalsFor(126 + 1, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyForeignRecursiveClass"));
		// UnknownField on "return field"
		assertThatProposals(proposalsFor(144 + 1, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyForeignRecursiveClass"));
	}

	@Test
	void withFQClassLiteral_asAnnotationParameter_suggestsBundles() {
		String header = "package test; " + "import simple.annotation.MyTag;" + "@MyTag(";
		String source = header + "iface.bundle.MyInterface.class)" + "class " + DEFAULT_CLASS_NAME + "{" + "}";

		assertThatProposals(proposalsFor(header.length() + 2, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyInterface"));
	}

	@Disabled("Disabled due to Eclipse bug")
	@Test
	void withFQClassLiteral_inheritingFromInterfaceFromAnotherBundle_asAnnotationParameter_suggestsBundles() {
		String header = "package test; " + "import simple.annotation.MyTag;" + "@MyTag(";
		String source = header + "simple.pkg.ClassWithInterfaceExtendingMyInterface.class)" + "class "
			+ DEFAULT_CLASS_NAME + "{" + "}";

		assertThatProposals(proposalsFor(header.length() + 2, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyInterface"));
	}

	@Test
	void withUnqualifiedClassLiteral_extendingInterfaceFromAnotherBundle_asAnnotationParameter_suggestsBundles() {
		// This scenario causes a "TypeMismatch" because it doesn't know the
		// superinterface of InterfaceExtendingInterfaceFromAnotherBundle
		String header = "package test; "
			+ "import simple.annotation.MyTag; import simple.pkg.InterfaceExtendingInterfaceFromAnotherBundle;"
			+ "@MyTag(";
		String source = header + "InterfaceExtendingInterfaceFromAnotherBundle.class)" + "class " + DEFAULT_CLASS_NAME
			+ "{" + "}";

		assertThatProposals(proposalsFor(header.length() + 2, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyInterface"));
	}

}
