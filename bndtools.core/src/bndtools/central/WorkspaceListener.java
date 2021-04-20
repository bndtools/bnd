package bndtools.central;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.service.BndListener;

public class WorkspaceListener extends BndListener {
	final IWorkspace		iworkspace	= ResourcesPlugin.getWorkspace();
	final IWorkspaceRoot	iroot		= iworkspace.getRoot();

	// guard
	final Set<IResource>	toRefresh	= new HashSet<>();
	// guard by toRefresh
	boolean					active		= false;

	public WorkspaceListener(Workspace workspace) {}

	@Override
	public void changed(File file) {
		Central.refresher.changed(file, false);
	}

	@Override
	public void built(Project model, Collection<File> files) {
		Central.refresher.changed(files, false);
		Central.getProject(model)
			.ifPresent(project -> {
				EclipseWorkspaceRepository eclipseWorkspaceRepository = Central.getEclipseWorkspaceRepository();
				eclipseWorkspaceRepository.index(project, files);
			});
	}

}
