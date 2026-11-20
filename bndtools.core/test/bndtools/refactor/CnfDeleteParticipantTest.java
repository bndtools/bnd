package bndtools.refactor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.junit.jupiter.api.Test;

class CnfDeleteParticipantTest {

	@Test
	void refreshesUntrackedFilesBeforeDeletingCnf() throws Exception {
		IProject cnf = project("/workspace/cnf");
		CnfDeleteParticipant participant = new CnfDeleteParticipant();
		assertThat(participant.initialize(cnf)).isTrue();
		NullProgressMonitor monitor = new NullProgressMonitor();

		assertThat(participant.checkConditions(monitor, new CheckConditionsContext()).isOK()).isTrue();

		verify(cnf).refreshLocal(IResource.DEPTH_INFINITE, monitor);
		assertThat(participant.createChange(monitor)).isNull();
	}

	@Test
	void blocksDeletionWhenRefreshFails() throws Exception {
		IProject cnf = project("/workspace/cnf");
		CnfDeleteParticipant participant = new CnfDeleteParticipant();
		assertThat(participant.initialize(cnf)).isTrue();
		NullProgressMonitor monitor = new NullProgressMonitor();
		doThrow(new CoreException(new Status(IStatus.ERROR, "bndtools.core", "Refresh failed")))
			.when(cnf).refreshLocal(IResource.DEPTH_INFINITE, monitor);

		assertThat(participant.checkConditions(monitor, new CheckConditionsContext()).hasFatalError()).isTrue();
	}

	@Test
	void ignoresOtherProjects() {
		CnfDeleteParticipant participant = new CnfDeleteParticipant();
		assertThat(participant.initialize(project("/workspace/other"))).isFalse();
	}

	@Test
	void ignoresClosedCnfProjects() {
		IProject cnf = project("/workspace/cnf");
		when(cnf.isOpen()).thenReturn(false);
		assertThat(new CnfDeleteParticipant().initialize(cnf)).isFalse();
	}

	@Test
	void ignoresFilesAndFolders() {
		IResource resource = mock(IResource.class);
		assertThat(new CnfDeleteParticipant().initialize(resource)).isFalse();
		verifyNoInteractions(resource);
	}

	private IProject project(String location) {
		IProject project = mock(IProject.class);
		when(project.isOpen()).thenReturn(true);
		when(project.getLocation()).thenReturn(new Path(location));
		return project;
	}
}