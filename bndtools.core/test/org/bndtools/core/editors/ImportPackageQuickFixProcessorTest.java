package org.bndtools.core.editors;

import static org.assertj.core.api.Assertions.allOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.util.Sets.newLinkedHashSet;
import static org.bndtools.core.editors.ImportPackageQuickFixProcessor.ADD_BUNDLE;
import static org.bndtools.core.editors.ImportPackageQuickFixProcessor.ADD_BUNDLE_WORKSPACE;
import static org.eclipse.jdt.core.compiler.IProblem.AmbiguousField;
import static org.eclipse.jdt.core.compiler.IProblem.AmbiguousType;
import static org.eclipse.jdt.core.compiler.IProblem.ImportNotFound;
import static org.eclipse.jdt.core.compiler.IProblem.IsClassPathCorrect;
import static org.eclipse.jdt.core.compiler.IProblem.UndefinedType;
import static org.eclipse.jdt.core.compiler.IProblem.UnusedImport;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.assertj.core.api.Condition;
import org.bndtools.api.ILogger;
import org.bndtools.core.editors.ImportPackageQuickFixProcessor.AddBundleCompletionProposal;
import org.bndtools.core.editors.ImportPackageQuickFixProcessor.BndBuildPathHandler;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

import aQute.bnd.build.WorkspaceRepository;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.strings.Strings;

public class ImportPackageQuickFixProcessorTest {

	private static final String									WORKSPACE_REPO		= "Workspace repo";
	private static final String									JDT_PROBLEM			= "org.eclipse.core.markers.problem";

	private ImportPackageQuickFixProcessor						sut;

	private ILogger												logger;

	private CompilationUnit										cu;

	private List<VersionedClause>								buildPath;
	private CoreException										containsBundleException;

	private final Set<String>									frameworkBundles	= newLinkedHashSet();
	private final Set<String>									eclipseBundles		= newLinkedHashSet();

	private List<RepositoryPlugin>								plugins;

	private Map<String, Map<String, Collection<Capability>>>	repoMap;

	private WorkspaceRepository									workspacePlugin;
	private Repository											workspaceRepo;

	private interface PluginRepo extends RepositoryPlugin, Repository {}

	// This could go into a test utils package somewhere.
	public static final Answer<?> DO_NOT_CALL = invocation -> {
		throw new AssertionError("Method should not be called during testing");
	};

	private Map<String, Collection<Capability>> getRepoMapFor(String name) {
		Map<String, Collection<Capability>> thisRepoMap = repoMap.get(name);
		if (thisRepoMap == null) {
			thisRepoMap = new HashMap<>();
			repoMap.put(name, thisRepoMap);
		}
		return thisRepoMap;
	}

	private void clearRepos() {
		repoMap.clear();
		frameworkBundles.clear();
		eclipseBundles.clear();
	}

	static final Pattern FILTER = Pattern
		.compile(Pattern.quote(PackageNamespace.PACKAGE_NAMESPACE) + "\\s*=\\s*([^)= ]*)\\s*[)]");

	@SuppressWarnings("unchecked")
	private void mockFindProviders(final String name, Repository repo) {
		doAnswer(invocation -> {
			Collection<? extends Requirement> reqs = invocation.getArgument(0);
			final Map<String, Collection<Capability>> thisRepoMap = getRepoMapFor(name);
			Map<Requirement, Collection<Capability>> retval = new HashMap<>();
			int i = 0;
			for (Requirement req : reqs) {
				assertThat(req.getNamespace()).as("[" + i + "]namespace")
					.isEqualTo(PackageNamespace.PACKAGE_NAMESPACE);
				assertThat(req.getDirectives()).as("[" + i + "]directives")
					.containsOnlyKeys(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
				final String filter = req.getDirectives()
					.get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
				final Matcher matcher = FILTER.matcher(filter);
				assertThat(matcher.find()).as("[" + i + "]filter matches " + FILTER)
					.isTrue();
				final String pkg = matcher.group(1);
				final Collection<Capability> caps = thisRepoMap.get(pkg);
				retval.put(req, (caps == null ? Collections.emptyList() : caps));
				i++;
			}
			return retval;
		}).when(repo)
			.findProviders((Collection<? extends Requirement>) any());
	}

	private void mockRepositoryPlugin(String name, RepositoryPlugin p) {
		doReturn(name).when(p)
			.getName();
		doReturn("Repository for " + name).when(p)
			.toString();
	}

	private void buildMockPluginRepo(String name) {
		PluginRepo p = mock(PluginRepo.class, DO_NOT_CALL);
		mockRepositoryPlugin(name, p);
		mockFindProviders(name, p);
		plugins.add(p);
	}

	// Add the capabilities that describe a bundle to a fake repo.
	private void addBundleToRepo(String repo, String bsn, VersionedClause... exports) {
		final Map<String, Collection<Capability>> thisRepoMap = getRepoMapFor(repo);

		ResourceBuilder resource = new ResourceBuilder();

		Attrs attrs = new Attrs();
		attrs.put(Constants.BUNDLE_SYMBOLICNAME, bsn);
		Domain manifest = Domain.domain(attrs);

		try {
			resource.addManifest(manifest);
			attrs = new Attrs();
			attrs.put(BundleNamespace.BUNDLE_NAMESPACE, bsn);
			resource.addCapability(CapReqBuilder.getCapabilityFrom(BundleNamespace.BUNDLE_NAMESPACE, attrs));
			for (

			VersionedClause exportVersion : exports) {
				final String export = exportVersion.getName();
				if (export.equals("org.osgi.framework")) {
					frameworkBundles.add(bsn);
				}
				if (export.equals("org.eclipse.ui")) {
					eclipseBundles.add(bsn);
				}
				Attrs pkgAttrs = new Attrs();
				pkgAttrs.put(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, exportVersion.getVersionRange());
				resource.addExportPackage(export, pkgAttrs);
			}
			Resource res = resource.build();
			for (Capability cap : res.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE)) {
				String export = cap.getAttributes()
					.get(PackageNamespace.PACKAGE_NAMESPACE)
					.toString();
				Collection<Capability> bundles = thisRepoMap.get(export);
				if (bundles == null) {
					bundles = new HashSet<>();
					thisRepoMap.put(export, bundles);
				}
				bundles.add(cap);
			}
		} catch (Exception e) {
			fail("Exception trying to add dummy bundle " + bsn + " to test repo", e);
		}
	}

	@Test
	// Meta-test
	public void addBundleToRepo_createsCorrectCapability() throws Exception {
		clearRepos();
		Attrs v100 = new Attrs();
		v100.put(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, "1.0.0");
		Attrs v110 = new Attrs();
		v110.put(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, "1.1.0");
		addBundleToRepo("Repo 1", "my.test.bundle", new VersionedClause("org.osgi.framework", v100),
			new VersionedClause("my.test.pkg", v110));
		Map<String, Collection<Capability>> caps = repoMap.get("Repo 1");

		assertThat(caps).as("map keys")
			.containsKeys("org.osgi.framework", "my.test.pkg");

		assertThat(frameworkBundles).as("framework bundle")
			.containsExactly("my.test.bundle");

		Resource bundleResource = null;

		for (Map.Entry<String, Collection<Capability>> entry : caps.entrySet()) {
			Collection<Capability> packageCaps = entry.getValue();
			assertThat(packageCaps).isNotNull()
				.hasSize(1);
			for (Capability pkg : packageCaps) {
				assertThat(pkg).as("pkg")
					.isNotNull();
				assertThat(pkg.getResource()).as("resource")
					.isNotNull();
				if (bundleResource == null) {
					bundleResource = pkg.getResource();
				} else {
					assertThat(pkg.getResource()).as("resource")
						.isSameAs(bundleResource);
				}
			}
		}
		assertThat(bundleResource).as("bundleResource")
			.isNotNull();
	}

	@Before
	public void setUp() throws Exception {
		sut = new FakeImportPackageQuickFixProcessor();
		plugins = new ArrayList<>();
		repoMap = new HashMap<>();

		logger = mock(ILogger.class);
		ImportPackageQuickFixProcessor.logger = logger;

		workspacePlugin = mock(WorkspaceRepository.class, DO_NOT_CALL);
		workspaceRepo = mock(Repository.class, DO_NOT_CALL);
		mockRepositoryPlugin(WORKSPACE_REPO, workspacePlugin);
		mockFindProviders(WORKSPACE_REPO, workspaceRepo);
		plugins.add(workspacePlugin);

		buildMockPluginRepo("Repo 1");
		buildMockPluginRepo("Repo 2");

		Attrs v110 = new Attrs();
		v110.put(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, "1.1.0");
		Attrs v100 = new Attrs();
		v100.put(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, "1.0.0");
		Attrs v201 = new Attrs();
		v201.put(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, "2.0.1");
		addBundleToRepo("Repo 1", "my.test.bundle", new VersionedClause("org.osgi.framework", v100),
			new VersionedClause("org.eclipse.ui", v110), new VersionedClause("test", v110),
			new VersionedClause("Test", v201));
		addBundleToRepo("Repo 2", "my.second.bundle", new VersionedClause("org.osgi.framework", v110),
			new VersionedClause("test", v100), new VersionedClause("Test", v110));
		addBundleToRepo(WORKSPACE_REPO, "my.eclipse.bundle", new VersionedClause("org.eclipse.ui", v201),
			new VersionedClause("my.workspace.only.pkg", v110));

		// Uncomment if you need to debug the test setup.
		// dumpRepo();

		buildPath = new ArrayList<>();
	}

	private void assertThatContainsFrameworkBundles(IJavaCompletionProposal[] props) {
		assertThat(props).hasSize(frameworkBundles.size())
			.haveExactly(1, allOf(suggestsBundle("my.test.bundle", "Repo 1"), withRelevance(ADD_BUNDLE)))
			.haveExactly(1, allOf(suggestsBundle("my.second.bundle", "Repo 2"), withRelevance(ADD_BUNDLE)));
	}

	private void setupAST(String source) {
		ASTParser parser = ASTParser.newParser(AST.JLS11);
		Map<String, String> options = JavaCore.getOptions();
		// Need to set 1.5 or higher for the "import static" syntax to work.
		// Need to set 1.8 or higher to test parameterized type usages.
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
		parser.setCompilerOptions(options);
		parser.setSource(source.toCharArray());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		cu = (CompilationUnit) parser.createAST(null);
	}

	private void addToBuildPath(VersionedClause bundle) {
		buildPath.add(bundle);
	}

	private void addToBuildPath(String bundle) {
		buildPath.add(new VersionedClause(bundle, new Attrs()));
	}

	private static final String	CLASS_HEADER	= "class Test {";
	private static final String	CLASS_FOOTER	= " var};";

	@Test
	// Internal test for a fairly complicated helper method.
	public void testGetProblems() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> {
			getProblems("import `my.pkg.*';", new int[0]);
		});
		List<IProblemLocation> probs = getProblems("import ``my.`p'kg'.*';", ImportNotFound, UndefinedType,
			AmbiguousField);
		assertThat(cu.toString()
			.trim()).as("source")
				.isEqualTo("import my.pkg.*;");
		assertThat(probs).hasSize(3);
		IProblemLocation loc;
		loc = probs.get(0);
		assertThat(loc.getProblemId()).as("problem 0")
			.isEqualTo(ImportNotFound);
		assertThat(loc.getOffset()).as("offset 0")
			.isEqualTo(10);
		assertThat(loc.getLength()).as("length 0")
			.isEqualTo(1);
		loc = probs.get(1);
		assertThat(loc.getProblemId()).as("problem 1")
			.isEqualTo(UndefinedType);
		assertThat(loc.getOffset()).as("offset 1")
			.isEqualTo(7);
		assertThat(loc.getLength()).as("length 1")
			.isEqualTo(6);
		loc = probs.get(2);
		assertThat(loc.getProblemId()).as("problem 2")
			.isEqualTo(AmbiguousField);
		assertThat(loc.getOffset()).as("offset 2")
			.isEqualTo(7);
		assertThat(loc.getLength()).as("length 2")
			.isEqualTo(8);
	}

	/**
	 * Processes "marked source" to generate an AST with associated
	 * {@link IProblemLocation} instances. The "marked source" consists of plain
	 * Java source, with "`" to mark the start of a problem and "'" to mark the
	 * end. This syntax will obviously not handle source with character literals
	 * well (or strings with embedded ' characters), but that doesn't matter for
	 * this test usage. Marked problems can be nested, but cannot overlap as the
	 * closing "'" is always paired with the most recent starting "`".<br>
	 * The IDs for the problems are taken from the supplied <tt>problemIds</tt>
	 * array. They are applied in the order that the closing "'" characters are
	 * found. If the number of supplied problemIds is not equal to the number of
	 * problems marked in the source, the method will assert.<br>
	 * The method finally calls {@link #setupAST(String)} to set up the AST
	 * corresponding to the supplied source after the markers have been
	 * stripped.
	 *
	 * @param markedSource The string containing the marked-up Java source.
	 * @param problemIds Array of problem IDs used when building the
	 *            IProblemLocation objects. It is expected that the number of
	 *            elements in this array will match the number of "`'" pairs in
	 *            the marked source.
	 * @return The {@link List} of {@link IProblemLocation} objects that
	 *         correspond to the markers in the source.
	 */
	private List<IProblemLocation> getProblems(String markedSource, IProblemLocation... srcProblems) {
		List<IProblemLocation> problems = new ArrayList<>(srcProblems.length);
		StringBuilder source = new StringBuilder(markedSource.length());
		Deque<Integer> starts = new ArrayDeque<>();

		int problemIndex = 0;
		int j = 0;
		for (int i = 0; i < markedSource.length(); i++) {
			char current = markedSource.charAt(i);
			switch (current) {
				case '`' :
					starts.push(j);
					break;
				case '\'' :
					int currentStart = starts.isEmpty() ? 0 : starts.pop();
					assertThat(problemIndex).as("getProblems() problem count")
						.isLessThan(srcProblems.length);
					final IProblemLocation srcProb = srcProblems[problemIndex++];
					problems.add(new ProblemLocation(currentStart, j - currentStart, srcProb.getProblemId(),
						srcProb.getProblemArguments(), srcProb.isError(), srcProb.getMarkerType()));
					break;
				default :
					source.append(current);
					j++;
					break;
			}
		}
		assertThat(problems).as("getProblems() array length")
			.hasSize(srcProblems.length);
		setupAST(source.toString());
		return problems;
	}

	private List<IProblemLocation> getProblems(String markedSource, int... problemIds) {
		IProblemLocation[] srcProblems = new IProblemLocation[problemIds.length];
		for (int i = 0; i < problemIds.length; i++) {
			srcProblems[i] = new ProblemLocation(0, 0, problemIds[i], new String[0], true, JDT_PROBLEM);
		}
		return getProblems(markedSource, srcProblems);
	}

	// If there are no markers in the source, wrap the whole string in a pair of
	// markers.
	private static String autoMark(String source) {
		return source.indexOf('`') == -1 ? '`' + source + '\'' : source;
	}

	private AddBundleCompletionProposal[] proposalsForUndefType(String type) {
		final String markedSource = CLASS_HEADER + autoMark(type) + CLASS_FOOTER;
		List<IProblemLocation> locs = getProblems(markedSource, UndefinedType);

		return proposalsFor(new FakeInvocationContext(CLASS_HEADER.length(), 0), locs);
	}

	// Automatically mark up the full import statement if there is no marker.
	private AddBundleCompletionProposal[] proposalsForImport(String imp) {
		final String markedSource = "import " + autoMark(imp) + ';';

		List<IProblemLocation> locs = getProblems(markedSource, ImportNotFound);

		return proposalsFor(new FakeInvocationContext(7, 0), locs);
	}

	private AddBundleCompletionProposal[] proposalsFor(List<? extends IProblemLocation> locs) {
		return proposalsFor(new FakeInvocationContext(), locs);
	}

	private AddBundleCompletionProposal[] proposalsFor(IInvocationContext context,
		List<? extends IProblemLocation> locs) {
		final AtomicReference<IJavaCompletionProposal[]> ref = new AtomicReference<>();

		IProblemLocation[] locArray = new IProblemLocation[locs.size()];
		assertThatCode(() -> {
			ref.set(sut.getCorrections(context, locs.toArray(locArray)));
		}).doesNotThrowAnyException();

		final IJavaCompletionProposal[] props = ref.get();
		if (props == null) {
			return null;
		}
		assertThat(props).as("proposals")
			.hasOnlyElementsOfType(AddBundleCompletionProposal.class);
		AddBundleCompletionProposal[] retval = new AddBundleCompletionProposal[props.length];
		int i = 0;
		for (IJavaCompletionProposal prop : props) {
			retval[i++] = (AddBundleCompletionProposal) prop;
		}
		return retval;
	}

	@Test
	public void hasCorrections_withProblemIdImportPackage_returnsTrue() {
		assertThat(sut.hasCorrections(null, ImportNotFound)).isTrue();
	}

	@Test
	public void hasCorrections_withProblemIdUndefinedType_returnsTrue() {
		assertThat(sut.hasCorrections(null, UndefinedType)).isTrue();
	}

	@Test
	public void hasCorrections_withProblemIdIsClassPathCorrect_returnsTrue() {
		assertThat(sut.hasCorrections(null, IsClassPathCorrect)).isTrue();
	}

	@Test
	public void hasCorrections_withOtherProblemId_returnsFalse() {
		assertThat(sut.hasCorrections(null, UnusedImport)).isFalse();
	}

	@Test
	public void getCorrections_withNoMatches_forUndefType_returnsNull() {
		assertThat(proposalsForUndefType("my.unknown.type.MyClass")).isNull();
	}

	@Test
	public void getCorrections_withUnqualifiedType_returnsNull() {
		assertThat(proposalsForUndefType("BundleActivator")).isNull();
	}

	@Test
	public void getCorrections_withAnnotatedUnqualifiedType_returnsNull() {
		assertThat(proposalsForUndefType("@NonNull `BundleActivator'")).isNull();
	}

	@Test
	public void getCorrections_withInnerType_returnsNull() {
		assertThat(proposalsForUndefType("BundleActivator.Inner")).isNull();
	}

	@Test
	public void getCorrections_withMarkerOnSimpleInnerType_returnsNull() {
		assertThat(proposalsForUndefType("`BundleActivator'.Inner")).isNull();
	}

	@Test
	public void getCorrections_withFQType_suggestsBundles() {
		assertThatContainsFrameworkBundles(proposalsForUndefType("org.osgi.framework.BundleActivator"));
	}

	@Test
	public void getCorrections_withMarkerOnSimpleType_suggestsBundles() {
		assertThatContainsFrameworkBundles(proposalsForUndefType("org.osgi.framework.`BundleActivator'"));
	}

	@Test
	public void getCorrections_withMarkerOnPackage_suggestsBundles() {
		assertThatContainsFrameworkBundles(proposalsForUndefType("`org.osgi.framework'.BundleActivator"));
	}

	@Test
	public void getCorrections_withFQNestedType_suggestsBundles() {
		assertThatContainsFrameworkBundles(proposalsForUndefType("org.osgi.framework.BundleActivator.Inner"));
	}

	@Test
	public void getCorrections_withFQNestedType_andMarkerOnInnerType_suggestsBundles() {
		assertThatContainsFrameworkBundles(proposalsForUndefType("org.osgi.framework.BundleActivator.`Inner'"));
	}

	@Test
	public void getCorrections_withFQNestedType_andMarkerOnOuterType_suggestsBundles() {
		assertThatContainsFrameworkBundles(proposalsForUndefType("org.osgi.framework.`BundleActivator'.Inner"));
	}

	@Test
	// Annotated type cause the AST to generate a NameQualifiedType; need to
	// handle this case.
	public void getCorrections_withAnnotatedFQNestedType_andMarkerOnOuterType_suggestsBundles() {
		assertThatContainsFrameworkBundles(
			proposalsForUndefType("org.osgi.framework.`BundleActivator'.@NotNull Inner"));
	}

	@Test
	public void getCorrections_withAnnotatedFQType_andPackageMarked_suggestsBundles() {
		assertThatContainsFrameworkBundles(proposalsForUndefType("`org.osgi.framework'.@NotNull BundleActivator"));
	}

	@Test
	public void getCorrections_withFQType_andOneLevelPackage_suggestsBundles() {
		// pkgMap.put("test", frameworkBundles);
		assertThatContainsFrameworkBundles(proposalsForUndefType("test.BundleActivator"));
	}

	@Test
	public void getCorrections_withAnnotatedFQType_andOneLevelPackage_suggestsBundles() {
		// pkgMap.put("test", frameworkBundles);
		assertThatContainsFrameworkBundles(proposalsForUndefType("`test'.@NotNull BundleActivator"));
	}

	@Test
	// Using a parameterized type as a qualifier forces Eclipse AST to generate
	// a QualifiedType
	public void getCorrections_withParameterisedOuterType_suggestsBundles() {
		assertThatContainsFrameworkBundles(
			proposalsForUndefType("`org.osgi.framework'.BundleActivator<String>.Inner.@NotNull BundleActivator"));
	}

	@Test
	@Ignore("Not yet implemented")
	// Force Eclipse AST to generate a QualifiedType
	public void getCorrections_withParameterisedOuterType_andMarkerOnInner_suggestsBundles() {
		// Due to the structure of QualifiedType (the qualifier is a Type rather
		// than simply a Name), it becomes
		// a little more complicated to recurse back through the type definition
		// to get the package name.
		// Moreover, the JDT doesn't seem to mark the inner type if it can't
		// find the package - it will mark
		// the package instead. So this kind of construction is unlikely to
		// occur in practice. If there becomes
		// a need, this test can be re-enabled and implemented.
		assertThatContainsFrameworkBundles(
			proposalsForUndefType("org.osgi.framework.BundleActivator<String>.`Inner'.@NotNull BundleActivator"));
	}

	@Test
	public void getCorrections_withUnnnotatedFQArrayType_andFullTypeMarked_suggestsBundles() {
		assertThatContainsFrameworkBundles(proposalsForUndefType("`org.osgi.framework.BundleActivator'[]"));
	}

	@Test
	public void getCorrections_withAnnotatedFQArrayType_andPackageMarked_suggestsBundles() {
		assertThatContainsFrameworkBundles(proposalsForUndefType("`org.osgi.framework'.@NotNull BundleActivator[]"));
	}

	@Test
	public void getCorrections_withAnnotatedFQDoubleArrayType_andPackageMarked_suggestsBundles() {
		assertThatContainsFrameworkBundles(proposalsForUndefType("`org.osgi.framework'.@NotNull BundleActivator[][]"));
	}

	@Test
	public void getCorrections_withAnnotatedFQType_andPackagePartMarked_suggestsBundles() {
		assertThatContainsFrameworkBundles(proposalsForUndefType("`org.osgi'.framework.@NotNull BundleActivator"));
	}

	@Test
	public void getCorrections_withParameter_suggestsBundles() {
		assertThatContainsFrameworkBundles(
			proposalsForUndefType("List<`org.osgi'.framework.@NotNull BundleActivator>"));
	}

	@Test
	public void getCorrections_withWildcardBound_suggestsBundles() {
		assertThatContainsFrameworkBundles(
			proposalsForUndefType("List<? extends `org.osgi'.framework.BundleActivator>"));
	}

	@Test
	public void getCorrections_withAnnotatedWildcardBound_suggestsBundles() {
		assertThatContainsFrameworkBundles(
			proposalsForUndefType("List<? extends `org.osgi'.framework.@NotNull BundleActivator>"));
	}

	@Test
	public void getCorrections_withUnqualifiedNameType_returnsNull() {
		// If the type is a simple name, it must refer to a type and not a
		// package; therefore don't provide package
		// import suggestions even if we have a package with the matching name.
		assertThat(proposalsForUndefType("Test")).as("capitalized")
			.isNull();
		assertThat(proposalsForUndefType("test")).as("uncapitalized")
			.isNull();
	}

	@Test
	public void getCorrection_withParameterizedType_suggestsBundles() {
		assertThatContainsFrameworkBundles(proposalsForUndefType("`org.osgi.framework'.BundleActivator<String>"));
	}

	@Test
	public void getCorrection_withParameterizedType_thatLooksLikePackage_returnsNull() {
		assertThat(proposalsForUndefType("org.osgi.framework<String>.`BundleActivator'"));
	}

	// Enabling the handling of markers like these is feasible but complicates
	// the code a fair bit. Also, the JDT
	// doesn't seem to mark it like this, but if anything will do the whole
	// string or part of the package string.
	// Leaving the test here in case we decide to add the functionality in the
	// future, but at the moment it won't
	// pass.
	@Test
	public void getCorrections_withAnnotatedFQType_andMarkerOnType_suggestsBundles() {
		assertThatContainsFrameworkBundles(proposalsForUndefType("org.osgi.framework.@NotNull `BundleActivator'"));
	}

	@Test
	public void getCorrections_withAnnotatedFQNestedType_andMarkerOnInnerType_suggestsBundles() {
		assertThatContainsFrameworkBundles(
			proposalsForUndefType("org.osgi.framework.BundleActivator.@NotNull `Inner'"));
	}

	@Test
	public void getCorrections_withAnnotatedFQType_andWholeDecMarked_suggestsBundles() {
		assertThatContainsFrameworkBundles(proposalsForUndefType("org.osgi.framework.@NotNull BundleActivator"));
	}

	@Test
	public void getCorrections_withNoMatches_returnsNull() {
		clearRepos();
		assertThat(proposalsForImport("org.osgi.framework.*")).as("import")
			.isNull();
		assertThat(proposalsForUndefType("org.osgi.framework.BundleActivator")).as("type")
			.isNull();
	}

	@Test
	public void getCorrections_withOnDemandImport_altPackage_suggestsBundles() {
		AddBundleCompletionProposal[] props = proposalsForImport("`org.eclipse'.ui.*");
		assertThat(props).hasSize(eclipseBundles.size())
			.haveExactly(1, allOf(suggestsBundle("my.test.bundle", "Repo 1"), withRelevance(ADD_BUNDLE)))
			.haveExactly(1,
				allOf(suggestsBundle("my.eclipse.bundle", WORKSPACE_REPO), withRelevance(ADD_BUNDLE_WORKSPACE)));
	}

	@Test
	public void getCorrections_withOnlyIrrelvantProblems_skipsAll_andReturnsNull() {
		List<IProblemLocation> locs = getProblems("import `org.`osgi'.framework'.*", UnusedImport, AmbiguousType);

		assertThat(proposalsFor(locs)).isNull();
	}

	@Test
	public void getCorrections_skipsIrrelevantProblems_andReturnsBundles() {
		List<IProblemLocation> locs = getProblems("import `org.osgi'`.framework'.*", ImportNotFound, UnusedImport);

		assertThatContainsFrameworkBundles(proposalsFor(locs));
	}

	@Test
	public void getCorrections_withMarkerInMiddle_suggestsBundles() {
		assertThatContainsFrameworkBundles(proposalsForImport("org.`osgi.'framework.*"));
	}

	@Test
	public void getCorrections_withMarkerAroundWildcard_suggestsBundles() {
		assertThatContainsFrameworkBundles(proposalsForImport("org.`osgi.framework.*'"));
	}

	@Test
	public void getCorrections_withMarkerAroundClass_suggestsBundles() {
		assertThatContainsFrameworkBundles(proposalsForImport("org.`osgi.framework.BundleActivator'"));
	}

	@Test
	public void getCorrections_withMarkerAroundStatic_suggestsBundles() {
		assertThatContainsFrameworkBundles(proposalsForImport("`static org.osgi.framework'.Clazz.member"));
	}

	@Test
	public void getCorrections_withClassImport_suggestsBundles() {
		assertThatContainsFrameworkBundles(proposalsForImport("`org.osgi'.framework.BundleActivator"));
	}

	@Test
	public void getCorrections_withInnerClassImport_suggestsBundles() {
		assertThatContainsFrameworkBundles(proposalsForImport("`org.osgi'.framework.BundleActivator.Inner"));
	}

	@Test
	public void getCorrections_withOnDemandImport_suggestsBundles() {
		assertThatContainsFrameworkBundles(proposalsForImport("`org.osgi'.framework.*"));
	}

	@Test
	public void getCorrections_withOnDemandStaticImport_suggestsBundles() {
		assertThatContainsFrameworkBundles(proposalsForImport("static `org.osgi'.framework.Clazz.*"));
	}

	@Test
	public void getCorrections_withStaticImport_suggestsBundles() {
		assertThatContainsFrameworkBundles(proposalsForImport("static `org.osgi'.framework.Clazz.member"));
	}

	@Test
	public void getCorrections_whenUnversionedBundleAlreadyIncluded_returnsNull() {
		addToBuildPath("my.test.bundle");
		AddBundleCompletionProposal[] props = proposalsForImport("`org.osgi'.framework.*");
		assertThat(props).hasSize(1)
			.haveExactly(1, suggestsBundle("my.second.bundle"));
	}

	@Test
	public void getCorrections_whenVersionedBundleAlreadyIncluded_returnsNull() {
		Attrs attrs = new Attrs();
		attrs.put("version", "1.0.2");
		addToBuildPath(new VersionedClause("my.second.bundle", attrs));
		AddBundleCompletionProposal[] props = proposalsForImport("`org.osgi'.framework.*");
		assertThat(props).hasSize(1)
			.haveExactly(1, suggestsBundle("my.test.bundle"));
	}

	@Test
	public void getCorrections_withSimplePackageName_suggestsBundles() {
		assertThatContainsFrameworkBundles(proposalsForImport("test.Clazz"));
	}

	@Test
	public void getCorrections_withOnDemandSimplePackageName_suggestsBundles() {
		assertThatContainsFrameworkBundles(proposalsForImport("`test'.*"));
	}

	@Test
	public void getCorrections_withOnDemandSimpleCapitalisedPackageName_suggestsBundles() {
		// The qualifier for a class in a package name that is only one layer
		// deep
		// is a simple name and not a qualified name. Normally we assume that if
		// it is
		// capitalized it must refer to a type rather than a package, but
		// syntactically
		// this isn't permitted in the first position of an import statement, so
		// we know
		// that a package must be meant.
		assertThatContainsFrameworkBundles(proposalsForImport("`Test'.*"));
	}

	@Test
	public void getCorrections_withOnDemandStatic_forClassInMainNamespace_returnsNull() {
		// The qualifier for a class in a package name that is only one layer
		// deep
		// is a simple name and not a qualified name. Make sure that
		// getPackageName can handle that.
		assertThat(proposalsForImport("static >Test<.*")).isNull();
	}

	@Test
	public void getCorrections_forClassInMainNamespace_returnsNull() {
		// The qualifier for a class in a package name that is only one layer
		// deep
		// is a simple name and not a qualified name. Make sure that
		// getPackageName can handle that.
		assertThat(proposalsForImport("Test")).isNull();
	}

	@Test
	public void getCorrections_forStaticImportClassInMainNamespace_returnsNull() {
		// The qualifier for a class in a package name that is only one layer
		// deep
		// is a simple name and not a qualified name. Make sure that
		// getPackageName can handle that.
		assertThat(proposalsForImport("static Test.member")).isNull();
	}

	@Test
	public void getCorrections_forKnownImport_fromDifferentPackage_suggestsBundles() {
		List<IProblemLocation> locs = getProblems("package my.other.pkg;\n\nimport `org.osgi'.framework.Clazz;",
			ImportNotFound);

		assertThatContainsFrameworkBundles(proposalsFor(locs));
	}

	@Test
	public void getCorrections_forKnownImport_fromSamePackage_returnsNull() {
		List<IProblemLocation> locs = getProblems("package org.osgi.framework;\n\nimport `org.osgi.framework'.Clazz;",
			ImportNotFound);

		assertThat(proposalsFor(locs)).isNull();
	}

	@Test
	public void getCorrections_forWorkspaceOnlyImport_suggestsBundle_withWorkspaceRelevance() {
		AddBundleCompletionProposal[] props = proposalsForImport("`my.workspace.only.pkg'.*");
		assertThat(props).hasSize(1)
			.haveExactly(1, allOf(suggestsBundle("my.eclipse.bundle"), withRelevance(ADD_BUNDLE_WORKSPACE)));
	}

	@Test
	public void getCorrections_forIsClassPath_suggestsBundle() {
		List<IProblemLocation> locs = getProblems("public class Test { Test2 test = new `Test2()'; }",
			new ProblemLocation(0, 0, IsClassPathCorrect, new String[] {
				"org.osgi.framework.Clazz"
			}, true, JDT_PROBLEM));

		assertThatContainsFrameworkBundles(proposalsFor(locs));
	}

	@Test
	public void getCorrections_forIsClassPath_withLowerCaseClass_suggestsBundle() {
		List<IProblemLocation> locs = getProblems("public class Test { Test2 test = new `Test2()'; }",
			new ProblemLocation(0, 0, IsClassPathCorrect, new String[] {
				"org.osgi.framework.clazz"
			}, true, JDT_PROBLEM));

		assertThatContainsFrameworkBundles(proposalsFor(locs));
	}

	@Test
	public void getCorrections_forIsClassPath_withClassInMainNamespace_returnsNull() {
		List<IProblemLocation> locs = getProblems("package other; public class Test { Test2 test = new `Test2()'; }",
			new ProblemLocation(0, 0, IsClassPathCorrect, new String[] {
				"Test"
			}, true, JDT_PROBLEM));
		assertThat(proposalsFor(locs)).isNull();
	}

	@Test
	public void getCorrections_forIsClassPath_withLowerCaseClassInMainNamespace_returnsNull() {
		List<IProblemLocation> locs = getProblems("package other; public class Test { Test2 test = new `Test2()'; }",
			new ProblemLocation(0, 0, IsClassPathCorrect, new String[] {
				"test"
			}, true, JDT_PROBLEM));
		assertThat(proposalsFor(locs)).isNull();
	}

	// These next three types are paranoia checks, because I'm not quite sure
	// how Eclipse will behave in all
	// circumstances, so I'm being defensive.
	@Test
	public void getCorrections_forIsClassPath_withEmptyArgs_returnsNullGracefully() {
		List<IProblemLocation> locs = getProblems("public class Test { Test2 test = new `Test2()'; }",
			new ProblemLocation(0, 0, IsClassPathCorrect, new String[0], true, JDT_PROBLEM));

		assertThat(proposalsFor(locs)).isNull();
	}

	@Test
	public void getCorrections_forIsClassPath_withBadType_returnsNullGracefully_andLogsWarning() {
		List<IProblemLocation> locs = getProblems("package other; public class Test { Test2 test = new `Test2()'; }",
			new ProblemLocation(0, 0, IsClassPathCorrect, new String[] {
				"invalid type/string"
			}, true, JDT_PROBLEM));

		assertThat(proposalsFor(locs)).isNull();
		verify(logger).logWarning(eq("Illegal type 'invalid type/string'"), any(IllegalArgumentException.class));
	}

	@Test
	public void getCorrections_forIsClassPath_withMissingArgs_returnsNullGracefully() {
		List<IProblemLocation> locs = getProblems("public class Test { Test2 test = new `Test2()'; }",
			new ProblemLocation(0, 0, IsClassPathCorrect, null, true, JDT_PROBLEM));

		assertThat(proposalsFor(locs)).isNull();
	}

	@Test
	public void getCorrections_forBundleInTwoRepos_describesBundles() {
		addBundleToRepo("Repo 2", "my.test.bundle", new VersionedClause("org.osgi.framework", new Attrs()));
		AddBundleCompletionProposal[] props = proposalsForImport("`org.osgi.framework'.*");
		assertThat(props).haveExactly(1, suggestsBundle("my.test.bundle", "Repo 1", "Repo 2"));
	}

	@Test
	public void getCorrections_forBundleInThreeRepos_describesBundles() {
		addBundleToRepo("Repo 2", "my.test.bundle", new VersionedClause("org.osgi.framework", new Attrs()));
		addBundleToRepo(WORKSPACE_REPO, "my.test.bundle", new VersionedClause("org.osgi.framework", new Attrs()));
		AddBundleCompletionProposal[] props = proposalsForImport("`org.osgi.framework'.*");
		assertThat(props).haveExactly(1, allOf(suggestsBundle("my.test.bundle", "Repo 1", WORKSPACE_REPO, "Repo 2"),
			withRelevance(ADD_BUNDLE_WORKSPACE)));
	}

	Exception makeSUTgetWSRepo_throwException() {
		final Exception ex = new Exception();
		sut = new FakeImportPackageQuickFixProcessor() {
			@Override
			Repository getWorkspaceRepo() throws Exception {
				throw (Exception) ex.fillInStackTrace();
			}
		};
		return ex;
	}

	@Test
	public void getCorrections_forWorkspaceOnlyImport_whenWorkspaceFails_logsErrorOnce_andReturnsNull() {
		Exception ex = makeSUTgetWSRepo_throwException();

		AddBundleCompletionProposal[] props = proposalsForImport("`my.workspace.only.pkg'.*");

		assertThat(props).as("proposals")
			.isNull();
		verify(logger, times(1)).logError("Error trying to fetch the repository for the current workspace", ex);
	}

	@Test
	public void getCorrections_whenWorkspaceFails_returnsProposalsFromNonWorkspaceRepos() {
		makeSUTgetWSRepo_throwException();

		AddBundleCompletionProposal[] props = proposalsForImport("`org.eclipse.ui'.*");

		assertThat(props).hasSize(1)
			.have(suggestsBundle("my.test.bundle"));
	}

	@Test
	public void getCorrections_whenWorkspaceFails_andMultiplePackages_onlyLogsErrorOnce() {
		Exception ex = makeSUTgetWSRepo_throwException();

		List<IProblemLocation> locs = getProblems("import `org.eclipse.ui'.*; import `org.osgi.framework'.*;",
			new int[] {
				ImportNotFound, ImportNotFound
			});

		proposalsFor(new FakeInvocationContext(7, 0), locs);

		verify(logger, times(1)).logError("Error trying to fetch the repository for the current workspace", ex);
	}

	@Test
	public void getCorrections_whenContainsBundleFails_throwsException() {
		containsBundleException = new CoreException(new Status(IStatus.ERROR, "bundle", "Hi there"));

		List<IProblemLocation> locs = getProblems("import `org.osgi.framework'.*;", ImportNotFound);
		IProblemLocation[] locArray = new IProblemLocation[locs.size()];
		assertThatExceptionOfType(CoreException.class).isThrownBy(() -> {
			sut.getCorrections(new FakeInvocationContext(), locs.toArray(locArray));
		})
			.isSameAs(containsBundleException);
	}

	static class MatchDisplayString extends Condition<IJavaCompletionProposal> {
		private final Pattern		p;
		private final Set<String>	repos;

		public MatchDisplayString(String bundle, String... repos) {
			super(Strings.format("A suggestion for bundle '%s'", bundle)
				+ ((repos != null && repos.length > 0) ? Strings.format(" from repos %s", Arrays.toString(repos))
					: ""));
			String re = String.format("^Add bundle '%s' to Bnd build path", bundle);
			if (repos != null) {
				re += " [(]from (.*?)(?: [+] (?:(1) other|(\\d+) others))?[)]$";
				this.repos = new HashSet<>();
				for (String repo : repos) {
					this.repos.add(repo);
				}
			} else {
				this.repos = null;
			}
			p = Pattern.compile(re);
		}

		@Override
		public boolean matches(IJavaCompletionProposal value) {
			if (value == null || value.getDisplayString() == null) {
				return false;
			}
			final Matcher m = p.matcher(value.getDisplayString());
			if (!m.find()) {
				return false;
			}
			if (repos == null || repos.isEmpty()) {
				return true;
			}
			if (!repos.contains(m.group(1))) {
				return false;
			}
			if (m.group(2) != null) {
				return repos.size() == 2;
			}
			if (m.group(3) != null) {
				int v = Integer.valueOf(m.group(3));
				return repos.size() == v + 1;
			}
			return repos.size() == 1;
		}
	}

	static Condition<IJavaCompletionProposal> suggestsBundle(String bundle, String... repos) {
		return new MatchDisplayString(bundle, repos);
	}

	static Condition<IJavaCompletionProposal> withRelevance(final int relevance) {
		return new Condition<IJavaCompletionProposal>("Suggestion has relevance " + relevance) {
			@Override
			public boolean matches(IJavaCompletionProposal value) {
				return value != null && value.getRelevance() == relevance;
			}
		};
	}

	class FakeInvocationContext implements IInvocationContext {

		NodeFinder	fNodeFinder;
		int			fSelectionOffset;
		int			fSelectionLength;

		public FakeInvocationContext() {
			this(0, 0);
		}

		public FakeInvocationContext(int offset, int length) {
			fSelectionOffset = offset;
			fSelectionLength = length;
		}

		@Override
		public ICompilationUnit getCompilationUnit() {
			fail("Fake method not implemented - should not be called during testing");
			return null;
		}

		@Override
		public int getSelectionOffset() {
			return fSelectionOffset;
		}

		@Override
		public int getSelectionLength() {
			return fSelectionLength;
		}

		@Override
		public CompilationUnit getASTRoot() {
			return cu;
		}

		/*
		 * Copied from
		 * org.eclipse.jdt.internal.ui.text.correction.AssistContext;
		 * @see
		 * org.eclipse.jdt.ui.text.java.IInvocationContext#getCoveringNode()
		 */
		@Override
		public ASTNode getCoveringNode() {
			if (fNodeFinder == null) {
				fNodeFinder = new NodeFinder(getASTRoot(), getSelectionOffset(), getSelectionLength());
			}
			return fNodeFinder.getCoveringNode();
		}

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.jdt.ui.text.java.IInvocationContext#getCoveredNode()
		 */
		@Override
		public ASTNode getCoveredNode() {
			if (fNodeFinder == null) {
				fNodeFinder = new NodeFinder(getASTRoot(), getSelectionOffset(), getSelectionLength());
			}
			return fNodeFinder.getCoveredNode();
		}
	}

	class FakeBndBuildPathHandler extends BndBuildPathHandler {

		public FakeBndBuildPathHandler(IInvocationContext context) {
			super(context);
		}

		@Override
		void loadFileInfo() {
			fail("Fake method not implemented - should not be called during testing");
		}

		@Override
		void loadModel() {
			fail("Fake method not implemented - should not be called during testing");
		}

		@Override
		public List<VersionedClause> getBuildPath() {
			return buildPath;
		}

		@Override
		public boolean containsBundle(String bundle) throws CoreException {
			if (containsBundleException != null) {
				throw (CoreException) containsBundleException.fillInStackTrace();
			}
			for (VersionedClause versionedBundle : buildPath) {
				if (versionedBundle.getName()
					.equals(bundle)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public void addBundle(VersionedClause versionedBundle) {
			buildPath.add(versionedBundle);
		}
	}

	class FakeImportPackageQuickFixProcessor extends ImportPackageQuickFixProcessor {

		@Override
		List<RepositoryPlugin> listRepositories() {
			return plugins;
		}

		@Override
		Repository getWorkspaceRepo() throws Exception {
			return workspaceRepo;
		}

		@Override
		BndBuildPathHandler getBuildPathHandler(IInvocationContext context) {
			return new FakeBndBuildPathHandler(context);
		}
	}
}
