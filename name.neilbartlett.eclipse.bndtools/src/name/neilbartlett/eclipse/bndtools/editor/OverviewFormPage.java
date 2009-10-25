package name.neilbartlett.eclipse.bndtools.editor;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

public class OverviewFormPage extends FormPage {

	public OverviewFormPage(FormEditor editor, String id, String title) {
		super(editor, id, title);
	}

	public OverviewFormPage(String id, String title) {
		super(id, title);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		FormToolkit toolkit = managedForm.getToolkit();
		ScrolledForm form = managedForm.getForm();
		form.setText("Overview");
		
		Composite body = form.getBody();
		
		TableWrapLayout layout = new TableWrapLayout();
        layout.bottomMargin = 10;
        layout.topMargin = 5;
        layout.leftMargin = 10;
        layout.rightMargin = 10;
        layout.numColumns = 2;
        layout.horizontalSpacing = 10;
        
        body.setLayout(layout);
        body.setLayoutData(new TableWrapData(TableWrapData.FILL));
        
        GeneralInfoSectionPart bundleDetailsSection = new GeneralInfoSectionPart(body, toolkit);
        managedForm.addPart(bundleDetailsSection);
	}


}
