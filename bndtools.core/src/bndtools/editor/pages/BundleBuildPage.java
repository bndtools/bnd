package bndtools.editor.pages;


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
import bndtools.editor.project.BuildSectionPart;
import bndtools.editor.project.ClassPathPart;
import bndtools.utils.MessageHyperlinkAdapter;

public class BundleBuildPage extends FormPage {

	private final BndEditModel model;

	public BundleBuildPage(FormEditor editor, BndEditModel model, String id, String title) {
		super(editor, id, title);
		this.model = model;
	}
	@Override
	protected void createFormContent(IManagedForm managedForm) {
		managedForm.setInput(model);

		FormToolkit tk = managedForm.getToolkit();
		ScrolledForm form = managedForm.getForm();
		form.setText("Bundle Build");
		tk.decorateFormHeading(form.getForm());
		form.getForm().addMessageHyperlinkListener(new MessageHyperlinkAdapter(getEditor()));

		// Create Controls
        Composite body = form.getBody();

		Composite panel1 = tk.createComposite(body);

        ClassPathPart classPathPart = new ClassPathPart(panel1, tk, Section.TITLE_BAR | Section.EXPANDED | Section.DESCRIPTION);
        managedForm.addPart(classPathPart);

        BuildSectionPart buildPart = new BuildSectionPart(panel1, tk, Section.TITLE_BAR);
        managedForm.addPart(buildPart);

        Composite panel2 = tk.createComposite(body);

		// Layout
		GridLayout layout;
		GridData gd;

		layout = new GridLayout(2, true);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		body.setLayout(layout);

		gd = new GridData(SWT.FILL, SWT.TOP, true, true);
		panel1.setLayoutData(gd);

		layout = new GridLayout(1, false);
		panel1.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        classPathPart.getSection().setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.TOP, true, true);
        buildPart.getSection().setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.TOP, true, true);
        panel2.setLayoutData(gd);
	};
}