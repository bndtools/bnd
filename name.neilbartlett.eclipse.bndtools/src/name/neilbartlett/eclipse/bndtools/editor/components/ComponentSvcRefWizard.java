package name.neilbartlett.eclipse.bndtools.editor.components;

import java.util.Set;

import name.neilbartlett.eclipse.bndtools.editor.model.ComponentSvcReference;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.wizard.Wizard;

public class ComponentSvcRefWizard extends Wizard {

	private final ComponentSvcRefWizardPage mainPage;
	
	private final ComponentSvcReference serviceRef;
	private final IJavaProject javaProject;
	private final String componentClassName;
	
	// Store a copy of the unedited object to restore from in case of cancellation.
	private final ComponentSvcReference pristine;

	public ComponentSvcRefWizard(Set<String> existingNames, IJavaProject javaProject, String componentClassName) {
		this(new ComponentSvcReference(), existingNames, javaProject, componentClassName);
	}
	
	public ComponentSvcRefWizard(ComponentSvcReference serviceRef, Set<String> existingNames, IJavaProject javaProject, String componentClassName) {
		this.serviceRef = serviceRef;
		this.javaProject = javaProject;
		this.componentClassName = componentClassName;
		
		this.pristine = serviceRef.clone();
		this.mainPage = new ComponentSvcRefWizardPage(serviceRef, "main", existingNames, javaProject, componentClassName);
	}
	
	@Override
	public void addPages() {
		addPage(mainPage);
	}
	
	@Override
	public boolean performCancel() {
		serviceRef.copyFrom(pristine);
		return true;
	}

	@Override
	public boolean performFinish() {
		return true;
	}
	
	public ComponentSvcReference getResult() {
		return serviceRef;
	}
}
