package bndtools.editor.pages;


import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

import bndtools.editor.common.MDSashForm;
import bndtools.editor.model.BndEditModel;
import bndtools.editor.project.RunBundlesPart;
import bndtools.editor.project.RunFrameworkPart;
import bndtools.editor.project.RunPropertiesPart;
import bndtools.editor.project.RunVMArgsPart;
import bndtools.utils.MessageHyperlinkAdapter;

public class ProjectRunPage extends FormPage {

	private final BndEditModel model;

	public ProjectRunPage(FormEditor editor, BndEditModel model, String id, String title) {
		super(editor, id, title);
		this.model = model;
	}
	@Override
	protected void createFormContent(IManagedForm managedForm) {
		managedForm.setInput(model);

		FormToolkit tk = managedForm.getToolkit();
		ScrolledForm form = managedForm.getForm();
		form.setText("Project Run");
		tk.decorateFormHeading(form.getForm());
		form.getForm().addMessageHyperlinkListener(new MessageHyperlinkAdapter(getEditor()));

		// Create Controls
		Composite body = form.getBody();

		MDSashForm sashForm = new MDSashForm(body, SWT.HORIZONTAL, managedForm);
		tk.adapt(sashForm, false, false);
		sashForm.setSashWidth(6);

		Composite panel1 = tk.createComposite(sashForm);
		Composite panel2 = tk.createComposite(sashForm);

		RunFrameworkPart runFwkPart = new RunFrameworkPart(panel1, tk, Section.TITLE_BAR | Section.EXPANDED);
		managedForm.addPart(runFwkPart);

		RunBundlesPart runBundlesPart = new RunBundlesPart(panel1, tk, Section.TITLE_BAR | Section.EXPANDED | Section.DESCRIPTION);
		managedForm.addPart(runBundlesPart);

        RunPropertiesPart runPropertiesPart = new RunPropertiesPart(panel2, tk, Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED | Section.DESCRIPTION);
		managedForm.addPart(runPropertiesPart);

		RunVMArgsPart vmArgsPart = new RunVMArgsPart(panel2, tk, Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED);
		managedForm.addPart(vmArgsPart);

		sashForm.hookResizeListener();

		// Layout
		GridLayout layout;
		GridData gd;

		body.setLayout(new FillLayout());

		gd = new GridData(SWT.FILL, SWT.TOP, false, true);
		panel1.setLayoutData(gd);

		layout = new GridLayout(1, false);
		layout.verticalSpacing = 10;
		panel1.setLayout(layout);

		layout = new GridLayout(1, false);
		layout.verticalSpacing = 10;
		panel2.setLayout(layout);

		gd = new GridData(SWT.FILL, SWT.TOP, false, true);
		panel2.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        runFwkPart.getSection().setLayoutData(gd);

		gd = new GridData(SWT.FILL, SWT.TOP, true, false);
		runBundlesPart.getSection().setLayoutData(gd);

		gd = new GridData(SWT.FILL, SWT.TOP, true, false);
		runPropertiesPart.getSection().setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        vmArgsPart.getSection().setLayoutData(gd);
	};
}