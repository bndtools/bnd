package name.neilbartlett.eclipse.bndtools.editor.components;

import name.neilbartlett.eclipse.bndtools.editor.BndEditor;
import name.neilbartlett.eclipse.bndtools.editor.model.BndEditModel;

import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class ComponentsPage extends FormPage {

	private final ComponentsBlock block = new ComponentsBlock();
	private BndEditModel model;

	public ComponentsPage(BndEditor editor, String id, String title) {
		super(editor, id, title);
	}
	
	@Override
	protected void createFormContent(IManagedForm managedForm) {
		managedForm.setInput(model);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		toolkit.decorateFormHeading(form.getForm());
		
		form.setText("Components");
		block.createContent(managedForm);
	}
	
	@Override
	public void initialize(FormEditor editor) {
		super.initialize(editor);
		this.model = ((BndEditor) editor).getBndModel();
	}
}
