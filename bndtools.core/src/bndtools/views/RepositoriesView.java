package bndtools.views;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.viewers.ViewerFilter;
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
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.ServiceRegistration;

import aQute.bnd.build.Workspace;
import aQute.bnd.service.RepositoryListenerPlugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.osgi.Jar;
import bndtools.Activator;
import bndtools.Central;
import bndtools.Plugin;
import bndtools.model.repo.RepositoryTreeContentProvider;
import bndtools.model.repo.RepositoryTreeLabelProvider;
import bndtools.model.repo.WrappingObrRepository;
import bndtools.utils.SWTConcurrencyUtil;
import bndtools.utils.SelectionDragAdapter;
import bndtools.wizards.workspace.AddFilesToRepositoryWizard;

public class RepositoriesView extends FilteredViewPart implements RepositoryListenerPlugin {

    private static final String CACHE_REPO = "cache";

    private TreeViewer viewer;

    private Action collapseAllAction;
    private Action refreshAction;
    private Action addBundlesAction;

    private ServiceRegistration registration;

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
        viewer.addDragSupport(DND.DROP_COPY | DND.DROP_MOVE, new Transfer[] { LocalSelectionTransfer.getTransfer() }, new SelectionDragAdapter(viewer));

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
                        IFile file = (IFile) ((IAdaptable) selection.getFirstElement()).getAdapter(IFile.class);

                        if (file != null) {
                            IWorkbenchPage page = getSite().getPage();
                            try {
                                IDE.openEditor(page, file);
                            } catch (PartInitException e) {
                                Plugin.logError("Error opening editor for " + file, e);
                            }
                        }
                    }
                }
            }
        });

        createContextMenu();

        // LOAD
        loadRepositories();

        // LAYOUT
        GridLayout layout = new GridLayout(1,false);
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 0;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        container.setLayout(layout);

        tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        registration = Activator.getDefault().getBundleContext().registerService(RepositoryListenerPlugin.class.getName(), this, null);
    }

    void loadRepositories() {
        try {
            Workspace workspace = Central.getWorkspace();
            List<RepositoryPlugin> plugins = workspace.getPlugins(RepositoryPlugin.class);

            List<RepositoryPlugin> repos = new ArrayList<RepositoryPlugin>(plugins.size() + 1);
            repos.add(new WrappingObrRepository(Central.getWorkspaceObrProvider(), null, workspace));

            for (RepositoryPlugin plugin : plugins) {
                if (!CACHE_REPO.equals(plugin.getName()))
                    repos.add(plugin);
            }

            viewer.setInput(repos);
        } catch (Exception e) {
            Plugin.logError("Error loading repositories", e);
        }
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

    @Override
    protected void updatedFilter(String filterString) {
        if(filterString == null || filterString.length() == 0) {
            viewer.setFilters(new ViewerFilter[0]);
        } else {
            viewer.setFilters(new ViewerFilter[] { new RepositoryBsnFilter(filterString) });
            viewer.expandToLevel(2);
        }
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
                loadRepositories();
            };
        };
        refreshAction.setText("Refresh");
        refreshAction.setToolTipText("Refresh Repositories Tree");
        refreshAction.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/arrow_refresh.png"));

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

    void createContextMenu() {
        MenuManager mgr = new MenuManager();
        Menu menu = mgr.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
        mgr.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        getSite().registerContextMenu(mgr, viewer);
    }

    @Override
    protected void fillToolBar(IToolBarManager toolBar) {
        toolBar.add(refreshAction);
        toolBar.add(collapseAllAction);
        toolBar.add(addBundlesAction);
        toolBar.add(new Separator());

        super.fillToolBar(toolBar);
    }

    public void bundleAdded(final RepositoryPlugin repository, Jar jar, File file) {
        if (viewer != null) SWTConcurrencyUtil.execForControl(viewer.getControl(), true, new Runnable() {
            public void run() {
                viewer.refresh(repository);
            }
        });
    }
}