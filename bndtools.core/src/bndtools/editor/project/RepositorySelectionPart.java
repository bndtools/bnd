package bndtools.editor.project;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
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
import bndtools.Plugin;
import bndtools.WorkspaceObrProvider;
import bndtools.editor.common.BndEditorPart;
import bndtools.model.repo.RepositoryBundle;
import bndtools.model.repo.RepositoryBundleVersion;
import bndtools.model.repo.RepositoryTreeContentProvider;
import bndtools.model.repo.RepositoryUtils;
import bndtools.model.repo.WrappingObrRepository;
import bndtools.utils.SelectionDragAdapter;
import bndtools.views.RepositoryBsnFilter;

public class RepositorySelectionPart extends BndEditorPart {

    private final Image refreshImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/arrow_refresh.png").createImage();
    private final Image bundleImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/brick.png").createImage();
    private final Image checkedImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/tick.png").createImage();
    private final Image uncheckedImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/cross.png").createImage();

    private final Image projectImg = PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);
    private final Image repoImg = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
    private final Image nonObrRepoImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/warning_obj.gif").createImage();

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

        txtSearch.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                if(e.detail == SWT.CANCEL)
                    updatedFilter("");
                else
                    updatedFilter(txtSearch.getText());
            }
        });

        Composite treeContainer = new Composite(container, SWT.NONE);
//        TreeColumnLayout treeLayout = new TreeColumnLayout();
        treeContainer.setLayout(new FillLayout());
        treeContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Tree tree = toolkit.createTree(treeContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL);
        tree.setHeaderVisible(true);
        tree.setLinesVisible(false);

        ControlDecoration treeDecoration = new ControlDecoration(tree, SWT.RIGHT | SWT.TOP, container);
        treeDecoration.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL).getImage());
        treeDecoration.setDescriptionText("Double-click a repository entry to toggle selection");
        treeDecoration.setMarginWidth(3);
        treeDecoration.setShowHover(true);

        viewer = new TreeViewer(tree);
        viewer.setContentProvider(new RepositoryTreeContentProvider());

        final Color enabledColor = section.getDisplay().getSystemColor(SWT.COLOR_BLACK);
        final Color enabledVersionColor = JFaceResources.getColorRegistry().get(JFacePreferences.COUNTER_COLOR);
        final Color disabledColor = section.getDisplay().getSystemColor(SWT.COLOR_GRAY);

        TreeColumn colSelected = new TreeColumn(tree, SWT.CENTER);
        colSelected.setWidth(45);
        colSelected.setMoveable(false);
        colSelected.setResizable(false);

        TreeColumn colName = new TreeColumn(tree, SWT.NONE);
        colName.setText("Name");
        colName.setWidth(450);
        colName.setMoveable(false);
        colName.setResizable(true);

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

                    if (repo instanceof OBRIndexProvider) {
                        boolean included = isIncludedRepo(repo);
                        if (column == 1) {
                            label = repo.getName();
                            image = repoImg;
                            if (repo instanceof WrappingObrRepository) {
                                if (((WrappingObrRepository) repo).getDelegate() instanceof WorkspaceObrProvider)
                                    image = projectImg;
                            }
                        } else if (column == 0) {
                            image = included ? checkedImg : uncheckedImg;
                        }
                        foreground = included ? enabledColor : disabledColor;
                    } else {
                        foreground = disabledColor;
                        if (column == 0) image = nonObrRepoImg;
                        else if (column == 1) {
                            image = repoImg;
                            label = repo.getName();
                        }
                    }
                } else if (element instanceof RepositoryBundle) {
                    RepositoryBundle bundle = (RepositoryBundle) element;
                    RepositoryPlugin repo = bundle.getRepo();

                    if (column == 1) {
                        label = bundle.getBsn();
                        image = bundleImg;
                    }
                    foreground = isIncludedRepo(repo) ? enabledColor : disabledColor;
                } else if (element instanceof RepositoryBundleVersion) {
                    RepositoryBundleVersion bundleVersion = (RepositoryBundleVersion) element;
                    RepositoryPlugin repo = bundleVersion.getBundle().getRepo();
                    if (column == 1)
                        label = bundleVersion.getVersion().toString();
                    foreground = isIncludedRepo(repo) ? enabledVersionColor : disabledColor;
                } else {
                    label = element.toString();
                }

                cell.setText(label);
                cell.setImage(image);
                cell.setForeground(foreground);
            }

            @Override
            public String getToolTipText(Object element) {
                String tooltip = null;
                if (element instanceof RepositoryPlugin) {
                    if (element instanceof OBRIndexProvider) {
                        if (isIncludedRepo((RepositoryPlugin) element))
                            tooltip = "Included in OBR resolution.";
                        else
                            tooltip = "Excluded from OBR resolution.";
                    } else {
                        tooltip = "Not an OBR repository, cannot be used for OBR resolution.";
                    }
                }
                return tooltip;
            }
        });

        viewer.addOpenListener(new IOpenListener() {
            public void open(OpenEvent event) {
                IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                for (Object element : selection.toList()) {
                    if (element instanceof RepositoryPlugin)
                        toggleSelection((RepositoryPlugin) element);
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
                        if (element instanceof RepositoryBundle)
                            event.doit = isIncludedRepo(((RepositoryBundle) element).getRepo());
                        else if (element instanceof RepositoryBundleVersion)
                            event.doit = isIncludedRepo(((RepositoryBundleVersion) element).getBundle().getRepo());
                        else
                            event.doit = false;

                        if (!event.doit)
                            break;
                    }
                }

                if (event.doit) {
                    LocalSelectionTransfer.getTransfer().setSelection(selection);
                    LocalSelectionTransfer.getTransfer().setSelectionSetTime(event.time & 0xFFFFFFFFL);
                }
            }
        });

        ColumnViewerToolTipSupport.enableFor(viewer, ToolTip.NO_RECREATE);

        reloadRepos();
    }

    void updatedFilter(String filterString) {
        if(filterString == null || filterString.length() == 0) {
            viewer.setFilters(new ViewerFilter[0]);
        } else {
            viewer.setFilters(new ViewerFilter[] { new RepositoryBsnFilter(filterString) });
            viewer.expandToLevel(2);
        }
    }

    boolean isIncludedRepo(RepositoryPlugin repo) {
        boolean included = false;
        if (repo instanceof OBRIndexProvider) {
            OBRIndexProvider obrProvider = (OBRIndexProvider) repo;
            included = obrProvider.getSupportedModes().contains(OBRResolutionMode.runtime) && (includedRepos == null || includedRepos.contains(repo.getName()));
        }
        return included;
    }

    void toggleSelection(RepositoryPlugin selectedRepo) {
        if (!(selectedRepo instanceof OBRIndexProvider)) {
            return;
        }

        OBRIndexProvider obrProvider = (OBRIndexProvider) selectedRepo;
        if (!obrProvider.getSupportedModes().contains(OBRResolutionMode.runtime)) {
            return;
        }

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
        allRepos.addAll(RepositoryUtils.listRepositories(true));
        viewer.setInput(allRepos);
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
        nonObrRepoImg.dispose();
    }

}
