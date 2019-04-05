package aQute.launchpad;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import aQute.bnd.build.Workspace;
import aQute.bnd.remoteworkspace.client.RemoteWorkspaceClientFactory;
import aQute.lib.io.IO;

/**
 * Workspace related tests
 */
public class WorkspaceTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void testDeletePortRegistry() throws Exception {
		File tmpWs = folder.newFolder("tmp");
		IO.copy(IO.getFile("testresources/ws1"), tmpWs);
		File portFile;
		try (Workspace ws = Workspace.findWorkspace(tmpWs)) {

			File portDirectory = RemoteWorkspaceClientFactory.getPortDirectory(tmpWs, tmpWs);
			assertThat(portDirectory).isDirectory();
			String[] files = portDirectory.list();
			assertThat(files).hasSize(1);

			String port = files[0];

			portFile = new File(portDirectory, port);
			assertThat(portFile).exists();

			IO.delete(portDirectory);

			while (true) {
				try {
					if (portDirectory.isDirectory())
						break;
				} catch (Exception e) {
					Thread.sleep(100);
				}
			}
			assertThat(portDirectory.isDirectory());
			assertThat(portFile).exists();
		}
		assertThat(portFile).doesNotExist();
	}

}
