package bndtools.editor.pages;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

import bndtools.editor.IPageFactory;
import bndtools.editor.model.BndEditModel;
import bndtools.editor.workspace.PluginsPart;
import bndtools.model.clauses.HeaderClause;
import bndtools.utils.MessageHyperlinkAdapter;

public class WorkspacePage extends FormPage {

    private final BndEditModel model;
    private PluginsPart pluginsPart;

    public static IPageFactory FACTORY = new IPageFactory() {
        public IFormPage createPage(FormEditor editor, BndEditModel model, String id) throws IllegalArgumentException {
            return new WorkspacePage(editor, model, id, "Workspace");
        }
    };

    private WorkspacePage(FormEditor editor, BndEditModel model, String id, String title) {
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
        pluginsPart = new PluginsPart(body, tk, Section.TITLE_BAR | Section.EXPANDED | Section.DESCRIPTION);
        managedForm.addPart(pluginsPart);

        // Layout
        TableWrapLayout twl = new TableWrapLayout();
        body.setLayout(twl);
        TableWrapData twd = new TableWrapData();
        twd.align = TableWrapData.FILL;
        twd.valign = TableWrapData.FILL;
        twd.grabVertical = true;
        twd.grabHorizontal = true;
        twd.maxWidth = 200;
        twd.heightHint = 300;
        pluginsPart.getSection().setLayoutData(twd);
    }

    public void setSelectedPlugin(HeaderClause header) {
        pluginsPart.getSelectionProvider().setSelection(new StructuredSelection(header));
    }
}
