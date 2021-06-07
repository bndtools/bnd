package bndtools.jareditor.internal;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.FileEditorInput;

import aQute.lib.io.IO;

public class JARTreePart extends AbstractFormPart {

	final static IWorkbench			wb			= PlatformUI.getWorkbench();
	protected final IManagedForm	managedForm;

	private final Tree				tree;
	private final TreeViewer		viewer;
	private List<File>				tempFolders	= new ArrayList<>();

	public JARTreePart(Composite parent, final IManagedForm managedForm) {
		this.managedForm = managedForm;

		FormToolkit toolkit = managedForm.getToolkit();
		Section section = toolkit.createSection(parent, ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED);

		section.setText("Content Tree");
		tree = toolkit.createTree(section, SWT.FULL_SELECTION | SWT.SINGLE);
		tree.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TREE_BORDER);

		section.setClient(tree);
		toolkit.paintBordersFor(section);

		viewer = new TreeViewer(tree);
		viewer.setContentProvider(new JARTreeContentProvider());
		viewer.setLabelProvider(new WorkbenchLabelProvider());

		Transfer[] transfers = new Transfer[] {
			FileTransfer.getInstance()
		};

		viewer.addDragSupport(DND.DROP_COPY, transfers, new DragSourceListener() {

			@Override
			public void dragStart(DragSourceEvent event) {
				ITreeSelection s = viewer.getStructuredSelection();
				event.doit = s != null && s.size() > 0;
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				if (FileTransfer.getInstance()
					.isSupportedType(event.dataType)) {
					ITreeSelection s = viewer.getStructuredSelection();
					List<String> errors = new ArrayList<>();
					List<String> files = new ArrayList<>();

					for (TreePath p : s.getPaths())
						try {

							if (p.getSegmentCount() == 0)
								continue;

							Object lastSegment = p.getLastSegment();
							if (!(lastSegment instanceof IFile))
								continue;

							IFile r = (IFile) lastSegment;
							File tempDirectory = Files.createTempDirectory(r.getParent()
								.getName())
								.toFile();
							tempFolders.add(tempDirectory);
							File tempFile = new File(tempDirectory, r.getName());
							IO.copy(r.getContents(), tempFile);
							files.add(tempFile.getAbsolutePath());

						} catch (Exception e) {
							errors.add(p + " : " + e.getMessage());
						}
					event.data = files.toArray(new String[files.size()]);

					if (!errors.isEmpty()) {
						JAREditor.error(null, "Some errors occurred during the reading of the resources > %s", errors);
					}
				}
			}

			@Override
			public void dragFinished(DragSourceEvent event) {}
		});

		managedForm.addPart(this);
		viewer.addSelectionChangedListener(
			event -> JARTreePart.this.managedForm.fireSelectionChanged(JARTreePart.this, event.getSelection()));
		viewer.addDoubleClickListener(event -> {
			StructuredSelection selection = (StructuredSelection) event.getSelection();

			IResource node = (IResource) selection.getFirstElement();
			if (node instanceof IFile) {
				IFile r = (IFile) node;
				try {
					IWorkbenchWindow window = wb.getActiveWorkbenchWindow();
					if (window != null) {
						IWorkbenchPage page = window.getActivePage();
						if (page != null) {
							IEditorDescriptor d = IDE.getEditorDescriptor(r.getName(), true, true);
							FileEditorInput fe = new FileEditorInput(r);
							IEditorPart openEditor = page.openEditor(fe, d.getId(), true);
						}
					}
				} catch (Exception e) {
					JAREditor.error(e, "Failed to start the editor for %s ", r);
				}

			} else {
				viewer.setExpandedState(node, !viewer.getExpandedState(node));
			}
		});

		parent.setLayout(new GridLayout());
		section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}

	@Override
	public boolean setFormInput(Object input) {
		String[] selectedPath = getSelectedPath();
		if (selectedPath == null)
			selectedPath = new String[] {
				"META-INF", "MANIFEST.MF"
			};

		viewer.setInput(input);
		setSelectedPath(selectedPath);
		return false;
	}

	@Override
	public void dispose() {
		tempFolders.forEach(IO::delete);
		super.dispose();
	}

	private TreePath toInstances(IContainer container, String[] path) {
		IResource[] instances = new IResource[path.length];
		IResource rover = container;
		for (int i = 0; i < path.length; i++) {

			if (!(rover instanceof IContainer))
				return null;

			rover = ((IContainer) rover).findMember(path[i]);
			if (rover == null)
				return null;

			instances[i] = rover;
		}
		return new TreePath(instances);
	}

	private String[] fromInstances(TreePath instances) {
		if (instances == null)
			return null;

		String[] path = new String[instances.getSegmentCount()];
		for (int i = 0; i < path.length; i++) {
			IResource r = (IResource) instances.getSegment(i);
			if (r == null)
				return null;

			path[i] = r.getName();
		}
		return path;
	}

	void setSelectedPath(String[] path) {
		IContainer input = (IContainer) viewer.getInput();
		if (input != null) {

			TreePath treePath = toInstances(input, path);
			if (treePath != null) {
				viewer.setSelection(new TreeSelection(treePath), true);
				return;
			}
		}
		viewer.setSelection(TreeSelection.EMPTY);
	}

	String[] getSelectedPath() {
		String[] result;
		if (viewer.getSelection()
			.isEmpty()) {
			result = null;
		} else {
			TreeSelection selection = (TreeSelection) viewer.getSelection();
			TreePath treePath = selection.getPaths()[0];
			result = fromInstances(treePath);
		}
		return result;
	}

	private static class JARTreeContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getChildren(final Object parentElement) {
			try {
				IContainer parentNode = (IContainer) parentElement;
				return parentNode.members();
			} catch (CoreException e) {
				return new Object[0];
			}
		}

		@Override
		public Object getParent(final Object element) {
			return ((IResource) element).getParent();
		}

		@Override
		public boolean hasChildren(final Object element) {
			return element instanceof IContainer;
		}

		@Override
		public Object[] getElements(final Object inputElement) {
			return getChildren(inputElement);
		}
	}

}
