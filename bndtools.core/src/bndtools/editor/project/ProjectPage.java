package bndtools.editor.project;


import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

import bndtools.editor.model.BndEditModel;
import bndtools.utils.MessageHyperlinkAdapter;

public class ProjectPage extends FormPage {
	
	private BndEditModel model;

	public ProjectPage(FormEditor editor, BndEditModel model, String id, String title) {
		super(editor, id, title);
		this.model = model;
	}
	@Override
	protected void createFormContent(IManagedForm managedForm) {
		managedForm.setInput(model);

		FormToolkit tk = managedForm.getToolkit();
		ScrolledForm form = managedForm.getForm();
		form.setText("Project");
		tk.decorateFormHeading(form.getForm());
		form.getForm().addMessageHyperlinkListener(new MessageHyperlinkAdapter(getEditor()));

		// Create Controls
		Composite body = form.getBody();
		
		Composite panel1 = tk.createComposite(body);
		Composite panel2 = tk.createComposite(body);
		
		SubBundlesPart subBundlesPart = new SubBundlesPart(panel1, tk, Section.TITLE_BAR | Section.EXPANDED | Section.DESCRIPTION);
		managedForm.addPart(subBundlesPart);
		
		BuildPathPart buildPathPart = new BuildPathPart(panel1, tk, Section.TITLE_BAR | Section.EXPANDED | Section.DESCRIPTION);
		managedForm.addPart(buildPathPart);

		RunBundlesPart runBundlesPart = new RunBundlesPart(panel2, tk, Section.TITLE_BAR | Section.EXPANDED | Section.DESCRIPTION);
		managedForm.addPart(runBundlesPart);
		
        RunPropertiesPart runPropertiesPart = new RunPropertiesPart(panel2, tk, Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED);
		managedForm.addPart(runPropertiesPart);

		// Layout
		GridLayout layout;
		GridData gd;

		layout = new GridLayout(2, false);
		layout.verticalSpacing = 10;
		layout.horizontalSpacing = 10;
		body.setLayout(layout);
		
		gd = new GridData(SWT.FILL, SWT.TOP, false, true);
		panel1.setLayoutData(gd);
		
		layout = new GridLayout(1, false);
		layout.verticalSpacing = 10;
		layout.horizontalSpacing = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		panel1.setLayout(layout);
		
		layout = new GridLayout(1, false);
		layout.verticalSpacing = 10;
		layout.horizontalSpacing = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		panel2.setLayout(layout);

		gd = new GridData(SWT.FILL, SWT.TOP, false, true);
		panel2.setLayoutData(gd);

		gd = new GridData(SWT.FILL, SWT.TOP, true, false);
		buildPathPart.getSection().setLayoutData(gd);
		gd = new GridData(SWT.FILL, SWT.TOP, true, false);
		subBundlesPart.getSection().setLayoutData(gd);
		gd = new GridData(SWT.FILL, SWT.TOP, true, false);
		runBundlesPart.getSection().setLayoutData(gd);
		gd = new GridData(SWT.FILL, SWT.TOP, true, false);
		runPropertiesPart.getSection().setLayoutData(gd);
	};
}