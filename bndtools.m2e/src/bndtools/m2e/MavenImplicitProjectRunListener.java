package bndtools.m2e;

import org.bndtools.api.RunListener;
import org.bndtools.api.RunMode;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;

@Component(property = Constants.SERVICE_RANKING + ":Integer=1000")
public class MavenImplicitProjectRunListener implements MavenRunListenerHelper, RunListener {

	@Override
	public void create(Run run) throws Exception {
		IResource resource = getResource(run);

		if (!isMavenProject(resource)) {
			return;
		}

		Workspace workspace = run.getWorkspace();

		RunMode runMode = RunMode.get(run);

		if (workspace.getPlugin(MavenImplicitProjectRepository.class) == null) {
			IMavenProjectFacade projectFacade = getMavenProjectFacade(resource);

			final MavenImplicitProjectRepository implicitRepo = new MavenImplicitProjectRepository(projectFacade, run);

			if (runMode == RunMode.EDIT) {
				new Job("Create implicit repo") {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						implicitRepo.createRepo(projectFacade, monitor);
						return Status.OK_STATUS;
					}
				}.schedule();
			} else {
				implicitRepo.createRepo(projectFacade, new NullProgressMonitor());
			}

			workspace.getRepositories()
				.add(0, implicitRepo);
			workspace.addBasicPlugin(implicitRepo);

			mavenProjectRegistry.addMavenProjectChangedListener( //
				implicitRepo);
			iWorkspace.addResourceChangeListener( //
				implicitRepo, IResourceChangeEvent.POST_CHANGE);
		}
	}

	@Override
	public void end(Run run) throws Exception {
		IResource resource = getResource(run);

		if (!isMavenProject(resource)) {
			return;
		}

		Workspace workspace = run.getWorkspace();

		MavenImplicitProjectRepository implicitRepo = workspace.getPlugin(MavenImplicitProjectRepository.class);

		if (implicitRepo != null) {
			workspace.getRepositories()
				.remove(implicitRepo);
			workspace.removeBasicPlugin(implicitRepo);
			implicitRepo.cleanup();

			mavenProjectRegistry.removeMavenProjectChangedListener(implicitRepo);
			iWorkspace.removeResourceChangeListener(implicitRepo);
		}
	}

}
