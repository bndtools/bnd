package bndtools.core.test.editors.quickfix;

import static bndtools.core.test.utils.TaskUtils.log;
import static bndtools.core.test.utils.TaskUtils.synchronously;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jdt.core.compiler.IProblem.HierarchyHasProblems;
import static org.eclipse.jdt.core.compiler.IProblem.ImportNotFound;
import static org.eclipse.jdt.core.compiler.IProblem.IsClassPathCorrect;
import static org.eclipse.jdt.core.compiler.IProblem.ParameterMismatch;
import static org.eclipse.jdt.core.compiler.IProblem.TypeMismatch;
import static org.eclipse.jdt.core.compiler.IProblem.UndefinedField;
import static org.eclipse.jdt.core.compiler.IProblem.UndefinedMethod;
import static org.eclipse.jdt.core.compiler.IProblem.UndefinedName;
import static org.eclipse.jdt.core.compiler.IProblem.UndefinedType;
import static org.eclipse.jdt.core.compiler.IProblem.UnresolvedVariable;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
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
import org.assertj.core.api.junit.jupiter.SoftlyExtension;
import org.assertj.core.presentation.Representation;
import org.assertj.core.presentation.StandardRepresentation;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
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

//@Disabled("Currently disabled due to startup flakiness, see https://github.com/bndtools/bnd/issues/4253")
@ExtendWith(SoftlyExtension.class)
@WorkbenchTest
public class BuildpathQuickFixProcessorTest {
	static IPackageFragment						pack;
	static Class<? extends IQuickFixProcessor>	sutClass;

	// Will be injected by WorkbenchExtension
	static WorkspaceImporter					importer;

	// Will be injected by SoftlyExtension
	SoftAssertions								softly;
	IQuickFixProcessor							sut;

	IProblemLocation[]							locs;
	List<IProblem>								problems;
	IProblem									problem;

	IProject									eclipseProject;
	IJavaProject								javaProject;
	Project										bndProject;
	AssistContext								assistContext;
	String										source;

	// TODO: Here are some problem types we could potentially quick-fix that
	// aren't covered:
	// Discouraged access (can be fixed by adding a bundle that actually exports
	// the packages)

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

	static IResourceVisitor VISITOR = resource -> {
		System.err.println(resource.getFullPath());
		return true;
	};

	static void dumpWorkspace() throws CoreException {
		IWorkspace ws = ResourcesPlugin.getWorkspace();
		ws.getRoot()
			.accept(VISITOR);
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
				dumpWorkspace();
				System.err.println("Repositories: " + Central.getWorkspace()
					.getRepositories());
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
		locs = null;
		problems = null;
		problem = null;
		eclipseProject = ResourcesPlugin.getWorkspace()
			.getRoot()
			.getProject("test");
		if (eclipseProject == null) {
			dumpWorkspace();
			throw new IllegalStateException("Could not get project \"test\" from the current workspace");
		}
		bndProject = Central.getProject(eclipseProject);
		if (bndProject == null) {
			System.err.println("eclipseProject: " + eclipseProject.getName());
			dumpWorkspace();
			throw new IllegalStateException("Could not get bndProject from the current workspace");
		}
		synchronously("open project", eclipseProject::open);
		IJavaProject javaProject = JavaCore.create(eclipseProject);

		IFolder sourceFolder = eclipseProject.getFolder("src");
		if (!sourceFolder.exists()) {
			synchronously("create src", monitor -> sourceFolder.create(true, true, monitor));
		}

		// This folder is not strictly needed for the tests but it prevents a
		// build error from BndtoolsBuilder
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
		return proposalsFor(23, 0, "package test; import " + imp + ";");
	}

	private IJavaCompletionProposal[] proposalsForLiteral(String type) {
		String header = CLASS_HEADER + "Class<?> clazz = ";
		return proposalsFor(header.length(), 0, header + type + ".class;" + CLASS_FOOTER);
	}

	private IJavaCompletionProposal[] proposalsForAnnotation(String annotation) {
		String header = "package test; @";
		return proposalsFor(header.length(), 0, header + annotation + " public class Test {}");
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
	//

	String toString(IProblem p) {
		final int originalStart = p.getSourceStart();
		int startIndex = originalStart;
		while (startIndex > 0) {
			if (source.charAt(startIndex) == '\n') {
				startIndex++;
				break;
			}
			startIndex--;
		}
		StringBuilder retval = new StringBuilder();
		int endIndex = p.getSourceEnd() + 1;
		retval.append(source.substring(startIndex, originalStart))
			.append(">>>")
			.append(source.substring(originalStart, endIndex))
			.append("<<<");
		char c;
		while (endIndex < source.length() && (c = source.charAt(endIndex++)) != '\n') {
			retval.append(c);
		}
		retval.append('\n')
			.append(p)
			.append('\n')
			.append(new ProblemLocation(p));
		return retval.toString();
	}

	void problemsFor(int offset, int length, String className, String source) {
		try {
			problems = null;
			problem = null;
			locs = null;
			this.source = source;
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

			assistContext = new AssistContext(icu, offset, length);

			problems = Arrays.asList(cu.getProblems());

			// Note: IProblem instances seem to give more useful diagnostics
			// than the IProblemLocation instances
			// System.err.println("Unfiltered problems: ");
			// dumpProblems(Stream.of(cu.getProblems()));
			// When you hover in the GUI, if there are multiple overlapping
			// problems
			// at the point where you are hovering, only one of them is fetched
			// and
			// passed in to the quick fix processors to calculate proposals.
			// To emulate this, we filter the problems that contain the hover
			// region,
			// and then we find the smallest such problem. This seems to be what
			// Eclipse does, but also finding the smallest means that we can
			// directly
			// test the other problems if we want by specifying a hover point
			// somewhere else in the bigger region.
			problem = Stream.of(cu.getProblems())
				// Find problems that contain the "hover point"
				.filter(problem -> (problem.getSourceEnd() >= offset && problem.getSourceStart() <= (offset + length)))
				// Find the smallest
				.min((a, b) -> Integer.compare(a.getSourceEnd() - a.getSourceStart(),
					b.getSourceEnd() - b.getSourceStart()))
				.orElseThrow(() -> new IllegalArgumentException(
					"No problems found after filtering: " + Stream.of(cu.getProblems())
						.map(this::toString)
						.collect(Collectors.joining(","))));
			locs = new IProblemLocation[] {
				new ProblemLocation(problem)
			};
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * Workhorse method for driving the quick fix processor and getting the
	 * results. Works in two parts:
	 * <ol>
	 * <li>a call to problemsFor() to generate the problems and
	 * AssistContext;</li>
	 * <li>a call to proposalsFor() with the results from the previous call to
	 * generate the proposals.</li>
	 * </ol>
	 * If necessary (eg, the test wishes to verify that the right number/type of
	 * problems has been generated to test what we're trying to test) then the
	 * test can call problemsFor() and proposalsFor() directly.
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
		problemsFor(offset, length, className, source);
		return proposals();
	}

	IJavaCompletionProposal[] proposals() {
		try {
			IJavaCompletionProposal[] proposals = sut.getCorrections(assistContext, locs);

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
	void hasCorrections_forSupportedProblemTypes_returnsTrue(IProblem problem) {
		softly.assertThat(sut.hasCorrections(null, problem.getID()))
			.as(problem.getMessage())
			.isTrue();
	}

	static final Set<Integer> SUPPORTED = Stream
		.of(ImportNotFound, UndefinedType, IsClassPathCorrect, HierarchyHasProblems, ParameterMismatch, TypeMismatch,
			UndefinedField, UndefinedMethod, UndefinedName, UnresolvedVariable)
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
			.map(BuildpathQuickFixProcessorTest::getProblem);
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
	void withInconsistentHierarchy_forClassDefinition_thatImplementsAnInterfaceFromAnotherBundle_suggestsBundles() {
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
	void withInconsistentHierarchy_forClassDefinition_thatExtendsClassFromAnotherBundle_suggestsBundles() {
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
	void withInconsistentHierarchy_forInterfaceDefinition_thatExtendsAnInterfaceFromAnotherBundle_suggestsBundles() {
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
	void withMissingMethod_fromInterfaceFromAnotherBundle_suggestsBundles() {
		addBundlesToBuildpath("bndtools.core.test.fodder.simple");

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
		addBundlesToBuildpath("bndtools.core.test.fodder.simple");

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
		addBundlesToBuildpath("bndtools.core.test.fodder.simple");

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
		addBundlesToBuildpath("bndtools.core.test.fodder.simple");

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
		addBundlesToBuildpath("bndtools.core.test.fodder.simple");

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
		addBundlesToBuildpath("bndtools.core.test.fodder.simple");

		String header = "package test; class ";
		String source = header + DEFAULT_CLASS_NAME + " extends simple.pkg.ClassExtendingClassFromAnotherBundle {\n"
			+ "  void myMethod() {" + "    String s = super.bField;" + "  }" + "}";

		// UnknownField is on the method at [123,129]
		assertThatProposals(proposalsFor(123, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyForeignClass"));
	}

	// This is based on a real-world scenario I encountered while using CXF and
	// SOAP.
	@Test
	void withInconsistentHierarchy_forComplicatedGenericHierarchy_suggestsBundles() {
		addBundlesToBuildpath("bndtools.core.test.fodder.simple");

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
		addBundlesToBuildpath("bndtools.core.test.fodder.simple");

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
	void withFQClassLiteral_asAnnotationParameter_suggestsBundles() {
		addBundlesToBuildpath("bndtools.core.test.fodder.simple");

		String header = "package test; " + "import simple.annotation.MyTag;" + "@MyTag(";
		String source = header + "iface.bundle.MyInterface.class)" + "class " + DEFAULT_CLASS_NAME + "{" + "}";

		assertThatProposals(proposalsFor(header.length() + 2, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyInterface"));
	}

	// This should in theory work. However, when stepping through it carefully
	// in Eclipse, I've seen that TypeBinding.getInterfaces() returns an empty
	// array for the type binding of the type literal. This is in spite of the
	// fact that inspecting the binding's fields in the debugger shows that the
	// binding internally has a reference to the (unresolved) superinterface.
	// Pretty sure that this is an Eclipse bug - getInterfaces() should return
	// the type bindings of the directly implemented interfaces even if they are
	// not fully resolved.
	@Disabled("Disabled due to Eclipse bug")
	@Test
	void withFQClassLiteral_inheritingFromInterfaceFromAnotherBundle_asAnnotationParameter_suggestsBundles() {
		addBundlesToBuildpath("bndtools.core.test.fodder.simple");

		String header = "package test; " + "import simple.annotation.MyTag;" + "@MyTag(";
		String source = header + "simple.pkg.ClassWithInterfaceExtendingMyInterface.class)" + "class "
			+ DEFAULT_CLASS_NAME + "{" + "}";

		assertThatProposals(proposalsFor(header.length() + 2, 0, source)).haveExactly(1,
			suggestsBundle("bndtools.core.test.fodder.iface", "1.0.0", "iface.bundle.MyInterface"));
	}

	@Test
	void withUnqualifiedClassLiteral_extendingInterfaceFromAnotherBundle_asAnnotationParameter_suggestsBundles() {
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

		assertThatProposals(proposalsFor(start, 3, source)).isEmpty();
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
		addBundlesToBuildpath("junit-jupiter-api");

		String source = "package test; " + "import org.junit.jupiter.api.extension.ExtendWith; " + "@ExtendWith(";
		int start = source.length();
		source += "SoftAssertionsExtension.cl";
		int middleOfClass = source.length();
		source += "ass)\n" + "class " + DEFAULT_CLASS_NAME + "{" + "}";

		assertThatProposals(proposalsFor(middleOfClass, 0, source)).as("middle of .class")
			.haveExactly(1,
				suggestsBundle("assertj-core", "3.16.1", "org.assertj.core.api.junit.jupiter.SoftAssertionsExtension"));
	}

	// It also does another sanity check for the start
	// of the literal where the two problems overlap to make sure it's not
	// generating duplicate suggestions.
	@Test
	void removesDuplicateProposals() {
		addBundlesToBuildpath("junit-jupiter-api");

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
		String desc = toString(problem);
		if (proposals == null) {
			return softly.assertThat(EMPTY_LIST)
				.as(desc);
		}
		return softly.assertThat(proposals)
			.as(desc)
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

	private void assertThatContainsListenerHookSuggestions(IJavaCompletionProposal[] proposals) {
		assertThatContainsFrameworkBundles(proposals, "org.osgi.framework.hooks.service.ListenerHook");
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
