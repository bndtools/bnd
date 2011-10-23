package bndtools.editor.pages;

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

import bndtools.editor.model.BndEditModel;
import bndtools.editor.project.LaunchPart;
import bndtools.editor.project.RunBundlesPart;
import bndtools.editor.project.RunFrameworkPart;
import bndtools.editor.project.RunPropertiesPart;
import bndtools.editor.project.RunRequirementsPart;
import bndtools.editor.project.RunVMArgsPart;
import bndtools.utils.MessageHyperlinkAdapter;

public class ProjectRunPage extends FormPage {

    private final BndEditModel model;

    public static final IPageFactory FACTORY = new IPageFactory() {
        public IFormPage createPage(FormEditor editor, BndEditModel model, String id) throws IllegalArgumentException {
            return new ProjectRunPage(editor, model, id, "Run");
        }
    };

    public ProjectRunPage(FormEditor editor, BndEditModel model, String id, String title) {
        super(editor, id, title);
        this.model = model;
    }

    @Override
    protected void createFormContent(IManagedForm managedForm) {
        managedForm.setInput(model);

        FormToolkit tk = managedForm.getToolkit();
        final ScrolledForm form = managedForm.getForm();
        form.setText("Run");
        tk.decorateFormHeading(form.getForm());
        form.getForm().addMessageHyperlinkListener(new MessageHyperlinkAdapter(getEditor()));

        // Create Controls
        Composite body = form.getBody();
        final Composite left = tk.createComposite(body);
        final Composite right = tk.createComposite(body);

        RunFrameworkPart runFwkPart = new RunFrameworkPart(left, tk, Section.TITLE_BAR | Section.EXPANDED);
        managedForm.addPart(runFwkPart);

        RunRequirementsPart requirementsPart = new RunRequirementsPart(left, tk, Section.TITLE_BAR | Section.EXPANDED | Section.DESCRIPTION);
        managedForm.addPart(requirementsPart);

        RunBundlesPart runBundlesPart = new RunBundlesPart(left, tk, Section.TITLE_BAR | Section.TWISTIE);
        managedForm.addPart(runBundlesPart);

        LaunchPart launchPart = new LaunchPart(right, tk, Section.TITLE_BAR | Section.EXPANDED);
        managedForm.addPart(launchPart);

        RunPropertiesPart runPropertiesPart = new RunPropertiesPart(right, tk, Section.TITLE_BAR | Section.TWISTIE | Section.DESCRIPTION);
        managedForm.addPart(runPropertiesPart);

        RunVMArgsPart vmArgsPart = new RunVMArgsPart(right, tk, Section.TITLE_BAR | Section.TWISTIE);
        managedForm.addPart(vmArgsPart);

        TableWrapLayout twl;
        TableWrapData twd;

        twl = new TableWrapLayout();
        twl.numColumns = 2;
        twl.makeColumnsEqualWidth = true;
        body.setLayout(twl);

        twd = new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.FILL_GRAB);
        twd.valign = TableWrapData.TOP;
        left.setLayoutData(twd);

        twd = new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.FILL_GRAB);
        twd.valign = TableWrapData.TOP;
        right.setLayoutData(twd);

        twl = new TableWrapLayout();
        twl.numColumns = 1;
        left.setLayout(twl);

        twl = new TableWrapLayout();
        twl.numColumns = 1;
        right.setLayout(twl);

        twd = new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.FILL);
        twd.maxWidth = 200;
        runFwkPart.getSection().setLayoutData(twd);

        twd = new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.FILL);
        twd.maxWidth = 200;
        runBundlesPart.getSection().setLayoutData(twd);

        twd = new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.FILL);
        twd.maxWidth = 200;
        twd.heightHint = 200;
        requirementsPart.getSection().setLayoutData(twd);

        twd = new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.FILL);
        twd.maxWidth = 200;
        launchPart.getSection().setLayoutData(twd);

        twd = new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.FILL);
        twd.maxWidth = 200;
        runPropertiesPart.getSection().setLayoutData(twd);

        twd = new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.FILL);
        twd.maxWidth = 200;
        vmArgsPart.getSection().setLayoutData(twd);
    };
}