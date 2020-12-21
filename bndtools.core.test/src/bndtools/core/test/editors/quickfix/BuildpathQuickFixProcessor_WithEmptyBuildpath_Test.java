package bndtools.core.test.editors.quickfix;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.stream.Stream;

import org.eclipse.jdt.core.compiler.IProblem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import aQute.lib.exceptions.Exceptions;

//@Disabled("Currently disabled due to startup flakiness, see https://github.com/bndtools/bnd/issues/4253")
public class BuildpathQuickFixProcessor_WithEmptyBuildpath_Test extends AbstractBuildpathQuickFixProcessorTest {

	@BeforeAll
	static void beforeAll() throws Exception {
		clearBuildpath();
	}

	@ParameterizedTest
	@MethodSource("supportedProblemTypes")
	void hasCorrections_forSupportedProblemTypes_returnsTrue(IProblem problem) {
		softly.assertThat(sut.hasCorrections(null, problem.getID()))
			.as(problem.getMessage())
			.isTrue();
	}

	static Stream<IProblem> supportedProblemTypes() {
		return allProblemTypes().filter(p -> SUPPORTED.contains(p.getID()));
	}

	@ParameterizedTest
	@MethodSource("unsupportedProblemTypes")
	void hasCorrections_forUnsupportedProblemTypes_returnsFalse(IProblem problem) {
		softly.assertThat(sut.hasCorrections(null, problem.getID()))
			.as(problem.getMessage())
			.isFalse();
	}

	static Stream<IProblem> unsupportedProblemTypes() {
		return allProblemTypes().filter(p -> !SUPPORTED.contains(p.getID()))
			.limit(50);
	}

	static IProblem getProblem(Field f) {
		try {
			int problemId = f.getInt(null);
			return new DummyProblem(problemId, f.getName());
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	static Stream<IProblem> allProblemTypes() {
		return Stream.of(IProblem.class.getFields())
			.map(BuildpathQuickFixProcessor_WithEmptyBuildpath_Test::getProblem);
	}

	@Test
	void withNoMatches_forUndefType_returnsNull() {
		assertThat(proposalsForUndefType("my.unknown.type.MyClass")).isNull();
	}

	@Test
	void withUnqualifiedType_suggestsBundles() {
		assertThatContainsBundleActivatorSuggestions(proposalsForUndefType("BundleActivator"));
	}

	@Test
	void withAnnotatedUnqualifiedType_suggestsBundles() {
		assertThatContainsBundleActivatorSuggestions(proposalsForUndefType(10, "@NonNull BundleActivator"));
	}

	@Disabled("Not yet implemented")
	@Test
	void withSimpleInnerType_suggestsBundles() {
		assertThatContainsListenerInfoSuggestions(proposalsForUndefType("ListenerInfo"));
	}

	@Test
	void withPartlyQualifiedInnerType_suggestsBundles() {
		assertThatContainsListenerHookSuggestions(proposalsForUndefType("ListenerHook.ListenerInfo"));
	}

	@Test
	void withFQType_suggestsBundles() {
		assertThatContainsBundleActivatorSuggestions(proposalsForUndefType("org.osgi.framework.BundleActivator"));
	}

	@Test
	void withFQNestedType_suggestsBundles() {
		assertThatContainsListenerHookSuggestions(
			proposalsForUndefType("org.osgi.framework.hooks.service.ListenerHook.ListenerInfo"));
	}

	@Test
	void withAnnotatedFQNestedType_suggestsBundles() {
		assertThatContainsListenerHookSuggestions(
			proposalsForUndefType("org.osgi.framework.hooks.service.ListenerHook.@NotNull ListenerInfo"));
	}

	@Test
	void withAnnotatedFQType_suggestsBundles() {
		assertThatContainsBundleActivatorSuggestions(
			proposalsForUndefType("org.osgi.framework.@NotNull BundleActivator"));
	}

	@Test
	void withAnnotatedSimpleType_suggestsBundles() {
		assertThatContainsBundleActivatorSuggestions(proposalsForUndefType(10, "@NotNull BundleActivator"));
	}

	@Test
	void withFQType_andOneLevelPackage_suggestsBundles() {
		assertThatContainsMyClassSuggestions(proposalsForUndefType("simple.MyClass"));
	}

	@Test
	void withAnnotatedFQType_andOneLevelPackage_suggestsBundles() {
		assertThatContainsMyClassSuggestions(proposalsForUndefType("simple.@NotNull MyClass"));
	}

	@Test
	void withGenericType_suggestsBundles() {
		assertThatContainsPromiseSuggestions(proposalsForUndefType("org.osgi.util.promise.Promise<String>"));
	}

	@Test
	void withParameterisedOuter_suggestsBundles() {
		assertThatContainsSimpleSuggestions(
			proposalsForUndefType("simple.pkg.MyParameterizedClass<String>.@NotNull MyInner"),
			"simple.pkg.MyParameterizedClass");
	}

	@Test
	void withUnannotatedFQArrayType_suggestsBundles() {
		assertThatContainsBundleActivatorSuggestions(proposalsForUndefType("org.osgi.framework.BundleActivator[]"));
	}

	@Test
	void withAnnotatedFQArrayType_suggestsBundles() {
		assertThatContainsBundleActivatorSuggestions(
			proposalsForUndefType("org.osgi.framework.@NotNull BundleActivator[]"));
	}

	@Test
	void withAnnotatedFQDoubleArrayType_suggestsBundles() {
		assertThatContainsBundleActivatorSuggestions(
			proposalsForUndefType("org.osgi.framework.@NotNull BundleActivator[][]"));
	}

	@Test
	void withSimpleUnannotatedParameter_suggestsBundles() {
		assertThatContainsBundleActivatorSuggestions(proposalsForUndefType(7, "List<BundleActivator>"));
	}

	@Test
	void withSimpleAnnotatedParameter_suggestsBundles() {
		assertThatContainsBundleActivatorSuggestions(proposalsForUndefType(15, "List<@NotNull BundleActivator>"));
	}

	@Test
	void withFQUnannotatedParameter_suggestsBundles() {
		assertThatContainsBundleActivatorSuggestions(
			proposalsForUndefType(6, "List<org.osgi.framework.BundleActivator>"));
	}

	@Test
	void withFQAnnotatedParameter_suggestsBundles() {
		assertThatContainsBundleActivatorSuggestions(
			proposalsForUndefType(6, "List<org.osgi.framework.@NotNull BundleActivator>"));
	}

	@Test
	void withFQWildcardBound_suggestsBundles() {
		assertThatContainsBundleActivatorSuggestions(
			proposalsForUndefType(16, "List<? extends org.osgi.framework.BundleActivator>"));
	}

	@Test
	void withFQAnnotatedWildcardBound_suggestsBundles() {
		assertThatContainsBundleActivatorSuggestions(
			proposalsForUndefType(16, "List<? extends org.osgi.framework.@NotNull BundleActivator>"));
	}

	@Test
	void withUnqualifiedNameType_returnsNull() {
		// If the type is a simple (non-qualified) name, it must refer to a type
		// and not a package; therefore we shouldn't provide package import
		// suggestions even if we have a package with the matching name. So
		// these should return null even though we have a bundle with the
		// package "simple".
		softly.assertThat(proposalsForUndefType("Simple"))
			.as("capitalized")
			.isNull();
		softly.assertThat(proposalsForUndefType("simple"))
			.as("uncapitalized")
			.isNull();
	}

	@Test
	void withParameterizedType_thatLooksLikePackage_returnsNull() {
		assertThat(proposalsForUndefType("org.osgi.framework<String>.BundleActivator")).isNull();
	}

	@Test
	void withNoMatches_returnsNull() {
		assertThat(proposalsFor(0, 0, "asdfasdfsadf;")).isNull();
	}

	@Test
	void withUnqualifiedNameImport_returnsNull() {
		// If the import statement is a simple (non-qualified) name and it's not
		// an on-demand import, it must refer to a type in the default package.
		// Therefore we shouldn't generate matches for packages of the same
		// name.
		softly.assertThat(proposalsForImport("Simple"))
			.as("capitalized")
			.isNull();
		softly.assertThat(proposalsForImport("simple"))
			.as("uncapitalized")
			.isNull();
	}

	@Test
	void withClassImport_suggestsBundles() {
		assertThatContainsBundleActivatorSuggestions(proposalsForImport("org.osgi.framework.BundleActivator"));
	}

	@Test
	void withInnerClassImport_suggestsBundles() {
		assertThatContainsListenerInfoSuggestions(
			proposalsForImport("org.osgi.framework.hooks.service.ListenerHook.ListenerInfo"));
	}

	@Test
	void withOnDemandImport_suggestsBundles() {
		assertThatContainsFrameworkBundles(proposalsForImport("org.osgi.framework.*"), "org.osgi.framework");
	}

	@Test
	void withOnDemandImport_ofNestedClasses_suggestsBundles() {
		assertThatContainsListenerInfoSuggestions(
			proposalsForImport("org.osgi.framework.hooks.service.ListenerHook.ListenerInfo.*"));
	}

	@Test
	void withOnDemandStaticImport_suggestsBundles() {
		assertThatContainsBundleSuggestions(proposalsForStaticImport("org.osgi.framework.Bundle.*"));
	}

	@Test
	void withStaticImport_suggestsBundles() {
		assertThatContainsBundleSuggestions(proposalsForStaticImport("org.osgi.framework.Bundle.INSTALLED"));
	}

	@Test
	void withSimplePackageName_suggestsBundles() {
		assertThatContainsSimpleSuggestions(proposalsForImport("simple.MyClass"), "simple.MyClass");
	}

	@Test
	void withOnDemandSimplePackageName_suggestsBundles() {
		assertThatContainsSimpleSuggestions(proposalsForImport("simple.*"), "simple");
	}

	@Test
	void withClassLiteral_suggestsBundles() {
		assertThatContainsBundleSuggestions(proposalsForLiteral("Bundle"));
	}

	@Test
	void withMissingAnnotations_suggestsBundles() {
		assertThatContainsSimpleSuggestions(proposalsForAnnotation("SimpleAnnotation"),
			"simple.annotation.SimpleAnnotation");
		assertThatContainsSimpleSuggestions(proposalsForAnnotation("simple.annotation.SimpleAnnotation"),
			"simple.annotation.SimpleAnnotation");
		assertThatContainsSimpleSuggestions(proposalsForAnnotation("SimpleAnnotation(String.class)"),
			"simple.annotation.SimpleAnnotation");
	}

	@Test
	void withReference_toStaticMethodOfMissingType_suggestsBundles() {
		String header = "package test; class ";
		String source = header + DEFAULT_CLASS_NAME + " {\n" + "Object o = ";
		int begin = source.length();
		source += "FrameworkUtil";
		String prefixSource = source;
		int end = source.length();
		// For our purposes it doesn't matter that this static method doesn't
		// actually exist
		source += ".staticMethod();}";

		assertThatContainsFrameworkBundles(proposalsFor(begin, 1, source), "org.osgi.framework.FrameworkUtil");
	}

	@Test
	void withReference_toStaticFieldOfMissingType_suggestsBundles() {
		String header = "package test; class ";
		String source = header + DEFAULT_CLASS_NAME + " {\n" + "Object o = ";
		int begin = source.length();
		source += "FrameworkUtil";
		String prefixSource = source;
		int end = source.length();
		// For our purposes it doesn't matter that this static field doesn't
		// actually exist
		source += ".staticField;}";

		assertThatContainsFrameworkBundles(proposalsFor(begin, 1, source), "org.osgi.framework.FrameworkUtil");
	}

	@Test
	void withReference_toStaticFieldOfMissingNestedType_suggestsBundles() {
		String header = "package test; class ";
		String source = header + DEFAULT_CLASS_NAME + " {\n" + "Object o = ";
		int begin = source.length();
		source += "ListenerHook.ListenerInfo";
		String prefixSource = source;
		int end = source.length();
		// For our purposes it doesn't matter that this static field doesn't
		// actually exist
		source += ".staticField;}";

		assertThatContainsListenerHookSuggestions(proposalsFor(begin, 1, source));
	}

	@Test
	void withReference_toStaticFieldOfFQMissingType_suggestsBundles() {
		String header = "package test; class ";
		String source = header + DEFAULT_CLASS_NAME + " {\n" + "Object o = ";
		int begin = source.length();
		source += "org.osgi.framework.FrameworkUtil";
		String prefixSource = source;
		int end = source.length();
		// For our purposes it doesn't matter that this static field doesn't
		// actually exist
		source += ".staticField;}";

		assertThatContainsFrameworkBundles(proposalsFor(begin, 1, source), "org.osgi.framework.FrameworkUtil");
	}

	@Test
	void withUnqualifiedClassLiteral_forUnimportedType_asAnnotationParameter_suggestsBundles() {
		String header = "package test; " + "import simple.annotation.MyTag; " + "@MyTag(";
		String source = header + "MyInterface.class)" + "class " + DEFAULT_CLASS_NAME + "{" + "}";

		assertThatProposals(proposalsFor(header.length() + 2, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyInterface"));
	}

	/*
	 * Check that if a suggestion's fqn can be found on the buildpath it is not
	 * proposed.
	 */
	@Test
	void testWithShortClassNameInRepoAndBuildpath() {
		String source = "package test; public class " + DEFAULT_CLASS_NAME + "{ ";
		int start = source.length();
		source += "Tag unqualifiedTagType; }";

		assertThatProposals(proposalsFor(start, 3, source)).haveExactly(1,
			suggestsBundle("junit-jupiter-api", "5.6.2", "org.junit.jupiter.api.Tag"));

		// Now put it on the buildpath

		addBundlesToBuildpath("junit-jupiter-api");

		try {
			assertThatProposals(proposalsFor(start, 3, source)).isEmpty();
		} finally {
			clearBuildpath();
		}
	}

	@Test
	void simpleReference_toWorkspaceClass_suggestsBundles() {
		String header = "package test; class " + DEFAULT_CLASS_NAME + "{";
		String source = header + "WSClass wsClass; }";

		assertThatProposals(proposalsFor(header.length() + 2, 0, source)).haveExactly(1,
			suggestsBundle("local-proj", "0.0.0", "my.local.ws.pkg.WSClass"));
	}
}
