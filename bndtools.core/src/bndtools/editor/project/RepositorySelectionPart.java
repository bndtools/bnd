package bndtools.editor.project;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.bndtools.core.ui.icons.Icons;
import org.bndtools.utils.Function;
import org.bndtools.utils.collections.CollectionUtils;
import org.bndtools.utils.jface.StrikeoutStyler;
import org.bndtools.utils.swt.SWTConcurrencyUtil;
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
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.ToolTip;
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
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
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

import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.HeaderClause;
import aQute.bnd.build.model.conversions.Converter;
import bndtools.BndConstants;
import bndtools.Plugin;
import bndtools.UIConstants;
import bndtools.central.Central;
import bndtools.central.WorkspaceR5Repository;
import bndtools.editor.common.BndEditorPart;

public class RepositorySelectionPart extends BndEditorPart {

    private static final String PROP_STANDALONE_BACKUP = "x-ignore-standalone";

    private static final String PROP_STANDALONE = "-standalone";

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

    private Composite cmpMainContainer;
    private Button btnStandaloneCheckbox;
    private Composite cmpStackContainer;
    private StackLayout stackLayout;

    private Control saveToRefreshControl;

    private Composite cmpStandalone;
    private TableViewer standaloneLinksViewer;

    private Composite cmpBndLayout;
    private CheckboxTableViewer runReposViewer;
    private ToolItem btnMoveUp;
    private ToolItem btnMoveDown;

    private boolean needsSave = false;
    private List<HeaderClause> standaloneLinks = null;
    private String backupStandaloneLinks = null;

    private final ArrayList<Repository> allRepos = new ArrayList<Repository>();
    private List<String> includedRepos = null;

    /**
     * Create the SectionPart.
     *
     * @param parent
     * @param toolkit
     * @param style
     */
    public RepositorySelectionPart(EditorPart editor, Composite parent, FormToolkit toolkit, int style) {
        super(parent, toolkit, style);
        this.editor = editor;
        Section section = getSection();
        createClient(section, toolkit);
    }

    /**
     * Fill the section.
     */
    private void createClient(final Section section, FormToolkit toolkit) {
        section.setText("Repositories");

        GridLayout gl;
        GridData gd;

        // Create main container with -standalone checkbox
        cmpMainContainer = toolkit.createComposite(section);
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
                boolean standalone = btnStandaloneCheckbox.getSelection();
                if (standalone) {
                    restoreStandaloneLinksFromBackup();
                    standaloneLinksViewer.setInput(standaloneLinks);
                } else {
                    saveStandaloneLinksToBackup();
                    standaloneLinks = null;
                    standaloneLinksViewer.setInput(standaloneLinks);
                }
                needsSave = true;
                updateStack();
                markDirty();
            }
        });

        // Create stacked container for the three(!) possible contents
        cmpStackContainer = toolkit.createComposite(cmpMainContainer);
        stackLayout = new StackLayout();
        stackLayout.marginHeight = 0;
        stackLayout.marginWidth = 0;
        cmpStackContainer.setLayout(stackLayout);
        gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        gd.heightHint = 140;
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

        // Create contents for the standalone layout workspace
        cmpStandalone = toolkit.createComposite(cmpStackContainer);
        gl = new GridLayout(1, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        cmpStandalone.setLayout(gl);

        Table tblStandaloneLinks = toolkit.createTable(cmpStandalone, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL);
        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.widthHint = 50;
        tblStandaloneLinks.setLayoutData(gd);

        standaloneLinksViewer = new TableViewer(tblStandaloneLinks);
        standaloneLinksViewer.setContentProvider(ArrayContentProvider.getInstance());
        standaloneLinksViewer.setLabelProvider(new StyledCellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                HeaderClause clause = (HeaderClause) cell.getElement();
                String linkStr = clause.getName();

                cell.setImage(imgLink);

                StyledString label = new StyledString(linkStr, UIConstants.BOLD_STYLER);

                boolean first = true;
                for (Entry<String,String> entry : clause.getAttribs().entrySet()) {
                    if (!first)
                        label.append(";", StyledString.QUALIFIER_STYLER);
                    label.append(String.format(" %s=%s", entry.getKey(), entry.getValue()), StyledString.QUALIFIER_STYLER);
                    first = false;
                }
                cell.setText(label.toString());
                cell.setStyleRanges(label.getStyleRanges());
            }
        });

        // Create contents for bnd layout workspace
        cmpBndLayout = toolkit.createComposite(cmpStackContainer);
        gl = new GridLayout(1, false);
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
                return isIncludedRepo(element);
            }

            @Override
            public boolean isGrayed(Object element) {
                return false;
            }
        });

        ToolBar toolbar = new ToolBar(cmpBndLayout, SWT.FLAT | SWT.HORIZONTAL | SWT.RIGHT);
        toolbar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

        btnMoveUp = new ToolItem(toolbar, SWT.PUSH);
        btnMoveUp.setText("Up");
        btnMoveUp.setImage(imgUp);
        btnMoveUp.setEnabled(false);

        btnMoveDown = new ToolItem(toolbar, SWT.PUSH);
        btnMoveDown.setText("Down");
        btnMoveDown.setImage(imgDown);
        btnMoveDown.setEnabled(false);

        final Styler strikeoutStyler = new StrikeoutStyler(StyledString.QUALIFIER_STYLER, JFaceResources.getColorRegistry().get(JFacePreferences.QUALIFIER_COLOR));

        runReposViewer.setLabelProvider(new StyledCellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                Object element = cell.getElement();

                String label = null;
                Image image = null;
                Styler styler = null;

                Repository repo = (Repository) element;
                label = repo.toString();
                image = repoImg;

                if (repo instanceof WorkspaceR5Repository) {
                    image = projectImg;
                }

                boolean included = isIncludedRepo(repo);
                styler = included ? null : strikeoutStyler;

                StyledString styledLabel = new StyledString(label, styler);
                cell.setText(styledLabel.getString());
                cell.setStyleRanges(styledLabel.getStyleRanges());
                cell.setImage(image);
            }

            @Override
            public String getToolTipText(Object element) {
                String tooltip = null;
                if (isIncludedRepo(element)) {
                    tooltip = "Included for resolution.";
                } else {
                    tooltip = "Excluded from resolution.";
                }
                return tooltip;
            }
        });

        ViewerSorter sorter = new ViewerSorter() {
            @Override
            public int compare(Viewer viewer, Object e1, Object e2) {
                if (isIncludedRepo(e1)) {
                    if (isIncludedRepo(e2)) {
                        // Both included => sort on position in included list
                        if (includedRepos != null) {
                            return includedRepos.indexOf(e1.toString()) - includedRepos.indexOf(e2.toString());
                        }
                        return allRepos.indexOf(e1) - allRepos.indexOf(e2);
                    }
                    // e1 included but e2 not => 11 comes first
                    return -1;
                }
                if (isIncludedRepo(e2)) {
                    // e1 not included but e2 is => e2 comes first
                    return +1;
                }
                // Neither included => sort on name
                return e1.toString().compareTo(e2.toString());
            }
        };

        runReposViewer.setSorter(sorter);

        runReposViewer.addCheckStateListener(new ICheckStateListener() {
            @Override
            public void checkStateChanged(CheckStateChangedEvent event) {
                Object repo = event.getElement();
                toggleSelection(repo);
            }
        });
        runReposViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                updateButtons();
            }
        });

        btnMoveUp.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doMoveUp();
            }
        });
        btnMoveDown.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doMoveDown();
            }
        });

        ColumnViewerToolTipSupport.enableFor(runReposViewer, ToolTip.NO_RECREATE);
    }

    void updateButtons() {
        boolean enable = !runReposViewer.getSelection().isEmpty();
        IStructuredSelection sel = (IStructuredSelection) runReposViewer.getSelection();
        for (Object elem : sel.toList()) {
            if (!isIncludedRepo(elem)) {
                enable = false;
                break;
            }
        }

        btnMoveUp.setEnabled(enable);
        btnMoveDown.setEnabled(enable);
    }

    boolean isIncludedRepo(Object repo) {
        boolean included = false;
        included = includedRepos == null || includedRepos.contains(repo.toString());
        return included;
    }

    void toggleSelection(Object selectedRepo) {
        lazyInitIncludedRepos();
        if (!includedRepos.remove(selectedRepo.toString())) {
            includedRepos.add(selectedRepo.toString());
        }

        runReposViewer.refresh();
        updateButtons();

        markDirty();
    }

    void lazyInitIncludedRepos() {
        if (includedRepos == null) {
            includedRepos = new LinkedList<String>();
            for (Repository repo : allRepos) {
                includedRepos.add(repo.toString());
            }
        }
    }

    private void reloadRepos() {
        final IMessageManager messages = getManagedForm().getMessageManager();
        messages.removeMessage(MESSAGE_KEY, runReposViewer.getControl());
        allRepos.clear();

        try {
            allRepos.addAll(model.getWorkspace().getPlugins(Repository.class));
        } catch (Exception e) {
            messages.addMessage(MESSAGE_KEY, "Repository List: Unable to load OSGi Repositories. " + e.getMessage(), e, IMessageProvider.ERROR, runReposViewer.getControl());

            // Load the repos and clear the error message if the Workspace is initialised later.
            Central.onWorkspaceInit(new Function<Workspace,Void>() {
                @Override
                public Void run(final Workspace ws) {
                    SWTConcurrencyUtil.execForControl(runReposViewer.getControl(), true, new Runnable() {
                        @Override
                        public void run() {
                            allRepos.clear();
                            allRepos.addAll(ws.getPlugins(Repository.class));
                            messages.removeMessage(MESSAGE_KEY, runReposViewer.getControl());
                        }
                    });
                    return null;
                }
            });
        }
        runReposViewer.setInput(allRepos);
        updateButtons();
    }

    void doMoveUp() {
        int[] selectedIndexes = findSelectedIndexes();
        if (CollectionUtils.moveUp(includedRepos, selectedIndexes)) {
            runReposViewer.refresh();
            updateButtons();

            markDirty();
        }
    }

    void doMoveDown() {
        int[] selectedIndexes = findSelectedIndexes();
        if (CollectionUtils.moveDown(includedRepos, selectedIndexes)) {
            runReposViewer.refresh();
            updateButtons();
            markDirty();
        }
    }

    int[] findSelectedIndexes() {
        lazyInitIncludedRepos();
        Object[] selection = ((IStructuredSelection) runReposViewer.getSelection()).toArray();
        int[] selectionIndexes = new int[selection.length];

        for (int i = 0; i < selection.length; i++) {
            Object item = selection[i];
            selectionIndexes[i] = includedRepos.indexOf(item.toString());
        }
        return selectionIndexes;
    }

    private void saveStandaloneLinksToBackup() {
        backupStandaloneLinks = model.lookupFormatter(PROP_STANDALONE).convert(standaloneLinks);
    }

    private void restoreStandaloneLinksFromBackup() {
        if (backupStandaloneLinks != null) {
            Converter<Object,String> converter = model.lookupConverter(PROP_STANDALONE);
            @SuppressWarnings("unchecked")
            List<HeaderClause> restored = (List<HeaderClause>) converter.convert(backupStandaloneLinks);
            standaloneLinks = restored;
            backupStandaloneLinks = null;
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
        List<String> tmp = model.getRunRepos();
        includedRepos = tmp == null ? null : new LinkedList<String>(tmp);
        reloadRepos();

        standaloneLinks = model.getStandaloneLinks();
        btnStandaloneCheckbox.setSelection(standaloneLinks != null);
        standaloneLinksViewer.setInput(standaloneLinks);

        backupStandaloneLinks = model.getGenericString(PROP_STANDALONE_BACKUP);

        updateStack();
        updateButtons();
    }

    private void updateStack() {
        GridData gd;
        if (needsSave) {
            stackLayout.topControl = saveToRefreshControl;
            gd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
            gd.heightHint = 40;
        } else {
            stackLayout.topControl = standaloneLinks != null ? cmpStandalone : cmpBndLayout;
            gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        }
        cmpStackContainer.setLayoutData(gd);
        cmpMainContainer.layout(true, true);
    }

    @Override
    protected void commitToModel(boolean onSave) {
        if (onSave)
            needsSave = false;
        model.setStandaloneLinks(standaloneLinks);
        model.setGenericString(PROP_STANDALONE_BACKUP, backupStandaloneLinks);
        model.setRunRepos(includedRepos != null ? new ArrayList<String>(includedRepos) : null);
    }

    @Override
    public void dispose() {
        super.dispose();

        refreshImg.dispose();
        bundleImg.dispose();

        nonObrRepoImg.dispose();

        imgUp.dispose();
        imgDown.dispose();

        imgLink.dispose();
    }

}
