package bndtools.javamodel;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.ide.ResourceUtil;

public class FormPartJavaSearchContext implements IJavaSearchContext {

	private AbstractFormPart formPart;

	public FormPartJavaSearchContext(AbstractFormPart formPart) {
		this.formPart = formPart;
	}

	@Override
	public IJavaProject getJavaProject() {
		IFormPage page = getFormPage();
		if (page == null)
			return null;
		IResource resource = ResourceUtil.getResource(page.getEditorInput());
		if (resource == null)
			return null;

		return JavaCore.create(resource.getProject());
	}

	private IFormPage getFormPage() {
		IManagedForm managedForm = formPart.getManagedForm();
		if (managedForm == null)
			return null;

		Object container = managedForm.getContainer();
		if (!(container instanceof IFormPage))
			return null;
		return (IFormPage) container;
	}

	@Override
	public IRunnableContext getRunContext() {
		IFormPage page = getFormPage();
		if (page == null)
			return null;

		return page.getEditorSite()
			.getWorkbenchWindow();
	}

}
