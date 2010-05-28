package bndtools.views;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;

import aQute.bnd.plugin.Central;
import bndtools.Plugin;
import bndtools.model.repo.ProjectBundle;
import bndtools.model.repo.RepositoryBundle;
import bndtools.model.repo.RepositoryTreeContentProvider;
import bndtools.model.repo.RepositoryTreeLabelProvider;

public class RepositoriesView extends FilteredViewPart {

    private TreeViewer viewer;

    private class BsnFilter extends ViewerFilter {
        private final String filterStr;
        public BsnFilter(String filterStr) {
            this.filterStr = filterStr;
        }
        @Override
        public boolean select(Viewer viewer, Object parentElement, Object element) {
            String bsn = null;
            if(element instanceof RepositoryBundle) {
                bsn = ((RepositoryBundle) element).getBsn();
            } else if (element instanceof ProjectBundle) {
                bsn = ((ProjectBundle) element).getBsn();
            }
            if(bsn != null) {
                if(filterStr != null && filterStr.length() > 0 && bsn.toLowerCase().indexOf(filterStr) == -1) {
                    return false;
                }
            }
            return true;
        }
    };


    @Override
    protected void createMainControl(Composite container) {
        // CREATE CONTROLS
        Tree tree = new Tree(container, SWT.FULL_SELECTION);

        viewer = new TreeViewer(tree);
        viewer.setContentProvider(new RepositoryTreeContentProvider());
        viewer.setLabelProvider(new RepositoryTreeLabelProvider());

        // LOAD
        try {
            viewer.setInput(Central.getWorkspace());
        } catch (Exception e) {
            Plugin.logError("Error loading repositories", e);
        }

        // LAYOUT
        GridLayout layout = new GridLayout(1,false);
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 0;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        container.setLayout(layout);

        tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    }

    @Override
    protected void updatedFilter(String filterString) {
        if(filterString == null || filterString.length() == 0) {
            viewer.setFilters(new ViewerFilter[0]);
        } else {
            viewer.setFilters(new ViewerFilter[] { new BsnFilter(filterString) });
            viewer.expandToLevel(2);
        }
    }
}