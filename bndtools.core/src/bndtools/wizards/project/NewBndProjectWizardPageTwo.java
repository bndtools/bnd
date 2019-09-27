package bndtools.wizards.project;

import java.lang.reflect.InvocationTargetException;

import org.bndtools.api.BndtoolsConstants;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageOne;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo;

import bndtools.Plugin;

public class NewBndProjectWizardPageTwo extends NewJavaProjectWizardPageTwo {

	public NewBndProjectWizardPageTwo(NewJavaProjectWizardPageOne pageOne) {
		super(pageOne);
	}

	@Override
	public void configureJavaProject(IProgressMonitor monitor) throws CoreException, InterruptedException {
		super.configureJavaProject(monitor);

		IProject project = getJavaProject().getProject();
		IProjectDescription desc = project.getDescription();
		String[] natures = desc.getNatureIds();
		for (String nature : natures) {
			if (BndtoolsConstants.NATURE_ID.equals(nature))
				return;
		}
		String[] newNatures = new String[natures.length + 1];
		System.arraycopy(natures, 0, newNatures, 0, natures.length);
		newNatures[natures.length] = BndtoolsConstants.NATURE_ID;
		desc.setNatureIds(newNatures);
		project.setDescription(desc, null);
	}

	void doSetProjectDesc(final IProject project, final IProjectDescription desc) throws CoreException {
		final IWorkspaceRunnable workspaceOp = monitor -> project.setDescription(desc, monitor);
		try {
			getContainer().run(true, true, monitor -> {
				try {
					project.getWorkspace()
						.run(workspaceOp, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			});
		} catch (InvocationTargetException e) {
			throw (CoreException) e.getTargetException();
		} catch (InterruptedException e) {
			throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0,
				"Interrupted while adding Bnd OSGi Project nature to project.", e));
		}
	}

	@Override
	public boolean isPageComplete() {
		boolean resultFromSuperClass = super.isPageComplete();
		int nr = 0;
		try {
			IClasspathEntry[] entries = getJavaProject().getResolvedClasspath(true);
			for (IClasspathEntry entry : entries) {
				if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					nr++;
					// here we could do more validation on the paths if we want
					// to
					// for now we just count pages
				}
			}
		} catch (Exception e) {
			// if for some reason we cannot access the resolved classpath
			// we simply set an error message
			setErrorMessage("Could not access resolved classpaths: " + e);
		}
		// we're okay if we have exactly at most two valid source paths
		// most templates use 2 source sets (main + test) but some do not
		// have the test source set
		return resultFromSuperClass && nr >= 1;
	}
}
