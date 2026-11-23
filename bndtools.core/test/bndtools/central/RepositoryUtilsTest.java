package bndtools.central;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import aQute.bnd.build.Workspace;
import aQute.bnd.service.RepositoryPlugin;

class RepositoryUtilsTest {

	@Test
	void defaultWorkspaceDoesNotExposeRepositories() {
		Workspace workspace = mock(Workspace.class);
		when(workspace.isDefaultWorkspace()).thenReturn(true);

		assertThat(RepositoryUtils.listRepositories(workspace, false)).isEmpty();
		assertThat(RepositoryUtils.listRepositories(workspace, true)).isEmpty();
		verify(workspace, never()).getPlugins(RepositoryPlugin.class);
	}
}