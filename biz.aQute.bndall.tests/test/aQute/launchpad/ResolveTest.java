package aQute.launchpad;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import aQute.bnd.remoteworkspace.client.RemoteWorkspaceClientFactory;
import aQute.bnd.service.remoteworkspace.RemoteWorkspace;
import aQute.bnd.service.remoteworkspace.RemoteWorkspaceClient;
import aQute.lib.io.IO;

public class ResolveTest {

	private LaunchpadBuilder	builder;
	private Workspace			workspace;

	@Before
	public void before() throws Exception {
		File tmp = IO.getFile("generated/tmpws1");
		IO.delete(tmp);
		tmp.mkdirs();
		IO.copy(IO.getFile("resources/launchpad/ws1"), tmp);
		workspace = new Workspace(tmp);
		Project project = workspace.getProject("p1");
		RemoteWorkspace remoteWs = RemoteWorkspaceClientFactory.create(project.getBase(),
			new RemoteWorkspaceClient() {});
		builder = new LaunchpadBuilder(remoteWs);
	}

	@After
	public void after() throws Exception {
		IO.closeAll(builder, workspace);
	}

	/*
	 * No -runbundles, must resolve before calling getRunBundles()
	 */
	@Test
	public void testLaunchpadBeforeLaunch() throws Exception {
		Project project = workspace.getProject("p1");
		try (Launchpad lp = builder.bndrun(project.getFile("beforelaunch.bndrun"))
			.create()) {
			assertThat(lp.getBundleContext()
				.getBundles()).hasSize(4);
		}
	}

	/*
	 * No -runbundles, pretending running in BATCH mode, should resolve
	 */
	@Test
	public void testLaunchpadBatchInBatchEnvironment() throws Exception {
		workspace.setProperty("-gestalt", Constants.GESTALT_BATCH);
		Project project = workspace.getProject("p1");
		try (Launchpad lp = builder.bndrun(project.getFile("batch.bndrun"))
			.create()) {
			assertThat(lp.getBundleContext()
				.getBundles()).hasSize(4);
		}
	}

	/*
	 * No -runbundles, not in batch mode, must resolve because there are no
	 * runbundles set
	 */
	@Test
	public void testLaunchpadBatchInNonBatchEnvironment() throws Exception {
		workspace.setProperty("-gestalt", "");
		Project project = workspace.getProject("p1");
		try (Launchpad lp = builder.bndrun(project.getFile("batch.bndrun"))
			.create()) {
			assertThat(lp.getBundleContext()
				.getBundles()).hasSize(4);
		}
	}

	/*
	 * No -runbundles, not in batch mode, must not resolve because there is 1
	 * runbundle set (so one less than resolve would give us).
	 */
	@Test
	public void testLaunchpadBatchInNonBatchEnvironmentWithRunbundles() throws Exception {
		workspace.setProperty("-gestalt", "");
		Project project = workspace.getProject("p1");
		try (Launchpad lp = builder.bndrun(project.getFile("batch-with-bundles.bndrun"))
			.create()) {
			assertThat(lp.getBundleContext()
				.getBundles()).hasSize(3);
		}
	}
}
