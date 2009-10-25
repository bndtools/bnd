package name.neilbartlett.eclipse.jareditor.internal;

import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;

public class JARContentPage extends FormPage implements IFormPart {
	
	private JARContentMasterDetailsBlock contentMasterDetails = new JARContentMasterDetailsBlock();
	
	public JARContentPage(FormEditor editor, String id, String title) {
		super(editor, id, title);
	}
	
	protected void createFormContent(IManagedForm managedForm) {
		contentMasterDetails.createContent(managedForm);
	}

	public void commit(boolean onSave) {
	}

	public void initialize(IManagedForm form) {
	}

	public boolean isStale() {
		return false;
	}

	public void refresh() {
		
	}

	public boolean setFormInput(Object input) {
		contentMasterDetails.setMasterPartInput(input);
		return false;
	}

}
