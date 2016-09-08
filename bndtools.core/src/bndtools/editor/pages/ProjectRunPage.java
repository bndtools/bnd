package bndtools.editor.pages;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.bndtools.core.ui.ExtendedFormEditor;
import org.bndtools.core.ui.IFormPageFactory;
import org.bndtools.core.ui.icons.Icons;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Success;

import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.BndEditModel;
import bndtools.Plugin;
import bndtools.central.Central;
import bndtools.editor.common.MDSashForm;
import bndtools.editor.project.AvailableBundlesPart;
import bndtools.editor.project.RepositorySelectionPart;
import bndtools.editor.project.RunBlacklistPart;
import bndtools.editor.project.RunBundlesPart;
import bndtools.editor.project.RunFrameworkPart;
import bndtools.editor.project.RunPropertiesPart;
import bndtools.editor.project.RunRequirementsPart;
import bndtools.utils.MessageHyperlinkAdapter;

public class ProjectRunPage extends FormPage {

    private final BndEditModel model;

    private final Image imgBndLayout = Icons.desc("bnd.workspace.bndlayout").createImage();
    private final Image imgStandaloneLayout = Icons.desc("bnd.workspace.standalone").createImage();

    public static final IFormPageFactory FACTORY_PROJECT = new IFormPageFactory() {
        @Override
        public IFormPage createPage(ExtendedFormEditor editor, BndEditModel model, String id) throws IllegalArgumentException {
            //
            // Re-enabling Resolve see: https://github.com/bndtools/bndtools/issues/651
            // There might be some issues around this
            // but we have to figure them out
            //
            return new ProjectRunPage(editor, model, id, "Run", true);
        }

        @Override
        public boolean supportsMode(Mode mode) {
            return mode == Mode.project;
        }
    };

    public static final IFormPageFactory FACTORY_BNDRUN = new IFormPageFactory() {
        @Override
        public IFormPage createPage(ExtendedFormEditor editor, BndEditModel model, String id) throws IllegalArgumentException {
            return new ProjectRunPage(editor, model, id, "Run", true);
        }

        @Override
        public boolean supportsMode(Mode mode) {
            return mode == Mode.bndrun;
        }
    };

    private final boolean supportsResolve;

    public ProjectRunPage(FormEditor editor, BndEditModel model, String id, String title, boolean supportsResolve) {
        super(editor, id, title);
        this.model = model;
        this.supportsResolve = supportsResolve;
    }

    @Override
    protected void createFormContent(IManagedForm managedForm) {
        managedForm.setInput(model);

        FormToolkit tk = managedForm.getToolkit();
        final ScrolledForm form = managedForm.getForm();
        form.setText("Resolve/Run");

        Central.onWorkspaceInit(new Success<Workspace,Void>() {
            @Override
            public Promise<Void> call(Promise<Workspace> resolved) throws Exception {
                final Deferred<Void> completion = new Deferred<>();
                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            updateFormImage(form);
                            completion.resolve(null);
                        } catch (Exception e) {
                            completion.fail(e);
                        }
                    }
                });
                return completion.getPromise();
            }
        });

        tk.decorateFormHeading(form.getForm());
        form.getForm().addMessageHyperlinkListener(new MessageHyperlinkAdapter(getEditor()));

        // Toolbar Actions
        RunAction runAction = new RunAction(this, "run");
        runAction.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/run.gif"));
        runAction.setText("Run OSGi");
        ActionContributionItem runContrib = new ActionContributionItem(runAction);
        runContrib.setMode(ActionContributionItem.MODE_FORCE_TEXT);
        form.getToolBarManager().add(runContrib);

        RunAction debugAction = new RunAction(this, "debug");
        debugAction.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/debug.gif"));
        debugAction.setText("Debug OSGi");
        ActionContributionItem debugContrib = new ActionContributionItem(debugAction);
        debugContrib.setMode(ActionContributionItem.MODE_FORCE_TEXT);
        form.getToolBarManager().add(debugContrib);

        ExportAction exportAction = new ExportAction(getEditorSite().getShell(), getEditor(), model);
        exportAction.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/run_export.png"));
        exportAction.setText("Export");
        ActionContributionItem exportContrib = new ActionContributionItem(exportAction);
        exportContrib.setMode(ActionContributionItem.MODE_FORCE_TEXT);
        if (exportAction.shouldEnable())
            form.getToolBarManager().add(exportContrib);

        form.getToolBarManager().update(true);

        GridLayout gl;
        GridData gd;

        // Create Controls
        final Composite body = form.getBody();

        MDSashForm sashForm = new MDSashForm(body, SWT.HORIZONTAL, managedForm);
        sashForm.setSashWidth(6);
        tk.adapt(sashForm);

        final Composite left = tk.createComposite(sashForm);
        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        left.setLayoutData(gd);
        gl = new GridLayout(1, true);
        left.setLayout(gl);

        final Composite right = tk.createComposite(sashForm);
        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        right.setLayoutData(gd);
        gl = new GridLayout(1, true);
        right.setLayout(gl);

        // First column
        RepositorySelectionPart reposPart = new RepositorySelectionPart(getEditor(), left, tk, Section.TITLE_BAR | Section.TWISTIE);
        managedForm.addPart(reposPart);
        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.widthHint = 50;
        gd.heightHint = 50;
        reposPart.getSection().setLayoutData(PageLayoutUtils.createCollapsed());

        AvailableBundlesPart availableBundlesPart = new AvailableBundlesPart(left, tk, Section.TITLE_BAR | Section.EXPANDED);
        managedForm.addPart(availableBundlesPart);
        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.widthHint = 50;
        gd.heightHint = 50;
        availableBundlesPart.getSection().setLayoutData(PageLayoutUtils.createExpanded());

        RunFrameworkPart runFwkPart = new RunFrameworkPart(left, tk, Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED);
        managedForm.addPart(runFwkPart);
        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        runFwkPart.getSection().setLayoutData(gd);

        RunPropertiesPart runPropertiesPart = new RunPropertiesPart(left, tk, Section.TITLE_BAR | Section.TWISTIE);
        managedForm.addPart(runPropertiesPart);
        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        runPropertiesPart.getSection().setLayoutData(gd);

        // SECOND COLUMN
        if (supportsResolve) {
            RunRequirementsPart requirementsPart = new RunRequirementsPart(right, tk, Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED | Section.DESCRIPTION);
            managedForm.addPart(requirementsPart);
            requirementsPart.getSection().setLayoutData(PageLayoutUtils.createExpanded());
            requirementsPart.getSection().addExpansionListener(new ResizeExpansionAdapter(requirementsPart.getSection()));

            RunBlacklistPart blacklistPart = new RunBlacklistPart(right, tk, Section.TITLE_BAR | Section.TWISTIE | Section.COMPACT | Section.DESCRIPTION);
            managedForm.addPart(blacklistPart);
            blacklistPart.getSection().setLayoutData(PageLayoutUtils.createCollapsed());
            blacklistPart.getSection().addExpansionListener(new ResizeExpansionAdapter(blacklistPart.getSection()));

            RunBundlesPart runBundlesPart = new RunBundlesPart(right, tk, Section.TITLE_BAR | Section.TWISTIE);
            managedForm.addPart(runBundlesPart);
            runBundlesPart.getSection().setLayoutData(PageLayoutUtils.createCollapsed());
            runBundlesPart.getSection().addExpansionListener(new ResizeExpansionAdapter(runBundlesPart.getSection()));
        } else {
            RunBundlesPart runBundlesPart = new RunBundlesPart(right, tk, Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED);
            managedForm.addPart(runBundlesPart);
            runBundlesPart.getSection().setLayoutData(PageLayoutUtils.createExpanded());
            runBundlesPart.getSection().addExpansionListener(new ResizeExpansionAdapter(runBundlesPart.getSection()));
        }

        // Listeners
        model.addPropertyChangeListener(BndEditModel.PROP_WORKSPACE, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        updateFormImage(form);
                    }
                });
            }
        });

        sashForm.setWeights(new int[] {
                1, 1
        });
        sashForm.hookResizeListener();
        body.setLayout(new FillLayout());
    }

    @Override
    public void dispose() {
        super.dispose();
        imgBndLayout.dispose();
        imgStandaloneLayout.dispose();
    }

    private void updateFormImage(final ScrolledForm form) {
        Workspace ws = model.getWorkspace();
        switch (ws.getLayout()) {
        case BND :
            form.setImage(imgBndLayout);
            break;
        case STANDALONE :
            form.setImage(imgStandaloneLayout);
            break;
        default :
        }
    }
}