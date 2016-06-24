package bndtools.editor.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.bndtools.core.ui.wizards.jpm.AddJpmDependenciesWizard;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.dnd.URLTransfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.ResourceTransfer;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.Constants;
import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;
import aQute.bnd.version.Version;
import bndtools.Plugin;
import bndtools.central.RepositoryUtils;
import bndtools.editor.common.BndEditorPart;
import bndtools.model.clauses.VersionedClauseLabelProvider;
import bndtools.model.repo.DependencyPhase;
import bndtools.model.repo.ProjectBundle;
import bndtools.model.repo.RepositoryBundle;
import bndtools.model.repo.RepositoryBundleVersion;
import bndtools.model.repo.RepositoryResourceElement;
import bndtools.preferences.BndPreferences;
import bndtools.types.Pair;
import bndtools.wizards.repo.RepoBundleSelectionWizard;
import bndtools.wizards.workspace.AddFilesToRepositoryWizard;

public abstract class RepositoryBundleSelectionPart extends BndEditorPart implements PropertyChangeListener {
    private final String propertyName;
    private final DependencyPhase phase;

    private Table table;
    protected TableViewer viewer;

    private BndEditModel model;
    protected List<VersionedClause> bundles;
    protected ToolItem removeItemTool;

    protected RepositoryBundleSelectionPart(String propertyName, DependencyPhase phase, Composite parent, FormToolkit toolkit, int style) {
        super(parent, toolkit, style);
        this.propertyName = propertyName;
        this.phase = phase;
        createSection(getSection(), toolkit);
    }

    @Override
    protected String[] getProperties() {
        return new String[] {
                propertyName
        };
    }

    protected ToolItem createAddItemTool(ToolBar toolbar) {
        ToolItem tool = new ToolItem(toolbar, SWT.PUSH);

        tool.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ADD));
        tool.setToolTipText("Add Bundle");
        tool.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doAdd();
            }
        });

        return tool;
    }

    protected ToolItem createRemoveItemTool(ToolBar toolbar) {
        ToolItem tool = new ToolItem(toolbar, SWT.PUSH);

        tool.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE));
        tool.setDisabledImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE_DISABLED));
        tool.setToolTipText("Remove");
        tool.setEnabled(false);
        tool.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doRemove();
            }
        });

        return tool;
    }

    protected ToolItem getRemoveItemTool() {
        return removeItemTool;
    }

    protected void fillToolBar(ToolBar toolbar) {
        createAddItemTool(toolbar);
        this.removeItemTool = createRemoveItemTool(toolbar);
    }

    @SuppressWarnings("static-method")
    protected IBaseLabelProvider getLabelProvider() {
        return new VersionedClauseLabelProvider();
    }

    void createSection(Section section, FormToolkit toolkit) {
        // Toolbar buttons
        ToolBar toolbar = new ToolBar(section, SWT.FLAT);
        section.setTextClient(toolbar);
        fillToolBar(toolbar);

        Composite composite = toolkit.createComposite(section);
        section.setClient(composite);

        table = toolkit.createTable(composite, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER | SWT.H_SCROLL);

        viewer = new TableViewer(table);
        viewer.setContentProvider(new ArrayContentProvider());
        viewer.setLabelProvider(getLabelProvider());

        // Listeners
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                ToolItem remove = getRemoveItemTool();
                if (remove != null)
                    remove.setEnabled(isRemovable(event.getSelection()));
            }
        });
        ViewerDropAdapter dropAdapter = new ViewerDropAdapter(viewer) {
            @Override
            public void dragEnter(DropTargetEvent event) {
                super.dragEnter(event);
                event.detail = DND.DROP_COPY;
            }

            @Override
            public boolean validateDrop(Object target, int operation, TransferData transferType) {
                if (FileTransfer.getInstance().isSupportedType(transferType))
                    return true;

                if (ResourceTransfer.getInstance().isSupportedType(transferType))
                    return true;

                if (URLTransfer.getInstance().isSupportedType(transferType))
                    return true;

                ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
                if (selection.isEmpty() || !(selection instanceof IStructuredSelection)) {
                    return false;
                }

                Iterator< ? > iterator = ((IStructuredSelection) selection).iterator();
                while (iterator.hasNext()) {
                    if (!selectionIsDroppable(iterator.next())) {
                        return false;
                    }
                }
                return true;
            }

            private boolean selectionIsDroppable(Object element) {
                return element instanceof RepositoryBundle || element instanceof RepositoryBundleVersion || element instanceof ProjectBundle || element instanceof RepositoryResourceElement;
            }

            @Override
            public boolean performDrop(Object data) {
                TransferData transfer = getCurrentEvent().currentDataType;

                if (URLTransfer.getInstance().isSupportedType(transfer)) {
                    String urlStr = (String) URLTransfer.getInstance().nativeToJava(transfer);
                    return handleURLDrop(urlStr);
                } else if (data instanceof String[]) {
                    return handleFileNameDrop((String[]) data);
                } else if (data instanceof IResource[]) {
                    return handleResourceDrop((IResource[]) data);
                } else {
                    return handleSelectionDrop();
                }
            }

            private boolean handleResourceDrop(IResource[] resources) {
                File[] files = new File[resources.length];
                for (int i = 0; i < resources.length; i++) {
                    files[i] = resources[i].getLocation().toFile();
                }
                return handleFileDrop(files);
            }

            private boolean handleFileNameDrop(String[] paths) {
                File[] files = new File[paths.length];
                for (int i = 0; i < paths.length; i++) {
                    files[i] = new File(paths[i]);
                }
                return handleFileDrop(files);
            }

            private boolean handleFileDrop(File[] files) {
                if (files.length > 0) {
                    BndPreferences prefs = new BndPreferences();
                    boolean hideWarning = prefs.getHideWarningExternalFile();
                    if (!hideWarning) {
                        MessageDialogWithToggle dialog = MessageDialogWithToggle.openWarning(getSection().getShell(), "Add External Files",
                                "External files cannot be directly added to a project, they must be added to a local repository first.", "Do not show this warning again.", false, null, null);
                        if (Window.CANCEL == dialog.getReturnCode())
                            return false;
                        if (dialog.getToggleState()) {
                            prefs.setHideWarningExternalFile(true);
                        }
                    }

                    AddFilesToRepositoryWizard wizard = new AddFilesToRepositoryWizard(null, files);
                    WizardDialog dialog = new WizardDialog(getSection().getShell(), wizard);
                    if (Window.OK == dialog.open()) {
                        List<Pair<String,String>> addingBundles = wizard.getSelectedBundles();
                        List<VersionedClause> addingClauses = new ArrayList<VersionedClause>(addingBundles.size());

                        for (Pair<String,String> addingBundle : addingBundles) {
                            Attrs attribs = new Attrs();
                            attribs.put(Constants.VERSION_ATTRIBUTE, addingBundle.getSecond());
                            addingClauses.add(new VersionedClause(addingBundle.getFirst(), attribs));
                        }

                        handleAdd(addingClauses);
                    }
                    return true;
                }
                return false;
            }

            private boolean handleSelectionDrop() {
                ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
                if (selection.isEmpty() || !(selection instanceof IStructuredSelection)) {
                    return false;
                }
                List<VersionedClause> adding = new LinkedList<VersionedClause>();
                Iterator< ? > iterator = ((IStructuredSelection) selection).iterator();
                while (iterator.hasNext()) {
                    Object item = iterator.next();
                    if (item instanceof RepositoryBundle) {
                        VersionedClause newClause = RepositoryUtils.convertRepoBundle((RepositoryBundle) item);
                        adding.add(newClause);
                    } else if (item instanceof RepositoryBundleVersion) {
                        RepositoryBundleVersion bundleVersion = (RepositoryBundleVersion) item;
                        VersionedClause newClause = RepositoryUtils.convertRepoBundleVersion(bundleVersion, phase);
                        adding.add(newClause);
                    } else if (item instanceof RepositoryResourceElement) {
                        RepositoryResourceElement elt = (RepositoryResourceElement) item;
                        VersionedClause newClause = RepositoryUtils.convertRepoBundleVersion(elt.getRepositoryBundleVersion(), phase);
                        adding.add(newClause);
                    }
                }

                handleAdd(adding);
                return true;
            }

            private boolean handleURLDrop(String urlStr) {
                try {
                    URI uri = new URI(sanitizeUrl(urlStr));
                    AddJpmDependenciesWizard wizard = new AddJpmDependenciesWizard(uri);
                    WizardDialog dialog = new WizardDialog(getSection().getShell(), wizard);
                    if (dialog.open() == Window.OK) {
                        Set<ResourceDescriptor> resources = wizard.getResult();
                        List<VersionedClause> newBundles = new ArrayList<VersionedClause>(resources.size());
                        for (ResourceDescriptor resource : resources) {
                            Attrs attrs = new Attrs();
                            attrs.put(Constants.VERSION_ATTRIBUTE, resource.version != null ? resource.version.toString() : Version.emptyVersion.toString());
                            VersionedClause clause = new VersionedClause(resource.bsn, attrs);
                            newBundles.add(clause);
                        }

                        handleAdd(newBundles);
                        return true;
                    }
                    return false;
                } catch (URISyntaxException e) {
                    MessageDialog.openError(getSection().getShell(), "Error", "The dropped URL was invalid: " + urlStr);
                    return false;
                }
            }

            private String sanitizeUrl(String urlStr) {
                int newline = urlStr.indexOf('\n');
                if (newline > -1)
                    return urlStr.substring(0, newline).trim();
                return urlStr;
            }

            private void handleAdd(Collection<VersionedClause> newClauses) {
                if (newClauses == null || newClauses.isEmpty())
                    return;

                List<VersionedClause> toAdd = new LinkedList<VersionedClause>();
                for (VersionedClause newClause : newClauses) {
                    boolean found = false;
                    for (ListIterator<VersionedClause> iter = bundles.listIterator(); iter.hasNext();) {
                        VersionedClause existing = iter.next();
                        if (newClause.getName().equals(existing.getName())) {
                            int index = iter.previousIndex();
                            iter.set(newClause);
                            viewer.replace(newClause, index);

                            found = true;
                            break;
                        }
                    }
                    if (!found)
                        toAdd.add(newClause);
                }

                bundles.addAll(toAdd);
                viewer.add(toAdd.toArray());

                markDirty();
            }
        };
        dropAdapter.setFeedbackEnabled(false);
        dropAdapter.setExpandEnabled(false);
        viewer.addDropSupport(DND.DROP_COPY | DND.DROP_MOVE, new Transfer[] {
                LocalSelectionTransfer.getTransfer(), FileTransfer.getInstance(), ResourceTransfer.getInstance(), URLTransfer.getInstance()
        }, dropAdapter);

        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.character == SWT.DEL) {
                    doRemove();
                } else if (e.character == '+') {
                    doAdd();
                }
            }
        });

        // Layout
        GridLayout layout = new GridLayout(1, false);
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 0;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        composite.setLayout(layout);

        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.widthHint = 50;
        gd.heightHint = getTableHeightHint();
        table.setLayoutData(gd);
    }

    private static boolean isRemovable(ISelection selection) {
        if (selection.isEmpty())
            return false;

        if (selection instanceof IStructuredSelection) {
            List< ? > list = ((IStructuredSelection) selection).toList();
            for (Object object : list) {
                if (!(object instanceof VersionedClause)) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }

    @SuppressWarnings("static-method")
    protected int getTableHeightHint() {
        return SWT.DEFAULT;
    }

    protected List<VersionedClause> getBundles() {
        return bundles;
    }

    protected void setBundles(final List<VersionedClause> bundles) {
        this.bundles = bundles;
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                viewer.setInput(bundles);
            }
        });
    }

    private void doAdd() {
        try {
            RepoBundleSelectionWizard wizard = createBundleSelectionWizard(getBundles());
            if (wizard != null) {
                WizardDialog dialog = new WizardDialog(getSection().getShell(), wizard);
                if (dialog.open() == Window.OK) {
                    setBundles(wizard.getSelectedBundles());
                    markDirty();
                }
            }
        } catch (Exception e) {
            ErrorDialog.openError(getSection().getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error opening bundle resolver wizard.", e));
        }
    }

    private void doRemove() {
        if (!isRemovable(viewer.getSelection()))
            return;

        IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
        if (!selection.isEmpty()) {
            Iterator< ? > elements = selection.iterator();
            List<Object> removed = new LinkedList<Object>();
            while (elements.hasNext()) {
                Object element = elements.next();
                if (bundles.remove(element))
                    removed.add(element);
            }

            if (!removed.isEmpty()) {
                viewer.remove(removed.toArray());
                markDirty();
            }
        }
    }

    @Override
    public void commitToModel(boolean onSave) {
        saveToModel(model, bundles);
    }

    protected abstract void saveToModel(BndEditModel model, List<VersionedClause> bundles);

    protected abstract List<VersionedClause> loadFromModel(BndEditModel model);

    protected final RepoBundleSelectionWizard createBundleSelectionWizard(List<VersionedClause> bundles) throws Exception {
        RepoBundleSelectionWizard wizard = new RepoBundleSelectionWizard(bundles, phase);
        setSelectionWizardTitleAndMessage(wizard);

        return wizard;
    }

    protected abstract void setSelectionWizardTitleAndMessage(RepoBundleSelectionWizard wizard);

    @Override
    public void refreshFromModel() {
        List<VersionedClause> bundles = loadFromModel(model);
        if (bundles != null) {
            setBundles(new ArrayList<VersionedClause>(bundles));
        } else {
            setBundles(new ArrayList<VersionedClause>());
        }
    }

    @Override
    public void initialize(IManagedForm form) {
        super.initialize(form);

        model = (BndEditModel) form.getInput();
        model.addPropertyChangeListener(propertyName, this);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (model != null)
            model.removePropertyChangeListener(propertyName, this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        IFormPage page = (IFormPage) getManagedForm().getContainer();
        if (page.isActive()) {
            refresh();
        } else {
            markStale();
        }
    }
}