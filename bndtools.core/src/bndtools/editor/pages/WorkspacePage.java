package bndtools.editor.pages;

import org.eclipse.jface.viewers.StructuredSelection;
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
import bndtools.editor.workspace.PluginsPart;
import bndtools.model.clauses.HeaderClause;
import bndtools.utils.MessageHyperlinkAdapter;

public class WorkspacePage extends FormPage {

    private final BndEditModel model;
    private PluginsPart pluginsPart;

    public WorkspacePage(FormEditor editor, BndEditModel model, String id, String title) {
        super(editor, id, title);
        this.model = model;
    }

    @Override
    protected void createFormContent(IManagedForm managedForm) {
        managedForm.setInput(model);

        FormToolkit tk = managedForm.getToolkit();
        ScrolledForm form = managedForm.getForm();
        form.setText("Workspace Config");
        tk.decorateFormHeading(form.getForm());
        form.getForm().addMessageHyperlinkListener(new MessageHyperlinkAdapter(getEditor()));

        // Create controls
        Composite body = form.getBody();
        MDSashForm sashForm = new MDSashForm(body, SWT.HORIZONTAL, managedForm);
        sashForm.setSashWidth(6);
        tk.adapt(sashForm, false, false);

        Composite panel1 = tk.createComposite(sashForm);
        Composite panel2 = tk.createComposite(sashForm);

        pluginsPart = new PluginsPart(panel1, tk, Section.TITLE_BAR | Section.EXPANDED | Section.DESCRIPTION);
        managedForm.addPart(pluginsPart);

        // Layout
        body.setLayout(new FillLayout());

        GridLayout layout;
        GridData gd;

        gd = new GridData(SWT.FILL, SWT.TOP, false, true);
        panel1.setLayoutData(gd);

        layout = new GridLayout(1, false);
        panel1.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        pluginsPart.getSection().setLayoutData(gd);
    }

    public void setSelectedPlugin(HeaderClause header) {
        pluginsPart.getSelectionProvider().setSelection(new StructuredSelection(header));
    }
}
