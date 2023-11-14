package bndtools.core.test.builder;

import static bndtools.core.test.utils.TaskUtils.log;
import static org.bndtools.api.BndtoolsConstants.MARKER_BND_PATH_PROBLEM;
import static org.eclipse.core.resources.IResource.DEPTH_ZERO;

import java.util.function.Consumer;

import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.bndtools.builder.facade.ProjectBuilderDelegate;
import org.bndtools.test.assertj.bndtools.imarker.BndPathProblemAssert;
import org.bndtools.test.assertj.eclipse.resources.ResourcesSoftAssertions;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.test.common.annotation.InjectService;

import aQute.bnd.build.Project;
import aQute.bnd.deployer.repository.LocalIndexedRepo;
import aQute.lib.io.IO;
import bndtools.central.Central;
import bndtools.core.test.utils.LoggingProgressMonitor;
import bndtools.core.test.utils.TaskUtils;
import bndtools.core.test.utils.WorkbenchTest;

@ExtendWith(SoftAssertionsExtension.class)
@WorkbenchTest
public class BndtoolsBuilderTest {

	static IPackageFragment						pack;
	static Class<? extends IQuickFixProcessor>	sutClass;
	// Injected by WorkbenchExtension
	// static WorkspaceImporter importer;
	static IProject								eclipseProject;
	static Project								bndProject;
	// Injected by SoftAssertionsExtension
	@InjectSoftAssertions
	protected ResourcesSoftAssertions			softly;

	@InjectService(filter = "(component.name=org.bndtools.builder.impl.BndtoolsBuilder)")
	static ProjectBuilderDelegate				builder;

	@BeforeAll
	static void beforeAllBase() throws Exception {
		final LocalIndexedRepo localRepo = (LocalIndexedRepo) Central.getWorkspace()
			.getRepository("Local Index");

		if (localRepo == null) {
			log("Central.getWorkspace(): " + Central.getWorkspace()
				.getBase());
			TaskUtils.dumpWorkspace();
			throw new IllegalStateException("Could not find Local Index");
		}

		TaskUtils.updateWorkspace("beforeAllBase()");

		// * create project test with 1 class - imported by WorkbenchTest
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
		eclipseProject.open(new LoggingProgressMonitor("open()ing project"));
		IJavaProject javaProject = JavaCore.create(eclipseProject);

		IFolder sourceFolder = eclipseProject.getFolder("src");
		if (!sourceFolder.exists()) {
			sourceFolder.create(true, true, new LoggingProgressMonitor("create()ing source folder"));
		}

		IPackageFragmentRoot root = javaProject.getPackageFragmentRoot(sourceFolder);
		pack = root.createPackageFragment("test", false,
			new LoggingProgressMonitor("createPackageFragment() \"test\""));
	}

	@BeforeEach
	void beforeEach() {
		TaskUtils.buildClean("beforeEach");
	}

	@Test
	void clean_emptiesGenerated() throws Exception {
		TaskUtils.buildIncremental("clean");
		IFolder generated = eclipseProject.getFolder("generated");
		// Want to check that the entire generated folder is recursively removed
		// - create some more things in the folder.
		IFile dummy = generated.getFile("dummy.txt");
		dummy.create(IO.stream("dummy content"), true, null);
		IFolder subFolder = generated.getFolder("subFolder");
		subFolder.create(true, true, null);
		IFile subDummy = subFolder.getFile("dummy2.txt");
		subDummy.create(IO.stream("other content"), true, null);

		// Preconditions
		softly.assertThat(generated.getFile("test.jar"))
			.exists();
		softly.assertThat(subFolder)
			.as("subFolder")
			.exists();
		softly.assertThat(subDummy)
			.as("subDummy")
			.exists();

		// * Clean
		TaskUtils.buildClean("clean_emptiesGenerated()");
		softly.assertThat(generated.members())
			.as("generated")
			.isNullOrEmpty();
		softly.assertThat(generated.getFile("test.jar"))
			.doesNotExist();
		softly.assertThat(subFolder)
			.doesNotExist();
		softly.assertThat(subDummy)
			.doesNotExist();
	}

	private IFile getBuildFile() {
		return ResourcesPlugin.getWorkspace()
			.getRoot()
			.getProject("cnf")
			.getFile("build.bnd");
	}

	private IFile getBndFile() {
		return eclipseProject.getFile("bnd.bnd");
	}

	private IMarker[] getBndMarkers(String type) throws CoreException {
		IMarker[] markers = getBndFile().findMarkers(type, false, DEPTH_ZERO);
		return markers;
	}

	// private IMarker getBndMarker(String type) throws CoreException {
	// IMarker[] markers = getBndMarkers(type);
	// Assertions.assertThat(markers)
	// .hasSize(1);
	// return markers[0];
	// }

	private long getJarModStamp() {
		IFolder generated = eclipseProject.getFolder("generated");
		if (generated.exists()) {
			IFile file = generated.getFile("test.jar");
			if (file.exists()) {
				return file.getModificationStamp();
			}
			return IResource.NULL_STAMP;
		}
		return IResource.NULL_STAMP;
	}

	private IFile getJarFile() {
		return eclipseProject.getFile("generated/test.jar");
	}

	Consumer<IMarker> nonExistingBundle() {
		return marker -> {
			ResourcesSoftAssertions softly = new ResourcesSoftAssertions();
			softly.assertThat(marker)
				.isError()
				.hasMessageContaining("non.existing.bundle")
				.hasMessageContainingMatch("(?i)not found")
				.hasType(BndPathProblemAssert.BND_PATH_PROBLEM)
				.hasBndtoolsContext("non.existing.bundle")
				.hasBndtoolsHeader("-buildpath")
				.hasBndtoolsProject("test");
			softly.assertAll();
		};
	}

	@Test
	void nonExistingBuildpath_generatesErrorMarker() throws CoreException {
		TaskUtils.addBundlesToBuildpath(bndProject, "non.existing.bundle");
		TaskUtils.buildIncremental("nonExistingBuildpath - turn on autobuild");

		softly.assertThat(getBndMarkers(MARKER_BND_PATH_PROBLEM))
			.as("markers")
			.anySatisfy(nonExistingBundle());

		TaskUtils.clearBuildpath(bndProject);
		TaskUtils.buildIncremental("nonExistingBuildpath - after");

		softly.assertThat(getBndMarkers(MARKER_BND_PATH_PROBLEM))
			.as("markers after clear")
			.noneSatisfy(nonExistingBundle());
	}

	@Test
	void deleteOutputFile_shouldRebuild() throws CoreException {
		TaskUtils.buildIncremental("deleteOutputFile");
		// TaskUtils.doAutobuildAndWait("deleteOutputFile_shouldRebuild");
		IFile jarFile = getJarFile();
		softly.assertThat(jarFile)
			.as("before")
			.exists();
		long stamp = getJarModStamp();

		jarFile.delete(true, null);

		// softly.assertThat(jarFile)
		// .as("after delete")
		// .doesNotExist();
		//
		TaskUtils.buildIncremental("deleteOutputFile");
		// TaskUtils.doAutobuildAndWait("deleteOutputFile_shouldRebuild");

		softly.assertThat(jarFile)
			.as("after build")
			.exists()
			.doesNotHaveModificationStamp(stamp);
	}

	@Test
	void compileError_preventsBuild() throws CoreException {
		long stamp = getJarModStamp();
		IFile file = eclipseProject.getFile("src/testpkg/Uncompilable.java");
		IFile jarFile = getJarFile();

		file.create(IO.stream("package testpkg; public class Uncompilable {}}"), true, null);
		// file.refreshLocal(DEPTH_ZERO, null);
		try {
			TaskUtils.buildIncremental("compileError - with the error");

			softly.assertThat(jarFile)
				.hasModificationStamp(stamp);
			// No marker is generated in this case.
			// IMarker marker = getBndMarker(MARKER_BND_PROBLEM);
			//
			// softly.assertThat(marker)
			// .as("marker")
			// .hasBndtoolsContext("blah");
		} finally {
			file.delete(true, null);
		}
		// todo: * remove compile error -> build again
		TaskUtils.buildIncremental("compileError - after delete");
		softly.assertThat(jarFile)
			.doesNotHaveModificationStamp(stamp);
	}

	// @Test
	// void addingVariableToBuild_causesRebuild() throws Exception {
	// TaskUtils.setAutobuild(true);
	// TaskUtils.waitForAutobuild("addingVariableToBuild");
	//
	// long stamp = getJarFile().getModificationStamp();
	//
	// IFile build = Central.getWorkspaceBuildFile();
	// UTF8Properties p = new UTF8Properties();
	// p.load(build.getContents());
	//
	// p.put("Foo", "foo");
	// try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
	// p.store(os);
	// byte[] data = os.toByteArray();
	// build.setContents(new ByteArrayInputStream(data), IResource.FORCE, null);
	// }
	//
	// TaskUtils.waitForAutobuild("after update");
	//
	// softly.assertThat(getJarFile())
	// .as("after")
	// .doesNotHaveModificationStamp(stamp);
	// }
	//
	// * add Foo=foo to build.bnd -> check in test.jar
	// * add include bar.bnd to build.bnd, add Bar=bar -> check in test.jar
	// * touch bar.bnd -> see if manifest is updated in JAR (Jar viewer does
	// not refresh very well, so reopen)

	// * touch build.bnd -> verify rebuild

	@Disabled("According to the comments, this test should pass - but it currently fails")
	@Test
	void whenBuildFileIsTouched_outputIsRebuilt() throws CoreException {
		TaskUtils.buildIncremental("touchBuildFile");

		long stamp = getJarModStamp();
		getBuildFile().touch(null);
		TaskUtils.buildIncremental("afterTouchBndFile");

		softly.assertThat(getJarFile())
			.as("after")
			.doesNotHaveModificationStamp(stamp);
	}

	// * touch bnd.bnd in test -> verify rebuild

	@Disabled("According to the comments, this test should pass - but it currently fails")
	@Test
	void whenBndFileIsTouched_outputIsRebuilt() throws CoreException {
		TaskUtils.buildIncremental("touchBndFile");

		long stamp = getJarModStamp();
		getBndFile().touch(null);
		TaskUtils.buildIncremental("afterTouchBndFile");

		softly.assertThat(getJarFile())
			.as("after")
			.doesNotHaveModificationStamp(stamp);
	}

	// *
	// * create project test.2, add -buildpath: test

	@Test
	void whenDependentProjectAdded_outputIsRebuilt() {
		TaskUtils.buildIncremental("dependentProjectAdded");

		long stamp = getJarModStamp();

		TaskUtils.addBundlesToBuildpath(bndProject, "local-proj");
		TaskUtils.buildIncremental("added local-proj");

		softly.assertThat(getJarFile())
			.as("after")
			.doesNotHaveModificationStamp(stamp);
	}

	IFile getJar(IProject project) {
		return project.getFile("generated/" + project.getName() + ".jar");
	}

	@Test
	void whenDependentProjectUpdated_outputIsRebuilt() throws CoreException {
		TaskUtils.addBundlesToBuildpath(bndProject, "local-proj");

		TaskUtils.buildIncremental("dependentProjectUpdated");

		eclipseProject.getFile("generated/test.jar");

		long stamp = getJarModStamp();

		IProject localProject = ResourcesPlugin.getWorkspace()
			.getRoot()
			.getProject("local-proj");

		if (localProject == null) {
			TaskUtils.dumpWorkspace();
			throw new IllegalStateException("Could not get project \"local-proj\" from the current workspace");
		}

		IFile localJar = getJar(localProject);
		IFile file = localProject.getFile("src/my/local/ws/pkg/Compilable.java");
		IFile jarFile = getJarFile();
		file.create(IO.stream("package my.local.ws.pkg; public class Compilable {}"), true, null);
		try {
			TaskUtils.buildIncremental("after upstream dependency updated");

			softly.assertThat(jarFile)
				.doesNotHaveModificationStamp(stamp);
			stamp = getJarModStamp();
		} finally {
			file.delete(true, null);
		}
		TaskUtils.buildIncremental("removed added file");

		softly.assertThat(getJarFile())
			.as("after")
			.doesNotHaveModificationStamp(stamp);
	}
}
