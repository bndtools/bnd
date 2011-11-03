package bndtools.views;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import bndtools.model.repo.ProjectBundle;
import bndtools.model.repo.RepositoryBundle;

public class RepositoryBsnFilter extends ViewerFilter {
    private final String filterStr;
    public RepositoryBsnFilter(String filterStr) {
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
}