package bndtools.views;

import java.io.File;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import aQute.bnd.plugin.Central;
import aQute.bnd.service.RepositoryPlugin;
import bndtools.Plugin;
import bndtools.model.repo.ProjectBundle;
import bndtools.model.repo.RepositoryBundle;
import bndtools.model.repo.RepositoryTreeContentProvider;
import bndtools.model.repo.RepositoryTreeLabelProvider;
import bndtools.utils.SelectionDragAdapter;
import bndtools.wizards.workspace.AddFilesToRepositoryWizard;
import bndtools.wizards.workspace.ImportBundleRepositoryWizard;

public class RepositoriesView extends FilteredViewPart {

    private TreeViewer viewer;

    private Action collapseAllAction;
    private Action importRepoAction;
    private Action addBundlesAction;

    private Action removeBundlesAction;

    private class BsnFilter extends ViewerFilter {
        private final String filterStr;
        public BsnFilter(String filterStr) {
            this.filterStr = filterStr;
        }
        @Override
        public boolean select(Viewer viewer, Object parentElement, Object element) {
            String bsn = null;
            if(element instanceof RepositoryBundle) {
                bsn = ((RepositoryBundle) element).getBsn();
            } else if (element instanceof ProjectBundle) {
                bsn = ((ProjectBundle) element).getBsn();
            }
            if(bsn != null) {
                if(filterStr != null && filterStr.length() > 0 && bsn.toLowerCase().indexOf(filterStr) == -1) {
                    return false;
                }
            }
            return true;
        }
    };


    @Override
    protected void createMainControl(Composite container) {
        // CREATE CONTROLS
        Tree tree = new Tree(container, SWT.FULL_SELECTION | SWT.MULTI);

        viewer = new TreeViewer(tree);
        viewer.setContentProvider(new RepositoryTreeContentProvider());
        viewer.setLabelProvider(new RepositoryTreeLabelProvider());
        getViewSite().setSelectionProvider(viewer);

        createActions();

        // LISTENERS
        ViewerDropAdapter dropAdapter = new ViewerDropAdapter(viewer) {;
            @Override
            public boolean validateDrop(Object target, int operation, TransferData transferType) {
                boolean valid = false;
                if(target instanceof RepositoryPlugin) {
                    valid = ((RepositoryPlugin) target).canWrite();
                }
                return valid;
            }
            @Override
            public void dragEnter(DropTargetEvent event) {
                super.dragEnter(event);
                event.detail = DND.DROP_COPY;
            }
            @Override
            public boolean performDrop(Object data) {
                boolean copied = false;
                if(data instanceof String[]) {
                    String[] paths = (String[]) data;
                    File[] files = new File[paths.length];
                    for (int i = 0; i < paths.length; i++) {
                        files[i] = new File(paths[i]);
                    }
                    copied = addFilesToRepository((RepositoryPlugin) getCurrentTarget(), files);
                } else if(data instanceof IResource[]) {
                    IResource[] resources = (IResource[]) data;
                    File[] files = new File[resources.length];
                    for (int i = 0; i < resources.length; i++) {
                        files[i] = resources[i].getLocation().toFile();
                    }
                    copied = addFilesToRepository((RepositoryPlugin) getCurrentTarget(), files);
                }
                return copied;
            }
        };
        dropAdapter.setFeedbackEnabled(false);
        dropAdapter.setExpandEnabled(false);
        viewer.addDropSupport(DND.DROP_COPY | DND.DROP_MOVE, new Transfer[] { FileTransfer.getInstance(), ResourceTransfer.getInstance() }, dropAdapter);
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                boolean writableRepoSelected = false;
                IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
                Object element = selection.getFirstElement();
                if (element instanceof RepositoryPlugin) {
                    RepositoryPlugin repo = (RepositoryPlugin) element;
                    writableRepoSelected = repo.canWrite();
                }
                addBundlesAction.setEnabled(writableRepoSelected);
            }
        });
        viewer.addDragSupport(DND.DROP_COPY | DND.DROP_MOVE, new Transfer[] { LocalSelectionTransfer.getTransfer() }, new SelectionDragAdapter(viewer));

        // LOAD
        try {
            viewer.setInput(Central.getWorkspace());
        } catch (Exception e) {
            Plugin.logError("Error loading repositories", e);
        }

        // LAYOUT
        GridLayout layout = new GridLayout(1,false);
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 0;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        container.setLayout(layout);

        tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    }

    boolean addFilesToRepository(RepositoryPlugin repo, File[] files) {
        AddFilesToRepositoryWizard wizard = new AddFilesToRepositoryWizard(repo, files);
        WizardDialog dialog = new WizardDialog(getViewSite().getShell(), wizard);
        dialog.open();
        viewer.refresh(repo);
        return true;
    }

    @Override
    protected void updatedFilter(String filterString) {
        if(filterString == null || filterString.length() == 0) {
            viewer.setFilters(new ViewerFilter[0]);
        } else {
            viewer.setFilters(new ViewerFilter[] { new BsnFilter(filterString) });
            viewer.expandToLevel(2);
        }
    }

    void createActions() {
        collapseAllAction = new Action() {
            @Override
            public void run() {
                viewer.collapseAll();
            };
        };
        collapseAllAction.setText("Collapse All");
        collapseAllAction.setToolTipText("Collapse All");
        collapseAllAction.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/collapseall.gif"));

        importRepoAction = new Action() {
            @Override
            public void run() {
                ImportBundleRepositoryWizard wizard = new ImportBundleRepositoryWizard();
                WizardDialog dialog = new WizardDialog(getViewSite().getShell(), wizard);
                if(dialog.open() == Window.OK) {
                    viewer.refresh();
                }
            };
        };
        importRepoAction.setText("Import");
        importRepoAction.setToolTipText("Import External Repositories");
        importRepoAction.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/import_wiz.gif"));

        addBundlesAction = new Action() {
            @Override
            public void run() {
                IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
                Object element = selection.getFirstElement();
                if(element != null && element instanceof RepositoryPlugin) {
                    RepositoryPlugin repo = (RepositoryPlugin) element;
                    if(repo.canWrite()) {
                        AddFilesToRepositoryWizard wizard = new AddFilesToRepositoryWizard(repo, new File[0]);
                        WizardDialog dialog = new WizardDialog(getViewSite().getShell(), wizard);
                        dialog.open();

                        viewer.refresh(repo);
                    }
                }
            };
        };
        addBundlesAction.setEnabled(false);
        addBundlesAction.setText("Add");
        addBundlesAction.setToolTipText("Add Bundles to Repository");
        addBundlesAction.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/brick_add.png"));
    }

    @Override
    protected void fillToolBar(IToolBarManager toolBar) {
        toolBar.add(importRepoAction);
        toolBar.add(addBundlesAction);
        toolBar.add(new Separator());
        toolBar.add(collapseAllAction);

        super.fillToolBar(toolBar);
    }
}