package bndtools.core.test.editors.quickfix;

import static bndtools.core.test.utils.TaskUtils.log;
import static bndtools.core.test.utils.TaskUtils.synchronously;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jdt.core.compiler.IProblem.HierarchyHasProblems;
import static org.eclipse.jdt.core.compiler.IProblem.ImportNotFound;
import static org.eclipse.jdt.core.compiler.IProblem.IsClassPathCorrect;
import static org.eclipse.jdt.core.compiler.IProblem.ParameterMismatch;
import static org.eclipse.jdt.core.compiler.IProblem.TypeArgumentMismatch;
import static org.eclipse.jdt.core.compiler.IProblem.TypeMismatch;
import static org.eclipse.jdt.core.compiler.IProblem.UndefinedConstructor;
import static org.eclipse.jdt.core.compiler.IProblem.UndefinedField;
import static org.eclipse.jdt.core.compiler.IProblem.UndefinedMethod;
import static org.eclipse.jdt.core.compiler.IProblem.UndefinedName;
import static org.eclipse.jdt.core.compiler.IProblem.UndefinedType;
import static org.eclipse.jdt.core.compiler.IProblem.UnresolvedVariable;

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
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.assertj.core.presentation.Representation;
import org.assertj.core.presentation.StandardRepresentation;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.Job;
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
import org.junit.jupiter.api.extension.ExtendWith;
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
import aQute.lib.unmodifiable.Sets;
import bndtools.central.Central;
import bndtools.core.test.utils.TaskUtils;
import bndtools.core.test.utils.WorkbenchTest;

@ExtendWith(SoftAssertionsExtension.class)
@WorkbenchTest
abstract class AbstractBuildpathQuickFixProcessorTest {

	static IPackageFragment						pack;
	static Class<? extends IQuickFixProcessor>	sutClass;
	// Injected by WorkbenchExtension
	// static WorkspaceImporter importer;
	static IProject								eclipseProject;
	static Project								bndProject;
	// Injected by SoftAssertionsExtension
	@InjectSoftAssertions
	protected SoftAssertions					softly;
	protected IQuickFixProcessor				sut;
	protected IProblemLocation[]				locs;
	protected List<IProblem>					problems;
	IProblem									problem;
	AssistContext								assistContext;
	protected String							source;

	@SuppressWarnings("unchecked")
	static void initSUTClass() throws Exception {
		BundleContext bc = FrameworkUtil.getBundle(AbstractBuildpathQuickFixProcessorTest.class)
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
	static void beforeAllBase() throws Exception {
		// Get a handle on the repo. I have seen this come back null on occasion
		// but not exactly sure why and spinning doesn't seem to fix it; ref
		// #4253
		final LocalIndexedRepo localRepo = (LocalIndexedRepo) Central.getWorkspace()
			.getRepository("Local Index");

		if (localRepo == null) {
			log("Central.getWorkspace(): " + Central.getWorkspace()
				.getBase());
			TaskUtils.dumpWorkspace();
			throw new IllegalStateException("Could not find Local Index");
		}

		Path bundleRoot = Paths.get(System.getProperty("bndtools.core.test.dir"))
			.resolve("./generated/");
		Files.walk(bundleRoot, 1)
			.filter(x -> x.getFileName()
				.toString()
				.contains(".fodder."))
			.forEach(bundle -> {
				try {
					localRepo.put(IO.stream(bundle), null);
				} catch (Exception e) {
					throw Exceptions.duck(e);
				}
			});
		initSUTClass();

		eclipseProject = ResourcesPlugin.getWorkspace()
			.getRoot()
			.getProject("test");
		if (eclipseProject == null) {
			TaskUtils.dumpWorkspace();
			throw new IllegalStateException("Could not get project \"test\" from the current workspace");
		}
		bndProject = Central.getProject(eclipseProject);
		if (bndProject == null) {
			System.err.println("eclipseProject: " + eclipseProject.getName());
			TaskUtils.dumpWorkspace();
			throw new IllegalStateException("Could not get bndProject from the current workspace");
		}
		synchronously("open project", eclipseProject::open);
		IJavaProject javaProject = JavaCore.create(eclipseProject);

		IFolder sourceFolder = eclipseProject.getFolder("src");
		if (!sourceFolder.exists()) {
			synchronously("create src", monitor -> sourceFolder.create(true, true, monitor));
		}

		IPackageFragmentRoot root = javaProject.getPackageFragmentRoot(sourceFolder);
		synchronously("createPackageFragment", monitor -> pack = root.createPackageFragment("test", false, monitor));

		// Wait for build to finish - attempted fix for #4553.
		//
		// Note that there is a possible race condition here as I'm not sure if
		// it's guaranteed that the build would have started yet; if we start
		// trying to wait for it before it actually starts it might return
		// straight away instead of waiting for the finish. Need to think of a
		// way to make sure we don't start waiting for it to finish until we
		// know that it's already started.
		Job.getJobManager()
			.join(ResourcesPlugin.FAMILY_MANUAL_BUILD, null);
		Job.getJobManager()
			.join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
	}

	protected static final String DEFAULT_CLASS_NAME = "Test";

	@BeforeEach
	void beforeEach() throws Exception {
		locs = null;
		problems = null;
		problem = null;
	}

	static void waitForClasspathUpdate() throws Exception {
		waitForClasspathUpdate(null);
	}

	static void waitForClasspathUpdate(String context) throws Exception {
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

	static void clearBuildpath() {
		log("clearing buildpath");
		try {
			BndEditModel model = new BndEditModel(bndProject);
			model.load();
			List<VersionedClause> buildPath = model.getBuildPath();
			if (buildPath != null && !buildPath.isEmpty()) {
				model.setBuildPath(Collections.emptyList());
				model.saveChanges();
				Central.refresh(bndProject);
				waitForClasspathUpdate("clearBuildpath()");
			} else {
				log("buildpath was not set; not trying to clear it");
			}
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	static void addBundlesToBuildpath(String... bundleNames) {
		try {
			BndEditModel model = new BndEditModel(bndProject);
			model.load();

			for (String bundleName : bundleNames) {
				model.addPath(new VersionedClause(bundleName, null), Constants.BUILDPATH);
			}
			model.saveChanges();
			Central.refresh(bndProject);
			waitForClasspathUpdate("addBundleToBuildpath");
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	// Listener used for synchronizing on classpath changes
	static class ClasspathChangedListener implements IElementChangedListener {

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

	// void dumpProblems(Stream<? extends IProblem> problems) {
	// problems.map(problem -> problem + "[" + problem.getSourceStart() + "," +
	// problem.getSourceEnd() + "]")
	// .forEach(System.err::println);
	// }
	//

	@BeforeEach
	void before() throws Exception {
		sut = sutClass.newInstance();
	}

	private static final String			CLASS_HEADER	= "package test; import java.util.List;\n" + "" + "class "
		+ DEFAULT_CLASS_NAME + " {";
	private static final String			CLASS_FOOTER	= " var};";
	protected static final Set<Integer>	SUPPORTED		= Sets.of(ImportNotFound, UndefinedType, IsClassPathCorrect,
		HierarchyHasProblems, ParameterMismatch, TypeMismatch, UndefinedConstructor, UndefinedField, UndefinedMethod,
		UndefinedName, UnresolvedVariable, TypeArgumentMismatch);

	protected IJavaCompletionProposal[] proposalsForStaticImport(String imp) {
		return proposalsFor(29, 0, "package test; import static " + imp + ";");
	}

	protected IJavaCompletionProposal[] proposalsForImport(String imp) {
		return proposalsFor(23, 0, "package test; import " + imp + ";");
	}

	protected IJavaCompletionProposal[] proposalsForLiteral(String type) {
		String header = CLASS_HEADER + "Class<?> clazz = ";
		return proposalsFor(header.length(), 0, header + type + ".class;" + CLASS_FOOTER);
	}

	protected IJavaCompletionProposal[] proposalsForAnnotation(String annotation) {
		String header = "package test; @";
		return proposalsFor(header.length(), 0, header + annotation + " public class Test {}");
	}

	protected IJavaCompletionProposal[] proposalsForUndefType(String type) {
		return proposalsForUndefType(0, type);
	}

	protected IJavaCompletionProposal[] proposalsForUndefType(int offset, String type) {
		return proposalsFor(CLASS_HEADER.length() + offset, 0, CLASS_HEADER + type + CLASS_FOOTER);
	}

	protected IJavaCompletionProposal[] proposalsFor(int offset, int length, String source) {
		return proposalsFor(offset, length, DEFAULT_CLASS_NAME, source);
	}

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

	protected void problemsFor(int offset, int length, String className, String source) {
		try {
			problems = null;
			problem = null;
			locs = null;
			this.source = source;

			// First create our AST
			ICompilationUnit icu = pack.createCompilationUnit(className + ".java", source, true, null);

			ASTParser parser = ASTParser.newParser(AST.JLS14);
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
			// problems at the point where you are hovering, only one of them is
			// fetched and passed in to the quick fix processors to calculate
			// proposals. To emulate this, we filter the problems that contain
			// the hover region, and then we find the smallest such problem.
			// This seems to be what Eclipse does, but also finding the smallest
			// means that we can directly test the other problems if we want by
			// specifying a hover point somewhere else in the bigger region.
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

	protected IJavaCompletionProposal[] proposals() {
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

	// This is just to give nice error feedback
	protected static class DummyProblem extends DefaultProblem {
		public DummyProblem(int id, String message) {
			super(null, message, id, null, 0, 0, 0, 0, 0);
		}
	}

	static final IJavaCompletionProposal[] EMPTY_LIST = new IJavaCompletionProposal[0];

	protected void assertThatContainsPromiseSuggestions(IJavaCompletionProposal[] proposals) {
		if (proposals == null) {
			softly.fail("no proposals returned");
		} else {
			softly.assertThat(proposals)
				.withRepresentation(PROPOSAL)
				.hasSize(1)
				.haveExactly(1, suggestsBundle("org.osgi.util.promise", "1.1.1", "org.osgi.util.promise.Promise"));
		}
	}

	static final Representation PROPOSAL = new StandardRepresentation() {
		@Override
		public String toStringOf(Object object) {
			if (object instanceof IJavaCompletionProposal) {
				return ((IJavaCompletionProposal) object).getDisplayString();
			}
			return super.toStringOf(object);
		}
	};

	protected ProxyableObjectArrayAssert<IJavaCompletionProposal> assertThatProposals(
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

	protected void assertThatContainsSimpleSuggestions(IJavaCompletionProposal[] proposals, String fqn) {
		assertThatProposals(proposals).hasSize(1)
			.haveExactly(1, suggestsBundle("bndtools.core.test.fodder.simple", "1.0.0", fqn));
	}

	protected void assertThatContainsMyClassSuggestions(IJavaCompletionProposal[] proposals) {
		assertThatContainsSimpleSuggestions(proposals, "simple.MyClass");
	}

	protected void assertThatContainsBundleSuggestions(IJavaCompletionProposal[] proposals) {
		assertThatContainsFrameworkBundles(proposals, "org.osgi.framework.Bundle");
	}

	protected void assertThatContainsBundleActivatorSuggestions(IJavaCompletionProposal[] proposals) {
		assertThatContainsFrameworkBundles(proposals, "org.osgi.framework.BundleActivator");
	}

	protected void assertThatContainsListenerInfoSuggestions(IJavaCompletionProposal[] proposals) {
		assertThatContainsFrameworkBundles(proposals, "org.osgi.framework.hooks.service.ListenerHook.ListenerInfo");
	}

	protected void assertThatContainsListenerHookSuggestions(IJavaCompletionProposal[] proposals) {
		assertThatContainsFrameworkBundles(proposals, "org.osgi.framework.hooks.service.ListenerHook");
	}

	protected static class MatchDisplayString extends Condition<IJavaCompletionProposal> {
		private final Pattern p;

		public MatchDisplayString(String bundle, String version, String fqName, boolean test) {
			super(String.format("Suggestion to add '%s' to -%spath for class %s", bundle, test ? "test" : "build",
				fqName));
			String re = String.format("^Add \\Q%s\\E to -\\Q%s\\Epath [(]found \\Q%s\\E[)]", bundle,
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

	protected void assertThatContainsFrameworkBundles(IJavaCompletionProposal[] proposals, String fqName) {
		assertThatProposals(proposals).withRepresentation(PROPOSAL)
			.hasSize(1)
			.haveExactly(1, suggestsBundle("org.osgi.framework", "1.8.0", fqName));
	}

	protected static Condition<IJavaCompletionProposal> suggestsBundle(String bundle, String version, String fqName) {
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

	public AbstractBuildpathQuickFixProcessorTest() {
		super();
	}

}
