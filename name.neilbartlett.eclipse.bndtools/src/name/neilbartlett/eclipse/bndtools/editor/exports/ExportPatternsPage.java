package name.neilbartlett.eclipse.bndtools.editor.exports;

import name.neilbartlett.eclipse.bndtools.editor.model.BndEditModel;
import name.neilbartlett.eclipse.bndtools.utils.MessageHyperlinkAdapter;

import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class ExportPatternsPage extends FormPage {

	private final ExportPatternsBlock block = new ExportPatternsBlock();
	private final BndEditModel model;

	public ExportPatternsPage(FormEditor editor, BndEditModel model, String id, String title) {
		super(editor, id, title);
		this.model = model;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		FormToolkit toolkit = managedForm.getToolkit();
		managedForm.setInput(model);
		
		ScrolledForm scrolledForm = managedForm.getForm();
		scrolledForm.setText("Export Patterns");
		
		Form form = scrolledForm.getForm();
		toolkit.decorateFormHeading(form);
		form.addMessageHyperlinkListener(new MessageHyperlinkAdapter());
		
		block.createContent(managedForm);
	}
}
