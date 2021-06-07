package bndtools.launch;

import java.io.File;

import org.bndtools.api.RunMode;
import org.bndtools.api.RunProvider;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import biz.aQute.resolve.Bndrun;
import bndtools.central.Central;
import bndtools.launch.util.LaunchUtils;

@Component(property = Constants.SERVICE_RANKING + ":Integer=2000")
public class DefaultRunProvider implements RunProvider {

	@Override
	public Bndrun create(IResource targetResource, RunMode mode) throws Exception {
		Workspace ws = null;

		if (LaunchUtils.isInBndWorkspaceProject(targetResource)) {
			ws = Central.getWorkspaceIfPresent();
		}

		if (targetResource.getType() == IResource.PROJECT) {
			// This is a bnd project --> find the bnd.bnd file
			IProject project = (IProject) targetResource;
			if (ws == null)
				return null;
			File bndFile = project.getFile(Project.BNDFILE)
				.getLocation()
				.toFile();
			if (bndFile == null || !bndFile.isFile())
				return null;

			return Bndrun.createBndrun(ws, bndFile);
		} else if (targetResource.getType() == IResource.FILE) {
			// This is file, use directly
			File file = targetResource.getLocation()
				.toFile();
			if (file == null || !file.isFile())
				return null;
			return Bndrun.createBndrun(ws, file);
		} else {
			return null;
		}
	}

}
