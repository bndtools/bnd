package bndtools.editor.pages;

import org.bndtools.core.ui.ExtendedFormEditor;
import org.bndtools.core.ui.IFormPageFactory;
import org.bndtools.core.ui.IFormPageFactory.Mode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
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
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;
import bndtools.api.IBndModel;
import bndtools.editor.common.MDSashForm;
import bndtools.editor.common.SaneDetailsPart;
import bndtools.editor.contents.TestSuitesPart;
import bndtools.utils.MessageHyperlinkAdapter;

public class TestSuitesPage extends FormPage {

    private final IBndModel model;
    private final Image junitImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/junit.gif").createImage();

    public static final IFormPageFactory FACTORY = new IFormPageFactory() {
        public IFormPage createPage(ExtendedFormEditor editor, IBndModel model, String id) throws IllegalArgumentException {
            return new TestSuitesPage(editor, model, id, "Tests");
        }

        public boolean supportsMode(Mode mode) {
            return mode == Mode.bundle;
        }
    };

    public TestSuitesPage(FormEditor editor, IBndModel model, String id, String title) {
        super(editor, id, title);
        this.model = model;
    }

    @Override
    protected void createFormContent(IManagedForm managedForm) {
        managedForm.setInput(model);

        FormToolkit toolkit = managedForm.getToolkit();
        ScrolledForm form = managedForm.getForm();
        form.setText("Tests");
        form.setImage(junitImg);
        toolkit.decorateFormHeading(form.getForm());
        form.getForm().addMessageHyperlinkListener(new MessageHyperlinkAdapter(getEditor()));

        Composite body = form.getBody();
        MDSashForm sashForm = new MDSashForm(body, SWT.HORIZONTAL, managedForm);
        sashForm.setSashWidth(6);
        toolkit.adapt(sashForm, false, false);

        Composite leftPanel = toolkit.createComposite(sashForm);
        Composite rightPanel = toolkit.createComposite(sashForm);

        TestSuitesPart suitesPart = new TestSuitesPart(leftPanel, toolkit, Section.TITLE_BAR | Section.EXPANDED);
        managedForm.addPart(suitesPart);

        SaneDetailsPart detailsPart = new SaneDetailsPart();
        managedForm.addPart(detailsPart);
        // TODO: add details pages here
        detailsPart.createContents(toolkit, rightPanel);

        sashForm.hookResizeListener();

        // LAYOUT
        body.setLayout(new FillLayout());

        GridLayout layout;
        GridData gd;

        layout = new GridLayout();
        leftPanel.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        suitesPart.getSection().setLayoutData(gd);

        layout = new GridLayout();
        rightPanel.setLayout(layout);
    }

    @Override
    public void dispose() {
        super.dispose();
        junitImg.dispose();
    }
}
