package bndtools.editor.project;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import aQute.bnd.build.WorkspaceRepository;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.ResolutionPhase;
import bndtools.BndConstants;
import bndtools.Plugin;
import bndtools.central.RepositoryUtils;
import bndtools.editor.common.BndEditorPart;
import bndtools.model.repo.RepositoryBundle;
import bndtools.model.repo.RepositoryTreeContentProvider;
import bndtools.model.repo.RepositoryTreeLabelProvider;
import bndtools.utils.SelectionDragAdapter;

public class AvailableBundlesPart extends BndEditorPart {

    // Number of millis to wait for the user to stop typing in the filter box
    private static final long SEARCH_DELAY = 1000;

    private String searchStr = "";
    private ScheduledFuture< ? > scheduledFilterUpdate = null;

    private final RepositoryTreeContentProvider contentProvider = new RepositoryTreeContentProvider(ResolutionPhase.runtime);
    private Text txtSearch;
    private TreeViewer viewer;

    private Set<String> includedRepos;

    private final ViewerFilter includedRepoFilter = new ViewerFilter() {
        @Override
        public boolean select(Viewer viewer, Object parentElement, Object element) {
            boolean select = false;
            if (element instanceof RepositoryBundle) {
                RepositoryBundle repoBundle = (RepositoryBundle) element;
                RepositoryPlugin repo = repoBundle.getRepo();

                if (includedRepos == null) {
                    select = true;
                } else if (repo instanceof WorkspaceRepository) {
                    select = includedRepos.contains("Workspace");
                } else {
                    select = includedRepos.contains(repoBundle.getRepo().getName());
                }
            } else {
                select = true;
            }
            return select;
        }
    };

    private final Runnable updateFilterTask = new Runnable() {
        public void run() {
            Display display = viewer.getControl().getDisplay();

            Runnable update = new Runnable() {
                public void run() {
                    updatedFilter(searchStr);
                }
            };

            if (display.getThread() == Thread.currentThread())
                update.run();
            else
                display.asyncExec(update);
        }
    };

    public AvailableBundlesPart(Composite parent, FormToolkit toolkit, int style) {
        super(parent, toolkit, style);
        Section section = getSection();
        section.setDescription("Available bundles. These can be dragged to the Run Requirements list to the right.");
        createClient(section, toolkit);
    }

    private void createClient(Section section, FormToolkit toolkit) {
        section.setText("Available Bundles");

        // Create contents
        Composite container = toolkit.createComposite(section);
        section.setClient(container);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        container.setLayout(layout);

        txtSearch = toolkit.createText(container, "", SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL | SWT.BORDER);
        txtSearch.setMessage("Enter search string");
        txtSearch.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Tree tree = toolkit.createTree(container, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL);
        tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

        viewer = new TreeViewer(tree);
        contentProvider.setShowRepos(false);
        viewer.setContentProvider(contentProvider);
        viewer.setLabelProvider(new RepositoryTreeLabelProvider(true));
        viewer.setFilters(new ViewerFilter[] {
            includedRepoFilter
        });

        txtSearch.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                if (scheduledFilterUpdate != null)
                    scheduledFilterUpdate.cancel(true);

                searchStr = txtSearch.getText();
                scheduledFilterUpdate = Plugin.getDefault().getScheduler().schedule(updateFilterTask, SEARCH_DELAY, TimeUnit.MILLISECONDS);
            }
        });
        txtSearch.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                if (scheduledFilterUpdate != null)
                    scheduledFilterUpdate.cancel(true);
                scheduledFilterUpdate = null;

                searchStr = txtSearch.getText();
                updateFilterTask.run();
            }
        });

        viewer.addDragSupport(DND.DROP_COPY | DND.DROP_MOVE, new Transfer[] {
            LocalSelectionTransfer.getTransfer()
        }, new SelectionDragAdapter(viewer) {
            @Override
            public void dragStart(DragSourceEvent event) {
                IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
                if (!selection.isEmpty()) {
                    LocalSelectionTransfer.getTransfer().setSelection(selection);
                    LocalSelectionTransfer.getTransfer().setSelectionSetTime(event.time & 0xFFFFFFFFL);
                } else {
                    event.doit = false;
                }
            }
        });
    }

    private void updatedFilter(String filterString) {
        String newFilter;
        if (filterString == null || filterString.length() == 0 || filterString.trim().equals("*"))
            newFilter = null;
        else
            newFilter = "*" + filterString.trim() + "*";

        contentProvider.setFilter(newFilter);
        viewer.refresh(true);
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
        includedRepos = (tmp == null) ? null : new HashSet<String>(tmp);
        viewer.setInput(RepositoryUtils.listRepositories(true));
    }

    @Override
    protected void commitToModel(boolean onSave) {
        // Nothing to do
    }

}
