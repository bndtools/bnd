package bndtools.editor.project;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.bndtools.core.utils.collections.CollectionUtils;
import org.bndtools.core.utils.jface.StrikeoutStyler;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.JFaceResources;
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
import org.eclipse.jface.viewers.ViewerFilter;
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
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import aQute.bnd.service.OBRIndexProvider;
import aQute.bnd.service.OBRResolutionMode;
import aQute.bnd.service.RepositoryPlugin;
import bndtools.BndConstants;
import bndtools.Plugin;
import bndtools.WorkspaceObrProvider;
import bndtools.editor.common.BndEditorPart;
import bndtools.model.repo.RepositoryBundle;
import bndtools.model.repo.RepositoryBundleVersion;
import bndtools.model.repo.RepositoryTreeContentProvider;
import bndtools.model.repo.RepositoryUtils;
import bndtools.model.repo.WrappingObrRepository;

public class RepositorySelectionPart extends BndEditorPart {

    private final Image refreshImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/arrow_refresh.png").createImage();
    private final Image bundleImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/brick.png").createImage();

    private final Image projectImg = PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);
    private final Image repoImg = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
    private final Image nonObrRepoImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/warning_obj.gif").createImage();

    private final Image imgUp = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/arrow_up.png").createImage();
    private final Image imgDown = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/arrow_down.png").createImage();

    private CheckboxTableViewer viewer;
    private ToolItem btnMoveUp;
    private ToolItem btnMoveDown;

    private final ArrayList<RepositoryPlugin> allRepos = new ArrayList<RepositoryPlugin>();
    private List<String> includedRepos = null;

    /**
     * Create the SectionPart.
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

        Table table = toolkit.createTable(container, SWT.CHECK | SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        viewer = new CheckboxTableViewer(table);
        viewer.setContentProvider(new RepositoryTreeContentProvider());
        viewer.setFilters(new ViewerFilter[] { new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                return isAvailableRepo(element);
            }
        }});
        viewer.setCheckStateProvider(new ICheckStateProvider() {
            public boolean isChecked(Object element) {
                return isIncludedRepo((RepositoryPlugin) element);
            }
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

                RepositoryPlugin repo = (RepositoryPlugin) element;
                label = repo.getName();
                image = repoImg;

                if (repo instanceof WrappingObrRepository) {
                    if (((WrappingObrRepository) repo).getDelegate() instanceof WorkspaceObrProvider)
                        image = projectImg;
                }
                
                boolean available = isAvailableRepo(repo);
                if (available) {
                    boolean included = isIncludedRepo(repo);
                    styler = included ? null : strikeoutStyler;
                } else {
                    styler = StyledString.QUALIFIER_STYLER;
                }
                
                StyledString styledLabel = new StyledString(label, styler);
                cell.setText(styledLabel.getString());
                cell.setStyleRanges(styledLabel.getStyleRanges());
                cell.setImage(image);
            }

            @Override
            public String getToolTipText(Object element) {
                String tooltip = null;
                if (isIncludedRepo((RepositoryPlugin) element)) {
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
                if (e1 instanceof RepositoryPlugin && e2 instanceof RepositoryPlugin) {
                    return compareRepos((RepositoryPlugin) e1, (RepositoryPlugin) e2);
                } else if (e1 instanceof RepositoryBundle && e2 instanceof RepositoryBundle) {
                    return compareBundles((RepositoryBundle) e1, (RepositoryBundle) e2);
                } else if (e1 instanceof RepositoryBundleVersion && e2 instanceof RepositoryBundleVersion) {
                    return compareBundleVersions((RepositoryBundleVersion) e1, (RepositoryBundleVersion) e2);
                }
                return 0;
            }

            public int compareRepos(RepositoryPlugin r1, RepositoryPlugin r2) {
                if (isIncludedRepo(r1)) {
                    if (isIncludedRepo(r2)) {
                        // Both included => sort on position in included list
                        if (includedRepos != null) {
                            return includedRepos.indexOf(r1.getName()) - includedRepos.indexOf(r2.getName());
                        } else {
                            return allRepos.indexOf(r1) - allRepos.indexOf(r2);
                        }
                    }
                    // r1 included but r2 not => r1 comes first
                    return -1;
                }
                if (isIncludedRepo(r2)) {
                    // r1 not included but r2 is => r2 comes first
                    return +1;
                }
                // Neither included => sort on name
                return r1.getName().compareTo(r2.getName());
            }

            public int compareBundles(RepositoryBundle b1, RepositoryBundle b2) {
                return b1.getBsn().compareTo(b2.getBsn());
            }

            public int compareBundleVersions(RepositoryBundleVersion v1, RepositoryBundleVersion v2) {
                return v1.getVersion().compareTo(v2.getVersion());
            }
        };

        viewer.setSorter(sorter);
        
        viewer.addCheckStateListener(new ICheckStateListener() {
            public void checkStateChanged(CheckStateChangedEvent event) {
                RepositoryPlugin repo = (RepositoryPlugin) event.getElement();
                toggleSelection(repo);
            }
        });
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
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

        reloadRepos();
    }

    void updateButtons() {
        boolean enable = !viewer.getSelection().isEmpty();
        IStructuredSelection sel = (IStructuredSelection) viewer.getSelection();
        for (Object elem : sel.toList()) {
            if (!(elem instanceof RepositoryPlugin)) {
                enable = false;
                break;
            }
            RepositoryPlugin plugin = (RepositoryPlugin) elem;
            if (!isIncludedRepo(plugin)) {
                enable = false;
                break;
            }
        }

        btnMoveUp.setEnabled(enable);
        btnMoveDown.setEnabled(enable);
    }

    boolean isAvailableRepo(Object repoObj) {
        if (repoObj instanceof OBRIndexProvider) {
            return ((OBRIndexProvider) repoObj).getSupportedModes().contains(OBRResolutionMode.runtime);
        }
        return false;
    }

    boolean isIncludedRepo(RepositoryPlugin repo) {
        boolean included = false;
        included = includedRepos == null || includedRepos.contains(repo.getName());
        return included;
    }

    void toggleSelection(RepositoryPlugin selectedRepo) {
        if (!isAvailableRepo(selectedRepo))
            return;

        lazyInitIncludedRepos();
        if (!includedRepos.remove(selectedRepo.getName()))
            includedRepos.add(selectedRepo.getName());

        viewer.refresh();
        updateButtons();

//        markDirty();
        commit(false);
    }

    void lazyInitIncludedRepos() {
        if (includedRepos == null) {
            includedRepos = new LinkedList<String>();
            for (RepositoryPlugin repo : allRepos) {
                if (isAvailableRepo(repo) ) {
                    includedRepos.add(repo.getName());
                }
            }
        }
    }

    private void reloadRepos() {
        allRepos.clear();
        allRepos.addAll(RepositoryUtils.listRepositories(true));
        viewer.setInput(allRepos);
        updateButtons();
    }

    private void fillToolBar(ToolBar toolbar) {
        ToolItem refreshTool = new ToolItem(toolbar, SWT.PUSH);
        refreshTool.setImage(refreshImg);

        refreshTool.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                reloadRepos();
            }
        });
    }

    void doMoveUp() {
        int[] selectedIndexes = findSelectedIndexes();
        if (CollectionUtils.moveUp(includedRepos, selectedIndexes)) {
            viewer.refresh();
            updateButtons();

            //markDirty();
            commit(false);
        }
    }

    void doMoveDown() {
        int[] selectedIndexes = findSelectedIndexes();
        if (CollectionUtils.moveDown(includedRepos, selectedIndexes)) {
            viewer.refresh();
            updateButtons();
            //markDirty();
            commit(false);
        }
    }

    int[] findSelectedIndexes() {
        Object[] selection = ((IStructuredSelection) viewer.getSelection()).toArray();
        int[] selectionIndexes = new int[selection.length];

        for(int i=0; i<selection.length; i++) {
            RepositoryPlugin item = (RepositoryPlugin) selection[i];
            selectionIndexes[i] = includedRepos.indexOf(item.getName());
        }
        return selectionIndexes;
    }


    @Override
    protected String[] getProperties() {
        return new String[] { BndConstants.RUNREPOS };
    }

    @Override
    protected void refreshFromModel() {
        List<String> tmp = model.getRunRepos();
        includedRepos = tmp == null ? null : new LinkedList<String>(tmp);
        viewer.refresh(true);
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
