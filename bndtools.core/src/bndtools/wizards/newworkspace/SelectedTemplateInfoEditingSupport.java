package bndtools.wizards.newworkspace;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;

import aQute.bnd.wstemplates.FragmentTemplateEngine.SelectedTemplateInfo;

public class SelectedTemplateInfoEditingSupport extends EditingSupport {

    private final TableViewer viewer;

    public SelectedTemplateInfoEditingSupport(TableViewer viewer) {
        super(viewer);
        this.viewer = viewer;
    }

    @Override
    protected CellEditor getCellEditor(Object element) {
		// return new CheckboxCellEditor(null, SWT.CHECK | SWT.READ_ONLY);
		String[] versions = new String[] {
			"default", "snapshot"
		};
		return new ComboBoxCellEditor(viewer.getTable(), versions);

    }

    @Override
    protected boolean canEdit(Object element) {
        return true;
    }

    @Override
    protected Object getValue(Object element) {
		SelectedTemplateInfo sti = (SelectedTemplateInfo) element;
		return sti.useSnapshot() ? 1 : 0;

    }

    @Override
    protected void setValue(Object element, Object value) {
		if (((Integer) value) == 0) {
			((SelectedTemplateInfo) element).setUseSnapshot(false);
		} else if (((Integer) value) == 1) {
			((SelectedTemplateInfo) element).setUseSnapshot(true);
		}
		viewer.update(element, null);
    }
}