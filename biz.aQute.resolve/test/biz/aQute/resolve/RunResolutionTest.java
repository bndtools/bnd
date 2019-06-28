package biz.aQute.resolve;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.osgi.Constants;
import aQute.lib.io.IO;

public class RunResolutionTest {

	Workspace workspace;

	@Before
	public void setup() throws Exception {
		workspace = Workspace.findWorkspace(IO.getFile("testdata/pre-buildworkspace"));
		assertThat(workspace).isNotNull();
	}

	@Test
	public void testSimple() throws Exception {
		Bndrun bndrun = Bndrun.createBndrun(workspace,
			IO.getFile("testdata/pre-buildworkspace/test.simple/resolve.bndrun"));
		String resolve = bndrun.resolve(false, false);
		assertThat(bndrun.check()).isTrue();
	}

	@Test
	public void testUpdateBundles() throws Exception {
		Bndrun bndrun = Bndrun.createBndrun(workspace,
			IO.getFile("testdata/pre-buildworkspace/test.simple/resolve.bndrun"));
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
			IO.getFile("testdata/pre-buildworkspace/test.simple/resolve.bndrun"));

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
			IO.getFile("testdata/pre-buildworkspace/test.simple/resolve.bndrun"));

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
			IO.getFile("testdata/pre-buildworkspace/test.simple/resolve.bndrun"));
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
			IO.getFile("testdata/pre-buildworkspace/test.simple/resolve.bndrun"));

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
			IO.getFile("testdata/pre-buildworkspace/test.simple/resolve.bndrun"));

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
