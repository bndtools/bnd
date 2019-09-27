package bndtools.editor.contents;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.ResourceTransfer;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.osgi.Constants;
import bndtools.editor.common.PackageDropAdapter;
import bndtools.editor.common.PrivatePackageTableLabelProvider;
import bndtools.internal.pkgselection.IPackageFilter;
import bndtools.internal.pkgselection.JavaSearchScopePackageLister;
import bndtools.internal.pkgselection.PackageSelectionDialog;

public class PrivatePackagesPart extends SectionPart implements PropertyChangeListener {

	private BndEditModel	model;
	private List<String>	packages	= new ArrayList<>();

	private Table			table;
	private TableViewer		viewer;
	private IManagedForm	managedForm;

	public PrivatePackagesPart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style);
		createSection(getSection(), toolkit);
	}

	void createSection(Section section, FormToolkit toolkit) {
		section.setText("Private Packages");
		section.setDescription("The listed packages will be included in the bundle but not exported.");

		ToolBar toolbar = new ToolBar(section, SWT.FLAT);
		section.setTextClient(toolbar);
		final ToolItem addItem = new ToolItem(toolbar, SWT.PUSH);
		addItem.setImage(PlatformUI.getWorkbench()
			.getSharedImages()
			.getImage(ISharedImages.IMG_OBJ_ADD));
		addItem.setToolTipText("Add");

		final ToolItem removeItem = new ToolItem(toolbar, SWT.PUSH);
		removeItem.setImage(PlatformUI.getWorkbench()
			.getSharedImages()
			.getImage(ISharedImages.IMG_TOOL_DELETE));
		removeItem.setDisabledImage(PlatformUI.getWorkbench()
			.getSharedImages()
			.getImage(ISharedImages.IMG_TOOL_DELETE_DISABLED));
		removeItem.setToolTipText("Remove");
		removeItem.setEnabled(false);

		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);

		table = toolkit.createTable(composite, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);

		viewer = new TableViewer(table);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new PrivatePackageTableLabelProvider());

		// Listeners
		viewer.addSelectionChangedListener(event -> {
			managedForm.fireSelectionChanged(PrivatePackagesPart.this, event.getSelection());
			removeItem.setEnabled(!viewer.getSelection()
				.isEmpty());
		});
		viewer.addDropSupport(DND.DROP_COPY | DND.DROP_MOVE, new Transfer[] {
			LocalSelectionTransfer.getTransfer(), TextTransfer.getInstance(), ResourceTransfer.getInstance()
		}, new PackageDropAdapter<String>(viewer) {
			@Override
			protected String createNewEntry(String packageName) {
				return packageName;
			}

			@Override
			protected void addRows(int index, Collection<String> rows) {
				if (rows.isEmpty())
					return; // skip marking dirty
				if (index == -1) {
					packages.addAll(rows);
					viewer.add(rows.toArray());
				} else {
					packages.addAll(index, rows);
					viewer.refresh();
				}
				viewer.setSelection(new StructuredSelection(rows));
				markDirty();
			}

			@Override
			protected int indexOf(Object object) {
				return 0;
			}
		});
		table.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.character == SWT.DEL) {
					doRemovePackages();
				} else if (e.character == '+') {
					doAddPackages();
				}
			}
		});
		addItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doAddPackages();
			}
		});
		removeItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doRemovePackages();
			}
		});

		// Layout
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);

		GridData gd;

		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 75;
		gd.widthHint = 75;
		table.setLayoutData(gd);
	}

	private void doAddPackages() {
		// Prepare the exclusion list based on existing private packages
		final Set<String> packageNameSet = new HashSet<>(packages);

		// Create a filter from the exclusion list and packages matching
		// "java.*", which must not be included in a bundle
		IPackageFilter filter = packageName -> !packageName.equals("java") && !packageName.startsWith("java.")
			&& !packageNameSet.contains(packageName);
		IFormPage page = (IFormPage) getManagedForm().getContainer();
		IWorkbenchWindow window = page.getEditorSite()
			.getWorkbenchWindow();

		// Prepare the package lister from the Java project
		IJavaProject javaProject = getJavaProject();
		if (javaProject == null) {
			MessageDialog.openError(getSection().getShell(), "Error",
				"Cannot add packages: unable to find a Java project associated with the editor input.");
			return;
		}
		IJavaSearchScope searchScope = SearchEngine.createJavaSearchScope(new IJavaElement[] {
			javaProject
		});
		JavaSearchScopePackageLister packageLister = new JavaSearchScopePackageLister(searchScope, window);

		// Create and open the dialog
		PackageSelectionDialog dialog = new PackageSelectionDialog(getSection().getShell(), packageLister, filter,
			"Select new packages to include in the bundle.");
		dialog.setSourceOnly(true);
		dialog.setMultipleSelection(true);
		if (dialog.open() == Window.OK) {
			Object[] results = dialog.getResult();
			List<String> added = new LinkedList<>();

			// Select the results
			for (Object result : results) {
				String newPackageName = (String) result;
				if (packages.add(newPackageName)) {
					added.add(newPackageName);
				}
			}

			// Update the model and view
			if (!added.isEmpty()) {
				viewer.add(added.toArray());
				markDirty();
			}
		}
	}

	private void doRemovePackages() {
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		if (!selection.isEmpty()) {
			Iterator<?> elements = selection.iterator();
			List<Object> removed = new LinkedList<>();
			while (elements.hasNext()) {
				Object pkg = elements.next();
				if (packages.remove(pkg))
					removed.add(pkg);
			}

			if (!removed.isEmpty()) {
				viewer.remove(removed.toArray());
				markDirty();
			}
		}
	}

	@Override
	public void commit(boolean onSave) {
		super.commit(onSave);
		model.setPrivatePackages(packages);
	}

	@Override
	public void refresh() {
		List<String> tmp = model.getPrivatePackages();
		if (tmp != null)
			packages = new ArrayList<>(tmp);
		else
			packages = new ArrayList<>();
		viewer.setInput(packages);
		super.refresh();
	}

	@Override
	public void initialize(IManagedForm form) {
		super.initialize(form);
		this.managedForm = form;
		model = (BndEditModel) form.getInput();
		model.addPropertyChangeListener(Constants.PRIVATE_PACKAGE, this);
	}

	@Override
	public void dispose() {
		super.dispose();
		if (this.model != null)
			this.model.removePropertyChangeListener(this);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		IFormPage page = (IFormPage) getManagedForm().getContainer();
		if (page.isActive()) {
			refresh();
		} else {
			markStale();
		}
	}

	private IJavaProject getJavaProject() {
		IFormPage page = (IFormPage) getManagedForm().getContainer();
		IEditorInput input = page.getEditorInput();
		if (!IFileEditorInput.class.isInstance(input)) {
			return null;
		}
		IProject project = ((IFileEditorInput) input).getFile()
			.getProject();
		return JavaCore.create(project);
	}

	public ISelectionProvider getSelectionProvider() {
		return viewer;
	}
}
