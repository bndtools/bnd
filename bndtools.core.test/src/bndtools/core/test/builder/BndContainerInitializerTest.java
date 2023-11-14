package bndtools.core.test.builder;

import static bndtools.core.test.utils.TaskUtils.importFodder;
import static org.eclipse.jdt.core.compiler.IProblem.DiscouragedReference;
import static org.eclipse.jdt.core.compiler.IProblem.ImportNotFound;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.bndtools.api.BndtoolsConstants;
import org.bndtools.test.assertj.eclipse.jdt.core.compiler.imarker.JavaProblemMarkerAssert;
import org.bndtools.test.assertj.eclipse.jdt.core.compiler.iproblem.IProblemMap;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.common.exceptions.Exceptions;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import bndtools.central.Central;
import bndtools.core.test.utils.LoggingProgressMonitor;
import bndtools.core.test.utils.TaskUtils;
import bndtools.core.test.utils.WorkbenchTest;

@ExtendWith(SoftAssertionsExtension.class)
@WorkbenchTest
public class BndContainerInitializerTest {

	IPackageFragment			pack;
	// Injected by WorkbenchExtension
	// static WorkspaceImporter importer;
	IProject					eclipseProject;
	Project						bndProject;
	IJavaProject				javaProject;
	// Injected by SoftAssertionsExtension
	@InjectSoftAssertions
	protected AllSoftAssertions	softly;

	List<IProject>				toDelete;

	@InjectService(filter = "(component.name=org.bndtools.builder.classpath.BndContainerInitializer)")
	static ClasspathContainerInitializer	bci;

	@BeforeAll
	static void beforeAllBase() throws Exception {
		importFodder();
	}

	TestInfo info;

	@BeforeEach
	void beforeEach(TestInfo info) throws Exception {
		toDelete = new ArrayList<>();
		this.info = info;

		TaskUtils.createTestDirectories("test");
		createDefaultProject("test", "-nobundles: true");

		TaskUtils.buildClean("beforeEach");
	}

	@AfterEach
	void afterEach() throws Exception {
		// Batch it up to reduce number of resource notifications
		ResourcesPlugin.getWorkspace()
			.run(monitor -> {
				for (IProject project : toDelete) {
					try {
						project.delete(true, true, null);
					} catch (Exception e) {
						System.err.println("Failed to remove project: " + project + ", " + e);
						e.printStackTrace();
					}
				}
			}, null);
	}

	IClasspathEntry[] containerEntries() {
		try {
			IClasspathContainer c = JavaModelManager.getJavaModelManager()
				.containerGet(javaProject, BndtoolsConstants.BND_CLASSPATH_ID);
			Assertions.assertThat(c)
				.as("bndContainer")
				.isNotNull();
			Assertions.assertThat(c.getClass()
				.getName())
				.as("bndContainer.type")
				.endsWith("BndContainer");
			return c.getClasspathEntries();
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	@Test
	void addsProjects_andLibraries_inOrder() throws Exception {
		Project proj1 = createBndProject("other.proj.one",
		//@formatter:off
			"-buildpath: bndtools.core.test.fodder.iface,local-proj,bndtools.core.test.fodder.simple\n" +
			"-privatepackage: iface.bundle,some.priv.pack\n" +
			"Export-Package: some.exported.pack\n" +
			""
		//@formatter:on
		);

		TaskUtils.createEmptyClass(proj1, "some.excluded.pack", "ExcludedClass");
		TaskUtils.createEmptyClass(proj1, "some.priv.pack", "PrivClass");
		TaskUtils.createEmptyClass(proj1, "some.exported.pack", "ExportedClass");
		TaskUtils.buildIncremental("newProj");
		TaskUtils.addBundlesToBuildpath(bndProject, "bndtools.core.test.fodder.simple", "local-proj");

		// Test dependencies aren't treated as test dependencies in Eclipse by
		// Bndtools unless the project itself has a test folder.
		IFolder testFolder = eclipseProject.getFolder("test");
		if (!testFolder.exists()) {
			testFolder.create(true, true, new LoggingProgressMonitor("create()ing test folder for " + eclipseProject));
		}
		IFolder testBinFolder = eclipseProject.getFolder("bin_test");
		if (!testBinFolder.exists()) {
			testBinFolder.create(true, true,
				new LoggingProgressMonitor("create()ing test bin folder for " + eclipseProject));
		}
		IPackageFragmentRoot root = javaProject.getPackageFragmentRoot(testFolder);
		IPackageFragment packFragment = root.createPackageFragment("testpkg", false,
			new LoggingProgressMonitor("createPackageFragment() \"testpkg\""));

		TaskUtils.addBundlesToTestpath(bndProject, "other.proj.one", "bndtools.core.test.fodder.iface");
		// ,
		// "bndtools.core.test.fodder.iface");
		IClasspathEntry[] entries = containerEntries();// .getResolvedClasspath(false);

		int i = 0;
		softly.assertThat(entries[i])
			.as(String.valueOf(i++))
			.isLibrary()
			.isNotExported()
			.hasPathThat()
			.asPortableString()
			.matches("^/cnf/local/bndtools.core.test.fodder.simple/.*jar$");

		// Project should add both the source and the jar
		softly.assertThat(entries[i])
			.as(String.valueOf(i++))
			.isProject()
			.refersToProject("local-proj")
			.isNotExported();

		softly.assertThat(entries[i])
			.as(String.valueOf(i++))
			.isLibrary()
			.isNotExported()
			.hasPathThat()
			.asPortableString()
			.matches("^/local-proj/generated/local-proj.jar");

		softly.assertThat(entries[i])
			.as(String.valueOf(i++))
			.isProject()
			.refersToProject(proj1.getName())
			.isNotExported()
			.isTest();

		softly.assertThat(entries[i])
			.as(String.valueOf(i++))
			.isLibrary()
			.isNotExported()
			.isTest()
			.hasPathThat()
			.asPortableString()
			.matches("^/" + proj1.getName() + "/generated/" + proj1.getName() + ".jar$");

		softly.assertThat(entries[i])
			.as(String.valueOf(i++))
			.isLibrary()
			.isNotExported()
			.isTest()
			.hasPathThat()
			.asPortableString()
			.matches("^/cnf/local/bndtools.core.test.fodder.iface/.*jar$");
	}

	@Test
	void directDependencies() {
		TaskUtils.addBundlesToBuildpath(bndProject, "bndtools.core.test.fodder.simple", "local-proj");
		assertImports(
		//@formatter:off
			pack("simple.internal.InternalClass", DiscouragedReference, "direct library"),
			pack("iface.bundle.MyInterface", ImportNotFound, "unknown"),
			pack("iface.embedded.Embedded", DiscouragedReference, "direct library"),
			pack("iface.embedded.Unknown", ImportNotFound, "unknown"),
			pack("my.local.ws.pkg.WSClass", "direct project"),
			pack("java.util.Map", "JRE")
			//@formatter:on
		);
	}

	void createDefaultProject(String name, String bndFile) {
		try {
			bndProject = createBndProject(name, bndFile);
			eclipseProject = Central.getProject(bndProject)
				.get();
			javaProject = JavaCore.create(eclipseProject);

			IFolder sourceFolder = eclipseProject.getFolder("src");
			if (!sourceFolder.exists()) {
				sourceFolder.create(true, true, new LoggingProgressMonitor("create()ing source folder"));
			}

			IPackageFragmentRoot root = javaProject.getPackageFragmentRoot(sourceFolder);
			pack = root.createPackageFragment("blah", false,
				new LoggingProgressMonitor("createPackageFragment() \"blah\""));
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	Project createBndProject(String name) {
		return createBndProject(name, "");
	}

	Project createBndProject(String name, String bndFileContents) {
		try {
			Workspace bndWS = Central.getWorkspace();
			File bndProjectDir = new File(bndWS.getBase(), name);
			bndProjectDir.mkdir();
			File bndFile = new File(bndProjectDir, "bnd.bnd");
			boolean exists = bndFile.exists();
			IO.write(bndFileContents.getBytes(), bndFile);
			Project bndProject = bndWS.getProject(name);
			if (exists) {
				bndProject.refresh();
			}
			@SuppressWarnings("restriction")
			IProject eclipseProject = bndtools.central.sync.WorkspaceSynchronizer.createProject(bndProjectDir,
				bndProject, true, new LoggingProgressMonitor("creating eclipse project from bnd project: " + name));
			toDelete.add(eclipseProject);
			return bndProject;
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	static class Import {
		final String	pack;
		final int		problem;
		final String	desc;

		Import(String pack, int problem, String desc) {
			this.pack = pack;
			this.problem = problem;
			this.desc = desc;
		}

		@Override
		public String toString() {
			return pack + " " + IProblemMap.getProblemDescription(problem) + ", " + desc;
		}
	}

	Import pack(String pack, String desc) {
		return new Import(pack, 0, desc);
	}

	Import pack(String pack, int problem, String desc) {
		return new Import(pack, problem, desc);
	}

	@Test
	void transitiveDependencies() {
		Project proj1 = createBndProject("other.proj.one",
		//@formatter:off
			"-buildpath: bndtools.core.test.fodder.iface,local-proj,bndtools.core.test.fodder.simple\n" +
			"-privatepackage: iface.bundle,some.priv.pack\n" +
			"Export-Package: some.exported.pack\n" +
			""
		//@formatter:on
		);

		TaskUtils.createEmptyClass(proj1, "some.excluded.pack", "ExcludedClass");
		TaskUtils.createEmptyClass(proj1, "some.priv.pack", "PrivClass");
		TaskUtils.createEmptyClass(proj1, "some.exported.pack", "ExportedClass");

		Project proj2 = createBndProject("other.proj.two",
		//@formatter:off
			"-buildpath: local-proj\n" +
			"Export-Package: my.local.ws.pkg\n"
		//@formatter:on
		);

		TaskUtils.addBundlesToBuildpath(bndProject, "bndtools.core.test.fodder.simple", "other.proj.one",
			"other.proj.two");
		TaskUtils.buildIncremental("init");

		assertImports(
		//@formatter:off
			pack("simple.MyClass", "direct library"),
			pack("some.exported.pack.ExportedClass", "direct project"),
			pack("some.priv.pack.PrivClass", DiscouragedReference, "direct project"),
			// The current implementation still makes this class visible with "discouraged".
//			pack("some.excluded.pack.ExcludedClass", ImportNotFound, "direct project"),
			pack("iface.bundle.MyInterface", DiscouragedReference, "transitive library"),
			pack("iface.embedded.Unknown", ImportNotFound, "unknown"),
			pack("my.local.ws.pkg.WSClass", "transitive project"),
			pack("java.util.Map", "JRE")
		//@formatter:on
		);
	}

	@Test
	void transitiveProjectDependency_honorsExportOverPrivate_regardlessOfBuildpathOrder_whenIncludedViaMultiplePaths() {
		Project proj1 = createBndProject("other.proj.one",
		//@formatter:off
			"-buildpath: local-proj\n" +
			"-privatepackage: my.local.ws.pkg\n" +
			""
		//@formatter:on
		);

		Project proj2 = createBndProject("other.proj.two",
		//@formatter:off
			"-buildpath: local-proj\n" +
			"Export-Package: my.local.ws.pkg\n" +
			""
		//@formatter:on
		);

		TaskUtils.addBundlesToBuildpath(bndProject, "other.proj.one", "other.proj.two");
		TaskUtils.buildIncremental("init");

		assertImports(
		//@formatter:off
			pack("my.local.ws.pkg.WSClass", "transitive project, private in one and public in two")
		//@formatter:on
		);

		// Now swap the order
		TaskUtils.clearBuildpath(bndProject);
		TaskUtils.addBundlesToBuildpath(bndProject, "other.proj.two", "other.proj.one");
		TaskUtils.buildIncremental("init");

		assertImports(
		//@formatter:off
			pack("my.local.ws.pkg.WSClass", "transitive project reversed, private in one and public in two")
		//@formatter:on
		);
	}

	@Test
	void transitiveProjectDependency_honorsPrivateOverExcluded_regardlessOfBuildpathOrder_whenIncludedViaMultiplePaths() {
		Project proj1 = createBndProject("other.proj.one",
		//@formatter:off
			"-buildpath: local-proj\n" +
			""
		//@formatter:on
		);

		Project proj2 = createBndProject("other.proj.two",
		//@formatter:off
			"-buildpath: local-proj\n" +
			"-privatepackage: my.local.ws.pkg\n" +
			""
		//@formatter:on
		);

		TaskUtils.addBundlesToBuildpath(bndProject, "other.proj.one", "other.proj.two");
		TaskUtils.buildIncremental("init");

		assertImports(
		//@formatter:off
			pack("my.local.ws.pkg.WSClass", DiscouragedReference, "transitive project, excluded in one and private in two")
		//@formatter:on
		);

		// Now swap the order
		TaskUtils.clearBuildpath(bndProject);
		TaskUtils.addBundlesToBuildpath(bndProject, "other.proj.two", "other.proj.one");
		TaskUtils.buildIncremental("init");

		assertImports(
		//@formatter:off
			pack("my.local.ws.pkg.WSClass", DiscouragedReference, "transitive project reversed, excluded in one and private in two")
		//@formatter:on
		);
	}

	@Disabled("The current implementation doesn't do this, but maybe it should?")
	@Test
	void directProjectDependency_honorsExcluded() {
		Project proj1 = createBndProject("other.proj.one",
		//@formatter:off
		"-privatepackage: some.priv.pack\n" +
		"Export-Package: some.exported.pack\n" +
		""
		//@formatter:on
		);

		TaskUtils.createEmptyClass(proj1, "some.excluded.pack", "A");
		TaskUtils.createEmptyClass(proj1, "some.priv.pack", "B");
		TaskUtils.createEmptyClass(proj1, "some.exported.pack", "C");

		TaskUtils.addBundlesToBuildpath(bndProject, "other.proj.one");
		TaskUtils.buildIncremental("init");

		assertImports(
		//@formatter:off
			pack("some.excluded.pack.A", ImportNotFound, "excluded"),
			pack("some.priv.pack.B", DiscouragedReference, "private"),
			pack("some.exported.pack.C", "exported")
		//@formatter:on
		);
	}

	@Test
	void transitiveProjectDependency_honorsExcluded() {
		Project proj1 = createBndProject("other.proj.one",
		//@formatter:off
		"-privatepackage: some.priv.pack\n" +
		"Export-Package: some.exported.pack\n" +
		""
		//@formatter:on
		);

		TaskUtils.createEmptyClass(proj1, "some.excluded.pack", "A");
		TaskUtils.createEmptyClass(proj1, "some.priv.pack", "B");
		TaskUtils.createEmptyClass(proj1, "some.exported.pack", "C");

		Project proj2 = createBndProject("other.proj.two",
		//@formatter:off
		"-buildpath: other.proj.one\n" +
		"-privatepackage: some.priv.pack\n" +
		"Export-Package: some.exported.pack\n" +
		""
		//@formatter:on
		);
		TaskUtils.addBundlesToBuildpath(bndProject, "other.proj.two");
		TaskUtils.buildIncremental("init");

		assertImports(
		//@formatter:off
			pack("some.excluded.pack.A", ImportNotFound, "excluded"),
			pack("some.priv.pack.B", DiscouragedReference, "private"),
			pack("some.exported.pack.C", "exported")
		//@formatter:on
		);
	}

	@Test
	void transitiveLibraryDependency_honorsExportOverPrivate_regardlessOfBuildpathOrder_whenIncludedViaMultiplePaths() {
		Project proj1 = createBndProject("other.proj.one",
		//@formatter:off
			"-buildpath: bndtools.core.test.fodder.simple\n" +
			"-privatepackage: simple\n" +
			""
		//@formatter:on
		);

		Project proj2 = createBndProject("other.proj.two",
		//@formatter:off
			"-buildpath: bndtools.core.test.fodder.simple\n" +
			"Export-Package: simple\n" +
			""
		//@formatter:on
		);

		TaskUtils.addBundlesToBuildpath(bndProject, "other.proj.one", "other.proj.two");
		TaskUtils.buildIncremental("init");

		assertImports(
		//@formatter:off
			pack("simple.MyClass", "transitive library, private in one and exported in two")
		//@formatter:on
		);

		// Now swap the order
		TaskUtils.clearBuildpath(bndProject);
		TaskUtils.addBundlesToBuildpath(bndProject, "other.proj.two", "other.proj.one");
		TaskUtils.buildIncremental("init");

		assertImports(
		//@formatter:off
			pack("simple.MyClass", "transitive library reversed, private in one and exported in two")
		//@formatter:on
		);
	}

	@Test
	void transitiveLibraryDependency_honorsPrivateOverExcluded_regardlessOfBuildpathOrder_whenIncludedViaMultiplePaths() {
		Project proj1 = createBndProject("other.proj.one",
		//@formatter:off
			"-buildpath: bndtools.core.test.fodder.simple\n" +
			""
		//@formatter:on
		);

		Project proj2 = createBndProject("other.proj.two",
		//@formatter:off
			"-buildpath: bndtools.core.test.fodder.simple\n" +
			"-privatepackage: simple\n" +
			""
		//@formatter:on
		);

		TaskUtils.addBundlesToBuildpath(bndProject, "other.proj.one", "other.proj.two");
		TaskUtils.buildIncremental("init");

		assertImports(
		//@formatter:off
			pack("simple.MyClass", DiscouragedReference, "transitive library, excluded in one and private in two")
		//@formatter:on
		);

		// Now swap the order
		TaskUtils.clearBuildpath(bndProject);
		TaskUtils.addBundlesToBuildpath(bndProject, "other.proj.two", "other.proj.one");
		TaskUtils.buildIncremental("init");

		assertImports(
		//@formatter:off
			pack("simple.MyClass", DiscouragedReference, "transitive library reversed, excluded in one and private in two")
		//@formatter:on
		);
	}

	@Test
	void transitiveLibraryDependency_mergesExports_whenIncludedViaMultiplePaths() {
		Project proj1 = createBndProject("other.proj.one",
		//@formatter:off
			"-buildpath: bndtools.core.test.fodder.simple\n" +
			"Export-Package: simple.pkg"
		//@formatter:on
		);

		Project proj2 = createBndProject("other.proj.two",
		//@formatter:off
			"-buildpath: bndtools.core.test.fodder.simple\n" +
			"Export-Package: simple\n" +
			""
		//@formatter:on
		);

		TaskUtils.addBundlesToBuildpath(bndProject, "other.proj.one", "other.proj.two");
		TaskUtils.buildIncremental("init");

		assertImports(
		//@formatter:off
			pack("simple.pkg.RecursiveClass", "transitive library, exported by one"),
			pack("simple.MyClass", "transitive library, exported by two")
		//@formatter:on
		);
	}

	@Test
	void transitiveLibraryDependency_honorsExcluded() {
		Project proj1 = createBndProject("other.proj.one",
		//@formatter:off
		"-privatepackage: some.priv.pack\n" +
		"Export-Package: some.exported.pack\n" +
		""
		//@formatter:on
		);

		TaskUtils.createEmptyClass(proj1, "some.excluded.pack", "A");
		TaskUtils.createEmptyClass(proj1, "some.priv.pack", "B");
		TaskUtils.createEmptyClass(proj1, "some.exported.pack", "C");

		Project proj2 = createBndProject("other.proj.two",
		//@formatter:off
		"-buildpath: bndtools.core.test.fodder.simple\n" +
		"-privatepackage: simple\n" +
		"Export-Package: simple.pkg\n" +
		""
		//@formatter:on
		);
		TaskUtils.addBundlesToBuildpath(bndProject, "other.proj.two");
		TaskUtils.buildIncremental("init");

		assertImports(
		//@formatter:off
			pack("simple.internal.InternalClass", ImportNotFound, "excluded"),
			pack("simple.MyClass", DiscouragedReference, "private"),
			pack("simple.pkg.RecursiveClass", "exported")
		//@formatter:on
		);
	}

	static Condition<IClasspathEntry> library(String name) {
		return new Condition<IClasspathEntry>("Library with name " + name) {
			@Override
			public boolean matches(IClasspathEntry value) {
				return value != null && value.getEntryKind() == IClasspathEntry.CPE_LIBRARY && value.getPath()
					.toPortableString()
					.contains(name);
			}
		};
	}

	@Test
	void unexportedTransitiveLibraries_areNotAddedToClasspath() {
		Project proj1 = createBndProject("other.proj.one",
		//@formatter:off
		"-buildpath: bndtools.core.test.fodder.simple\n" +
		""
		//@formatter:on
		);

		TaskUtils.addBundlesToBuildpath(bndProject, "other.proj.one");
		TaskUtils.buildIncremental("init");

		IClasspathEntry[] entries = containerEntries();

		softly.assertThat(entries)
			.haveExactly(0, library("bndtools.core.test.fodder.simple"));
	}

	@Test
	void testLibraries_areNotVisibleToMainSource() throws Exception {
		TaskUtils.createTestDirectories("other.proj.one");
		createDefaultProject("other.proj.one",
		//@formatter:off
		"-testpath: bndtools.core.test.fodder.simple\n" +
		"-nobundles: true\n" +
		""
		//@formatter:on
		);

		TaskUtils.buildIncremental("after project refresh");
		TaskUtils.assertNoBndMarkers();

		assertImports(
		//@formatter:off
			pack("simple.internal.InternalClass", ImportNotFound, "private"),
			pack("simple.MyClass", ImportNotFound, "exported")
		//@formatter:on
		);
	}

	@Test
	void testLibraries_areVisibleToTestSource() throws CoreException {
		TaskUtils.createTestDirectories("other.proj.one");
		createDefaultProject("other.proj.one",
		//@formatter:off
		"-testpath: bndtools.core.test.fodder.simple\n" +
		""
		//@formatter:on
		);

		IFolder sourceFolder = eclipseProject.getFolder("test");
		if (!sourceFolder.exists()) {
			sourceFolder.create(true, true, new LoggingProgressMonitor("create()ing source folder"));
		}
		IPackageFragmentRoot root = javaProject.getPackageFragmentRoot(sourceFolder);
		pack = root.createPackageFragment("test", false,
			new LoggingProgressMonitor("createPackageFragment() \"test\""));

		assertImports(
		//@formatter:off
			pack("simple.internal.InternalClass", DiscouragedReference, "private"),
			pack("simple.MyClass", "exported")
		//@formatter:on
		);
	}

	@Test
	void testProjects_areNotVisibleToMainSource() {
		Project proj1 = createBndProject("other.proj.one",
		//@formatter:off
		"-privatepackage: some.priv.pack\n" +
		"Export-Package: some.exported.pack\n" +
		""
		//@formatter:on
		);

		TaskUtils.createEmptyClass(proj1, "some.excluded.pack", "A");
		TaskUtils.createEmptyClass(proj1, "some.priv.pack", "B");
		TaskUtils.createEmptyClass(proj1, "some.exported.pack", "C");

		// If you don't create non-empty test directories, then Bndtools
		// currently doesn't add the test=true flag to the -testpath
		// IClasspathEntry entries
		// Refer https://github.com/bndtools/bnd/issues/5089
		TaskUtils.createTestDirectories("other.proj.two");
		createDefaultProject("other.proj.two",
		//@formatter:off
			"-testpath: other.proj.one\n" +
			""
		//@formatter:on
		);

		TaskUtils.assertNoBndMarkers();

		assertImports(
		//@formatter:off
			pack("some.priv.pack.B", ImportNotFound, "private"),
			pack("some.exported.pack.C", ImportNotFound, "exported")
		//@formatter:on
		);
	}

	@Test
	void testProjects_areVisibleToTestSource() throws CoreException {
		Project proj1 = createBndProject("other.proj.one",
		//@formatter:off
		"-privatepackage: some.priv.pack\n" +
		"Export-Package: some.exported.pack\n" +
		""
		//@formatter:on
		);

		TaskUtils.createEmptyClass(proj1, "some.excluded.pack", "A");
		TaskUtils.createEmptyClass(proj1, "some.priv.pack", "B");
		TaskUtils.createEmptyClass(proj1, "some.exported.pack", "C");

		createDefaultProject("test",
		//@formatter:off
		"-testpath: other.proj.one\n" +
		"-nobundles: true\n" +
		""
		//@formatter:on
		);

		TaskUtils.buildIncremental("after setup");

		TaskUtils.assertNoBndMarkers();
		IFolder sourceFolder = eclipseProject.getFolder("test");
		if (!sourceFolder.exists()) {
			sourceFolder.create(true, true, new LoggingProgressMonitor("create()ing source folder"));
		}
		IPackageFragmentRoot root = javaProject.getPackageFragmentRoot(sourceFolder);
		pack = root.createPackageFragment("blah", false,
			new LoggingProgressMonitor("createPackageFragment() \"blah\""));

		assertImports(
		//@formatter:off
			//Currently this will be visible to downstream. Should not be?
			//pack("some.excluded.pack.A", ImportNotFound, "excluded"),
			pack("some.priv.pack.B", DiscouragedReference, "private"),
			pack("some.exported.pack.C", "exported")
		//@formatter:on
		);
	}

	@Test
	void noBundleProject_withExportContents_areVisible() throws CoreException {
		Project proj1 = createBndProject("other.proj.one",
		//@formatter:off
		"-exportcontents: *\n" +
		"-nobundles: true\n" +
		""
		//@formatter:on
		);

		TaskUtils.createEmptyClass(proj1, "some.exported.pack1", "A");
		TaskUtils.createEmptyClass(proj1, "some.exported.pack2", "B");
		TaskUtils.createEmptyClass(proj1, "some.exported.pack3", "C");

		createDefaultProject("test",
		//@formatter:off
		"-testpath: other.proj.one\n" +
		"-nobundles: true\n" +
		""
		//@formatter:on
		);

		TaskUtils.buildIncremental("after setup");

		TaskUtils.assertNoBndMarkers();
		IFolder sourceFolder = eclipseProject.getFolder("test");
		if (!sourceFolder.exists()) {
			sourceFolder.create(true, true, new LoggingProgressMonitor("create()ing source folder"));
		}
		IPackageFragmentRoot root = javaProject.getPackageFragmentRoot(sourceFolder);
		pack = root.createPackageFragment("blah", false,
			new LoggingProgressMonitor("createPackageFragment() \"blah\""));

		assertImports(
		//@formatter:off
			pack("some.exported.pack1.A", "exported"),
			pack("some.exported.pack2.B", "exported"),
			pack("some.exported.pack3.C", "exported")
		//@formatter:on
		);
	}

	private void dumpEntries() {
		try {
			final String context = "dumpEntries(" + info.getDisplayName() + ", " + javaProject.getElementName()
				+ "): \n\t";
			System.err.println(context + Arrays.stream(javaProject.getRawClasspath())
				.map(Object::toString)
				.collect(Collectors.joining(",\n\t")));

			System.err.println(context + Arrays.stream(containerEntries())
				.map(Object::toString)
				.collect(Collectors.joining(",\n\t")));

			System.err.println(context + Arrays.stream(javaProject.getResolvedClasspath(true))
				.map(Object::toString)
				.collect(Collectors.joining(",\n\t")));
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	String indent(String source) {
		return Strings.splitAsStream(source, Pattern.compile("\\n"))
			.map(x -> "  " + x + "\n")
			.collect(Collectors.joining());
	}

	static String markerToString(IMarker marker) {
		try {
			return marker.getAttribute("message") + ", " + marker.getAttribute("lineNumber");
		} catch (CoreException e) {
			throw Exceptions.duck(e);
		}
	}

	static Comparator<? super IMarker> BY_LINE = new Comparator<IMarker>() {
		@Override
		public int compare(IMarker a, IMarker b) {
			try {
				return Integer.compare((int) a.getAttribute("lineNumber"), (int) b.getAttribute("lineNumber"));
			} catch (CoreException e) {
				throw Exceptions.duck(e);
			}
		}
	};

	static final Pattern				DOT				= Pattern.compile("\\.");

	private void assertImports(Import... imports) {
		if (imports == null || imports.length == 0) {
			throw new IllegalArgumentException("No imports provided - this is almost certainly not what you want");
		}
		try {
			String source =
			//@formatter:off
				"package " + pack.getElementName() + ";\n" +
				Arrays.stream(imports).map(x -> "import " + x.pack + ";\n").collect(Collectors.joining()) +
				"@SuppressWarnings(\"unused\")\n" +
				"public class Blah {}"
			//@formatter:on
			;
			ICompilationUnit icu = TaskUtils.createCompilationUnit(pack, "Blah", source);
			// In testing I occasionally got build errors because of jar file
			// locking at the filesystem level. This is probably only an issue
			// on Windows. Add this little wait here to try and prevent that.
			// Unfortunately this is slow... up to a second.
			TaskUtils.waitForAutobuild("Added ICU");

			// Using TaskUtils.buildAST() did not suffer from the locking issue
			// above, however I discovered that ASTParser.createAST() (on which
			// it depends) doesn't honour the test=true attribute, which breaks
			// a number of the tests above that are trying to test that
			// behavior. Need the full JavaBuilder behaviour to get that.
			TaskUtils.buildClean("Added ICU");

			final int problemCount = (int) Arrays.stream(imports)
				.filter(x -> x.problem != 0)
				.count();
			IMarker[] problems = icu.getResource()
				.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_ZERO);
			Arrays.sort(problems, BY_LINE);

			if (problems.length != problemCount) {
				softly.fail(
					"%nExpecting code:%n%s%n%s%n---%nto have:%n <%d>%nproblems, but it has:%n <%d>%nProblems:%n %s",
					icu.getPath(), indent(source), problemCount, problems.length, Arrays.stream(problems)
						.map(BndContainerInitializerTest::markerToString)
						.collect(Collectors.joining(",\n ")));
			} else {
				int problem = 0;
				AtomicBoolean hadFailures = new AtomicBoolean(false);
				softly.setAfterAssertionErrorCollected(error -> hadFailures.set(true));
				for (int i = 0; i < imports.length; i++) {
					Import imp = imports[i];
					int code = imp.problem;
					if (code == 0) {
						continue;
					}
					IMarker p = problems[problem++];
					softly.assertThat(p)
						.as(imp.desc + " (" + imp.pack + ")")
						.hasType(JavaProblemMarkerAssert.JAVA_PROBLEM);
					// Unfortunately, hasType() breaks the soft assertion chain
					// by returning a non-soft-asserting assertion.
					softly.proxy(JavaProblemMarkerAssert.class, IMarker.class, p)
						.as(imp.desc + " (" + imp.pack + ")")
						.hasProblemID(imp.problem)
						.hasLineNumber(i + 2);
				}
				if (hadFailures.get()) {
					softly.fail("\nFull list of markers: " + Arrays.stream(problems)
						.map(BndContainerInitializerTest::markerToString)
						.collect(Collectors.joining("\n")));
					softly.fail("\nSource code:\n" + source);
				}
			}
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}
}
