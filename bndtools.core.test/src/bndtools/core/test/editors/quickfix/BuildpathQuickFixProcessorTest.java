package bndtools.core.test.editors.quickfix;

import static bndtools.core.test.utils.TaskUtils.synchronously;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jdt.core.compiler.IProblem.ImportNotFound;
import static org.eclipse.jdt.core.compiler.IProblem.IsClassPathCorrect;
import static org.eclipse.jdt.core.compiler.IProblem.UndefinedType;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Condition;
import org.assertj.core.api.ProxyableObjectArrayAssert;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.assertj.core.presentation.Representation;
import org.assertj.core.presentation.StandardRepresentation;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblem;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import aQute.bnd.deployer.repository.LocalIndexedRepo;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
import bndtools.central.Central;
import bndtools.core.test.utils.WorkbenchTest;

@ExtendWith(SoftAssertionsExtension.class)
@WorkbenchTest
class BuildpathQuickFixProcessorTest {
	static IPackageFragment						pack;
	static Class<? extends IQuickFixProcessor>	sutClass;

	SoftAssertions								softly;
	IQuickFixProcessor							sut;

	@SuppressWarnings("unchecked")
	static void initSUTClass() throws Exception {
		sutClass = (Class<? extends IQuickFixProcessor>) Central.class.getClassLoader()
			.loadClass("org.bndtools.core.editors.quickfix.BuildpathQuickFixProcessor");
		assertThat(IQuickFixProcessor.class).as("sutClass")
			.isAssignableFrom(sutClass);
	}

	@BeforeAll
	static void beforeAll() throws Exception {
		// Copy bundles from the parent project into our test workspace
		// LocalIndexedRepo
		LocalIndexedRepo localRepo = (LocalIndexedRepo) Central.getWorkspace()
			.getRepository("Local Index");
		Path bundleRoot = Paths.get(System.getProperty("bndtools.core.test.dir"))
			.resolve("./generated/");
		Files.walk(bundleRoot, 1)
			.filter(x -> x.getFileName()
				.toString()
				.endsWith("simple.jar"))
			.forEach(bundle -> {
				try {
					localRepo.put(IO.stream(bundle), null);
				} catch (Exception e) {
					throw Exceptions.duck(e);
				}
			});

		IProject project = ResourcesPlugin.getWorkspace()
			.getRoot()
			.getProject("test");
		if (!project.exists()) {
			synchronously("create project", project::create);
		}
		synchronously("open project", project::open);
		IJavaProject javaProject = JavaCore.create(project);

		IFolder sourceFolder = project.getFolder("src");
		if (!sourceFolder.exists()) {
			synchronously("create src", monitor -> sourceFolder.create(true, true, monitor));
		}

		IPackageFragmentRoot root = javaProject.getPackageFragmentRoot(sourceFolder);
		synchronously("createPackageFragment", monitor -> pack = root.createPackageFragment("test", false, monitor));

		initSUTClass();
	}

	@BeforeEach
	void before() throws Exception {
		sut = sutClass.newInstance();
	}

	private static final String	DEFAULT_CLASS_NAME	= "Test";
	private static final String	CLASS_HEADER		= "import java.util.List;\n" + "" + "class " + DEFAULT_CLASS_NAME
		+ " {";
	private static final String	CLASS_FOOTER		= " var};";

	private IJavaCompletionProposal[] proposalsForImport(String imp) {
		return proposalsFor(8, 0, "import " + imp + ";");
	}

	private IJavaCompletionProposal[] proposalsForLiteral(String type) {
		return proposalsFor(CLASS_HEADER.length(), 0,
			CLASS_HEADER + "Class<?> clazz = " + type + ".class;" + CLASS_FOOTER);
	}

	private IJavaCompletionProposal[] proposalsForUndefType(String type) {
		return proposalsFor(CLASS_HEADER.length(), 0, CLASS_HEADER + type + CLASS_FOOTER);
	}

	private IJavaCompletionProposal[] proposalsFor(int offset, int length, String source) {
		return proposalsFor(offset, length, DEFAULT_CLASS_NAME, source);
	}

	/**
	 * Workhorse method for driving the quick fix processor and getting the
	 * results.
	 *
	 * @param offset the location in the source of the start of the current
	 *            selection.
	 * @param length the length of the current selection (0 = no selection).
	 * @param className the name of the class, to use as the filename of the
	 *            compilation unit.
	 * @param source the source code of the class to compile.
	 * @return The completion proposals that were generated by the quick fix
	 *         processor.
	 */
	private IJavaCompletionProposal[] proposalsFor(int offset, int length, String className, String source) {

		try {
			ICompilationUnit icu = pack.createCompilationUnit(className + ".java", source, true, null);

			ASTParser parser = ASTParser.newParser(AST.JLS11);
			Map<String, String> options = JavaCore.getOptions();
			// Need to set 1.5 or higher for the "import static" syntax to work.
			// Need to set 1.8 or higher to test parameterized type usages.
			JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
			parser.setCompilerOptions(options);
			parser.setSource(icu);
			parser.setResolveBindings(true);
			parser.setBindingsRecovery(true);
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			parser.setUnitName(className + ".java");
			parser.setEnvironment(new String[] {}, new String[] {}, new String[] {}, true);
			CompilationUnit cu = (CompilationUnit) parser.createAST(null);

			// System.err.println("cu: " + cu);

			IProblemLocation[] locs = Stream.of(cu.getProblems())
				.map(ProblemLocation::new)
				.toArray(IProblemLocation[]::new);
			// System.err.println(
			// "Problems: " +
			// Stream.of(locs).map(IProblemLocation::toString).collect(Collectors.joining(",")));

			IInvocationContext context = new AssistContext(icu, offset, length);
			IJavaCompletionProposal[] proposals = sut.getCorrections(context, locs);

			// if (proposals != null) {
			// System.err.println("Proposals: " + Stream.of(proposals).map(x ->
			// {
			// return "toString: " + x.toString() + "\ndisplaystring: " +
			// x.getDisplayString();
			// }).collect(Collectors.joining("\n")));
			// } else {
			// System.err.println("No proposals");
			// }
			return proposals;
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	@ParameterizedTest
	@MethodSource("supportedProblemTypes")
	void hasCorrections_forSupportedProblemTypes_returnsTrue(IProblem problem, SoftAssertions softly) {
		softly.assertThat(sut.hasCorrections(null, problem.getID()))
			.as(problem.getMessage())
			.isTrue();
	}

	static final Set<Integer> SUPPORTED = Stream.of(ImportNotFound, UndefinedType, IsClassPathCorrect)
		.collect(Collectors.toSet());

	// This is just to give nice error feedback
	static class DummyProblem extends DefaultProblem {
		public DummyProblem(int id, String message) {
			super(null, message, id, null, 0, 0, 0, 0, 0);
		}
	}

	static Stream<IProblem> supportedProblemTypes() {
		return allProblemTypes().filter(p -> SUPPORTED.contains(p.getID()));
	}

	@ParameterizedTest
	@MethodSource("unsupportedProblemTypes")
	void hasCorrections_forUnsupportedProblemTypes_returnsFalse(IProblem problem, SoftAssertions softly) {
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
			.map(BuildpathQuickFixProcessorTest::getProblem);
	}

	@Test
	void withNoMatches_forUndefType_returnsNull() {
		assertThat(proposalsForUndefType("my.unknown.type.MyClass")).isNull();
	}

	@Test
	void withUnqualifiedType_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsBundleActivatorSuggestions(proposalsForUndefType("BundleActivator"));
	}

	@Test
	void withAnnotatedUnqualifiedType_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsBundleActivatorSuggestions(proposalsForUndefType("@NonNull BundleActivator"));
	}

	@Disabled("Not yet implemented")
	@Test
	void withSimpleInnerType_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsListenerInfoSuggestions(proposalsForUndefType("ListenerInfo"));
	}

	@Disabled("Not yet implemented")
	@Test
	void withPartlyQualifiedInnerType_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsListenerInfoSuggestions(proposalsForUndefType("ListenerHook.ListenerInfo"));
	}

	@Test
	void withFQType_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsBundleActivatorSuggestions(proposalsForUndefType("org.osgi.framework.BundleActivator"));
	}

	@Disabled("Not yet implemented")
	@Test
	void withFQNestedType_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsListenerInfoSuggestions(
			proposalsForUndefType("org.osgi.framework.hooks.service.ListenerHook.ListenerInfo"));
	}

	@Disabled("Not yet implemented")
	@Test
	void withAnnotatedFQNestedType_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsListenerInfoSuggestions(
			proposalsForUndefType("org.osgi.framework.hooks.service.ListenerHook.@NotNull ListenerInfo"));
	}

	@Disabled("Not yet implemented")
	@Test
	void withAnnotatedFQType_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsBundleActivatorSuggestions(
			proposalsForUndefType("org.osgi.framework.@NotNull BundleActivator"));
	}

	@Test
	void withAnnotatedSimpleType_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsBundleActivatorSuggestions(proposalsForUndefType("@NotNull BundleActivator"));
	}

	@Test
	void withFQType_andOneLevelPackage_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsMyClassSuggestions(proposalsForUndefType("simple.MyClass"));
	}

	@Disabled("Not yet implemented")
	@Test
	void withAnnotatedFQType_andOneLevelPackage_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsMyClassSuggestions(proposalsForUndefType("simple.@NotNull MyClass"));
	}

	@Test
	// Using a parameterized type as a qualifier forces Eclipse AST to generate
	// a QualifiedType
	void withGenericType_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsPromiseSuggestions(proposalsForUndefType("org.osgi.util.promise.Promise<String>"));
	}

	@Test
	void withParameterisedOuter_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsSimpleSuggestions(
			proposalsForUndefType("simple.pkg.MyParameterizedClass<String>.@NotNull MyInner"),
			"simple.pkg.MyParameterizedClass");
	}

	@Test
	void withUnannotatedFQArrayType_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsBundleActivatorSuggestions(proposalsForUndefType("org.osgi.framework.BundleActivator[]"));
	}

	@Disabled("Not yet implemented")
	@Test
	void withAnnotatedFQArrayType_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsBundleActivatorSuggestions(
			proposalsForUndefType("org.osgi.framework.@NotNull BundleActivator[]"));
	}

	@Disabled("Not yet implemented")
	@Test
	void withAnnotatedFQDoubleArrayType_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsBundleActivatorSuggestions(
			proposalsForUndefType("org.osgi.framework.@NotNull BundleActivator[][]"));
	}

	@Test
	void withSimpleUnannotatedParameter_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsBundleActivatorSuggestions(proposalsForUndefType("List<BundleActivator>"));
	}

	@Test
	void withSimpleAnnotatedParameter_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsBundleActivatorSuggestions(proposalsForUndefType("List<@NotNull BundleActivator>"));
	}

	@Test
	void withFQUnannotatedParameter_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsBundleActivatorSuggestions(proposalsForUndefType("List<org.osgi.framework.BundleActivator>"));
	}

	@Disabled("Not yet implemented")
	@Test
	void withFQAnnotatedParameter_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsBundleActivatorSuggestions(
			proposalsForUndefType("List<org.osgi.framework.@NotNull BundleActivator>"));
	}

	@Test
	void withFQWildcardBound_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsBundleActivatorSuggestions(
			proposalsForUndefType("List<? extends org.osgi.framework.BundleActivator>"));
	}

	@Disabled("Not yet implemented")
	@Test
	void withFQAnnotatedWildcardBound_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsBundleActivatorSuggestions(
			proposalsForUndefType("List<? extends org.osgi.framework.@NotNull BundleActivator>"));
	}

	@Disabled("Not yet implemented")
	@Test
	void withUnqualifiedNameType_returnsNull(SoftAssertions softly) {
		// If the type is a simple (non-qualified) name, it must refer to a type
		// and not a
		// package; therefore don't provide package import suggestions even if
		// we have a
		// package with the matching name.
		softly.assertThat(proposalsForUndefType("Simple"))
			.as("capitalized")
			.isNull();
		softly.assertThat(proposalsForUndefType("simple"))
			.as("uncapitalized")
			.isNull();
	}

	@Disabled("Not yet implemented")
	@Test
	void withParameterizedType_thatLooksLikePackage_returnsNull() {
		// Current implementation returns results for bundles containing package
		// org.osgi.framework, but it shouldn't because it should know that in
		// this
		// context "framework" must be a type and not a package.
		assertThat(proposalsForUndefType("org.osgi.framework<String>.BundleActivator")).isNull();
	}

	@Test
	void withNoMatches_returnsNull() {
		assertThat(proposalsForImport("my.unknown.package.*")).isNull();
	}

	@Test
	void withOnDemandImport_altPackage_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatProposals(proposalsForImport("simple.*")).hasSize(1)
			.haveExactly(1, suggestsBundle("bndtools.core.test.simple", "1.0.0", "simple"));
	}

	@Test
	void withClassImport_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsBundleActivatorSuggestions(proposalsForImport("org.osgi.framework.BundleActivator"));
	}

	@Disabled("Not yet implemented")
	@Test
	void withInnerClassImport_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsListenerInfoSuggestions(
			proposalsForImport("org.osgi.framework.hooks.service.ListenerHook.ListenerInfo"));
	}

	@Test
	void withOnDemandImport_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsFrameworkBundles(proposalsForImport("org.osgi.framework.*"), "org.osgi.framework");
	}

	@Test
	void withOnDemandStaticImport_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsBundleSuggestions(proposalsForImport("static org.osgi.framework.Bundle.*"));
	}

	@Disabled("Not yet implemented")
	@Test
	void withStaticImport_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsBundleSuggestions(proposalsForImport("static org.osgi.framework.Bundle.INSTALLED"));
	}

	@Test
	public void withSimplePackageName_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsSimpleSuggestions(proposalsForImport("simple.MyClass"), "simple.MyClass");
	}

	@Test
	public void withOnDemandSimplePackageName_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsSimpleSuggestions(proposalsForImport("simple.*"), "simple");
	}

	@Test
	void withClassLiteral_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsBundleSuggestions(proposalsForLiteral("Bundle"));
	}

	private void assertThatContainsPromiseSuggestions(IJavaCompletionProposal[] proposals) {
		if (proposals == null) {
			softly.fail("no proposals returned");
		} else {
			softly.assertThat(proposals)
				.withRepresentation(PROPOSAL)
				.hasSize(1)
				.haveExactly(1, suggestsBundle("org.osgi.util.promise", "1.1.1", "org.osgi.util.promise.Promise"));
		}
	}

	private ProxyableObjectArrayAssert<IJavaCompletionProposal> assertThatProposals(
		IJavaCompletionProposal[] proposals) {
		if (proposals == null) {
			throw new AssertionError("no proposals returned");
		}
		return softly.assertThat(proposals)
			.withRepresentation(PROPOSAL);
	}

	private void assertThatContainsSimpleSuggestions(IJavaCompletionProposal[] proposals, String fqn) {
		assertThatProposals(proposals).hasSize(1)
			.haveExactly(1, suggestsBundle("bndtools.core.test.simple", "1.0.0", fqn));
	}

	private void assertThatContainsMyClassSuggestions(IJavaCompletionProposal[] proposals) {
		assertThatContainsSimpleSuggestions(proposals, "simple.MyClass");
	}

	private void assertThatContainsBundleSuggestions(IJavaCompletionProposal[] proposals) {
		assertThatContainsFrameworkBundles(proposals, "org.osgi.framework.Bundle");
	}

	private void assertThatContainsBundleActivatorSuggestions(IJavaCompletionProposal[] proposals) {
		assertThatContainsFrameworkBundles(proposals, "org.osgi.framework.BundleActivator");
	}

	private void assertThatContainsListenerInfoSuggestions(IJavaCompletionProposal[] proposals) {
		assertThatContainsFrameworkBundles(proposals, "org.osgi.framework.hooks.service.ListenerHook.ListenerInfo");
	}

	// This gives us a more useful output in test failures.
	static Representation PROPOSAL = new StandardRepresentation() {
		@Override
		public String toStringOf(Object object) {
			if (object instanceof IJavaCompletionProposal) {
				return ((IJavaCompletionProposal) object).getDisplayString();
			}
			return super.toStringOf(object);
		}
	};

	private void assertThatContainsFrameworkBundles(IJavaCompletionProposal[] proposals, String fqName) {
		assertThatProposals(proposals).withRepresentation(PROPOSAL)
			.hasSize(2)
			.haveExactly(1, suggestsBundle("org.osgi.framework", "1.8.0", fqName))
			.haveExactly(1, suggestsBundle("org.osgi.framework", "1.9.0", fqName));
	}

	static class MatchDisplayString extends Condition<IJavaCompletionProposal> {
		private final Pattern p;

		public MatchDisplayString(String bundle, String version, String fqName, boolean test) {
			super(String.format("Suggestion to add '%s %s' to -%spath for class %s", bundle, version,
				test ? "test" : "build", fqName));
			String re = String.format("^Add \\Q%s\\E \\Q%s\\E to -\\Q%s\\Epath [(]found \\Q%s\\E[)]", bundle, version,
				test ? "test" : "build", fqName);
			p = Pattern.compile(re);
		}

		@Override
		public boolean matches(IJavaCompletionProposal value) {
			if (value == null || value.getDisplayString() == null) {
				return false;
			}
			final Matcher m = p.matcher(value.getDisplayString());
			return m.find();
		}
	}

	static Condition<IJavaCompletionProposal> suggestsBundle(String bundle, String version, String fqName) {
		return new MatchDisplayString(bundle, version, fqName, false);
	}

	static Condition<IJavaCompletionProposal> withRelevance(final int relevance) {
		return new Condition<IJavaCompletionProposal>("Suggestion has relevance " + relevance) {
			@Override
			public boolean matches(IJavaCompletionProposal value) {
				return value != null && value.getRelevance() == relevance;
			}
		};
	}
}
