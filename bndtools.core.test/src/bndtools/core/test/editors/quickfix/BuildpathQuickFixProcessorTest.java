package bndtools.core.test.editors.quickfix;

import static bndtools.core.test.utils.TaskUtils.log;
import static bndtools.core.test.utils.TaskUtils.synchronously;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jdt.core.compiler.IProblem.HierarchyHasProblems;
import static org.eclipse.jdt.core.compiler.IProblem.ImportNotFound;
import static org.eclipse.jdt.core.compiler.IProblem.IsClassPathCorrect;
import static org.eclipse.jdt.core.compiler.IProblem.ParameterMismatch;
import static org.eclipse.jdt.core.compiler.IProblem.TypeMismatch;
import static org.eclipse.jdt.core.compiler.IProblem.UndefinedType;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import aQute.bnd.build.Project;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.deployer.repository.LocalIndexedRepo;
import aQute.bnd.osgi.Constants;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
import bndtools.central.Central;
import bndtools.core.test.utils.WorkbenchTest;
import bndtools.core.test.utils.WorkspaceImporter;

@Disabled("Currently disabled due to startup flakiness, see https://github.com/bndtools/bnd/issues/4253")
@ExtendWith(SoftAssertionsExtension.class)
@WorkbenchTest
public class BuildpathQuickFixProcessorTest {
	static IPackageFragment						pack;
	static Class<? extends IQuickFixProcessor>	sutClass;

	// Will be injected by WorkbenchExtension
	static WorkspaceImporter					importer;

	SoftAssertions								softly;
	IQuickFixProcessor							sut;

	IProject									eclipseProject;
	IJavaProject								javaProject;
	Project										bndProject;

	// TODO: Here are some problem types we could potentially quick-fix that
	// aren't covered:
	// Discouraged access (can be fixed by adding a bundle that actually exports
	// the packages)
	// Missing method (sometimes caused when the hierarchy is incomplete,
	// similar to HierarchyHasProblems)

	@SuppressWarnings("unchecked")
	static void initSUTClass() throws Exception {
		BundleContext bc = FrameworkUtil.getBundle(BuildpathQuickFixProcessorTest.class)
			.getBundleContext();
		Bundle but = Stream.of(bc.getBundles())
			.filter(bundle -> bundle.getSymbolicName()
				.equals("bndtools.core.services"))
			.findAny()
			.orElseThrow(() -> new IllegalArgumentException("Couldn't find bndtools.core.services"));
		sutClass = (Class<? extends IQuickFixProcessor>) but
			.loadClass("org.bndtools.core.editors.quickfix.BuildpathQuickFixProcessor");
		assertThat(IQuickFixProcessor.class).as("sutClass")
			.isAssignableFrom(sutClass);
	}

	@BeforeAll
	static void beforeAll() throws Exception {
		// Get a handle on the repo. I have seen this come back null on occasion
		// so spin until we get it.
		LocalIndexedRepo localRepo = (LocalIndexedRepo) Central.getWorkspace()
			.getRepository("Local Index");
		int count = 0;
		while (localRepo == null) {
			Thread.sleep(100);
			localRepo = (LocalIndexedRepo) Central.getWorkspace()
				.getRepository("Local Index");
			if (count++ > 100) {
				throw new IllegalStateException("Timed out waiting for Local Index");
			}
		}
		// Copy bundles from the parent project into our test workspace
		// LocalIndexedRepo
		final LocalIndexedRepo theRepo = localRepo;
		Path bundleRoot = Paths.get(System.getProperty("bndtools.core.test.dir"))
			.resolve("./generated/");
		Files.walk(bundleRoot, 1)
			.filter(x -> x.getFileName()
				.toString()
				.contains(".fodder."))
			.forEach(bundle -> {
				try {
					theRepo.put(IO.stream(bundle), null);
				} catch (Exception e) {
					throw Exceptions.duck(e);
				}
			});
		initSUTClass();
	}

	@BeforeEach
	void beforeEach() throws Exception {
		eclipseProject = ResourcesPlugin.getWorkspace()
			.getRoot()
			.getProject("test");
		bndProject = Central.getProject(eclipseProject);
		synchronously("open project", eclipseProject::open);
		IJavaProject javaProject = JavaCore.create(eclipseProject);

		IFolder sourceFolder = eclipseProject.getFolder("src");
		if (!sourceFolder.exists()) {
			synchronously("create src", monitor -> sourceFolder.create(true, true, monitor));
		}

		// This folder is not strictly needed for the tests but it prevents a
		// build
		// error from BndtoolsBuilder
		IFolder testFolder = eclipseProject.getFolder("test");
		if (!testFolder.exists()) {
			synchronously("create test", monitor -> testFolder.create(true, true, monitor));
		}

		IPackageFragmentRoot root = javaProject.getPackageFragmentRoot(sourceFolder);
		synchronously("createPackageFragment", monitor -> pack = root.createPackageFragment("test", false, monitor));
		clearBuildpath();
	}

	// Listener used for synchronizing on classpath changes
	class ClasspathChangedListener implements IElementChangedListener {

		final CountDownLatch flag;

		ClasspathChangedListener(CountDownLatch flag) {
			this.flag = flag;
		}

		@Override
		public void elementChanged(ElementChangedEvent event) {
			visit(event.getDelta());
		}

		private void visit(IJavaElementDelta delta) {
			IJavaElement el = delta.getElement();
			switch (el.getElementType()) {
				case IJavaElement.JAVA_MODEL :
					visitChildren(delta);
					break;
				case IJavaElement.JAVA_PROJECT :
					if (isClasspathChanged(delta.getFlags())) {
						flag.countDown();
					}
					break;
				default :
					break;
			}
		}

		private boolean isClasspathChanged(int flags) {
			return 0 != (flags
				& (IJavaElementDelta.F_CLASSPATH_CHANGED | IJavaElementDelta.F_RESOLVED_CLASSPATH_CHANGED));
		}

		public void visitChildren(IJavaElementDelta delta) {
			for (IJavaElementDelta c : delta.getAffectedChildren()) {
				visit(c);
			}
		}
	}

	void waitForClasspathUpdate() throws Exception {
		waitForClasspathUpdate(null);
	}

	void waitForClasspathUpdate(String context) throws Exception {
		String prefix = context == null ? "" : context + ": ";
		CountDownLatch flag = new CountDownLatch(1);
		IElementChangedListener listener = new ClasspathChangedListener(flag);
		JavaCore.addElementChangedListener(listener, ElementChangedEvent.POST_CHANGE);
		try {
			// This call to forceRefresh() here is unnecessary if you are adding
			// to
			// the buildpath, but it seems to be necessary when clearing the
			// build
			// path. Without it, the Project object seems to hang around without
			// updating and doesn't update its buildpath setting.
			bndProject.forceRefresh();
			Central.refreshFile(bndProject.getPropertiesFile());
			log(prefix + "waiting for classpath to update");
			if (flag.await(10000, TimeUnit.MILLISECONDS)) {
				log(prefix + "done waiting for classpath to update");
			} else {
				log(prefix + "WARNING: timed out waiting for classpath to update");
			}
		} finally {
			JavaCore.removeElementChangedListener(listener);
		}
	}

	void clearBuildpath() {
		log("clearing buildpath");
		try {
			BndEditModel model = new BndEditModel(bndProject);
			model.load();
			List<VersionedClause> buildPath = model.getBuildPath();
			if (buildPath != null && !buildPath.isEmpty()) {
				model.setBuildPath(Collections.emptyList());
				model.saveChanges();
				waitForClasspathUpdate("clearBuildpath()");
			} else {
				log("buildpath was not set; not trying to clear it");
			}
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	void addBundlesToBuildpath(String... bundleNames) {
		try {
			BndEditModel model = new BndEditModel(bndProject);
			model.load();

			for (String bundleName : bundleNames) {
				model.addPath(new VersionedClause(bundleName, null), Constants.BUILDPATH);
			}
			model.saveChanges();
			waitForClasspathUpdate("addBundleToBuildpath");
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	@BeforeEach
	void before() throws Exception {
		sut = sutClass.newInstance();
	}

	private static final String	DEFAULT_CLASS_NAME	= "Test";
	private static final String	CLASS_HEADER		= "package test; import java.util.List;\n" + "" + "class "
		+ DEFAULT_CLASS_NAME + " {";
	private static final String	CLASS_FOOTER		= " var};";

	private IJavaCompletionProposal[] proposalsForStaticImport(String imp) {
		return proposalsFor(29, 0, "package test; import static " + imp + ";");
	}

	private IJavaCompletionProposal[] proposalsForImport(String imp) {
		return proposalsFor(22, 0, "package test; import " + imp + ";");
	}

	private IJavaCompletionProposal[] proposalsForLiteral(String type) {
		String header = CLASS_HEADER + "Class<?> clazz = ";
		return proposalsFor(header.length(), 0, header + type + ".class;" + CLASS_FOOTER);
	}

	private IJavaCompletionProposal[] proposalsForUndefType(String type) {
		return proposalsForUndefType(0, type);
	}

	private IJavaCompletionProposal[] proposalsForUndefType(int offset, String type) {
		return proposalsFor(CLASS_HEADER.length() + offset, 0, CLASS_HEADER + type + CLASS_FOOTER);
	}

	private IJavaCompletionProposal[] proposalsFor(int offset, int length, String source) {
		return proposalsFor(offset, length, DEFAULT_CLASS_NAME, source);
	}

	// void dumpProblems(Stream<? extends IProblem> problems) {
	// problems.map(problem -> problem + "[" + problem.getSourceStart() + "," +
	// problem.getSourceEnd() + "]")
	// .forEach(System.err::println);
	// }

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
			// First create our AST
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

			// Note: IProblem instances seem to give more useful diagnostics
			// than the IProblemLocation instances
			// System.err.println("Unfiltered problems: ");
			// dumpProblems(Stream.of(cu.getProblems()));
			// Filter out the problems that don't fall within the current editor
			// context.
			// This is to properly emulate what the GUI will do when hovering
			// over a particular point or selecting a particular point.
			List<IProblem> filtered = Stream.of(cu.getProblems())
				.filter(problem -> {
					return problem.getSourceEnd() >= offset && problem.getSourceStart() <= (offset + length);
				})
				.collect(Collectors.toList());
			// System.err.println("Filtered problems:");
			// dumpProblems(filtered.stream());
			IProblemLocation[] locs = filtered.stream()
				.map(ProblemLocation::new)
				.toArray(IProblemLocation[]::new);

			// System.err.println("Problems: " + Stream.of(locs)
			// .map(IProblemLocation::toString)
			// .collect(Collectors.joining(",")));

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

	static final Set<Integer> SUPPORTED = Stream
		.of(ImportNotFound, UndefinedType, IsClassPathCorrect, HierarchyHasProblems, ParameterMismatch, TypeMismatch)
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
		assertThatContainsBundleActivatorSuggestions(proposalsForUndefType(10, "@NonNull BundleActivator"));
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
		assertThatContainsBundleActivatorSuggestions(proposalsForUndefType(10, "@NotNull BundleActivator"));
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
		assertThatContainsBundleActivatorSuggestions(proposalsForUndefType(7, "List<BundleActivator>"));
	}

	@Test
	void withSimpleAnnotatedParameter_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsBundleActivatorSuggestions(proposalsForUndefType(15, "List<@NotNull BundleActivator>"));
	}

	@Test
	void withFQUnannotatedParameter_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsBundleActivatorSuggestions(
			proposalsForUndefType(6, "List<org.osgi.framework.BundleActivator>"));
	}

	@Disabled("Not yet implemented")
	@Test
	void withFQAnnotatedParameter_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsBundleActivatorSuggestions(
			proposalsForUndefType(6, "List<org.osgi.framework.@NotNull BundleActivator>"));
	}

	@Test
	void withFQWildcardBound_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsBundleActivatorSuggestions(
			proposalsForUndefType(16, "List<? extends org.osgi.framework.BundleActivator>"));
	}

	@Disabled("Not yet implemented")
	@Test
	void withFQAnnotatedWildcardBound_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsBundleActivatorSuggestions(
			proposalsForUndefType(16, "List<? extends org.osgi.framework.@NotNull BundleActivator>"));
	}

	@Disabled("Not yet implemented")
	@Test
	void withUnqualifiedNameType_returnsNull(SoftAssertions softly) {
		// If the type is a simple (non-qualified) name, it must refer to a type
		// and not
		// a package; therefore don't provide package import suggestions even if
		// we have a package with the matching name.
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
	void withOnDemandImport_altPackage_suggestsBundles(SoftAssertions softly) throws IOException {
		this.softly = softly;
		assertThatProposals(proposalsForImport("simple.*")).hasSize(1)
			.haveExactly(1, suggestsBundle("bndtools.core.test.fodder.simple", "1.0.0", "simple"));
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
		assertThatContainsBundleSuggestions(proposalsForStaticImport("org.osgi.framework.Bundle.*"));
	}

	@Disabled("Not yet implemented")
	@Test
	void withStaticImport_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsBundleSuggestions(proposalsForStaticImport("org.osgi.framework.Bundle.INSTALLED"));
	}

	@Test
	void withSimplePackageName_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsSimpleSuggestions(proposalsForImport("simple.MyClass"), "simple.MyClass");
	}

	@Test
	void withOnDemandSimplePackageName_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsSimpleSuggestions(proposalsForImport("simple.*"), "simple");
	}

	@Test
	void withClassLiteral_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;
		assertThatContainsBundleSuggestions(proposalsForLiteral("Bundle"));
	}

	@Test
	void withInconsistentHierarchy_forClassDefinition_thatImplementsAnInterfaceFromAnotherBundle_suggestsBundles(
		SoftAssertions softly) {
		this.softly = softly;

		addBundlesToBuildpath("bndtools.core.test.fodder.simple");

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
	void withInconsistentHierarchy_forClassDefinition_thatExtendsClassFromAnotherBundle_suggestsBundles(
		SoftAssertions softly) {
		this.softly = softly;

		addBundlesToBuildpath("bndtools.core.test.fodder.simple");

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
	void withInconsistentHierarchy_forInterfaceDefinition_thatExtendsAnInterfaceFromAnotherBundle_suggestsBundles(
		SoftAssertions softly) {
		this.softly = softly;

		addBundlesToBuildpath("bndtools.core.test.fodder.simple");

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
	void withInconsistentHierarchy_forClassUse_thatExtendsAnInterfaceFromAnotherBundle_suggestsBundles(
		SoftAssertions softly) {
		this.softly = softly;

		addBundlesToBuildpath("bndtools.core.test.fodder.simple");

		String header = "package test; class ";
		String source = header + DEFAULT_CLASS_NAME + "{\n"
			+ "  simple.pkg.InterfaceExtendingInterfaceFromAnotherBundle var;\n" + "  void myMethod() {"
			+ "    var.myInterfaceMethod();" + "  }" + "}";

		// IsClassPathCorrect occurs at [112, 134]
		assertThatProposals(proposalsFor(112, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyInterface"));
		// UnknownMethod is on the method at [116,132], which means that it's
		// redundant as the
		// IsClassPathCorrect problem covers it completely.
		assertThatProposals(proposalsFor(117, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyInterface"));
	}

	@Test
	void withInconsistentHierarchy_forClassUse_thatExtendsAClassFromAnotherBundle_suggestsBundles(
		SoftAssertions softly) {
		this.softly = softly;

		addBundlesToBuildpath("bndtools.core.test.fodder.simple");

		String header = "package test; class ";
		String source = header + DEFAULT_CLASS_NAME + "{\n" + "  simple.pkg.ClassExtendingClassFromAnotherBundle var;\n"
			+ "  void myMethod() {" + "    var.bMethod();" + "  }" + "}";

		// IsClassPathCorrect occurs at [112, 134]
		assertThatProposals(proposalsFor(112, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyInterface"));
		// UnknownMethod is on the method at [116,132], which means that it's
		// redundant as the
		// IsClassPathCorrect problem covers it completely.
		assertThatProposals(proposalsFor(117, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyInterface"));
	}

	// This is based on a real-world scenario I encountered while using CXF and
	// SOAP.
	@Test
	void withInconsistentHierarchy_forComplicatedGenericHierarchy_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;

		addBundlesToBuildpath("bndtools.core.test.fodder.simple");

		String header = "package test; import java.util.List;" + "import simple.pkg.MyParameterizedClass;"
			+ "import simple.MyClass;" + "import simple.pkg.ClassExtendingAbstractExtendingMyParameterizedClass;"
			+ "class ";
		String source = header + DEFAULT_CLASS_NAME + "{\n" + "  List<MyParameterizedClass<? extends MyClass>> var;\n"
			+ "  void myMethod() {" + "    var.add(new ClassExtendingAbstractExtendingMyParameterizedClass());" + "  }"
			+ "}";

		// ParameterMismatch [259, 261]
		assertThatProposals(proposalsFor(259, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyInterface"));
	}

	@Test
	void withFQClassLiteral_asAnnotationParameter_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;

		addBundlesToBuildpath("bndtools.core.test.fodder.simple");

		String header = "package test; " + "import simple.annotation.MyTag;" + "@MyTag(";
		String source = header + "iface.bundle.MyInterface.class)" + "class " + DEFAULT_CLASS_NAME + "{" + "}";

		assertThatProposals(proposalsFor(header.length() + 2, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyInterface"));
	}

	@Test
	void withUnqualifiedClassLiteral_asAnnotationParameter_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;

		addBundlesToBuildpath("bndtools.core.test.fodder.simple");

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

	@Test
	void withUnqualifiedClassLiteral_forUnimportedType_asAnnotationParameter_suggestsBundles(SoftAssertions softly) {
		this.softly = softly;

		String header = "package test; " + "import simple.annotation.MyTag; " + "@MyTag(";
		String source = header + "MyInterface.class)" + "class " + DEFAULT_CLASS_NAME + "{" + "}";

		assertThatProposals(proposalsFor(header.length() + 2, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyInterface"));
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

	final static IJavaCompletionProposal[] EMPTY_LIST = new IJavaCompletionProposal[0];

	private ProxyableObjectArrayAssert<IJavaCompletionProposal> assertThatProposals(
		IJavaCompletionProposal[] proposals) {
		if (proposals == null) {
			return softly.assertThat(EMPTY_LIST);
		}
		return softly.assertThat(proposals)
			.withRepresentation(PROPOSAL);
	}

	private void assertThatContainsSimpleSuggestions(IJavaCompletionProposal[] proposals, String fqn) {
		assertThatProposals(proposals).hasSize(1)
			.haveExactly(1, suggestsBundle("bndtools.core.test.fodder.simple", "1.0.0", fqn));
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

	// This gives us a more complete display when tests fail
	static final Representation PROPOSAL = new StandardRepresentation() {
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
