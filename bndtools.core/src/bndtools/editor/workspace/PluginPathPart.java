package bndtools.editor.workspace;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.ResourceUtil;

import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.osgi.Constants;
import bndtools.Plugin;
import bndtools.central.Central;

public class PluginPathPart extends SectionPart implements PropertyChangeListener {

	private TableViewer		viewer;
	private BndEditModel	model;
	private List<String>	data;
	private ToolItem		removeItem;

	/**
	 * Create the SectionPart.
	 *
	 * @param parent
	 * @param toolkit
	 * @param style
	 */
	public PluginPathPart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style);
		createClient(getSection(), toolkit);
	}

	/**
	 * Fill the section.
	 */
	private void createClient(Section section, FormToolkit toolkit) {
		section.setText("Plugin Path");

		createToolBar(section);

		Table table = new Table(section, SWT.BORDER | SWT.FULL_SELECTION);
		toolkit.adapt(table);
		toolkit.paintBordersFor(table);
		section.setClient(table);

		viewer = new TableViewer(table);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setLabelProvider(new PluginPathLabelProvider());

		viewer.addSelectionChangedListener(event -> {
			boolean enable = !viewer.getSelection()
				.isEmpty();
			removeItem.setEnabled(enable);
		});
		table.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.character == SWT.DEL) {
					doRemove();
				} else if (e.character == '+') {
					doAdd();
				}
			}
		});

	}

	private void createToolBar(Section section) {
		ToolBar toolbar = new ToolBar(section, SWT.FLAT);
		section.setTextClient(toolbar);

		ToolItem addItem = new ToolItem(toolbar, SWT.PUSH);
		addItem.setImage(PlatformUI.getWorkbench()
			.getSharedImages()
			.getImage(ISharedImages.IMG_OBJ_ADD));
		addItem.setToolTipText("Add Path");

		addItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doAdd();
			}
		});

		removeItem = new ToolItem(toolbar, SWT.PUSH);
		removeItem.setImage(PlatformUI.getWorkbench()
			.getSharedImages()
			.getImage(ISharedImages.IMG_TOOL_DELETE));
		removeItem.setDisabledImage(PlatformUI.getWorkbench()
			.getSharedImages()
			.getImage(ISharedImages.IMG_TOOL_DELETE_DISABLED));
		removeItem.setToolTipText("Remove");
		removeItem.setEnabled(false);

		removeItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doRemove();
			}
		});
	}

	@Override
	public void initialize(IManagedForm form) {
		super.initialize(form);

		model = (BndEditModel) form.getInput();
		model.addPropertyChangeListener(Constants.PLUGINPATH, this);
	}

	@Override
	public void dispose() {
		super.dispose();
		if (model != null)
			model.removePropertyChangeListener(Constants.PLUGINPATH, this);
	}

	@Override
	public void refresh() {
		List<String> modelData = model.getPluginPath();
		if (modelData != null)
			this.data = new ArrayList<>(modelData);
		else
			this.data = new LinkedList<>();
		viewer.setInput(this.data);
		super.refresh();
	}

	@Override
	public void commit(boolean onSave) {
		super.commit(onSave);
		model.setPluginPath(data);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		IFormPage page = (IFormPage) getManagedForm().getContainer();
		if (page.isActive())
			refresh();
		else
			markStale();
	}

	IFile getEditorFile() {
		IFormPage page = (IFormPage) getManagedForm().getContainer();
		IFile file = ResourceUtil.getFile(page.getEditorInput());
		return file;
	}

	void doAdd() {
		FileDialog dialog = new FileDialog(getManagedForm().getForm()
			.getShell(), SWT.OPEN | SWT.MULTI);
		try {
			File wsdir = Central.getWorkspace()
				.getBase();
			File cnfdir = new File(wsdir, Workspace.CNFDIR);
			dialog.setFilterPath(cnfdir.getAbsolutePath());
			dialog.setFilterExtensions(new String[] {
				"*.jar" //$NON-NLS-1$
			});

			String res = dialog.open();
			if (res != null) {
				File baseDir = new File(dialog.getFilterPath());
				String[] fileNames = dialog.getFileNames();
				if (fileNames != null && fileNames.length > 0) {
					for (String fileName : fileNames) {
						File file = new File(fileName);
						if (!file.isAbsolute())
							file = new File(baseDir, fileName);
						String addingPath = makeWorkspaceRelative(wsdir, file);
						data.add(addingPath);
						viewer.add(addingPath);
					}
					markDirty();
				}
			}
		} catch (Exception e) {
			ErrorDialog.openError(getSection().getShell(), "Error", null,
				new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error adding plugin path.", e));
			return;
		}

	}

	private static String makeWorkspaceRelative(File wsdir, File file) throws IOException {
		String wspath = wsdir.getCanonicalPath();
		String path = file.getCanonicalPath();

		if (path.startsWith(wspath))
			path = "${workspace}" + path.substring(wspath.length());

		return path;
	}

	void doRemove() {
		IStructuredSelection sel = (IStructuredSelection) viewer.getSelection();

		viewer.remove(sel.toArray());
		data.removeAll(sel.toList());

		if (!sel.isEmpty())
			markDirty();
	}

	private static final class PluginPathLabelProvider extends StyledCellLabelProvider {

		private final Image jarImg = Icons.desc("jar")
			.createImage();

		@Override
		public void update(ViewerCell cell) {
			String path = (String) cell.getElement();

			cell.setText(path);
			cell.setImage(jarImg);
		}

		@Override
		public void dispose() {
			super.dispose();
			jarImg.dispose();
		}
	}

}
