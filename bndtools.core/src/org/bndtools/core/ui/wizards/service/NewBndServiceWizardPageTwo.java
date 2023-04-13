package org.bndtools.core.ui.wizards.service;

import org.bndtools.api.BndtoolsConstants;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;

public class NewBndServiceWizardPageTwo extends NewJavaProjectWizardPageTwo {

	protected NewBndServiceWizardPageOne pageOne;

	public NewBndServiceWizardPageTwo(NewBndServiceWizardPageOne pageOne) {
		super(pageOne);
		this.pageOne = pageOne;
	}

	private IWorkingSet[] workingSets;

	@Override
	public void configureJavaProject(IProgressMonitor monitor) throws CoreException, InterruptedException {
		super.configureJavaProject(monitor);

		IJavaProject javaProject = getJavaProject();
		IProject project = javaProject.getProject();
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

		// Accessing pageOne.getWorkingSets() has to be on SWT thread
		Display.getDefault()
				.syncExec(() -> {
				workingSets = this.pageOne
						.getWorkingSets();
				});
		// If the api page has provided us with any working sets then we add
		// ourselves to those working sets as well
		if (this.workingSets != null && this.workingSets.length > 0) {
			PlatformUI.getWorkbench()
				.getWorkingSetManager()
				.addToWorkingSets(javaProject, this.workingSets);
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
				}
			}
		} catch (Exception e) {
			setErrorMessage("Could not access resolved classpaths: " + e);
		}
		// we're okay if we have exactly at most two valid source paths
		// most templates use 2 source sets (main + test) but some do not
		// have the test source set
		return resultFromSuperClass && nr >= 1;
	}

}
