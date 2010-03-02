package name.neilbartlett.eclipse.bndtools.classpath.ui;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.classpath.ExportedBundle;
import name.neilbartlett.eclipse.bndtools.classpath.WorkspaceRepositoryClasspathContainerInitializer;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class ExportedBundleSelectionDialog extends TitleAreaDialog {

	private TableViewer viewer;
	private ISelection selection;

	public ExportedBundleSelectionDialog(Shell parentShell) {
		super(parentShell);
	}

	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		
		final Text txtFilter = new Text(composite, SWT.BORDER | SWT.SEARCH);
		Table table = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		table.setHeaderVisible(true);
		
		TableColumn col;
		col = new TableColumn(table, SWT.NONE);
		col.setText("Symbolic-Name; Version");
		col.setWidth(250);
		
		col = new TableColumn(table, SWT.NONE);
		col.setText("Location");
		col.setWidth(250);
		
		viewer = new TableViewer(table);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new ExportedBundleLabelProvider());
		viewer.setInput(WorkspaceRepositoryClasspathContainerInitializer.getInstance().getAllWorkspaceExports());
		
		viewer.setSorter(new ViewerSorter() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				ExportedBundle export1 = (ExportedBundle) e1;
				ExportedBundle export2 = (ExportedBundle) e2;
				
				return export1.compareTo(export2);
			}
		});
		
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				selection = viewer.getSelection();
				updateButtons();
			}
		});
		
		txtFilter.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				final String filterStr = txtFilter.getText().trim().toLowerCase();
				if(filterStr.length() > 0) {
					ViewerFilter filter = new ViewerFilter() {
						public boolean select(Viewer viewer, Object parentElement, Object element) {
							ExportedBundle export = (ExportedBundle) element;
							return export.getSymbolicName().toLowerCase().indexOf(filterStr) > -1;
						}
					};
					viewer.setFilters(new ViewerFilter[] { filter });
				} else {
					viewer.setFilters(new ViewerFilter[0]);
				}
			}
		});
		
		// Layout
		GridLayout layout = (GridLayout) composite.getLayout();
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 5;
		layout.marginHeight = 5;
		layout.marginWidth = 5;
		
		txtFilter.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		return composite;
	}
	boolean validate() {
		return selection != null && !selection.isEmpty();
	}
	void updateButtons() {
		boolean valid = validate();
		Button okButton = getButton(IDialogConstants.OK_ID);
		if(okButton != null) {
			okButton.setEnabled(valid);
		}
	}
	@Override
	protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
		Button button = super.createButton(parent, id, label, defaultButton);
		if(id == IDialogConstants.OK_ID) {
			button.setEnabled(validate());
		}
		return button;
	}
	public ISelection getSelection() {
		return selection;
	}
}
class ExportedBundleLabelProvider extends StyledCellLabelProvider {
	
	Image bundleImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/brick.png").createImage();
	
	@Override
	public void update(ViewerCell cell) {
		ExportedBundle export = (ExportedBundle) cell.getElement();
		StyledString styledString = null;
		if(cell.getColumnIndex() == 0) {
			styledString = getStyledBundleId(export);
			//cell.setImage(bundleImg);
		} else if(cell.getColumnIndex() == 1) {
			styledString = getStyledLocation(export);
		}
		
		if(styledString != null) {
			cell.setText(styledString.getString());
			cell.setStyleRanges(styledString.getStyleRanges());
		}
	}
	private StyledString getStyledBundleId(ExportedBundle export) {
		StyledString styledString = new StyledString(export.getSymbolicName());
		styledString.append("; " + export.getVersion(), StyledString.COUNTER_STYLER);
		return styledString;
	}
	private StyledString getStyledLocation(ExportedBundle export) {
		return new StyledString(export.getPath().makeRelative().toString(), StyledString.DECORATIONS_STYLER);
	}
	@Override
	public void dispose() {
		super.dispose();
		bundleImg.dispose();
	}
}
