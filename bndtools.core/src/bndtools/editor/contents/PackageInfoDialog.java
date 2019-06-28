package bndtools.editor.contents;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ICellEditorListener;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.osgi.framework.Version;

public class PackageInfoDialog extends TitleAreaDialog {

	private final List<FileVersionTuple>		packages;
	private final Collection<FileVersionTuple>	selection;

	private boolean								dontAsk	= false;

	private Table								table;
	private CheckboxTableViewer					viewer;

	/**
	 * Create the dialog.
	 *
	 * @param parentShell
	 * @param packages
	 */
	public PackageInfoDialog(Shell parentShell, List<FileVersionTuple> packages) {
		super(parentShell);
		setShellStyle(SWT.BORDER | SWT.CLOSE | SWT.RESIZE);
		this.packages = packages;

		selection = new ArrayList<>(packages.size());
		selection.addAll(packages);
	}

	/**
	 * Create contents of the dialog.
	 *
	 * @param parent
	 */
	@SuppressWarnings("unused")
	@Override
	protected Control createDialogArea(Composite parent) {
		setMessage(Messages.PackageInfoDialog_Message);
		setTitle(Messages.PackageInfoDialog_Title);
		Composite container = (Composite) super.createDialogArea(parent);

		Composite composite = new Composite(container, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 10;
		layout.marginWidth = 10;
		composite.setLayout(layout);

		table = new Table(composite, SWT.BORDER | SWT.CHECK | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		GridData gd_table = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2);
		gd_table.heightHint = 100;
		table.setLayoutData(gd_table);
		table.setLinesVisible(true);

		TableColumn tblclmnPackage = new TableColumn(table, SWT.NONE);
		tblclmnPackage.setWidth(267);
		tblclmnPackage.setText(Messages.PackageInfoDialog_ExportedPackage);

		TableColumn tblclmnVersion = new TableColumn(table, SWT.NONE);
		tblclmnVersion.setWidth(77);
		tblclmnVersion.setText(Messages.PackageInfoDialog_Version);

		Button btnCheckAll = new Button(composite, SWT.NONE);
		btnCheckAll.setText(Messages.PackageInfoDialog_btnCheckAll_text);
		btnCheckAll.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1));
		btnCheckAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selection.clear();
				selection.addAll(packages);
				viewer.setCheckedElements(selection.toArray());
				validate();
			}
		});

		Button btnUncheckAll = new Button(composite, SWT.NONE);
		btnUncheckAll.setText(Messages.PackageInfoDialog_btnUncheckAll_text_1);
		btnUncheckAll.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1));
		btnUncheckAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selection.clear();
				viewer.setCheckedElements(selection.toArray());
				validate();
			}
		});

		final Button btnAlwaysGenerate = new Button(composite, SWT.CHECK);
		btnAlwaysGenerate.setText(Messages.PackageInfoDialog_AlwaysGenerate);

		viewer = new CheckboxTableViewer(table);

		TableViewerColumn tblViewerClmnPackage = new TableViewerColumn(viewer, tblclmnPackage);
		tblViewerClmnPackage.setLabelProvider(new PackageNameLabelProvider(table.getDisplay()));

		TableViewerColumn tblViewerClmnVersion = new TableViewerColumn(viewer, tblclmnVersion);
		tblViewerClmnVersion.setLabelProvider(new PackageVersionLabelProvider());
		tblViewerClmnVersion.setEditingSupport(new VersionEditingSupport(viewer));

		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				FileVersionTuple f1 = (FileVersionTuple) e1;
				FileVersionTuple f2 = (FileVersionTuple) e2;
				return f1.compareTo(f2);
			}
		});
		viewer.setInput(packages);
		viewer.setCheckedElements(selection.toArray());
		new Label(composite, SWT.NONE);

		btnAlwaysGenerate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				dontAsk = btnAlwaysGenerate.getSelection();
			}
		});
		viewer.addCheckStateListener(event -> {
			FileVersionTuple pkg = (FileVersionTuple) event.getElement();

			if (event.getChecked())
				selection.add(pkg);
			else
				selection.remove(pkg);
			validate();
		});

		return container;
	}

	private void validate() {
		String warning = null;

		if (selection.size() < packages.size())
			warning = Messages.PackageInfoDialog_Warning;

		if (warning != null)
			setMessage(warning, IMessageProvider.WARNING);
		else
			setMessage(Messages.PackageInfoDialog_Message);
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(500, 300);
	}

	public boolean isDontAsk() {
		return dontAsk;
	}

	public Collection<FileVersionTuple> getSelectedPackageDirs() {
		return new ArrayList<>(selection);
	}

	private class VersionEditingSupport extends EditingSupport {
		private final CellEditor editor;

		public VersionEditingSupport(TableViewer viewer) {
			super(viewer);

			final CellEditor textEditor = new TextCellEditor(viewer.getTable());
			textEditor.setValidator(value -> {
				String versionStr = String.valueOf(value);
				Version v = Version.emptyVersion;

				try {
					v = Version.parseVersion(String.valueOf(value));
				} catch (IllegalArgumentException iae) {
					return MessageFormat.format(Messages.PackageInfoDialog_VersionInvalid, versionStr);
				}

				if (v == Version.emptyVersion) {
					return Messages.PackageInfoDialog_VersionMissing;
				}

				return null;
			});
			textEditor.addListener(new ICellEditorListener() {
				@Override
				public void editorValueChanged(boolean oldValidState, boolean newValidState) {
					if (newValidState) {
						validate();
					} else {
						setMessage(textEditor.getErrorMessage(), IMessageProvider.ERROR);
					}
				}

				@Override
				public void cancelEditor() {
					validate();
				}

				@Override
				public void applyEditorValue() {
					validate();
				}
			});

			this.editor = textEditor;
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return this.editor;
		}

		@Override
		protected Object getValue(Object element) {
			FileVersionTuple pkg = (FileVersionTuple) element;
			return pkg.getVersion()
				.toString();
		}

		@Override
		protected void setValue(Object element, Object userInputValue) {
			FileVersionTuple pkg = (FileVersionTuple) element;
			pkg.setVersion(new Version(String.valueOf(userInputValue)));
			getViewer().update(element, null);
		}
	}

	private static class PackageNameLabelProvider extends StyledCellLabelProvider {

		private final Image image;

		public PackageNameLabelProvider(Device device) {
			image = Icons.desc("package") //$NON-NLS-1$
				.createImage(device);
		}

		@Override
		public void update(ViewerCell cell) {
			FileVersionTuple pkg = (FileVersionTuple) cell.getElement();

			cell.setImage(image);
			cell.setText(pkg.getName());
		}

		@Override
		public void dispose() {
			super.dispose();
			image.dispose();
		}
	}

	private static class PackageVersionLabelProvider extends StyledCellLabelProvider {

		@Override
		public void update(ViewerCell cell) {
			FileVersionTuple pkg = (FileVersionTuple) cell.getElement();

			StyledString label = new StyledString(pkg.getVersion()
				.toString(), StyledString.COUNTER_STYLER);
			cell.setText(label.getString());
			cell.setStyleRanges(label.getStyleRanges());
		}
	}

	public static class FileVersionTuple implements Comparable<FileVersionTuple> {
		private final String	name;
		private final File		file;
		private Version			version;

		public FileVersionTuple(String pkgName, File file) {
			this.name = pkgName;
			this.file = file;

			setVersion(new Version(1, 0, 0));
		}

		public String getName() {
			return name;
		}

		public File getFile() {
			return file;
		}

		public synchronized Version getVersion() {
			return version;
		}

		public synchronized void setVersion(Version version) {
			this.version = version;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			FileVersionTuple other = (FileVersionTuple) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}

		@Override
		public int compareTo(FileVersionTuple obj) {
			return name.compareTo(obj.name);
		}
	}
}
