package biz.aQute.resolve;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;

import aQute.bnd.build.Container;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.help.instructions.ResolutionInstructions.Runorder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.result.Result;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;
import biz.aQute.resolve.Bndrun.CacheReason;

public class RunResolutionTest {

	Workspace	workspace;

	@InjectTemporaryDirectory
	Path		tmp;
	Path		ws;

	@BeforeEach
	public void before() throws Exception {
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
		Bndrun bndrun = Bndrun.createBndrun(workspace, IO.getFile(ws.toFile(), "test.simple/resolve.bndrun"));
		String resolve = bndrun.resolve(false, false);
		assertThat(bndrun.check()).isTrue();
	}

	@Test
	public void testExcludeSystemResource() throws Exception {
		Bndrun bndrun = Bndrun.createBndrun(workspace, IO.getFile(ws.toFile(), "test.simple/resolve.bndrun"));
		RunResolution resolve = RunResolution.resolve(bndrun, null);
		Set<Resource> noFramework = resolve.getRequired()
			.keySet();

		bndrun.setProperty(Constants.RESOLVE_EXCLUDESYSTEM, "false");
		resolve = RunResolution.resolve(bndrun, null);
		Set<Resource> withFramework = new HashSet<>(resolve.getRequired()
			.keySet());

		withFramework.removeAll(noFramework);
		assertThat(withFramework).hasSize(1);
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

		Result<String> nonExistent = RunResolution.getRunBundles(bndrun, false);
		assertThat(nonExistent.unwrap()).isEmpty();

		Result<String> force = RunResolution.getRunBundles(bndrun, true);
		assertThat(force.unwrap()).isNotEmpty();

		Result<String> existent = RunResolution.getRunBundles(bndrun, false);
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
	public void testResolveCachedWithStandalone() throws Exception {
		Bndrun bndrun = Bndrun.createBndrun(workspace, IO.getFile(tmp.toFile(), "resolver.bndrun"));
		bndrun.setProperty("-resolve", "cache");
		Collection<Container> runbundles = bndrun.getRunbundles();
		assertThat(bndrun.testReason).isEqualTo(CacheReason.NOT_A_BND_LAYOUT);
	}

	@Test
	public void testResolveCached() throws Exception {
		Bndrun bndrun = Bndrun.createBndrun(workspace, workspace.getFile("test.simple/resolve.bndrun"));
		bndrun.setTrace(true);
		File file = bndrun.getPropertiesFile();
		assertTrue(bndrun.check());
		File cache = bndrun.getCacheFile(file);
		File build = IO.getFile(ws.toFile(), "cnf/build.bnd");
		File empty = IO.getFile(ws.toFile(), "test.simple/empty-included-in-resolve.bnd");

		try {

			System.out.println("get the embedded list of runbundles, this is out benchmark");
			bndrun.setProperty("-resolve", "manual");
			Collection<Container> manual = bndrun.getRunbundles();
			assertThat(manual).hasSize(2);

			System.out.println("remove the embedded list and set mode to 'cache'");
			bndrun.setProperty("-resolve", "cache");
			bndrun.unsetProperty("-runbundles");

			assertThat(cache).doesNotExist();

			System.out.println("First time we should resolve & create a cache file");
			Collection<Container> cached = bndrun.getRunbundles();
			assertTrue(bndrun.check());
			assertThat(cache).isFile();
			assertThat(cached).containsExactlyElementsOf(manual);
			assertThat(cache.lastModified()).isGreaterThan(bndrun.lastModified());
			assertThat(bndrun.testReason).isEqualTo(CacheReason.NO_CACHE_FILE);

			System.out.println("Second time, the cache file should used, so make it valid but empty ");
			long lastModified = cache.lastModified();
			IO.store("-runbundles ", cache);
			cached = bndrun.getRunbundles();
			assertTrue(bndrun.check());
			assertThat(cached).isEmpty();
			assertThat(bndrun.testReason).isEqualTo(CacheReason.USE_CACHE);

			System.out.println("Now make cache invalid, should be ignored");
			IO.store("-runbundles is not a valid file", cache);
			cached = bndrun.getRunbundles();
			assertTrue(bndrun.check());
			assertThat(cached).containsExactlyElementsOf(manual);
			assertThat(bndrun.testReason).isEqualTo(CacheReason.INVALID_CACHE);

			System.out.println("Now empty cache, but still use it");
			IO.store("-runbundles ", cache);
			cached = bndrun.getRunbundles();
			assertTrue(bndrun.check());
			assertThat(cached).isEmpty();
			assertThat(bndrun.testReason).isEqualTo(CacheReason.USE_CACHE);

			System.out.println("Refresh and check we still use the cache");
			assertFalse(bndrun.refresh());
			bndrun.setProperty("-resolve", "cache");
			bndrun.unsetProperty("-runbundles");
			cached = bndrun.getRunbundles();
			assertThat(cached).isEmpty();
			assertThat(bndrun.testReason).isEqualTo(CacheReason.USE_CACHE);

			System.out.println("Make sure modified time granularity is < then passed time");
			Thread.sleep(100);

			System.out.println("Update an include file, refresh and check we still use the cache");
			long now = System.currentTimeMillis();
			empty.setLastModified(now);
			now = empty.lastModified();

			assertThat(bndrun.getCacheReason(cache)).isEqualTo(CacheReason.USE_CACHE);

			assertThat(bndrun.lastModified()).isLessThan(now);
			assertThat(cache.lastModified()).isLessThan(now);
			assertTrue(bndrun.refresh());
			bndrun.setProperty("-resolve", "cache");
			bndrun.unsetProperty("-runbundles");
			assertThat(bndrun.getCacheReason(cache)).isEqualTo(CacheReason.CACHE_STALE_PROJECT);
			assertThat(bndrun.lastModified()).isGreaterThanOrEqualTo(now);
			bndrun.setPedantic(true);
			bndrun.setTrace(true);
			cached = bndrun.getRunbundles();
			assertTrue(bndrun.check());
			assertThat(cached).containsExactlyElementsOf(manual);
			assertThat(bndrun.testReason).isEqualTo(CacheReason.CACHE_STALE_PROJECT);
			assertThat(cache.lastModified()).isGreaterThanOrEqualTo(now);
			assertThat(cache.lastModified()).isGreaterThanOrEqualTo(bndrun.lastModified());

			System.out.println("Next we use the cache");
			cached = bndrun.getRunbundles();
			assertThat(cached).containsExactlyElementsOf(manual);
			assertThat(bndrun.testReason).isEqualTo(CacheReason.USE_CACHE);

			System.out.println("Make sure modified time granularity is < then passed time");
			Thread.sleep(100);

			System.out.println("Update the cnf/build file");
			now = System.currentTimeMillis();
			build.setLastModified(now);
			now = build.lastModified();

			System.out.println("Refresh the workspace");
			assertTrue(workspace.refresh());
			assertThat(bndrun.getCacheReason(cache)).isEqualTo(CacheReason.CACHE_STALE_WORKSPACE);
			cached = bndrun.getRunbundles();
			assertThat(bndrun.testReason).isEqualTo(CacheReason.CACHE_STALE_WORKSPACE);

			System.out.println("Next we use the cache again");
			cached = bndrun.getRunbundles();
			assertThat(bndrun.testReason).isEqualTo(CacheReason.USE_CACHE);

			assertTrue(bndrun.check());
		} catch (AssertionError e) {
			System.out.println("bndrun     = " + bndrun.lastModified());
			System.out.println("cache      = " + cache.lastModified());
			System.out.println("workspace  = " + workspace.lastModified());
			System.out.println("build      = " + build.lastModified());
			System.out.println("empty      = " + empty.lastModified());
			throw e;
		}
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
		Bndrun bndrun = Bndrun.createBndrun(workspace, IO.getFile(ws.toFile(), "test.simple/resolve.bndrun"));
		RunResolution resolution = bndrun.resolve();
		assertThat(bndrun.check()).isTrue();

		assertThat(resolution.updateBundles(bndrun.getModel())).isFalse();

		bndrun.getModel()
			.setRunBundles(Collections.emptyList());
		assertThat(resolution.updateBundles(bndrun.getModel())).isTrue();
	}

	@Test
	public void testStartLevelsLeastDependenciesFirst() throws Exception {
		Bndrun bndrun = Bndrun.createBndrun(workspace, IO.getFile(ws.toFile(), "test.simple/resolve.bndrun"));

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
		Bndrun bndrun = Bndrun.createBndrun(workspace, IO.getFile(ws.toFile(), "test.simple/resolve.bndrun"));

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
		Bndrun bndrun = Bndrun.createBndrun(workspace, IO.getFile(ws.toFile(), "test.simple/resolve.bndrun"));
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
		Bndrun bndrun = Bndrun.createBndrun(workspace, IO.getFile(ws.toFile(), "test.simple/resolve.bndrun"));

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
		Bndrun bndrun = Bndrun.createBndrun(workspace, IO.getFile(ws.toFile(), "test.simple/resolve.bndrun"));

		// First do not fail on changes
		bndrun.getModel()
			.setRunBundles(Collections.emptyList());
		String resolution = bndrun.resolve(false, false);
		assertThat(bndrun.check()).isTrue();

		// Now fail on changes
		bndrun.getModel()
			.setRunBundles(Collections.emptyList());
		resolution = bndrun.resolve(true, false);
		assertThat(bndrun.check("Fail on changes set to ", "Existing runbundles   \\[\\]", "Calculated runbundles",
			Pattern.quote(
				"Diff [osgi.enroute.junit.wrapper;version='[4.12.0,4.12.1)', test.simple;version=snapshot] exist in calculated runbundles but missing in existing runbundles")))
				.isTrue();


		// Now succeed because there are no changes
		resolution = bndrun.resolve(false, false);
		assertThat(bndrun.check()).isTrue();

	}

	@Test
	public void testPrintHumanReadableDifference() throws Exception {
		assertThat(Utils.printHumanReadableDifference(Set.of(1, 2, 3), Set.of(3, 4, 5), "set1", "set2"))
			.isEqualTo("[1, 2] exist in set1 but missing in set2, [4, 5] exist in set2 but missing in set1");

		assertThat(Utils.printHumanReadableDifference(Set.of(1, 2, 3), Set.of(1, 2, 3), "set1", "set2")).isNull();
		assertThat(Utils.printHumanReadableDifference(Set.of(), Set.of(1, 2, 3), "set1", "set2"))
			.isEqualTo("[1, 2, 3] exist in set2 but missing in set1");
		assertThat(Utils.printHumanReadableDifference(Set.of(1, 2, 3), Set.of(), "set1", "set2"))
			.isEqualTo("[1, 2, 3] exist in set1 but missing in set2");

	}

}
