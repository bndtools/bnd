package name.neilbartlett.eclipse.bndtools.editor.imports;

import name.neilbartlett.eclipse.bndtools.editor.BndEditor;
import name.neilbartlett.eclipse.bndtools.editor.MessageHyperlinkAdapter;
import name.neilbartlett.eclipse.bndtools.editor.model.BndEditModel;

import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.IMessage;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class ImportsPage extends FormPage {

	private ImportPatternsBlock block = new ImportPatternsBlock();
	private BndEditModel model;

	public ImportsPage(BndEditor editor, String id, String title) {
		super(editor, id, title);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		
		FormToolkit toolkit = managedForm.getToolkit();
		managedForm.setInput(model);
		
		ScrolledForm scrolledForm = managedForm.getForm();
		scrolledForm.setText("Import Patterns");
		
		Form form = scrolledForm.getForm();
		toolkit.decorateFormHeading(form);
		form.addMessageHyperlinkListener(new MessageHyperlinkAdapter());
		
		block.createContent(managedForm);
	}
	
	@Override
	public void initialize(FormEditor editor) {
		super.initialize(editor);
		this.model = ((BndEditor) editor).getBndModel();
	}
}
