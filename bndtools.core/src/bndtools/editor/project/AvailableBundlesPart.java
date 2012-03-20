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

import aQute.bnd.service.OBRResolutionMode;
import bndtools.BndConstants;
import bndtools.Plugin;
import bndtools.editor.common.BndEditorPart;
import bndtools.model.repo.RepositoryBundle;
import bndtools.model.repo.RepositoryTreeContentProvider;
import bndtools.model.repo.RepositoryTreeLabelProvider;
import bndtools.model.repo.RepositoryUtils;
import bndtools.utils.SelectionDragAdapter;
import bndtools.views.RepositoryBsnFilter;

public class AvailableBundlesPart extends BndEditorPart {
    
    // Number of millis to wait for the user to stop typing in the filter box
    private static final long SEARCH_DELAY = 1000;
    
    private String searchStr = "";
    private ScheduledFuture<?> scheduledFilterUpdate = null;
    
    private Text txtSearch;
    private TreeViewer viewer;
    
    private Set<String> includedRepos;
    
    private final ViewerFilter includedRepoFilter = new ViewerFilter() {
        @Override
        public boolean select(Viewer viewer, Object parentElement, Object element) {
            boolean select;
            if (element instanceof RepositoryBundle)
                select = includedRepos == null || includedRepos.contains(((RepositoryBundle) element).getRepo().getName());
            else
                select = true;
            return select;
        }
    };
    
    private final Runnable updateFilterTask = new Runnable() {
        public void run() {
            Display display = viewer.getControl().getDisplay();
            
            final ViewerFilter[] filters;
            if (searchStr == null || searchStr.trim().length() == 0)
                filters = new ViewerFilter[] { includedRepoFilter };
            else
                filters = new ViewerFilter[] { includedRepoFilter, new RepositoryBsnFilter(searchStr.trim()) };
            Runnable update = new Runnable() {
                public void run() {
                    viewer.setFilters(filters);
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
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        container.setLayout(layout);
        
        toolkit.createLabel(container, "Filter:");
        
        txtSearch = toolkit.createText(container, "", SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL | SWT.BORDER);
        txtSearch.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        Tree tree = toolkit.createTree(container, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL);
        tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
        
        viewer = new TreeViewer(tree);
        RepositoryTreeContentProvider contentProvider = new RepositoryTreeContentProvider(OBRResolutionMode.runtime);
        contentProvider.setShowRepos(false);
        viewer.setContentProvider(contentProvider);
        viewer.setLabelProvider(new RepositoryTreeLabelProvider());
        viewer.setFilters(new ViewerFilter[] { includedRepoFilter });
        
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
        
        viewer.addDragSupport(DND.DROP_COPY | DND.DROP_MOVE, new Transfer[] { LocalSelectionTransfer.getTransfer() }, new SelectionDragAdapter(viewer) {
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
        
        
        viewer.setInput(RepositoryUtils.listRepositories(true));
    }
    
    void updatedFilter(String filterString) {
        if(filterString == null || filterString.length() == 0) {
            viewer.setFilters(new ViewerFilter[0]);
        } else {
            viewer.setFilters(new ViewerFilter[] { new RepositoryBsnFilter(filterString) });
        }
    }

    @Override
    protected String[] getProperties() {
        return new String[] { BndConstants.RUNREPOS };
    }

    @Override
    protected void refreshFromModel() {
        List<String> tmp = model.getRunRepos();
        includedRepos = (tmp == null) ? null : new HashSet<String>(tmp);
        viewer.refresh(true);
    }

    @Override
    protected void commitToModel(boolean onSave) {
        // Nothing to do
    }

}
