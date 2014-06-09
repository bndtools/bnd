package bndtools.editor.project;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.bndtools.utils.Function;
import org.bndtools.utils.collections.CollectionUtils;
import org.bndtools.utils.jface.StrikeoutStyler;
import org.bndtools.utils.swt.SWTConcurrencyUtil;
import org.eclipse.jface.dialogs.IMessageProvider;
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
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.service.repository.Repository;

import aQute.bnd.build.Workspace;
import bndtools.BndConstants;
import bndtools.Plugin;
import bndtools.central.Central;
import bndtools.central.WorkspaceR5Repository;
import bndtools.editor.common.BndEditorPart;

public class RepositorySelectionPart extends BndEditorPart {

    private final Image refreshImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/arrow_refresh.png").createImage();
    private final Image bundleImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/brick.png").createImage();

    private final Image projectImg = PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);
    private final Image repoImg = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
    private final Image nonObrRepoImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/warning_obj.gif").createImage();

    private final Image imgUp = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/arrow_up.png").createImage();
    private final Image imgDown = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/arrow_down.png").createImage();

    private final Object MESSAGE_KEY = new Object();

    private CheckboxTableViewer viewer;
    private ToolItem btnMoveUp;
    private ToolItem btnMoveDown;

    private final ArrayList<Repository> allRepos = new ArrayList<Repository>();
    private List<String> includedRepos = null;

    /**
     * Create the SectionPart.
     *
     * @param parent
     * @param toolkit
     * @param style
     */
    public RepositorySelectionPart(Composite parent, FormToolkit toolkit, int style) {
        super(parent, toolkit, style);
        Section section = getSection();
        section.setDescription("Select the repositories that will be available for resolution.");
        createClient(section, toolkit);
    }

    /**
     * Fill the section.
     */
    private void createClient(final Section section, FormToolkit toolkit) {
        section.setText("Repository Selection");

        // Create toolbar
        ToolBar toolbar = new ToolBar(section, SWT.FLAT);
        section.setTextClient(toolbar);
        fillToolBar(toolbar);

        // Create contents
        Composite container = toolkit.createComposite(section);
        section.setClient(container);
        GridLayout gl_container = new GridLayout(1, false);
        gl_container.marginWidth = 0;
        gl_container.marginHeight = 0;
        container.setLayout(gl_container);

        Table table = toolkit.createTable(container, SWT.CHECK | SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.widthHint = 50;
        table.setLayoutData(gd);

        viewer = new CheckboxTableViewer(table);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setCheckStateProvider(new ICheckStateProvider() {
            @Override
            public boolean isChecked(Object element) {
                return isIncludedRepo(element);
            }

            @Override
            public boolean isGrayed(Object element) {
                return false;
            }
        });

        toolbar = new ToolBar(container, SWT.FLAT | SWT.HORIZONTAL | SWT.RIGHT);
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

        viewer.setLabelProvider(new StyledCellLabelProvider() {
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

        viewer.setSorter(sorter);

        viewer.addCheckStateListener(new ICheckStateListener() {
            @Override
            public void checkStateChanged(CheckStateChangedEvent event) {
                Object repo = event.getElement();
                toggleSelection(repo);
            }
        });
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
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

        ColumnViewerToolTipSupport.enableFor(viewer, ToolTip.NO_RECREATE);
    }

    void updateButtons() {
        boolean enable = !viewer.getSelection().isEmpty();
        IStructuredSelection sel = (IStructuredSelection) viewer.getSelection();
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
        if (!includedRepos.remove(selectedRepo.toString()))
            includedRepos.add(selectedRepo.toString());

        viewer.refresh();
        updateButtons();

        // markDirty();
        commit(false);
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
        messages.removeMessage(MESSAGE_KEY, viewer.getControl());
        allRepos.clear();

        try {
            allRepos.addAll(Central.getWorkspace().getPlugins(Repository.class));
        } catch (Exception e) {
            messages.addMessage(MESSAGE_KEY, "Repository List: Unable to load OSGi Repositories. " + e.getMessage(), e, IMessageProvider.ERROR, viewer.getControl());

            // Load the repos and clear the error message if the Workspace is initialised later.
            Central.onWorkspaceInit(new Function<Workspace,Void>() {
                @Override
                public Void run(final Workspace ws) {
                    SWTConcurrencyUtil.execForControl(viewer.getControl(), true, new Runnable() {
                        @Override
                        public void run() {
                            allRepos.clear();
                            allRepos.addAll(ws.getPlugins(Repository.class));
                            messages.removeMessage(MESSAGE_KEY, viewer.getControl());
                        }
                    });
                    return null;
                }
            });
        }
        viewer.setInput(allRepos);
        updateButtons();
    }

    private void fillToolBar(ToolBar toolbar) {
        ToolItem refreshTool = new ToolItem(toolbar, SWT.PUSH);
        refreshTool.setImage(refreshImg);

        refreshTool.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                reloadRepos();
            }
        });
    }

    void doMoveUp() {
        int[] selectedIndexes = findSelectedIndexes();
        if (CollectionUtils.moveUp(includedRepos, selectedIndexes)) {
            viewer.refresh();
            updateButtons();

            // markDirty();
            commit(false);
        }
    }

    void doMoveDown() {
        int[] selectedIndexes = findSelectedIndexes();
        if (CollectionUtils.moveDown(includedRepos, selectedIndexes)) {
            viewer.refresh();
            updateButtons();
            // markDirty();
            commit(false);
        }
    }

    int[] findSelectedIndexes() {
        lazyInitIncludedRepos();
        Object[] selection = ((IStructuredSelection) viewer.getSelection()).toArray();
        int[] selectionIndexes = new int[selection.length];

        for (int i = 0; i < selection.length; i++) {
            Object item = selection[i];
            selectionIndexes[i] = includedRepos.indexOf(item.toString());
        }
        return selectionIndexes;
    }

    @Override
    protected String[] getProperties() {
        return new String[] {
            BndConstants.RUNREPOS
        };
    }

    @Override
    protected void refreshFromModel() {
        List<String> tmp = model.getRunRepos();
        includedRepos = tmp == null ? null : new LinkedList<String>(tmp);
        reloadRepos();
        updateButtons();
    }

    @Override
    protected void commitToModel(boolean onSave) {
        model.setRunRepos(new ArrayList<String>(includedRepos));
    }

    @Override
    public void dispose() {
        super.dispose();

        refreshImg.dispose();

        imgUp.dispose();
        imgDown.dispose();

        bundleImg.dispose();
        nonObrRepoImg.dispose();
    }

}
