package biz.aQute.resolve;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import aQute.bnd.build.Workspace;
import aQute.lib.io.IO;

public class ResolutionTest {

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
