package bndtools.editor.pages;


import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

import bndtools.editor.common.MDSashForm;
import bndtools.editor.model.BndEditModel;
import bndtools.editor.project.BuildPathPart;
import bndtools.editor.project.SubBundlesPart;
import bndtools.utils.MessageHyperlinkAdapter;

public class ProjectBuildPage extends FormPage {

	private final BndEditModel model;

    public static final IPageFactory FACTORY = new IPageFactory() {
        public IFormPage createPage(FormEditor editor, BndEditModel model, String id) throws IllegalArgumentException {
            return new ProjectBuildPage(editor, model, id, "Build");
        }
    };

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

        GridLayout layout;
        GridData gd;

        // Create Controls
        Composite body = form.getBody();
        body.setLayout(new FillLayout());

        MDSashForm sashForm = new MDSashForm(body, SWT.HORIZONTAL, managedForm);
        sashForm.setSashWidth(6);
        tk.adapt(sashForm, false, false);
        sashForm.hookResizeListener();

        Composite leftPanel = tk.createComposite(sashForm);
        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        leftPanel.setLayoutData(gd);

        layout = new GridLayout(1, false);
        leftPanel.setLayout(layout);

        SubBundlesPart subBundlesPart = new SubBundlesPart(leftPanel, tk, Section.TITLE_BAR | Section.EXPANDED | Section.DESCRIPTION);
        managedForm.addPart(subBundlesPart);
        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        subBundlesPart.getSection().setLayoutData(gd);

        BuildPathPart buildPathPart = new BuildPathPart(leftPanel, tk, Section.TITLE_BAR | Section.EXPANDED | Section.DESCRIPTION);
        managedForm.addPart(buildPathPart);
        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.widthHint = 50;
        gd.heightHint = 50;
        buildPathPart.getSection().setLayoutData(gd);

        Composite rightPanel = tk.createComposite(sashForm);
        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        rightPanel.setLayoutData(gd);

        layout = new GridLayout(1, false);
        rightPanel.setLayout(layout);

    };
}