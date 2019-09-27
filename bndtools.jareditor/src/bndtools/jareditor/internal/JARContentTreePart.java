package bndtools.jareditor.internal;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class JARContentTreePart extends AbstractFormPart {

	protected final IManagedForm			managedForm;

	private final Tree						tree;
	private final TreeViewer				viewer;
	private final JARTreeContentProvider	contentProvider	= new JARTreeContentProvider();

	private String[]						selectedPath	= null;

	public JARContentTreePart(final Composite parent, final IManagedForm managedForm) {
		this.managedForm = managedForm;

		FormToolkit toolkit = managedForm.getToolkit();
		Section section = toolkit.createSection(parent, ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED);

		section.setText("Content Tree");
		tree = toolkit.createTree(section, SWT.FULL_SELECTION | SWT.SINGLE);
		tree.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TREE_BORDER);
		section.setClient(tree);
		toolkit.paintBordersFor(section);

		viewer = new TreeViewer(tree);
		viewer.setContentProvider(contentProvider);
		viewer.setLabelProvider(new JARTreeLabelProvider());

		managedForm.addPart(this);
		viewer.addSelectionChangedListener(event -> JARContentTreePart.this.managedForm
			.fireSelectionChanged(JARContentTreePart.this, event.getSelection()));
		viewer.addDoubleClickListener(event -> {
			StructuredSelection selection = (StructuredSelection) event.getSelection();

			ZipTreeNode node = (ZipTreeNode) selection.getFirstElement();

			if (node.hasChildren()) {
				viewer.setExpandedState(node, !viewer.getExpandedState(node));
			}
		});

		parent.setLayout(new GridLayout());
		section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}

	@Override
	public void initialize(final IManagedForm form) {
		super.initialize(form);
	}

	@Override
	public boolean isStale() {
		// Claim to always be stale, so we always get refresh events.
		return true;
	}

	@Override
	public void refresh() {
		super.refresh();
		Object input = getManagedForm().getInput();
		viewer.setInput(input);
		refreshSelectedPath();
	}

	private void refreshSelectedPath() {
		if (selectedPath != null) {
			TreePath treePath = contentProvider.findPath(selectedPath);
			if (treePath != null) {
				viewer.setSelection(new TreeSelection(treePath), true);
			} else {
				viewer.setSelection(TreeSelection.EMPTY);
			}
		}
	}

	@Override
	public boolean setFormInput(final Object input) {
		viewer.setInput(input);
		return false;
	}

	void setSelectedPath(final String[] path) {
		selectedPath = path;
		if ((viewer != null) && (viewer.getInput() != null)) {
			refreshSelectedPath();
		}
	}

	String[] getSelectedPath() {
		String[] result;
		if (viewer.getSelection()
			.isEmpty()) {
			result = null;
		} else {
			TreeSelection selection = (TreeSelection) viewer.getSelection();
			TreePath treePath = selection.getPaths()[0];
			result = new String[treePath.getSegmentCount()];
			for (int i = 0; i < result.length; i++) {
				result[i] = treePath.getSegment(i)
					.toString();
			}
		}
		return result;
	}

	private static class JARTreeLabelProvider extends StyledCellLabelProvider {

		private final Image	folderImg	= AbstractUIPlugin
			.imageDescriptorFromPlugin(PluginConstants.PLUGIN_ID, "/icons/fldr_obj.gif")
			.createImage();
		private final Image	fileImg		= AbstractUIPlugin
			.imageDescriptorFromPlugin(PluginConstants.PLUGIN_ID, "/icons/file_obj.gif")
			.createImage();

		public JARTreeLabelProvider() {
			super();
		}

		@Override
		public void update(final ViewerCell cell) {
			ZipTreeNode node = (ZipTreeNode) cell.getElement();

			String name = node.toString();

			StyledString label = new StyledString(name);

			if (name.endsWith("/")) {
				cell.setImage(folderImg);
			} else {
				cell.setImage(fileImg);
				ZipEntry entry = node.getZipEntry();
				if (entry != null) {
					label.append(String.format(" [sz: %,d; crc: %d]", entry.getSize(), entry.getCrc()),
						StyledString.QUALIFIER_STYLER);
				}
			}

			cell.setText(label.getString());
			cell.setStyleRanges(label.getStyleRanges());
		}

		@Override
		public void dispose() {
			super.dispose();
			folderImg.dispose();
			fileImg.dispose();
		}
	}

	private class JARTreeContentProvider implements ITreeContentProvider {

		Map<String, ZipTreeNode> entryMap;

		public JARTreeContentProvider() {
			super();
		}

		@Override
		public Object[] getChildren(final Object parentElement) {
			ZipTreeNode parentNode = (ZipTreeNode) parentElement;
			return parentNode.getChildren()
				.toArray();
		}

		@Override
		public Object getParent(final Object element) {
			return ((ZipTreeNode) element).getParent();
		}

		@Override
		public boolean hasChildren(final Object element) {
			return ((ZipTreeNode) element).hasChildren();
		}

		@Override
		public Object[] getElements(final Object inputElement) {
			return entryMap.values()
				.toArray();
		}

		@Override
		public void dispose() {}

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
			entryMap = new TreeMap<>();
			final URI uri = URIHelper.retrieveFileURI((IEditorInput) newInput);
			if (uri != null) {
				try (ZipInputStream zis = new ZipInputStream(uri.toURL()
					.openStream())) {
					ZipEntry entry;
					while ((entry = zis.getNextEntry()) != null) {
						ZipTreeNode.addEntry(entryMap, entry);
					}
				} catch (IOException e) {
					Status status = new Status(IStatus.ERROR, PluginConstants.PLUGIN_ID, 0,
						"I/O error reading JAR file contents", e);
					ErrorDialog.openError(managedForm.getForm()
						.getShell(), "Error", null, status);
				}
			}
		}

		public TreePath findPath(final String[] path) {
			if ((path == null) || (path.length == 0)) {
				return null;
			}

			TreePath result = TreePath.EMPTY;
			ZipTreeNode current = entryMap.get(path[0]);
			if (current == null) {
				return null;
			}
			result = result.createChildPath(current);

			segments: for (int i = 1; i < path.length; i++) {
				Collection<ZipTreeNode> children = current.getChildren();
				for (ZipTreeNode child : children) {
					if (path[i].equals(child.toString())) {
						current = child;
						result = result.createChildPath(child);
						continue segments;
					}
				}
				return null;
			}

			return result;
		}
	}
}
