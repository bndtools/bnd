package bndtools.editor.contents;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.utils.collections.CollectionUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.osgi.Constants;
import bndtools.Plugin;
import bndtools.internal.testcaseselection.ITestCaseFilter;
import bndtools.internal.testcaseselection.JavaSearchScopeTestCaseLister;
import bndtools.internal.testcaseselection.TestCaseSelectionDialog;

public class TestSuitesPart extends SectionPart implements PropertyChangeListener {
	private static final ILogger	logger	= Logger.getLogger(TestSuitesPart.class);

	private BndEditModel			model;
	private List<String>			testSuites;

	private TableViewer				viewer;

	private final Image				imgUp	= AbstractUIPlugin
		.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/arrow_up.png")
		.createImage();
	private final Image				imgDown	= AbstractUIPlugin
		.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/arrow_down.png")
		.createImage();

	public TestSuitesPart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style);

		createSection(getSection(), toolkit);
	}

	private void createSection(Section section, FormToolkit toolkit) {
		section.setText(Messages.TestSuitesPart_section_junit_tests);

		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);

		// Section toolbar buttons
		ToolBar toolbar = new ToolBar(section, SWT.FLAT);
		section.setTextClient(toolbar);

		final ToolItem addItem = new ToolItem(toolbar, SWT.PUSH);
		addItem.setImage(PlatformUI.getWorkbench()
			.getSharedImages()
			.getImage(ISharedImages.IMG_OBJ_ADD));
		addItem.setToolTipText(Messages.TestSuitesPart_add);

		final ToolItem removeItem = new ToolItem(toolbar, SWT.PUSH);
		removeItem.setImage(PlatformUI.getWorkbench()
			.getSharedImages()
			.getImage(ISharedImages.IMG_TOOL_DELETE));
		removeItem.setDisabledImage(PlatformUI.getWorkbench()
			.getSharedImages()
			.getImage(ISharedImages.IMG_TOOL_DELETE_DISABLED));
		removeItem.setToolTipText(Messages.TestSuitesPart_remove);
		removeItem.setEnabled(false);

		Table table = toolkit.createTable(composite, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);

		viewer = new TableViewer(table);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new TestSuiteLabelProvider());

		toolbar = new ToolBar(composite, SWT.FLAT | SWT.HORIZONTAL | SWT.RIGHT);

		final ToolItem btnMoveUp = new ToolItem(toolbar, SWT.PUSH);
		btnMoveUp.setText("Up");
		btnMoveUp.setImage(imgUp);
		btnMoveUp.setEnabled(false);

		final ToolItem btnMoveDown = new ToolItem(toolbar, SWT.PUSH);
		btnMoveDown.setText("Down");
		btnMoveDown.setImage(imgDown);
		btnMoveDown.setEnabled(false);

		// LISTENERS
		viewer.addSelectionChangedListener(event -> {
			ISelection selection = event.getSelection();
			boolean enabled = selection != null && !selection.isEmpty();
			removeItem.setEnabled(enabled);
			btnMoveUp.setEnabled(enabled);
			btnMoveDown.setEnabled(enabled);
			getManagedForm().fireSelectionChanged(TestSuitesPart.this, selection);
		});
		viewer.addOpenListener(event -> {
			String name = (String) ((IStructuredSelection) event.getSelection()).getFirstElement();
			if (name != null)
				doOpenSource(name);
		});
		viewer.addDropSupport(DND.DROP_COPY | DND.DROP_MOVE, new Transfer[] {
			ResourceTransfer.getInstance()
		}, new TestSuiteListDropAdapter());
		addItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doAdd();
			}
		});
		removeItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doRemove();
			}
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

		btnMoveUp.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doMoveUp();
			}
		});
		btnMoveDown.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doMoveDown();
			}
		});

		// Layout
		GridLayout layout;

		layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 0;
		layout.horizontalSpacing = 0;
		composite.setLayout(layout);

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		// gd.widthHint = 75;
		gd.heightHint = 75;
		table.setLayoutData(gd);
		toolbar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

	}

	void doOpenSource(String name) {
		IJavaProject javaProj = getJavaProject();
		if (javaProj != null) {
			try {
				IType type = javaProj.findType(name);
				if (type != null)
					JavaUI.openInEditor(type, true, true);
			} catch (PartInitException e) {
				e.printStackTrace();
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
	}

	void doAdd() {

		// Prepare the exclusion list based on existing test cases
		final Set<String> testSuitesSet = new HashSet<>(testSuites);

		// Create a filter from the exclusion list
		ITestCaseFilter filter = testCaseName -> !testSuitesSet.contains(testCaseName);
		IFormPage page = (IFormPage) getManagedForm().getContainer();
		IWorkbenchWindow window = page.getEditorSite()
			.getWorkbenchWindow();

		// Prepare the package lister from the Java project
		IJavaProject javaProject = getJavaProject();
		if (javaProject == null) {
			MessageDialog.openError(getSection().getShell(), "Error",
				"Cannot add test cases: unable to find a Java project associated with the editor input.");
			return;
		}

		IJavaSearchScope searchScope = SearchEngine.createJavaSearchScope(new IJavaElement[] {
			javaProject
		});
		JavaSearchScopeTestCaseLister testCaseLister = new JavaSearchScopeTestCaseLister(searchScope, window);

		// Create and open the dialog
		TestCaseSelectionDialog dialog = new TestCaseSelectionDialog(getSection().getShell(), testCaseLister, filter,
			Messages.TestSuitesPart_title);
		dialog.setSourceOnly(true);
		dialog.setMultipleSelection(true);
		if (dialog.open() == Window.OK) {
			Object[] results = dialog.getResult();
			List<String> added = new LinkedList<>();

			// Select the results
			for (Object result : results) {
				String newTestSuites = (String) result;
				if (testSuites.add(newTestSuites)) {
					added.add(newTestSuites);
				}
			}

			// Update the model and view
			if (!added.isEmpty()) {
				viewer.add(added.toArray());
				markDirty();
			}
		}
	}

	void doRemove() {
		IStructuredSelection sel = (IStructuredSelection) viewer.getSelection();
		if (!sel.isEmpty()) {
			testSuites.removeAll(sel.toList());
			viewer.remove(sel.toArray());
			markDirty();
			validate();
		}
	}

	void doMoveUp() {
		int[] selectedIndexes = findSelectedIndexes();
		if (CollectionUtils.moveUp(testSuites, selectedIndexes)) {
			viewer.refresh();
			validate();
			markDirty();
		}
	}

	void doMoveDown() {
		int[] selectedIndexes = findSelectedIndexes();
		if (CollectionUtils.moveDown(testSuites, selectedIndexes)) {
			viewer.refresh();
			validate();
			markDirty();
		}
	}

	int[] findSelectedIndexes() {
		Object[] selection = ((IStructuredSelection) viewer.getSelection()).toArray();
		int[] selectionIndexes = new int[selection.length];

		for (int i = 0; i < selection.length; i++) {
			selectionIndexes[i] = testSuites.indexOf(selection[i]);
		}
		return selectionIndexes;
	}

	@Override
	public void initialize(IManagedForm form) {
		super.initialize(form);

		this.model = (BndEditModel) form.getInput();
		this.model.addPropertyChangeListener(Constants.TESTCASES, this);
	}

	@Override
	public void refresh() {
		List<String> modelList = model.getTestSuites();
		testSuites = new ArrayList<>(modelList);
		viewer.setInput(testSuites);
		validate();
	}

	private void validate() {}

	@Override
	public void commit(boolean onSave) {
		try {
			model.removePropertyChangeListener(Constants.TESTCASES, this);
			model.setTestSuites(testSuites.isEmpty() ? null : testSuites);
		} finally {
			model.addPropertyChangeListener(Constants.TESTCASES, this);
			super.commit(onSave);
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		String propertyName = evt.getPropertyName();
		if (Constants.TESTCASES.equals(propertyName)) {
			IFormPage page = (IFormPage) getManagedForm().getContainer();
			if (page.isActive()) {
				refresh();
			} else {
				markStale();
			}
		} else if (Constants.PRIVATE_PACKAGE.equals(propertyName) || Constants.EXPORT_PACKAGE.equals(propertyName)) {
			validate();
		}
	}

	IJavaProject getJavaProject() {
		IFormPage page = (IFormPage) getManagedForm().getContainer();
		IEditorInput input = page.getEditorInput();

		IFile file = ResourceUtil.getFile(input);
		if (file != null) {
			return JavaCore.create(file.getProject());
		}
		return null;
	}

	private class TestSuiteListDropAdapter extends ViewerDropAdapter {

		protected TestSuiteListDropAdapter() {
			super(viewer);
		}

		@Override
		public void dragEnter(DropTargetEvent event) {
			event.detail = DND.DROP_COPY;
			super.dragEnter(event);
		}

		@Override
		public boolean validateDrop(Object target, int operation, TransferData transferType) {
			return ResourceTransfer.getInstance()
				.isSupportedType(transferType);
		}

		@Override
		public boolean performDrop(Object data) {
			Object target = getCurrentTarget();
			int loc = getCurrentLocation();

			int insertionIndex = -1;
			if (target != null) {
				insertionIndex = testSuites.indexOf(target);
				if (insertionIndex > -1 && loc == LOCATION_ON || loc == LOCATION_AFTER)
					insertionIndex++;
			}

			List<String> addedNames = new ArrayList<>();
			if (data instanceof IResource[]) {
				IResource[] resources = (IResource[]) data;
				for (IResource resource : resources) {
					IJavaElement javaElement = JavaCore.create(resource);
					if (javaElement != null) {
						try {
							if (javaElement instanceof IType) {
								IType type = (IType) javaElement;
								if (type.isClass() && Flags.isPublic(type.getFlags())) {
									String typeName = type.getPackageFragment()
										.getElementName() + "." + type.getElementName(); //$NON-NLS-1$
									addedNames.add(typeName);
								}
							} else if (javaElement instanceof ICompilationUnit) {
								IType[] allTypes = ((ICompilationUnit) javaElement).getAllTypes();
								for (IType type : allTypes) {
									if (type.isClass() && Flags.isPublic(type.getFlags())) {
										String typeName = type.getPackageFragment()
											.getElementName() + "." + type.getElementName(); //$NON-NLS-1$
										addedNames.add(typeName);
									}
								}
							}
						} catch (JavaModelException e) {
							logger.logError(Messages.TestSuitesPart_errorJavaType, e);
						}
					}
				}
			}

			if (!addedNames.isEmpty()) {
				if (insertionIndex == -1 || insertionIndex == testSuites.size()) {
					testSuites.addAll(addedNames);
					viewer.add(addedNames.toArray());
				} else {
					testSuites.addAll(insertionIndex, addedNames);
					viewer.refresh();
				}
				viewer.setSelection(new StructuredSelection(addedNames), true);
				validate();
				markDirty();
			}
			return true;
		}
	}
}

class TestSuiteLabelProvider extends StyledCellLabelProvider {
	private final Image suiteImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/tsuite.gif")
		.createImage();

	// private Image testImg =
	// AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID,
	// "/icons/test.gif").createImage();

	@Override
	public void update(ViewerCell cell) {
		String fqName = (String) cell.getElement();
		cell.setText(fqName);
		cell.setImage(suiteImg);
	}

	@Override
	public void dispose() {
		super.dispose();
		suiteImg.dispose();
	}

}
