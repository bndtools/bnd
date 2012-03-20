package bndtools.editor.pages;

import org.bndtools.core.ui.ExtendedFormEditor;
import org.bndtools.core.ui.IFormPageFactory;
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

import bndtools.api.IBndModel;
import bndtools.editor.workspace.PluginsPart;
import bndtools.editor.workspace.WorkspaceMainPart;
import bndtools.model.clauses.HeaderClause;
import bndtools.utils.MessageHyperlinkAdapter;

public class WorkspacePage extends FormPage {

    private final IBndModel model;
    private PluginsPart pluginsPart;

    public static IFormPageFactory MAIN_FACTORY = new IFormPageFactory() {
        public IFormPage createPage(ExtendedFormEditor editor, IBndModel model, String id) throws IllegalArgumentException {
            return new WorkspacePage(true, editor, model, id, "Workspace");
        }

        public boolean supportsMode(Mode mode) {
            return mode == Mode.workspace;
        }
    };

    public static IFormPageFactory EXT_FACTORY = new IFormPageFactory() {
        public IFormPage createPage(ExtendedFormEditor editor, IBndModel model, String id) throws IllegalArgumentException {
            return new WorkspacePage(false, editor, model, id, "Workspace");
        }

        public boolean supportsMode(Mode mode) {
            return mode == Mode.workspace;
        }
    };

    private final boolean mainBuildFile;

    private WorkspacePage(boolean mainBuildFile, FormEditor editor, IBndModel model, String id, String title) {
        super(editor, id, title);
        this.mainBuildFile = mainBuildFile;
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

        WorkspaceMainPart linksPart = new WorkspaceMainPart(mainBuildFile, body, tk, Section.TITLE_BAR | Section.EXPANDED | Section.DESCRIPTION);
        managedForm.addPart(linksPart);

        pluginsPart = new PluginsPart(body, tk, Section.TITLE_BAR | Section.EXPANDED | Section.DESCRIPTION);
        managedForm.addPart(pluginsPart);

        // Layout
        TableWrapLayout twl;
        TableWrapData twd;

        twl = new TableWrapLayout();
        body.setLayout(twl);

        twd = new TableWrapData();
        twd.align = TableWrapData.FILL;
        twd.valign = TableWrapData.FILL;
        twd.grabVertical = true;
        twd.grabHorizontal = true;
        twd.maxWidth = 200;
        linksPart.getSection().setLayoutData(twd);

        twd = new TableWrapData();
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
