package bndtools.editor.project;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
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
import bndtools.Central;
import bndtools.Plugin;
import bndtools.WorkspaceObrProvider;
import bndtools.editor.common.BndEditorPart;
import bndtools.model.repo.RepositoryBundle;
import bndtools.model.repo.RepositoryBundleVersion;
import bndtools.model.repo.RepositoryTreeContentProvider;
import bndtools.utils.SelectionDragAdapter;

public class RepositorySelectionPart extends BndEditorPart {

    private final Image refreshImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/arrow_refresh.png").createImage();
    private final Image bundleImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/brick.png").createImage();
    private final Image checkedImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/tick.png").createImage();
    private final Image uncheckedImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/cross.png").createImage();

    private final Image projectImg = PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);
    private final Image repoImg = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);

    private Text txtSearch;
    private TreeViewer viewer;

    private final ArrayList<RepositoryPlugin> allRepos = new ArrayList<RepositoryPlugin>();
    private Set<String> includedRepos = null;

    /**
     * Create the SectionPart.
     * @param parent
     * @param toolkit
     * @param style
     */
    public RepositorySelectionPart(Composite parent, FormToolkit toolkit, int style) {
        super(parent, toolkit, style);
        Section section = getSection();
        section.setDescription("Select the repositories that will be used for resolution. Bundles inside the repositories can be dragged into requirements.");
        createClient(getSection(), toolkit);
    }

    /**
     * Fill the section.
     */
    private void createClient(final Section section, FormToolkit toolkit) {
        section.setText("Run Repositories");
        Composite container = toolkit.createComposite(section);

        // Create toolbar
        ToolBar toolbar = new ToolBar(section, SWT.FLAT);
        section.setTextClient(toolbar);
        fillToolBar(toolbar);

        section.setClient(container);
        GridLayout gl_container = new GridLayout(1, false);
        gl_container.marginWidth = 0;
        gl_container.marginHeight = 0;
        gl_container.marginRight = 10;
        container.setLayout(gl_container);

        txtSearch = new Text(container, SWT.BORDER | SWT.H_SCROLL | SWT.SEARCH | SWT.ICON_SEARCH | SWT.CANCEL);
        txtSearch.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        toolkit.adapt(txtSearch, true, true);

        Composite treeContainer = new Composite(container, SWT.NONE);
        TreeColumnLayout treeLayout = new TreeColumnLayout();
        treeContainer.setLayout(treeLayout);
        treeContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Tree tree = new Tree(treeContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
        toolkit.adapt(tree);
        toolkit.paintBordersFor(tree);

        ControlDecoration treeDecoration = new ControlDecoration(tree, SWT.RIGHT | SWT.TOP, container);
        treeDecoration.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL).getImage());
        treeDecoration.setDescriptionText("Double-click a repository entry to toggle selection");
        treeDecoration.setMarginWidth(3);
        treeDecoration.setShowHover(true);

        viewer = new TreeViewer(tree);
        viewer.setContentProvider(new RepositoryTreeContentProvider());

        final Color enabledColor = section.getDisplay().getSystemColor(SWT.COLOR_BLACK);
        final Color disabledColor = section.getDisplay().getSystemColor(SWT.COLOR_GRAY);

        TreeColumn colName = new TreeColumn(tree, SWT.NONE);
        colName.setText("Name");
        treeLayout.setColumnData(colName, new ColumnWeightData(85, true));

        TreeColumn colSelected = new TreeColumn(tree, SWT.CENTER);
        colSelected.setText("Selected");
        treeLayout.setColumnData(colSelected, new ColumnPixelData(19, false));

        viewer.setLabelProvider(new StyledCellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                Object element = cell.getElement();
                int column = cell.getColumnIndex();

                String label = null;
                Image image = null;
                Color foreground = enabledColor;;

                if (element instanceof RepositoryPlugin) {
                    RepositoryPlugin repo = (RepositoryPlugin) element;
                    boolean included = isIncludedRepo(repo);
                    if (column == 0) {
                        label = repo.getName();
                        image = (element instanceof WorkspaceObrProvider) ? projectImg : repoImg;
                    } else if (column == 1) {
                        image = included ? checkedImg : uncheckedImg;
                    }
                    foreground = included ? enabledColor : disabledColor;
                } else if (element instanceof RepositoryBundle) {
                    RepositoryBundle bundle = (RepositoryBundle) element;
                    RepositoryPlugin repo = bundle.getRepo();

                    if (column == 0) {
                        label = bundle.getBsn();
                        image = bundleImg;
                    }
                    foreground = isIncludedRepo(repo) ? enabledColor : disabledColor;
                } else if (element instanceof RepositoryBundleVersion) {
                    RepositoryBundleVersion bundleVersion = (RepositoryBundleVersion) element;
                    RepositoryPlugin repo = bundleVersion.getBundle().getRepo();
                    if (column == 0)
                        label = bundleVersion.getVersion().toString();
                    foreground = isIncludedRepo(repo) ? enabledColor : disabledColor;
                } else {
                    label = element.toString();
                }

                cell.setText(label);
                cell.setImage(image);
                cell.setForeground(foreground);
            }
        });

        viewer.addOpenListener(new IOpenListener() {
            public void open(OpenEvent event) {
                IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                for (Object element : selection.toList()) {
                    if (element instanceof RepositoryPlugin) {
                        toggleSelection((RepositoryPlugin) element);
                    }
                }
            }
        });

        viewer.addDragSupport(DND.DROP_COPY | DND.DROP_MOVE, new Transfer[] { LocalSelectionTransfer.getTransfer() }, new SelectionDragAdapter(viewer) {
            @Override
            public void dragStart(DragSourceEvent event) {
                IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
                if (selection.isEmpty())
                    event.doit = false;
                else {
                    for (Object element : selection.toList()) {
                        if (!(element instanceof RepositoryBundle || element instanceof RepositoryBundleVersion)) {
                            event.doit = false;
                            break;
                        }
                    }
                }

                if (event.doit) {
                    LocalSelectionTransfer.getTransfer().setSelection(selection);
                    LocalSelectionTransfer.getTransfer().setSelectionSetTime(event.time & 0xFFFFFFFFL);
                }
            }
        });

        reloadRepos();
    }

    boolean isIncludedRepo(RepositoryPlugin repo) {
        return includedRepos == null || includedRepos.contains(repo.getName());
    }

    void toggleSelection(RepositoryPlugin selectedRepo) {
        if (includedRepos == null) {
            includedRepos = new LinkedHashSet<String>();

            for (RepositoryPlugin repo : allRepos) {
                includedRepos.add(repo.getName());
            }
        }

        if (!includedRepos.remove(selectedRepo.getName()))
            includedRepos.add(selectedRepo.getName());

        viewer.refresh(selectedRepo);
        markDirty();
    }

    private void reloadRepos() {
        allRepos.clear();
        try {
            List<OBRIndexProvider> repos = Central.getWorkspace().getPlugins(OBRIndexProvider.class);
            allRepos.ensureCapacity(repos.size());
            for (OBRIndexProvider repo : repos) {
                if (repo instanceof RepositoryPlugin && repo.getSupportedModes().contains(OBRResolutionMode.runtime))
                    allRepos.add((RepositoryPlugin) repo);
            }
        } catch (Exception e) {
            Plugin.logError("Error getting repository list from workspace", e);
        }

        viewer.setInput(allRepos);
    }

    private void fillToolBar(ToolBar toolbar) {
        ToolItem refreshTool = new ToolItem(toolbar, SWT.PUSH);
        refreshTool.setImage(refreshImg);
    }

    @Override
    protected String[] getProperties() {
        return new String[] { BndConstants.RUNREPOS };
    }

    @Override
    protected void refreshFromModel() {
        List<String> tmp = model.getRunRepos();
        includedRepos = tmp == null ? null : new LinkedHashSet<String>(tmp);
        viewer.refresh(true);
    }

    @Override
    protected void commitToModel(boolean onSave) {
        model.setRunRepos(new ArrayList<String>(includedRepos));
    }

    @Override
    public void dispose() {
        super.dispose();
        refreshImg.dispose();
        bundleImg.dispose();
        checkedImg.dispose();
    }

}
