package bndtools.views;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bndtools.core.utils.swt.FilterPanelPart;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.ServiceRegistration;

import aQute.bnd.osgi.Jar;
import aQute.bnd.service.Actionable;
import aQute.bnd.service.RepositoryListenerPlugin;
import aQute.bnd.service.RepositoryPlugin;
import bndtools.Activator;
import bndtools.Logger;
import bndtools.Plugin;
import bndtools.api.ILogger;
import bndtools.model.repo.RepositoryBundle;
import bndtools.model.repo.RepositoryBundleVersion;
import bndtools.model.repo.RepositoryTreeContentProvider;
import bndtools.model.repo.RepositoryTreeLabelProvider;
import bndtools.model.repo.RepositoryUtils;
import bndtools.utils.SWTConcurrencyUtil;
import bndtools.utils.SelectionDragAdapter;
import bndtools.wizards.workspace.AddFilesToRepositoryWizard;

public class RepositoriesView extends ViewPart implements RepositoryListenerPlugin {

    private static final ILogger logger = Logger.getLogger();

    private final FilterPanelPart filterPart = new FilterPanelPart(Plugin.getDefault().getScheduler());
    private final RepositoryTreeContentProvider contentProvider = new RepositoryTreeContentProvider();
    private TreeViewer viewer;

    private Action collapseAllAction;
    private Action refreshAction;
    private Action addBundlesAction;

    private ServiceRegistration registration;

    @Override
    public void createPartControl(Composite parent) {
        // CREATE CONTROLS
        Composite mainPanel = new Composite(parent, SWT.NONE);
        Control filterPanel = filterPart.createControl(mainPanel, 5, 5);
        Tree tree = new Tree(mainPanel, SWT.FULL_SELECTION | SWT.MULTI);
        filterPanel.setBackground(tree.getBackground());

        viewer = new TreeViewer(tree);
        viewer.setContentProvider(contentProvider);
        ColumnViewerToolTipSupport.enableFor(viewer);

        viewer.setLabelProvider(new RepositoryTreeLabelProvider(false));
        getViewSite().setSelectionProvider(viewer);

        createActions();

        // LISTENERS
        filterPart.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                String filter = (String) event.getNewValue();
                updatedFilter(filter);
            }
        });
        ViewerDropAdapter dropAdapter = new ViewerDropAdapter(viewer) {
            @Override
            public boolean validateDrop(Object target, int operation, TransferData transferType) {
                boolean valid = false;
                if (target instanceof RepositoryPlugin) {
                    if (((RepositoryPlugin) target).canWrite()) {
                        if (LocalSelectionTransfer.getTransfer().isSupportedType(transferType)) {
                            ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
                            if (selection instanceof IStructuredSelection) {
                                for (Iterator< ? > iter = ((IStructuredSelection) selection).iterator(); iter.hasNext();) {
                                    Object element = iter.next();
                                    if (element instanceof RepositoryBundle || element instanceof RepositoryBundleVersion) {
                                        valid = true;
                                        break;
                                    }
                                    if (element instanceof IFile) {
                                        valid = true;
                                        break;
                                    }
                                    if (element instanceof IAdaptable) {
                                        IFile file = (IFile) ((IAdaptable) element).getAdapter(IFile.class);
                                        if (file != null) {
                                            valid = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        } else {
                            valid = true;
                        }
                    }
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
                if (data instanceof String[]) {
                    String[] paths = (String[]) data;
                    File[] files = new File[paths.length];
                    for (int i = 0; i < paths.length; i++) {
                        files[i] = new File(paths[i]);
                    }
                    copied = addFilesToRepository((RepositoryPlugin) getCurrentTarget(), files);
                } else if (data instanceof IResource[]) {
                    IResource[] resources = (IResource[]) data;
                    File[] files = new File[resources.length];
                    for (int i = 0; i < resources.length; i++) {
                        files[i] = resources[i].getLocation().toFile();
                    }
                    copied = addFilesToRepository((RepositoryPlugin) getCurrentTarget(), files);
                } else if (data instanceof IStructuredSelection) {
                    File[] files = convertSelectionToFiles((IStructuredSelection) data);
                    if (files != null && files.length > 0)
                        copied = addFilesToRepository((RepositoryPlugin) getCurrentTarget(), files);
                }
                return copied;
            }
        };
        dropAdapter.setFeedbackEnabled(false);
        dropAdapter.setExpandEnabled(false);

        viewer.addDropSupport(DND.DROP_COPY | DND.DROP_MOVE, new Transfer[] {
                FileTransfer.getInstance(), ResourceTransfer.getInstance(), LocalSelectionTransfer.getTransfer()
        }, dropAdapter);
        viewer.addDragSupport(DND.DROP_COPY | DND.DROP_MOVE, new Transfer[] {
            LocalSelectionTransfer.getTransfer()
        }, new SelectionDragAdapter(viewer));

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

        viewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                if (!event.getSelection().isEmpty()) {
                    IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                    if (selection.getFirstElement() instanceof IAdaptable) {
                        URI uri = (URI) ((IAdaptable) selection.getFirstElement()).getAdapter(URI.class);
                        if (uri != null) {
                            IWorkbenchPage page = getSite().getPage();
                            try {
                                IFileStore fileStore = EFS.getLocalFileSystem().getStore(uri);
                                IDE.openEditorOnFileStore(page, fileStore);
                            } catch (PartInitException e) {
                                logger.logError("Error opening editor for " + uri, e);
                            }
                        }
                    }
                }
            }
        });

        createContextMenu();

        // LOAD
        viewer.setInput(RepositoryUtils.listRepositories(true));

        // LAYOUT
        GridLayout layout = new GridLayout(1, false);
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 0;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        mainPanel.setLayout(layout);

        tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        filterPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        // Toolbar
        createActions();
        fillToolBar(getViewSite().getActionBars().getToolBarManager());

        // Register as repository listener
        registration = Activator.getDefault().getBundleContext().registerService(RepositoryListenerPlugin.class.getName(), this, null);
    }

    @Override
    public void setFocus() {
        filterPart.setFocus();
    }

    private static File[] convertSelectionToFiles(ISelection selection) {
        if (!(selection instanceof IStructuredSelection))
            return new File[0];

        IStructuredSelection structSel = (IStructuredSelection) selection;
        List<File> files = new ArrayList<File>(structSel.size());

        for (Iterator< ? > iter = structSel.iterator(); iter.hasNext();) {
            Object element = iter.next();
            if (element instanceof IFile)
                files.add(((IFile) element).getLocation().toFile());
            else if (element instanceof IAdaptable) {
                IFile ifile = (IFile) ((IAdaptable) element).getAdapter(IFile.class);
                if (ifile != null)
                    files.add(ifile.getLocation().toFile());
            }
        }

        return files.toArray(new File[files.size()]);
    }

    @Override
    public void dispose() {
        registration.unregister();
        super.dispose();
    }

    boolean addFilesToRepository(RepositoryPlugin repo, File[] files) {
        AddFilesToRepositoryWizard wizard = new AddFilesToRepositoryWizard(repo, files);
        WizardDialog dialog = new WizardDialog(getViewSite().getShell(), wizard);
        dialog.open();
        viewer.refresh(repo);
        return true;
    }

    private void updatedFilter(String filterString) {
        String newFilter;
        if (filterString == null || filterString.length() == 0 || filterString.trim().equals("*"))
            newFilter = null;
        else
            newFilter = "*" + filterString.trim() + "*";

        contentProvider.setFilter(newFilter);
        viewer.refresh();
        if (newFilter != null)
            viewer.expandToLevel(2);
    }

    void createActions() {
        collapseAllAction = new Action() {
            @Override
            public void run() {
                viewer.collapseAll();
            }
        };
        collapseAllAction.setText("Collapse All");
        collapseAllAction.setToolTipText("Collapse All");
        collapseAllAction.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/collapseall.gif"));

        refreshAction = new Action() {
            @Override
            public void run() {
                viewer.setInput(RepositoryUtils.listRepositories(true));
            }
        };
        refreshAction.setText("Refresh");
        refreshAction.setToolTipText("Refresh Repositories Tree");
        refreshAction.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/arrow_refresh.png"));

        addBundlesAction = new Action() {
            @Override
            public void run() {
                IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
                Object element = selection.getFirstElement();
                if (element != null && element instanceof RepositoryPlugin) {
                    RepositoryPlugin repo = (RepositoryPlugin) element;
                    if (repo.canWrite()) {
                        AddFilesToRepositoryWizard wizard = new AddFilesToRepositoryWizard(repo, new File[0]);
                        WizardDialog dialog = new WizardDialog(getViewSite().getShell(), wizard);
                        dialog.open();

                        viewer.refresh(repo);
                    }
                }
            }
        };
        addBundlesAction.setEnabled(false);
        addBundlesAction.setText("Add");
        addBundlesAction.setToolTipText("Add Bundles to Repository");
        addBundlesAction.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/brick_add.png"));
    }

    void createContextMenu() {
        MenuManager mgr = new MenuManager();
        Menu menu = mgr.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
        mgr.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        getSite().registerContextMenu(mgr, viewer);

        mgr.addMenuListener(new IMenuListener() {

            public void menuAboutToShow(IMenuManager manager) {
                try {
                    manager.removeAll();
                    IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
                    if (!selection.isEmpty()) {
                        Object firstElement = selection.getFirstElement();
                        if (firstElement instanceof Actionable) {
                            //
                            // Use the Actionable interface to fill the menu
                            // Should extend this to allow other menu entries
                            // from the view, but currently there are none
                            //
                            Actionable act = (Actionable) firstElement;
                            Map<String,Runnable> actions = act.actions();
                            if (actions != null) {
                                for (final Entry<String,Runnable> e : actions.entrySet()) {
                                    final String label = e.getKey();
                                    final Action a = new Action(label) {
                                        @Override
                                        public void run() {
                                            e.getValue().run();
                                        }
                                    };
                                    manager.add(a);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void fillToolBar(IToolBarManager toolBar) {
        toolBar.add(refreshAction);
        toolBar.add(collapseAllAction);
        toolBar.add(addBundlesAction);
        toolBar.add(new Separator());
    }

    public void bundleAdded(final RepositoryPlugin repository, Jar jar, File file) {
        if (viewer != null)
            SWTConcurrencyUtil.execForControl(viewer.getControl(), true, new Runnable() {
                public void run() {
                    viewer.refresh(repository);
                }
            });
    }
}
