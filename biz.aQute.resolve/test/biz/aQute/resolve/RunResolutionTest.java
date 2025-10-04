package biz.aQute.resolve;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;

import aQute.bnd.build.Container;
import aQute.bnd.build.Container.TYPE;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.clauses.HeaderClause;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.build.model.conversions.NoopConverter;
import aQute.bnd.header.Parameters;
import aQute.bnd.help.instructions.ResolutionInstructions.Runorder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.result.Result;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.Strategy;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;
import biz.aQute.resolve.Bndrun.CacheReason;

@ExtendWith(SoftAssertionsExtension.class)
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

			"Diff .* exist in calculated runbundles but missing in existing runbundles"))
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

		Set<String> set1 = Set.of("com.fasterxml.jackson.core.jackson-annotations;version='[2.16.1,2.16.2)'",
			"com.fasterxml.jackson.core.jackson-core;version='[2.16.1,2.16.2)'",
			"com.fasterxml.jackson.core.jackson-databind;version='[2.16.1,2.16.2)'",
			"io.dropwizard.metrics.core;version='[4.2.19,4.2.20)'", "junit-jupiter-api;version='[5.9.0,5.9.1)'",
			"junit-jupiter-engine;version='[5.9.0,5.9.1)'", "junit-jupiter-params;version='[5.9.0,5.9.1)'",
			"junit-platform-commons;version='[1.9.0,1.9.1)'", "junit-platform-engine;version='[1.9.0,1.9.1)'",
			"junit-platform-launcher;version='[1.9.0,1.9.1)'",
			"org.apache.aries.component-dsl.component-dsl;version='[1.2.2,1.2.3)'",
			"org.apache.aries.typedevent.bus;version='[0.0.2,0.0.3)'",
			"org.apache.commons.commons-csv;version='[1.9.0,1.9.1)'",
			"org.apache.felix.configadmin;version='[1.9.24,1.9.25)'",
			"org.apache.felix.http.servlet-api;version='[2.1.0,2.1.1)'", "org.apache.felix.scr;version='[2.2.2,2.2.3)'",
			"org.eclipse.emf.common;version='[2.28.0,2.28.1)'", "org.eclipse.emf.ecore;version='[2.33.0,2.33.1)'",
			"org.eclipse.emf.ecore.xmi;version='[2.18.0,2.18.1)'",
			"org.eclipse.jetty.alpn.client;version='[11.0.13,11.0.14)'",
			"org.eclipse.jetty.client;version='[11.0.13,11.0.14)'",
			"org.eclipse.jetty.http;version='[11.0.13,11.0.14)'", "org.eclipse.jetty.io;version='[11.0.13,11.0.14)'",
			"org.eclipse.jetty.security;version='[11.0.13,11.0.14)'",
			"org.eclipse.jetty.server;version='[11.0.13,11.0.14)'",
			"org.eclipse.jetty.util;version='[11.0.13,11.0.14)'",
			"org.eclipse.sensinact.gateway.core.annotation;version='[0.0.2,0.0.3)'",
			"org.eclipse.sensinact.gateway.core.api;version='[0.0.2,0.0.3)'",
			"org.eclipse.sensinact.gateway.core.emf-api;version='[0.0.2,0.0.3)'",
			"org.eclipse.sensinact.gateway.core.geo-json;version='[0.0.2,0.0.3)'",
			"org.eclipse.sensinact.gateway.core.impl;version='[0.0.2,0.0.3)'",
			"org.eclipse.sensinact.gateway.core.models.metadata;version='[0.0.2,0.0.3)'",
			"org.eclipse.sensinact.gateway.core.models.provider;version='[0.0.2,0.0.3)'",
			"org.eclipse.sensinact.gateway.southbound.device-factory.device-factory-core;version='[0.0.2,0.0.3)'",
			"org.eclipse.sensinact.gateway.southbound.device-factory.parser-csv;version='[0.0.2,0.0.3)'",
			"org.eclipse.sensinact.gateway.southbound.http.http-device-factory;version='[0.0.2,0.0.3)'",
			"org.eclipse.sensinact.gateway.southbound.http.http-device-factory-tests;version='[0.0.2,0.0.3)'",
			"org.gecko.emf.osgi.component;version='[5.0.0,5.0.1)'", "org.opentest4j;version='[1.2.0,1.2.1)'",
			"org.osgi.service.cm;version='[1.6.1,1.6.2)'", "org.osgi.service.component;version='[1.5.0,1.5.1)'",
			"org.osgi.service.typedevent;version='[1.0.0,1.0.1)'", "org.osgi.test.common;version='[1.2.1,1.2.2)'",
			"org.osgi.test.junit5;version='[1.2.1,1.2.2)'", "org.osgi.util.converter;version='[1.0.9,1.0.10)'",
			"org.osgi.util.function;version='[1.1.0,1.1.1)'", "org.osgi.util.promise;version='[1.3.0,1.3.1)'",
			"org.osgi.util.pushstream;version='[1.0.2,1.0.3)'", "slf4j.api;version='[1.7.36,1.7.37)'",
			"slf4j.simple;version='[1.7.36,1.7.37)'");
		Set<String> set2 = Set.of("com.fasterxml.jackson.core.jackson-annotations;version='[2.16.1,2.16.2)'",
			"com.fasterxml.jackson.core.jackson-core;version='[2.16.1,2.16.2)'",
			"com.fasterxml.jackson.core.jackson-databind;version='[2.16.1,2.16.2)'",
			"io.dropwizard.metrics.core;version='[4.2.19,4.2.20)'", "junit-jupiter-api;version='[5.9.0,5.9.1)'",
			"junit-jupiter-engine;version='[5.9.0,5.9.1)'", "junit-jupiter-params;version='[5.9.0,5.9.1)'",
			"junit-platform-commons;version='[1.9.0,1.9.1)'", "junit-platform-engine;version='[1.9.0,1.9.1)'",
			"junit-platform-launcher;version='[1.9.0,1.9.1)'",
			"org.apache.aries.component-dsl.component-dsl;version='[1.2.2,1.2.3)'",
			"org.apache.aries.typedevent.bus;version='[0.0.2,0.0.3)'",
			"org.apache.commons.commons-csv;version='[1.9.0,1.9.1)'",
			"org.apache.felix.configadmin;version='[1.9.24,1.9.25)'",
			"org.apache.felix.http.servlet-api;version='[2.1.0,2.1.1)'", "org.apache.felix.scr;version='[2.2.2,2.2.3)'",
			"org.eclipse.emf.common;version='[2.28.0,2.28.1)'", "org.eclipse.emf.ecore;version='[2.33.0,2.33.1)'",
			"org.eclipse.emf.ecore.xmi;version='[2.18.0,2.18.1)'",
			"org.eclipse.jetty.alpn.client;version='[11.0.13,11.0.14)'",
			"org.eclipse.jetty.client;version='[11.0.13,11.0.14)'",
			"org.eclipse.jetty.http;version='[11.0.13,11.0.14)'", "org.eclipse.jetty.io;version='[11.0.13,11.0.14)'",
			"org.eclipse.jetty.security;version='[11.0.13,11.0.14)'",
			"org.eclipse.jetty.server;version='[11.0.13,11.0.14)'",
			"org.eclipse.jetty.util;version='[11.0.13,11.0.14)'",
			"org.eclipse.sensinact.gateway.core.annotation;version='[0.0.2,0.0.3)'",
			"org.eclipse.sensinact.gateway.core.api;version='[0.0.2,0.0.3)'",
			"org.eclipse.sensinact.gateway.core.emf-api;version='[0.0.2,0.0.3)'",
			"org.eclipse.sensinact.gateway.core.geo-json;version='[0.0.2,0.0.3)'",
			"org.eclipse.sensinact.gateway.core.impl;version='[0.0.2,0.0.3)'",
			"org.eclipse.sensinact.gateway.core.models.metadata;version='[0.0.2,0.0.3)'",
			"org.eclipse.sensinact.gateway.core.models.provider;version='[0.0.2,0.0.3)'",
			"org.eclipse.sensinact.gateway.southbound.device-factory.device-factory-core;version='[0.0.2,0.0.3)'",
			"org.eclipse.sensinact.gateway.southbound.device-factory.parser-csv;version='[0.0.2,0.0.3)'",
			"org.eclipse.sensinact.gateway.southbound.http.http-device-factory;version='[0.0.2,0.0.3)'",
			"org.eclipse.sensinact.gateway.southbound.http.http-device-factory-tests;version='[0.0.2,0.0.3)'",
			"org.gecko.emf.osgi.component;version='[5.0.0,5.0.1)'", "org.opentest4j;version='[1.2.0,1.2.1)'",
			"org.osgi.service.component;version='[1.5.0,1.5.1)'", "org.osgi.service.typedevent;version='[1.0.0,1.0.1)'",
			"org.osgi.test.common;version='[1.2.1,1.2.2)'", "org.osgi.test.junit5;version='[1.2.1,1.2.2)'",
			"org.osgi.util.converter;version='[1.0.9,1.0.10)'", "org.osgi.util.function;version='[1.1.0,1.1.1)'",
			"org.osgi.util.promise;version='[1.3.0,1.3.1)'", "org.osgi.util.pushstream;version='[1.0.2,1.0.3)'",
			"slf4j.api;version='[1.7.36,1.7.37)'", "slf4j.simple;version='[1.7.36,1.7.37)'");

		assertThat(Utils.printHumanReadableDifference(set1, set2, "set1", "set2"))
			.isEqualTo("[org.osgi.service.cm;version='[1.6.1,1.6.2)'] exist in set1 but missing in set2");

	}

	@Test
	public void testStartLevelDecoration(SoftAssertions softly) throws Exception {
		Bndrun bndrun = Bndrun.createBndrun(workspace, IO.getFile(ws.toFile(), "test.simple/resolveduplicates.bndrun"));
		bndrun.setProperty("-runstartlevel", "order=sortbynameversion,begin=100,step=10");

		// Decorate test.simple to get startlevel 90 (which would otherwise be 110 within the assigned runstartlevel).
		bndrun.setProperty("-runbundles+", "test.simple;startlevel=90");

		List<? extends HeaderClause> runBundles = List.copyOf(bndrun.resolve(false, false, new NoopConverter<>()));

		softly.assertThat(runBundles.stream()
			.map(rb -> rb.toString()))
			.containsExactlyInAnyOrder("org.apache.felix.gogo.runtime;version='[0.10.0,0.10.1)';startlevel=100",
				"org.apache.felix.gogo.runtime;version='[0.12.0,0.12.1)';startlevel=110",
				"osgi.enroute.junit.wrapper;version='[4.12.0,4.12.1)';startlevel=120",
				"test.simple;version=snapshot;startlevel=90");

		// check that HeaderClause.toParameters does not remove duplicates
		// this kind of happens inside bndrun.resolve() let's test explicitly
		// again
		Parameters params = HeaderClause.toParameters(runBundles);
		softly.assertThat(params.toString())
			.isEqualTo(
				"org.apache.felix.gogo.runtime;version=\"[0.10.0,0.10.1)\";startlevel=100,org.apache.felix.gogo.runtime;version=\"[0.12.0,0.12.1)\";startlevel=110,osgi.enroute.junit.wrapper;version=\"[4.12.0,4.12.1)\";startlevel=120,test.simple;version=snapshot;startlevel=90");

	}

	@SuppressWarnings("resource")
	@Test
	public void testBndRunPluginGetBundle(SoftAssertions softly) {
		try {

			Workspace workspace = Workspace.createDefaultWorkspace();

			// try with a normal Bndrun, which only considers Workspace bundles
			Bndrun bndrun = Bndrun.createBndrun(workspace, null);
			bndrun.addBasicPlugin(new MyPlugin());
			System.out.println("Bndrun: " + bndrun.getPlugin(MyPlugin.class));
			System.out.println("Workspace: " + workspace.getPlugin(MyPlugin.class));
			workspace.refresh();
			System.out.println("Workspace after refresh: " + workspace.getPlugin(MyPlugin.class));

			softly.assertThat(bndrun.getPlugin(MyPlugin.class))
				.isNotNull();
			Container bundle = bndrun.getBundle("testBndRunPluginGetBundle", "[1.0.0,2.0.0)", Strategy.HIGHEST, Map.of());
			softly.assertThat(bundle.getType())
				.isEqualTo(TYPE.ERROR);

			// now repeat with a custom MyBndrun class which overrides
			// getRepositories()
			MyBndrun bndrun2 = new MyBndrun(workspace, null);
			bndrun2.addBasicPlugin(new MyPlugin());
			softly.assertThat(bndrun2.getPlugin(MyPlugin.class))
				.isNotNull();
			Container bundle2 = bndrun2.getBundle("testBndRunPluginGetBundle", "[1.0.0,2.0.0)", Strategy.HIGHEST,
				Map.of());
			softly.assertThat(bundle2.getType())
				.isEqualTo(TYPE.REPO);
			softly.assertThat(bundle2.getBundleSymbolicName())
				.isEqualTo("testBndRunPluginGetBundle");


		} catch (Exception e) {
			fail();
		}
	}

	private static class MyBndrun extends Bndrun {

		public MyBndrun(Workspace workspace, File propertiesFile) throws Exception {
			super(workspace, propertiesFile);
		}

		@Override
		public List<RepositoryPlugin> getRepositories(String... tags) {
			return getPlugins(RepositoryPlugin.class);
		}

	}

	private final class MyPlugin implements RepositoryPlugin {

		@Override
		public PutResult put(InputStream stream, PutOptions options) throws Exception {
			return null;
		}

		@Override
		public File get(String bsn, Version version, Map<String, String> properties, DownloadListener... listeners)
			throws Exception {
			if("testBndRunPluginGetBundle".equals(bsn)) {
				return IO.getFile(RunResolutionTest.this.ws.toFile(),
					"testdata/jar/org.apache.felix.http.servlet-api-1.2.0.jar");
			}
			return null;
		}

		@Override
		public boolean canWrite() {
			return false;
		}

		@Override
		public List<String> list(String pattern) throws Exception {
			return null;
		}

		@Override
		public SortedSet<Version> versions(String bsn) throws Exception {
			return new TreeSet<Version>(Set.of(Version.ONE));
		}

		@Override
		public String getName() {
			return null;
		}

		@Override
		public String getLocation() {
			return null;
		}

	}

}
