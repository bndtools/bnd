package bndtools.core.test.editors.quickfix;

import java.util.Arrays;

import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class BuildpathQuickFixProcessor_WithJupiterOnBuildpath_Test extends AbstractBuildpathQuickFixProcessorTest {

	@BeforeAll
	static void beforeAll() throws Exception {
		AbstractBuildpathQuickFixProcessorTest.beforeAll();
		addBundlesToBuildpath("junit-jupiter-api");
	}

	@Test
	void withUnqualifiedClassLiteral_forUnimportedType_asAnnotationParameterBoundedByTypeOnClassPath_suggestsBundles() {
		// This one has come up frequently in my own development. However, it
		// seems that this test case produces slightly different results to the
		// real-world scenario. In this test, two problems are generated - one
		// on the type name (ie, excluding the ".class"), and one on the type
		// literal (including the ".class"). In the real-world, when you hover
		// over them, only the one on the .class is generated (the highest
		// "layer").
		// test makes sure that it tests the problem on the type literal by
		// testing proposals at a point in the middle of the ".class" part
		// of the literal.
		String source = "package test; " + "import org.junit.jupiter.api.extension.ExtendWith; " + "@ExtendWith(";
		int start = source.length();
		source += "SoftAssertionsExtension.cl";
		int middleOfClass = source.length();
		source += "ass)\n" + "class " + DEFAULT_CLASS_NAME + "{" + "}";

		assertThatProposals(proposalsFor(middleOfClass, 0, source)).as("middle of .class")
			.haveExactly(1,
				suggestsBundle("assertj-core", "3.16.1", "org.assertj.core.api.junit.jupiter.SoftAssertionsExtension"));
	}

	@Test
	void removesDuplicateProposals() {
		String source = "package test; " + "import org.junit.jupiter.api.extension.ExtendWith; " + "@ExtendWith(";
		int start = source.length();
		source += "SoftAssertionsExtension.cl";
		int middleOfClass = source.length();
		source += "ass)\n" + "class " + DEFAULT_CLASS_NAME + "{" + "}";

		problemsFor(start, 0, DEFAULT_CLASS_NAME, source);
		if (problems.size() < 2) {
			throw new IllegalStateException(
				"This test requires that multiple problems be generated in order to work, but only got: "
					+ Arrays.toString(locs));
		}
		locs = problems.stream()
			.map(ProblemLocation::new)
			.toArray(IProblemLocation[]::new);
		assertThatProposals(proposals()).haveExactly(1,
			suggestsBundle("assertj-core", "3.16.1", "org.assertj.core.api.junit.jupiter.SoftAssertionsExtension"));
	}

}
