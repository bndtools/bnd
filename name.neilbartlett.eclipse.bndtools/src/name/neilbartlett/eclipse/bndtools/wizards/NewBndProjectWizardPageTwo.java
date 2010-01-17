package name.neilbartlett.eclipse.bndtools.wizards;

import java.lang.reflect.InvocationTargetException;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.builder.BndProjectNature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageOne;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo;
import org.eclipse.jface.operation.IRunnableWithProgress;

public class NewBndProjectWizardPageTwo extends NewJavaProjectWizardPageTwo {
	
	private final NewBndProjectWizardFrameworkPage frameworkPage;

	public NewBndProjectWizardPageTwo(NewJavaProjectWizardPageOne pageOne, NewBndProjectWizardFrameworkPage frameworkPage) {
		super(pageOne);
		this.frameworkPage = frameworkPage;
	}
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if(!visible && getContainer().getCurrentPage() == frameworkPage) {
			removeProvisonalProject();
		}
	}
	@Override
	public void configureJavaProject(String newProjectCompliance,
			IProgressMonitor monitor) throws CoreException,
			InterruptedException {
		super.configureJavaProject(newProjectCompliance, monitor);
		
		IProject project = getJavaProject().getProject();
		IProjectDescription desc = project.getDescription();
		String[] natures = desc.getNatureIds();
		for (String nature : natures) {
			if(BndProjectNature.NATURE_ID.equals(nature))
				return;
		}
		String[] newNatures = new String[natures.length + 1];
		System.arraycopy(natures, 0, newNatures, 0, natures.length);
		newNatures[natures.length] = BndProjectNature.NATURE_ID;
		desc.setNatureIds(newNatures);
		project.setDescription(desc, null);
	}
	
	void doSetProjectDesc(final IProject project, final IProjectDescription desc) throws CoreException {
		final IWorkspaceRunnable workspaceOp = new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				project.setDescription(desc, monitor);
			}
		};
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						project.getWorkspace().run(workspaceOp, monitor);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (InvocationTargetException e) {
			throw (CoreException) e.getTargetException();
		} catch (InterruptedException e) {
			throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Interrupted while adding Bnd OSGi Project nature to project.", e));
		}
	}
}
