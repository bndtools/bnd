package bndtools.editor.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.api.ResolveMode;
import org.bndtools.core.resolve.ResolutionResult.Outcome;
import org.bndtools.core.resolve.ResolveJob;
import org.bndtools.core.resolve.ui.ResolutionWizard;
import org.bndtools.core.ui.resource.RequirementLabelProvider;
import org.bndtools.utils.dnd.AbstractViewerDropAdapter;
import org.bndtools.utils.dnd.SupportedTransfer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;

import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.Filters;
import aQute.bnd.version.Version;
import aQute.libg.filters.AndFilter;
import aQute.libg.filters.Filter;
import aQute.libg.filters.LiteralFilter;
import aQute.libg.filters.Operator;
import aQute.libg.filters.SimpleFilter;
import aQute.libg.qtokens.QuotedTokenizer;
import bndtools.BndConstants;
import bndtools.Plugin;
import bndtools.editor.common.BndEditorPart;
import bndtools.model.repo.DependencyPhase;
import bndtools.model.repo.ProjectBundle;
import bndtools.model.repo.RepositoryBundle;
import bndtools.model.repo.RepositoryBundleVersion;
import bndtools.wizards.repo.RepoBundleSelectionWizard;

public class RunRequirementsPart extends BndEditorPart implements PropertyChangeListener {

    @SuppressWarnings("deprecation")
    private static final String RUNREQUIRE = BndConstants.RUNREQUIRE;
    private static final ILogger logger = Logger.getLogger(RunRequirementsPart.class);

    private static final String[] SUBSCRIBE_PROPS = new String[] {
            RUNREQUIRE, BndConstants.RUNREQUIRES, BndConstants.RESOLVE_MODE
    };

    private Table table;
    private TableViewer viewer;
    private Button btnAutoResolve;

    private List<Requirement> requires;
    private ResolveMode resolveMode;

    private final Image resolveIcon = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/wand.png").createImage();

    private ToolItem addBundleTool;
    private ToolItem removeTool;
    private Button btnResolveNow;

    private boolean committing = false;

    public RunRequirementsPart(Composite parent, FormToolkit toolkit, int style) {
        super(parent, toolkit, style);
        createSection(getSection(), toolkit);
    }

    @Override
    protected String[] getProperties() {
        return SUBSCRIBE_PROPS;
    }

    private void createSection(Section section, FormToolkit tk) {
        section.setText("Run Requirements");
        section.setDescription("The specified requirements will be used to resolve a set of runtime bundles from available repositories.");

        // Create toolbar
        ToolBar toolbar = new ToolBar(section, SWT.FLAT);
        section.setTextClient(toolbar);
        fillToolBar(toolbar);

        // Create main panel
        Composite composite = tk.createComposite(section);
        section.setClient(composite);

        table = tk.createTable(composite, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
        viewer = new TableViewer(table);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setLabelProvider(new RequirementLabelProvider());

        btnAutoResolve = tk.createButton(composite, "Auto-resolve on save", SWT.CHECK);

        btnResolveNow = tk.createButton(composite, "Resolve", SWT.PUSH);
        btnResolveNow.setImage(resolveIcon);

        // Listeners
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                removeTool.setEnabled(!viewer.getSelection().isEmpty());
            }
        });
        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.character == SWT.DEL) {
                    doRemove();
                } else if (e.character == '+') {
                    doAddBundle();
                }
            }
        });
        RequirementViewerDropAdapter dropper = new RequirementViewerDropAdapter();
        dropper.install(viewer);
        btnAutoResolve.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                ResolveMode old = resolveMode;
                resolveMode = btnAutoResolve.getSelection() ? ResolveMode.auto : ResolveMode.manual;
                updateButtonStates();

                if (old != resolveMode)
                    markDirty();
            }
        });
        btnResolveNow.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                btnResolveNow.setEnabled(false);
                doResolve();
            }
        });

        // Layout
        GridLayout layout;
        GridData gd;

        layout = new GridLayout(2, false);
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 5;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        composite.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
        gd.widthHint = 50;
        gd.heightHint = 50;
        table.setLayoutData(gd);

        gd = new GridData(SWT.RIGHT, SWT.CENTER, true, false);
        btnResolveNow.setLayoutData(gd);
    }

    private void fillToolBar(ToolBar toolbar) {
        // Add Bundle
        addBundleTool = new ToolItem(toolbar, SWT.PUSH);
        addBundleTool.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ADD));
        addBundleTool.setToolTipText("Add Bundle Requirement");
        addBundleTool.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doAddBundle();
            }
        });

        // Remove
        removeTool = new ToolItem(toolbar, SWT.PUSH);
        removeTool.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE));
        removeTool.setDisabledImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE_DISABLED));
        removeTool.setToolTipText("Remove");
        removeTool.setEnabled(false);
        removeTool.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doRemove();
            }
        });
    }

    private static Requirement createRequirement(Object elem) {
        String bsn = null;
        Version version = null;

        if (elem instanceof RepositoryBundle) {
            bsn = ((RepositoryBundle) elem).getBsn();
        } else if (elem instanceof RepositoryBundleVersion) {
            RepositoryBundleVersion rbv = (RepositoryBundleVersion) elem;
            bsn = rbv.getBsn();
            version = rbv.getVersion();
        } else if (elem instanceof ProjectBundle) {
            bsn = ((ProjectBundle) elem).getBsn();
        }

        if (bsn != null) {
            Filter filter = new SimpleFilter(IdentityNamespace.IDENTITY_NAMESPACE, bsn);
            if (version != null) {
                filter = new AndFilter().addChild(filter).addChild(new SimpleFilter("version", Operator.GreaterThanOrEqual, version.toString()));
            }
            Requirement req = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE).addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter.toString()).buildSyntheticRequirement();
            return req;
        }
        return null;
    }

    private void doAddBundle() {
        try {
            RepoBundleSelectionWizard wizard = new RepoBundleSelectionWizard(new ArrayList<VersionedClause>(), DependencyPhase.Run);
            wizard.setSelectionPageTitle("Add Bundle Requirement");
            WizardDialog dialog = new WizardDialog(getSection().getShell(), wizard);

            if (Window.OK == dialog.open()) {
                List<VersionedClause> result = wizard.getSelectedBundles();
                Set<Requirement> adding = new LinkedHashSet<Requirement>(result.size());
                for (VersionedClause bundle : result) {
                    Filter filter = new SimpleFilter(IdentityNamespace.IDENTITY_NAMESPACE, bundle.getName());

                    String versionRange = bundle.getVersionRange();
                    if (versionRange != null && !"latest".equals(versionRange)) {
                        filter = new AndFilter().addChild(filter).addChild(new LiteralFilter(Filters.fromVersionRange(versionRange)));
                    }
                    Requirement req = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE).addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter.toString()).buildSyntheticRequirement();
                    adding.add(req);
                }
                updateViewerWithNewRequirements(adding);
            }
        } catch (Exception e) {
            ErrorDialog.openError(getSection().getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error selecting bundles.", e));
        }

    }

    private void doRemove() {
        IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
        if (!selection.isEmpty()) {
            Iterator< ? > elements = selection.iterator();
            List<Object> removed = new LinkedList<Object>();
            while (elements.hasNext()) {
                Object element = elements.next();
                if (requires.remove(element))
                    removed.add(element);
            }

            if (!removed.isEmpty()) {
                viewer.remove(removed.toArray());
                markDirty();
            }
        }
    }

    private void doResolve() {
        // Make sure all the parts of this editor page have committed their
        // dirty state to the model
        IFormPart[] parts = getManagedForm().getParts();
        for (IFormPart part : parts) {
            if (part.isDirty())
                part.commit(false);

        }

        final IFormPage page = (IFormPage) getManagedForm().getContainer();
        final IEditorInput input = page.getEditorInput();
        final IEditorPart editor = page.getEditor();
        final IFile file = ResourceUtil.getFile(input);
        final Shell parentShell = page.getEditor().getSite().getShell();

        // Create the wizard and pre-validate
        final ResolveJob job = new ResolveJob(model);
        IStatus validation = job.validateBeforeRun();
        if (!validation.isOK()) {
            ErrorDialog errorDialog = new ErrorDialog(parentShell, "Validation Problem", null, validation, IStatus.ERROR | IStatus.WARNING) {
                @Override
                protected void createButtonsForButtonBar(Composite parent) {
                    // create OK, Cancel and Details buttons
                    createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, false);
                    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, true);
                    createDetailsButton(parent);
                }
            };
            int response = errorDialog.open();
            if (Window.CANCEL == response || validation.getSeverity() >= IStatus.ERROR) {
                btnResolveNow.setEnabled(true);
                return;
            }
        }

        // Add the operation to perform at the end of the resolution job (i.e.,
        // showing the result)
        final Runnable showResult = new Runnable() {
            @Override
            public void run() {
                ResolutionWizard wizard = new ResolutionWizard(model, file, job.getResolutionResult());
                WizardDialog dialog = new WizardDialog(parentShell, wizard);
                boolean dirtyBeforeResolve = editor.isDirty();
                if (dialog.open() == Dialog.OK && !dirtyBeforeResolve) {
                    // only save the editor, when no unsaved changes happened before resolution
                    editor.getEditorSite().getPage().saveEditor(editor, false);
                }

                btnResolveNow.setEnabled(true);
            }
        };
        job.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                Outcome outcome = job.getResolutionResult().getOutcome();
                if (outcome != Outcome.Cancelled)
                    parentShell.getDisplay().asyncExec(showResult);
            }
        });

        job.setUser(true);
        job.schedule();
    }

    @Override
    public void commitToModel(boolean onSave) {
        try {
            committing = true;
            model.setRunRequires(requires);
            model.genericSet(RUNREQUIRE, null);
            setResolveMode();
        } finally {
            committing = false;
        }
    }

    @Override
    public void refreshFromModel() {
        List<Requirement> tmp = model.getRunRequires();
        if (tmp == null) {
            String legacyReqStr = (String) model.genericGet(RUNREQUIRE);
            if (legacyReqStr != null) {
                tmp = convertLegacyRequireList(legacyReqStr);
            }
        }

        requires = new ArrayList<Requirement>(tmp != null ? tmp : Collections.<Requirement> emptyList());
        viewer.setInput(requires);

        resolveMode = getResolveMode();
        btnAutoResolve.setSelection(resolveMode == ResolveMode.auto);
        updateButtonStates();
    }

    private void setResolveMode() {
        String formatted;
        if (resolveMode == ResolveMode.manual || resolveMode == null)
            formatted = null;
        else
            formatted = resolveMode.toString();
        model.genericSet(BndConstants.RESOLVE_MODE, formatted);
    }

    private ResolveMode getResolveMode() {
        ResolveMode resolveMode = ResolveMode.manual;
        try {
            String str = (String) model.genericGet(BndConstants.RESOLVE_MODE);
            if (str != null)
                resolveMode = Enum.valueOf(ResolveMode.class, str);
        } catch (Exception e) {
            logger.logError("Error parsing '-resolve' header.", e);
        }
        return resolveMode;
    }

    private List<Requirement> convertLegacyRequireList(String input) throws IllegalArgumentException {
        List<Requirement> result = new ArrayList<Requirement>();
        if (Constants.EMPTY_HEADER.equalsIgnoreCase(input.trim()))
            return result;

        QuotedTokenizer qt = new QuotedTokenizer(input, ",");
        String token = qt.nextToken();

        while (token != null) {
            String item = token.trim();
            int index = item.indexOf(":");
            if (index < 0)
                throw new IllegalArgumentException("Invalid format for requirement");

            String name = item.substring(0, index);
            String filter = item.substring(index + 1);
            Requirement req = new CapReqBuilder(name).addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter).buildSyntheticRequirement();
            result.add(req);

            token = qt.nextToken();
        }

        return result;
    }

    private void updateButtonStates() {
        // btnResolveNow.setEnabled(resolveMode != ResolveMode.auto);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (!committing) {
            IFormPage page = (IFormPage) getManagedForm().getContainer();
            if (page.isActive()) {
                refresh();
            } else {
                markStale();
            }
        }
    }

    /**
     * Update the requirements already available with new ones. Already existing requirements will be removed from the
     * given set.
     *
     * @param adding
     *            Set with {@link Requirement}s to add
     * @return true if requirements were added.
     */
    private boolean updateViewerWithNewRequirements(Set<Requirement> adding) {
        // remove duplicates
        adding.removeAll(requires);
        if (adding.isEmpty()) {
            return false;
        }
        requires.addAll(adding);
        viewer.add(adding.toArray());
        markDirty();
        return true;
    }

    private class RequirementViewerDropAdapter extends AbstractViewerDropAdapter {

        public RequirementViewerDropAdapter() {
            super(viewer, EnumSet.of(SupportedTransfer.LocalSelection));
        }

        @Override
        protected boolean performSelectionDrop(ISelection data, Object target, int location) {
            Set<Requirement> adding = new LinkedHashSet<Requirement>();

            if (data instanceof IStructuredSelection) {
                IStructuredSelection structSel = (IStructuredSelection) data;

                for (Object elem : structSel.toList()) {
                    Requirement requirement = createRequirement(elem);
                    if (requirement != null)
                        adding.add(requirement);
                }
            }

            return updateViewerWithNewRequirements(adding);
        }
    }
}
