package biz.aQute.resolve;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;

import aQute.bnd.build.Container;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.help.instructions.ResolutionInstructions.Runorder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.service.result.Result;
import aQute.lib.io.IO;

public class RunResolutionTest {

	Workspace	workspace;

	Path		tmp;
	Path		ws;

	@BeforeEach
	public void before(TestInfo info) throws Exception {
		Method testMethod = info.getTestMethod()
			.get();
		tmp = Paths.get("generated/tmp/test", getClass().getName(), testMethod.getName())
			.toAbsolutePath();
		IO.delete(tmp);
		IO.mkdirs(tmp);
		IO.copy(IO.getPath("testdata/enroute"), tmp);
		ws = IO.copy(IO.getPath("testdata/pre-buildworkspace"), tmp.resolve("workspace"));
		workspace = new Workspace(ws.toFile());
		assertThat(workspace).isNotNull();
	}

	@AfterEach
	public void after() {
		IO.close(workspace);
	}

	@Test
	public void testSimple() throws Exception {
		Bndrun bndrun = Bndrun.createBndrun(workspace,
			IO.getFile(ws.toFile(), "test.simple/resolve.bndrun"));
		String resolve = bndrun.resolve(false, false);
		assertThat(bndrun.check()).isTrue();
	}

	@Test
	public void testOrdering() throws Exception {
		Bndrun bndrun = Bndrun.createBndrun(workspace, IO.getFile("testdata/ordering.bndrun"));
		RunResolution resolution = bndrun.resolve();
		assertThat(bndrun.check()).isTrue();

		RunResolution resolution2 = bndrun.resolve();
		assertThat(bndrun.check()).isTrue();

		List<Resource> l1 = resolution.getOrderedResources(resolution.getRequired(), Runorder.LEASTDEPENDENCIESFIRST);
		List<Resource> l2 = resolution2.getOrderedResources(permutate(resolution.getRequired()),
			Runorder.LEASTDEPENDENCIESFIRST);

		assertThat(l1).isEqualTo(l2);
	}

	@Test
	public void testCachingOfResult() throws Exception {
		Bndrun bndrun = Bndrun.createBndrun(workspace, IO.getFile(tmp.toFile(), "resolver.bndrun"));
		bndrun.setProperty("-resolve", "beforelaunch");
		bndrun.unsetProperty("-runbundles");
		RunResolution.clearCache(bndrun.getWorkspace());

		Result<String, String> nonExistent = RunResolution.getRunBundles(bndrun, false);
		assertThat(nonExistent.unwrap()).isEmpty();

		Result<String, String> force = RunResolution.getRunBundles(bndrun, true);
		assertThat(force.unwrap()).isNotEmpty();

		Result<String, String> existent = RunResolution.getRunBundles(bndrun, false);
		assertThat(existent.unwrap()).isNotEmpty();

		System.out.println("Runbundles " + existent.unwrap());

		bndrun.setProperty("foo", "bar");
		nonExistent = RunResolution.getRunBundles(bndrun, false);
		assertThat(nonExistent.unwrap()).isEmpty();

		ProjectLauncher pl = bndrun.getProjectLauncher();
		assertThat(pl).isNotNull();

		Collection<Container> runbundles = pl.getProject()
			.getRunbundles();
		runbundles.forEach(c -> System.out.println(c.getFile()));
		// assertThat(runbundles).hasSize(22);
	}

	@Test
	public void testNotCachingOfResultForOtherResolveOption() throws Exception {
		Bndrun bndrun = Bndrun.createBndrun(workspace, IO.getFile(tmp.toFile(), "resolver.bndrun"));
		bndrun.setProperty("-resolve", "manual");
		bndrun.unsetProperty("-runbundles");
		RunResolution.clearCache(bndrun.getWorkspace());

		ProjectLauncher pl = bndrun.getProjectLauncher();

		assertThat(pl).isNotNull();
		Collection<Container> runbundles = pl.getProject()
			.getRunbundles();
		assertThat(runbundles).isEmpty();

		// The repo used is an XML and the URLs are not found when downloaded in
		// the background. Sometimes they're in,
		// sometimes not. This is valid since the Container will be error.
		if (!bndrun.isPerfect()) {
			assertThat(bndrun.check("Download java.io.FileNotFoundException:")).isTrue();
		}
	}

	@Test
	public void testLaunchWithBeforeLaunchResolve() throws Exception {
		Bndrun bndrun = Bndrun.createBndrun(workspace, IO.getFile(tmp.toFile(), "resolver.bndrun"));
		bndrun.setProperty("-resolve", "beforelaunch");
		bndrun.unsetProperty("-runbundles");
		RunResolution.clearCache(bndrun.getWorkspace());

		ProjectLauncher pl = bndrun.getProjectLauncher();
		pl.setCwd(tmp.toFile());
		assertThat(pl).isNotNull();
		Collection<Container> runbundles = pl.getProject()
			.getRunbundles();
	}

	private Map<Resource, List<Wire>> permutate(Map<Resource, List<Wire>> required) {
		TreeMap<Resource, List<Wire>> map = new TreeMap<>(required);
		map.entrySet()
			.forEach(e -> Collections.shuffle(e.getValue()));
		return map;
	}

	@Test
	public void testUpdateBundles() throws Exception {
		Bndrun bndrun = Bndrun.createBndrun(workspace,
			IO.getFile(ws.toFile(), "test.simple/resolve.bndrun"));
		RunResolution resolution = bndrun.resolve();
		assertThat(bndrun.check()).isTrue();

		assertThat(resolution.updateBundles(bndrun.getModel())).isFalse();

		bndrun.getModel()
			.setRunBundles(Collections.emptyList());
		assertThat(resolution.updateBundles(bndrun.getModel())).isTrue();
	}

	@Test
	public void testStartLevelsLeastDependenciesFirst() throws Exception {
		Bndrun bndrun = Bndrun.createBndrun(workspace,
			IO.getFile(ws.toFile(), "test.simple/resolve.bndrun"));

		bndrun.setProperty("-runstartlevel", "order=leastdependenciesfirst,begin=100,step=10");

		RunResolution resolution = bndrun.resolve();
		assertThat(bndrun.check()).isTrue();

		List<VersionedClause> runBundles = resolution.getRunBundles();
		assertThat(runBundles).hasSize(2);
		assertThat(runBundles.get(0)
			.getName()).isEqualTo("osgi.enroute.junit.wrapper");
		assertThat(runBundles.get(0)
			.getAttribs()).containsEntry(Constants.RUNBUNDLES_STARTLEVEL_ATTRIBUTE, "100");
		assertThat(runBundles.get(1)
			.getName()).isEqualTo("test.simple");
		assertThat(runBundles.get(1)
			.getAttribs()).containsEntry(Constants.RUNBUNDLES_STARTLEVEL_ATTRIBUTE, "110");

	}

	@Test
	public void testStartLevelsLeastDependenciesLast() throws Exception {
		Bndrun bndrun = Bndrun.createBndrun(workspace,
			IO.getFile(ws.toFile(), "test.simple/resolve.bndrun"));

		bndrun.setProperty("-runstartlevel", "order=leastdependencieslast,begin=100,step=10");

		RunResolution resolution = bndrun.resolve();
		assertThat(bndrun.check()).isTrue();

		List<VersionedClause> runBundles = resolution.getRunBundles();
		assertThat(runBundles).hasSize(2);
		assertThat(runBundles.get(0)
			.getName()).isEqualTo("test.simple");
		assertThat(runBundles.get(0)
			.getAttribs()).containsEntry(Constants.RUNBUNDLES_STARTLEVEL_ATTRIBUTE, "100");
		assertThat(runBundles.get(1)
			.getName()).isEqualTo("osgi.enroute.junit.wrapper");
		assertThat(runBundles.get(1)
			.getAttribs()).containsEntry(Constants.RUNBUNDLES_STARTLEVEL_ATTRIBUTE, "110");

	}

	@Test
	public void testStartLevelsStep() throws Exception {
		Bndrun bndrun = Bndrun.createBndrun(workspace,
			IO.getFile(ws.toFile(), "test.simple/resolve.bndrun"));
		bndrun.setProperty("-runstartlevel", "order=random,begin=10,step=1");

		RunResolution resolution = bndrun.resolve();
		assertThat(bndrun.check()).isTrue();

		List<VersionedClause> runBundles = resolution.getRunBundles();
		assertThat(runBundles).hasSize(2);
		assertThat(runBundles.get(0)
			.getAttribs()).containsEntry(Constants.RUNBUNDLES_STARTLEVEL_ATTRIBUTE, "10");
		assertThat(runBundles.get(1)
			.getAttribs()).containsEntry(Constants.RUNBUNDLES_STARTLEVEL_ATTRIBUTE, "11");

	}

	@Test
	public void testNoStartLevels() throws Exception {
		Bndrun bndrun = Bndrun.createBndrun(workspace,
			IO.getFile(ws.toFile(), "test.simple/resolve.bndrun"));

		RunResolution resolution = bndrun.resolve();
		assertThat(bndrun.check()).isTrue();

		List<VersionedClause> runBundles = resolution.getRunBundles();
		assertThat(runBundles).hasSize(2);
		assertThat(runBundles.get(0)
			.getAttribs()).doesNotContainKey(Constants.RUNBUNDLES_STARTLEVEL_ATTRIBUTE);
		assertThat(runBundles.get(1)
			.getAttribs()).doesNotContainKey(Constants.RUNBUNDLES_STARTLEVEL_ATTRIBUTE);

	}

	@Test
	public void testFailOnChanges() throws Exception {
		Bndrun bndrun = Bndrun.createBndrun(workspace,
			IO.getFile(ws.toFile(), "test.simple/resolve.bndrun"));

		// First do not fail on changes
		bndrun.getModel()
			.setRunBundles(Collections.emptyList());
		String resolution = bndrun.resolve(false, false);
		assertThat(bndrun.check()).isTrue();

		// Now fail on changes
		bndrun.getModel()
			.setRunBundles(Collections.emptyList());
		resolution = bndrun.resolve(true, false);
		assertThat(bndrun.check("Fail on changes set to ", "Existing runbundles", "Calculated runbundles")).isTrue();

		// Now succeed because there are no changes
		resolution = bndrun.resolve(false, false);
		assertThat(bndrun.check()).isTrue();

	}
}
