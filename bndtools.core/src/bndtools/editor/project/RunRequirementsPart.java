package bndtools.editor.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.felix.bundlerepository.DataModelHelper;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.impl.DataModelHelperImpl;
import org.bndtools.core.obr.ObrResolutionJob;
import org.bndtools.core.utils.dnd.AbstractViewerDropAdapter;
import org.bndtools.core.utils.dnd.SupportedTransfer;
import org.bndtools.core.utils.filters.ObrConstants;
import org.bndtools.core.utils.filters.ObrFilterUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
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
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.libg.version.VersionRange;
import bndtools.BndConstants;
import bndtools.Central;
import bndtools.Plugin;
import bndtools.editor.model.BndEditModel;
import bndtools.model.clauses.VersionedClause;
import bndtools.model.obr.RequirementLabelProvider;
import bndtools.model.repo.ProjectBundle;
import bndtools.model.repo.RepositoryBundle;
import bndtools.model.repo.RepositoryBundleVersion;
import bndtools.wizards.obr.ObrResolutionWizard;
import bndtools.wizards.repo.RepoBundleSelectionWizard;

public class RunRequirementsPart extends SectionPart implements PropertyChangeListener {

    private final DataModelHelper obrModelHelper = new DataModelHelperImpl();
    private Table table;
    private TableViewer viewer;
    private BndEditModel model;
    private List<Requirement> requires;

    private final Image addBundleIcon = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/brick_add.png").createImage();
    private final Image resolveIcon = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/wand.png").createImage();

    private ToolItem addBundleTool;
    private ToolItem removeTool;

    public RunRequirementsPart(Composite parent, FormToolkit toolkit, int style) {
        super(parent, toolkit, style);
        createSection(getSection(), toolkit);
    }

    private void createSection(Section section, FormToolkit tk) {
        section.setText("Run Requirements");
        section.setDescription("The specified requirements will be used to resolve a set of runtime bundles from available OBR repositories.");

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

        Button resolveButton = tk.createButton(composite, "Resolve", SWT.PUSH);
        resolveButton.setImage(resolveIcon);

        // Listeners
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
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
        resolveButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doResolve();
            }
        });

        // Layout
        GridLayout layout;
        GridData gd;

        layout = new GridLayout(1, false);
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 5;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        composite.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.heightHint = 100;
        table.setLayoutData(gd);

        gd = new GridData(SWT.RIGHT, SWT.CENTER, true, false);
        resolveButton.setLayoutData(gd);
    }

    private void fillToolBar(ToolBar toolbar) {
        // Add Bundle
        addBundleTool = new ToolItem(toolbar, SWT.PUSH);
        addBundleTool.setImage(addBundleIcon);
        addBundleTool.setToolTipText("Add Bundle Requirement");
        addBundleTool.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doAddBundle();
            };
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

    private Requirement createRequirement(Object elem) {
        String bsn = null;

        if (elem instanceof RepositoryBundle) {
            bsn = ((RepositoryBundle) elem).getBsn();
        } else if (elem instanceof RepositoryBundleVersion) {
            bsn = ((RepositoryBundleVersion) elem).getBundle().getBsn();
        } else if (elem instanceof ProjectBundle) {
            bsn = ((ProjectBundle) elem).getBsn();
        }

        if (bsn != null) {
            StringBuilder filterBuilder = new StringBuilder();
            ObrFilterUtil.appendBsnFilter(filterBuilder, bsn);
            Requirement requirement = obrModelHelper.requirement(ObrConstants.REQUIREMENT_BUNDLE, filterBuilder.toString());
            return requirement;
        }
        return null;
    }

    private void doAddBundle() {
        try {
            Project project = getProject();

            RepoBundleSelectionWizard wizard = new RepoBundleSelectionWizard(project, new ArrayList<VersionedClause>());
            wizard.setSelectionPageTitle("Add Bundle Requirement");
            WizardDialog dialog = new WizardDialog(getSection().getShell(), wizard);

            if (Window.OK == dialog.open()) {
                List<VersionedClause> result = wizard.getSelectedBundles();
                List<Requirement> adding = new ArrayList<Requirement>(result.size());
                for (VersionedClause bundle : result) {
                    StringBuilder filterBuilder = new StringBuilder();
                    String versionRangeStr = bundle.getVersionRange();
                    if (versionRangeStr != null && !"latest".equals(versionRangeStr)) {
                        VersionRange versionRange = new VersionRange(versionRangeStr);

                        filterBuilder.append("(&");
                        ObrFilterUtil.appendBsnFilter(filterBuilder, bundle.getName());
                        ObrFilterUtil.appendVersionFilter(filterBuilder, versionRange);
                        filterBuilder.append(")");
                    } else {
                        ObrFilterUtil.appendBsnFilter(filterBuilder, bundle.getName());
                    }
                    Requirement req = obrModelHelper.requirement(ObrConstants.REQUIREMENT_BUNDLE, filterBuilder.toString());
                    adding.add(req);
                }
                if (!adding.isEmpty()) {
                    requires.addAll(adding);
                    viewer.add(adding.toArray(new Object[adding.size()]));
                    markDirty();
                }
            }
        } catch (Exception e) {
            ErrorDialog.openError(getSection().getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error selecting bundles.", e));
        }

    }

    private void doRemove() {
        IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
        if(!selection.isEmpty()) {
            Iterator<?> elements = selection.iterator();
            List<Object> removed = new LinkedList<Object>();
            while(elements.hasNext()) {
                Object element = elements.next();
                if(requires.remove(element))
                    removed.add(element);
            }

            if(!removed.isEmpty()) {
                viewer.remove(removed.toArray(new Object[removed.size()]));
                markDirty();
            }
        }
    }

    private void doResolve() {
        // Make sure all the parts of this editor page have committed their dirty state to the model
        IFormPart[] parts = getManagedForm().getParts();
        for (IFormPart part : parts) {
            if (part.isDirty()) part.commit(false);
        }

        IFormPage page = (IFormPage) getManagedForm().getContainer();
        IEditorInput input = page.getEditorInput();
        final IFile file = ResourceUtil.getFile(input);

        final Shell parentShell = page.getEditor().getSite().getShell();

        // Create the wizard and pre-validate
        final ObrResolutionJob job = new ObrResolutionJob(file, model);
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
            if (Window.CANCEL == response || validation.getSeverity() >= IStatus.ERROR)
                return;
        }

        // Add the operation to perform at the end of the resolution job (i.e., showing the result)
        final Runnable showResult = new Runnable() {
            public void run() {
                ObrResolutionWizard wizard = new ObrResolutionWizard(model, file, job.getResolutionResult());
                WizardDialog dialog = new WizardDialog(parentShell, wizard);
                dialog.open();
            }
        };
        job.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                parentShell.getDisplay().asyncExec(showResult);
            }
        });

        job.setUser(true);
        job.schedule();
    }

    @Override
    public void initialize(IManagedForm form) {
        super.initialize(form);

        model = (BndEditModel) form.getInput();
        model.addPropertyChangeListener(BndConstants.RUNREQUIRE, this);
    }

    @Override
    public void dispose() {
        model.removePropertyChangeListener(BndConstants.RUNREQUIRE, this);
        super.dispose();
        addBundleIcon.dispose();
        resolveIcon.dispose();
    }

    @Override
    public void commit(boolean onSave) {
        super.commit(onSave);
        model.setRunRequire(requires);
    }

    @Override
    public void refresh() {
        List<Requirement> tmp = model.getRunRequire();
        requires = new ArrayList<Requirement>(tmp != null ? tmp : Collections.<Requirement> emptyList());
        viewer.setInput(requires);
        super.refresh();
    }

    public void propertyChange(PropertyChangeEvent evt) {
        IFormPage page = (IFormPage) getManagedForm().getContainer();
        if (page.isActive()) {
            refresh();
        } else {
            markStale();
        }
    }

    private class RequirementViewerDropAdapter extends AbstractViewerDropAdapter {

        public RequirementViewerDropAdapter() {
            super(viewer, EnumSet.of(SupportedTransfer.LocalSelection));
        }

        @Override
        protected boolean performSelectionDrop(ISelection data, Object target, int location) {
            List<Requirement> adding = new LinkedList<Requirement>();

            if (data instanceof IStructuredSelection) {
                IStructuredSelection structSel = (IStructuredSelection) data;

                for (Object elem : structSel.toList()) {
                    Requirement requirement = createRequirement(elem);
                    if (requirement != null)
                        adding.add(requirement);
                }
            }

            if (!adding.isEmpty()) {
                requires.addAll(adding);
                viewer.add(adding.toArray(new Object[adding.size()]));
                markDirty();
                return true;
            }
            return false;
        }
    }


    Project getProject() {
        Project project = null;
        try {
            BndEditModel model = (BndEditModel) getManagedForm().getInput();
            IResource resource = model.getBndResource();
            File projectDir = resource.getProject().getLocation().toFile();
            if (Project.BNDFILE.equals(resource.getName())) {
                project = Workspace.getProject(projectDir);
            } else {
                project = new Project(Central.getWorkspace(), projectDir, resource.getLocation().toFile());
            }
        } catch (Exception e) {
            Plugin.logError("Error getting project from editor model", e);
        }
        return project;
    }


}
