package bndtools.editor.project;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bndtools.core.ui.icons.Icons;
import org.bndtools.utils.jface.StrikeoutStyler;
import org.bndtools.utils.swt.AddRemoveButtonBarPart;
import org.bndtools.utils.swt.AddRemoveButtonBarPart.AddRemoveListener;
import org.bndtools.utils.swt.SWTConcurrencyUtil;
import org.eclipse.core.internal.events.ResourceChangeEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.service.repository.Repository;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Success;

import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.HeaderClause;
import aQute.bnd.deployer.repository.AbstractIndexedRepo;
import aQute.bnd.header.Attrs;
import aQute.bnd.repository.osgi.OSGiRepository;
import aQute.bnd.service.Actionable;
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.exceptions.Exceptions;
import bndtools.BndConstants;
import bndtools.Plugin;
import bndtools.central.Central;
import bndtools.central.WorkspaceR5Repository;
import bndtools.editor.common.BndEditorPart;
import bndtools.editor.common.UpDownButtonBarPart;
import bndtools.editor.common.UpDownButtonBarPart.UpDownListener;
import bndtools.shared.URLDialog;

public class RepositorySelectionPart extends BndEditorPart implements IResourceChangeListener {

    private final Image refreshImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/arrow_refresh.png").createImage();
    private final Image bundleImg = Icons.desc("bundle").createImage();
    private final Image nonObrRepoImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/warning_obj.gif").createImage();
    private final Image imgUp = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/arrow_up.png").createImage();
    private final Image imgDown = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/arrow_down.png").createImage();
    private final Image imgLink = Icons.desc("link").createImage();
    private final Image projectImg = PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);
    private final Image repoImg = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
    private final Object MESSAGE_KEY = new Object();
    private final EditorPart editor;
    private final Button btnStandaloneCheckbox;
    private final StackLayout stackLayout;
    private final Control saveToRefreshControl;
    private final CheckboxTableViewer runReposViewer;
    private final UpDownButtonBarPart upDownReposPart;
    private RepositoriesEditModel repositories;
    private AddRemoveButtonBarPart addRemove;
    private Set<IFile> workspaceIndexFiles;

    /**
     * Create the SectionPart.
     *
     * @param parent
     * @param toolkit
     * @param style
     */
    public RepositorySelectionPart(final EditorPart editor, Composite parent, FormToolkit toolkit, int style) {
        super(parent, toolkit, style);
        this.editor = editor;

        Section section = getSection();
        section.setText("Repositories");

        GridLayout gl;
        GridData gd;

        // Create main container with -standalone checkbox
        Composite cmpMainContainer = toolkit.createComposite(section);
        section.setClient(cmpMainContainer);

        gl = new GridLayout(1, false);
        gl.marginWidth = 0;
        gl.marginHeight = 0;
        cmpMainContainer.setLayout(gl);

        // Create -standalone checkbox
        btnStandaloneCheckbox = toolkit.createButton(cmpMainContainer, "Standalone Mode", SWT.CHECK);
        btnStandaloneCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        btnStandaloneCheckbox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    boolean standalone = btnStandaloneCheckbox.getSelection();
                    repositories = repositories.setStandalone(standalone, model);
                    markDirty();
                    refreshFromModel();
                } catch (Exception e1) {
                    throw Exceptions.duck(e1);
                }
            }
        });

        // Create stacked container for the three(!) possible contents
        Composite cmpStackContainer = toolkit.createComposite(cmpMainContainer);
        stackLayout = new StackLayout();
        stackLayout.marginHeight = 0;
        stackLayout.marginWidth = 0;
        cmpStackContainer.setLayout(stackLayout);
        gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        gd.heightHint = 100;
        cmpStackContainer.setLayoutData(gd);

        // Create contents for the "save to refresh" control
        Composite cmpSaveToRefresh = toolkit.createComposite(cmpStackContainer);
        Hyperlink btnSaveToRefresh = toolkit.createHyperlink(cmpSaveToRefresh, "Save file to reload repositories...", SWT.NONE);
        saveToRefreshControl = cmpSaveToRefresh;
        stackLayout.topControl = saveToRefreshControl;
        gl = new GridLayout(1, true);
        btnSaveToRefresh.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        cmpSaveToRefresh.setLayout(gl);
        btnSaveToRefresh.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(HyperlinkEvent ev) {
                IRunnableWithProgress runnable = new IRunnableWithProgress() {
                    @Override
                    public void run(IProgressMonitor monitor) {
                        editor.doSave(monitor);
                    }
                };
                try {
                    editor.getSite().getWorkbenchWindow().run(false, false, runnable);
                } catch (InterruptedException e) {
                    // let it go
                } catch (InvocationTargetException e) {
                    ErrorDialog.openError(editor.getSite().getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Save error", e.getTargetException()));
                }
            }
        });

        // Create contents for bnd layout workspace
        Composite cmpBndLayout = toolkit.createComposite(cmpStackContainer);
        gl = new GridLayout(2, false);
        gl.marginWidth = 0;
        gl.marginHeight = 0;
        cmpBndLayout.setLayout(gl);

        Table table = toolkit.createTable(cmpBndLayout, SWT.CHECK | SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.widthHint = 50;
        table.setLayoutData(gd);

        runReposViewer = new CheckboxTableViewer(table);
        runReposViewer.setContentProvider(ArrayContentProvider.getInstance());
        runReposViewer.setCheckStateProvider(new ICheckStateProvider() {
            @Override
            public boolean isChecked(Object element) {
                return repositories.isIncluded((Repository) element);
            }

            @Override
            public boolean isGrayed(Object element) {
                return false;
            }
        });
        runReposViewer.addCheckStateListener(new ICheckStateListener() {

            @Override
            public void checkStateChanged(CheckStateChangedEvent event) {
                repositories.setIncluded(event.getChecked(), (Repository) event.getElement());
            }
        });

        upDownReposPart = new UpDownButtonBarPart(runReposViewer);
        Control upDownReposControl = upDownReposPart.createControl(cmpBndLayout, SWT.FLAT | SWT.VERTICAL);
        upDownReposControl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        upDownReposPart.setEnabledUp(false);
        upDownReposPart.setEnabledDown(false);
        upDownReposPart.addListener(new UpDownListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void changed(List<Object> order) {
                @SuppressWarnings("rawtypes")
                List l = order;
                repositories.setOrder(l);
                updateButtons();
                markDirty();
            }
        });
        addRemove = new AddRemoveButtonBarPart();
        Control addRemoveControl = addRemove.createControl(cmpBndLayout, SWT.FLAT | SWT.HORIZONTAL);
        addRemoveControl.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        addRemove.setRemoveEnabled(false);
        addRemove.setAddEnabled(false);
        addRemove.addListener(new AddRemoveListener() {
            @Override
            public void addSelected() {
                doAddStandaloneLink();
            }

            @Override
            public void removeSelected() {
                doRemoveStandaloneLink();
            }
        });

        final Styler strikeoutStyler = new StrikeoutStyler(StyledString.QUALIFIER_STYLER, JFaceResources.getColorRegistry().get(JFacePreferences.QUALIFIER_COLOR));

        runReposViewer.setLabelProvider(new StyledCellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                Object element = cell.getElement();

                String label = null;
                Image image = null;
                Styler styler = null;

                Repository repo = (Repository) element;
                if (repo instanceof aQute.bnd.deployer.repository.wrapper.Plugin) {
                    @SuppressWarnings("resource")
                    aQute.bnd.deployer.repository.wrapper.Plugin wrapper = (aQute.bnd.deployer.repository.wrapper.Plugin) repo;
                    wrapper.init();
                    label = wrapper.toString();
                } else if (repo instanceof RepositoryPlugin) {
                    label = ((RepositoryPlugin) repo).getName();
                } else {
                    label = repo.toString();
                }
                image = repoImg;

                if (repo instanceof WorkspaceR5Repository) {
                    image = projectImg;
                }

                boolean included = repositories.isIncluded(repo);
                styler = included ? null : strikeoutStyler;

                StyledString styledLabel = new StyledString(label, styler);
                cell.setText(styledLabel.getString());
                cell.setStyleRanges(styledLabel.getStyleRanges());
                cell.setImage(image);
            }

            @Override
            public String getToolTipText(Object element) {
                String tooltip = null;
                if (element instanceof Actionable) {
                    try {
                        tooltip = ((Actionable) element).tooltip(new Object[] {
                                element
                        });
                    } catch (Exception e) {
                        // ignore
                    }
                }
                if (tooltip != null)
                    return tooltip;

                if (repositories.isIncluded((Repository) element)) {
                    tooltip = "Included for resolution.";
                } else {
                    tooltip = "Excluded from resolution.";
                }
                return tooltip;
            }
        });

        runReposViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                updateButtons();
            }
        });

        ResourcesPlugin.getWorkspace().addResourceChangeListener(this, ResourceChangeEvent.POST_CHANGE | ResourceChangeEvent.POST_BUILD);

        stackLayout.topControl = cmpBndLayout;
        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        cmpStackContainer.setLayoutData(gd);
        cmpMainContainer.layout(true, true);

        ColumnViewerToolTipSupport.enableFor(runReposViewer, ToolTip.NO_RECREATE);
    }

    void updateButtons() {
        boolean enableDown = false;
        boolean enableUp = false;
        boolean remove = false;
        boolean add = repositories.isStandalone();

        IStructuredSelection sel = (IStructuredSelection) runReposViewer.getSelection();
        if (!sel.isEmpty()) {
            @SuppressWarnings({
                    "unchecked"
            })
            List<Repository> list = sel.toList();

            List<Repository> ordered = repositories.getOrdered();
            for (Repository r : list) {
                int index = ordered.indexOf(r);
                if (index > 0) {
                    enableUp = true;
                }
                if (index < ordered.size() - 1) {
                    enableDown = true;
                }

                if (repositories.isStandaloneRepository(r)) {
                    remove = true;
                }
            }
        }

        upDownReposPart.setEnabledUp(enableUp);
        upDownReposPart.setEnabledDown(enableDown);
        addRemove.setRemoveEnabled(remove);
        addRemove.setAddEnabled(add);
    }

    private void reloadRepos() {
        final IMessageManager messages = getManagedForm().getMessageManager();
        messages.removeMessage(MESSAGE_KEY, runReposViewer.getControl());
        final List<Repository> allRepos = new ArrayList<>();

        try {
            allRepos.addAll(repositories.getOrdered());
            runReposViewer.setInput(allRepos);
        } catch (Exception e) {
            messages.addMessage(MESSAGE_KEY, "Repository List: Unable to load OSGi Repositories. " + e.getMessage(), e, IMessageProvider.ERROR, runReposViewer.getControl());

            // Load the repos and clear the error message if the Workspace is initialised later.
            Central.onWorkspaceInit(new Success<Workspace,Void>() {
                @Override
                public Promise<Void> call(final Promise<Workspace> resolved) throws Exception {
                    final Deferred<Void> completion = new Deferred<>();
                    SWTConcurrencyUtil.execForControl(runReposViewer.getControl(), true, new Runnable() {
                        @Override
                        public void run() {
                            try {
                                allRepos.clear();
                                allRepos.addAll(resolved.getValue().getPlugins(Repository.class));
                                runReposViewer.setInput(allRepos);
                                messages.removeMessage(MESSAGE_KEY, runReposViewer.getControl());
                                completion.resolve(null);
                            } catch (Exception e) {
                                completion.fail(e);
                            }
                        }
                    });
                    return completion.getPromise();
                }
            });
        }
        updateButtons();
    }

    private void doAddStandaloneLink() {
        try {
            URLDialog dialog = new URLDialog(editor.getSite().getShell(), "Add repository URL");
            if (dialog.open() == Window.OK) {
                URI location = dialog.getLocation();

                Attrs attrs = new Attrs();
                if (dialog.getName() != null)
                    attrs.put("name", dialog.getName());

                HeaderClause clause = new HeaderClause(location.toString(), attrs);
                repositories.add(clause);
                refreshFromModel();
                markDirty();
            }
        } catch (Exception e) {
            throw Exceptions.duck(e);
        }
    }

    private void doRemoveStandaloneLink() {
        try {
            ISelection selection = runReposViewer.getSelection();
            if (selection.isEmpty())
                return;

            if (selection instanceof IStructuredSelection) {
                @SuppressWarnings("unchecked")
                List<Object> list = ((IStructuredSelection) selection).toList();
                for (Object o : list) {
                    if (repositories.remove((Repository) o)) {
                        refreshFromModel();
                        markDirty();
                    }
                }
            }
        } catch (Exception e) {
            throw Exceptions.duck(e);
        }
    }

    @Override
    protected String[] getProperties() {
        return new String[] {
                BndConstants.RUNREPOS, BndEditModel.PROP_WORKSPACE
        };
    }

    @Override
    protected void refreshFromModel() {
        repositories = new RepositoriesEditModel(model);
        boolean standalone = repositories.isStandalone();
        btnStandaloneCheckbox.setSelection(standalone);
        workspaceIndexFiles = standalone ? getWorkspaceIndexFiles() : Collections.<IFile> emptySet();
        updateButtons();
        reloadRepos();
    }

    private Set<IFile> getWorkspaceIndexFiles() {
        Set<IFile> files = new HashSet<>();
        for (Repository repository : repositories.getOrdered()) {
            List<URI> locations = getRepoLocations(repository);

            for (URI u : locations) {
                IFile[] found = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(u, IWorkspaceRoot.INCLUDE_HIDDEN | IWorkspaceRoot.INCLUDE_TEAM_PRIVATE_MEMBERS);
                for (IFile file : found) {
                    files.add(file);
                }
            }
        }
        return files;
    }

    private List<URI> getRepoLocations(Repository repository) {
        List<URI> locations = new ArrayList<>();
        if (repository instanceof AbstractIndexedRepo) {
            try {
                locations.addAll(((AbstractIndexedRepo) repository).getIndexLocations());
            } catch (Exception e) {
                throw new RuntimeException("An error occurred trying to determine whether a standalone repository had changed", e);
            }
        }

        if (repository instanceof OSGiRepository) {
            String loc = ((OSGiRepository) repository).getLocation();
            if (loc != null) {
                for (String l : loc.split(",")) {
                    try {
                        URI uri = new URI(l);
                        if ("file".equalsIgnoreCase(uri.getScheme())) {
                            locations.add(uri);
                        }
                    } catch (URISyntaxException use) {
                        // Is this a straight file path?
                        try {
                            File f = new File(((OSGiRepository) repository).getRoot(), l);
                            if (f.exists()) {
                                locations.add(f.toURI());
                            }
                        } catch (Exception e) {
                            // Just ignore this location
                        }
                    }
                }
            }
        }
        return locations;
    }

    @Override
    protected void commitToModel(boolean onSave) {
        repositories.commitToModel(model);
    }

    @Override
    public void dispose() {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
        refreshImg.dispose();
        bundleImg.dispose();
        nonObrRepoImg.dispose();
        imgUp.dispose();
        imgDown.dispose();
        imgLink.dispose();
        super.dispose();
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        boolean reposChanged = false;
        for (IFile file : workspaceIndexFiles) {
            if (event.getDelta().findMember(file.getFullPath()) != null) {
                reposChanged = true;
                break;
            }
        }

        if (reposChanged) {

            getSection().getDisplay().asyncExec(new Runnable() {
                @Override
                public void run() {
                    try {
                        repositories.updateStandaloneWorkspace(model);
                    } catch (Exception e) {
                        throw Exceptions.duck(e);
                    }
                }
            });
        }
    }

}
