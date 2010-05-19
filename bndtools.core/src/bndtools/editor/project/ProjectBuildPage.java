package bndtools.editor.project;


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

import bndtools.editor.BuildSectionPart;
import bndtools.editor.ClassPathPart;
import bndtools.editor.MDSashForm;
import bndtools.editor.model.BndEditModel;
import bndtools.utils.MessageHyperlinkAdapter;

public class ProjectBuildPage extends FormPage {

	private final BndEditModel model;

	public ProjectBuildPage(FormEditor editor, BndEditModel model, String id, String title) {
		super(editor, id, title);
		this.model = model;
	}

    @Override
    protected void createFormContent(IManagedForm managedForm) {
        managedForm.setInput(model);

        FormToolkit tk = managedForm.getToolkit();
        ScrolledForm form = managedForm.getForm();
        form.setText("Project Build");
        tk.decorateFormHeading(form.getForm());
        form.getForm().addMessageHyperlinkListener(new MessageHyperlinkAdapter(getEditor()));

        // Create Controls
        Composite body = form.getBody();
        MDSashForm sashForm = new MDSashForm(body, SWT.HORIZONTAL, managedForm);
        sashForm.setSashWidth(6);
        tk.adapt(sashForm, false, false);

        Composite panel1 = tk.createComposite(sashForm);

        SubBundlesPart subBundlesPart = new SubBundlesPart(panel1, tk, Section.TITLE_BAR | Section.EXPANDED | Section.DESCRIPTION);
        managedForm.addPart(subBundlesPart);

        BuildPathPart buildPathPart = new BuildPathPart(panel1, tk, Section.TITLE_BAR | Section.EXPANDED | Section.DESCRIPTION);
        managedForm.addPart(buildPathPart);

        Composite panel2 = tk.createComposite(sashForm);

        ClassPathPart classPathPart = new ClassPathPart(panel2, tk, Section.TITLE_BAR | Section.EXPANDED | Section.DESCRIPTION);
        managedForm.addPart(classPathPart);

        BuildSectionPart buildPart = new BuildSectionPart(panel2, tk, Section.TITLE_BAR);
        managedForm.addPart(buildPart);

        sashForm.hookResizeListener();

        // Layout
        GridLayout layout;
        GridData gd;

        body.setLayout(new FillLayout());

        gd = new GridData(SWT.FILL, SWT.TOP, false, true);
        panel1.setLayoutData(gd);

        layout = new GridLayout(1, false);
        panel1.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        buildPathPart.getSection().setLayoutData(gd);
        gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        subBundlesPart.getSection().setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.TOP, false, true);
        panel2.setLayoutData(gd);

        layout = new GridLayout(1, false);
        panel2.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        classPathPart.getSection().setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        buildPart.getSection().setLayoutData(gd);
    };
}