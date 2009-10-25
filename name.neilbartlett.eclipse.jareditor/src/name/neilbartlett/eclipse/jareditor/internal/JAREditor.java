package name.neilbartlett.eclipse.jareditor.internal;


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;

public class JAREditor extends FormEditor {
	
	JARContentPage contentPage = new JARContentPage(this, "contentPage", "Content");
	
	protected void addPages() {
		try {
			addPage(contentPage);
		} catch (PartInitException e) {
			e.printStackTrace();
		}
	}

	public void doSave(IProgressMonitor monitor) {
	}

	public void doSaveAs() {
	}

	public boolean isSaveAsAllowed() {
		return false;
	}
	
	protected void setInput(IEditorInput input) {
		super.setInput(input);
		contentPage.setFormInput(input);
		
		if(input instanceof IFileEditorInput) {
			String name = ((IFileEditorInput) input).getFile().getName();
			setPartName(name);
		}
	}
}
