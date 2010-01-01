package name.neilbartlett.eclipse.bndtools.editor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import name.neilbartlett.eclipse.bndtools.editor.ExportedPackageSelectionDialog.ExportVersionPolicy;
import name.neilbartlett.eclipse.bndtools.internal.pkgselection.IPackageFilter;
import name.neilbartlett.eclipse.bndtools.internal.pkgselection.JavaSearchScopePackageLister;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import aQute.lib.osgi.Constants;

public class ExportedPackagesPart extends SectionPart implements PropertyChangeListener {

	private static final String VERSION_LINK_BUNDLE_VERSION = "${" + Constants.BUNDLE_VERSION + "}";

	private BndEditModel model;
	
	private Table table;
	private TableViewer viewer;
	private Button btnAdd;
	private Button btnRemove;

	public ExportedPackagesPart(Composite parent, FormToolkit toolkit) {
		super(parent, toolkit, ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED | Section.DESCRIPTION);
		createSection(getSection(), toolkit);
	}

	private void createSection(Section section, FormToolkit toolkit) {
		section.setText("Exported Packages");
		section.setDescription("The listed packages will be included in the bundle and exported.");
		
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		
		table = toolkit.createTable(composite, SWT.FULL_SELECTION | SWT.MULTI);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		TableColumn col;
		col = new TableColumn(table, SWT.NONE);
		col.setText("Package");
		col.setWidth(200);
		
		col = new TableColumn(table, SWT.NONE);
		col.setText("Version");
		col.setWidth(100);
		
		viewer = new TableViewer(table);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new ExportedPackageTableLabelProvider());
		
		btnAdd = toolkit.createButton(composite, "Add", SWT.PUSH);
		btnRemove = toolkit.createButton(composite, "Remove", SWT.PUSH);
		btnRemove.setEnabled(false);
		
		// Listeners
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				btnRemove.setEnabled(!viewer.getSelection().isEmpty());
			}
		});
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doAddPackages();
			}
		});
		btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doRemovePackages();
			}
		});
		
		// Layout
		GridLayout layout = new GridLayout(2, false);
		composite.setLayout(layout);
		
		GridData gd;
		
		gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 3);
		gd.widthHint = 300;
		gd.heightHint = 100;
		table.setLayoutData(gd);
		
		btnAdd.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		btnRemove.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}
	
	private void doAddPackages() {
		// Prepare the exclusion list based on existing exported packages
		Collection<ExportedPackage> packages = model.getExportedPackages();
		final Set<String> excludePkgNames = new HashSet<String>(packages.size());
		for (ExportedPackage pkg : packages) {
			excludePkgNames.add(pkg.getPackageName());
		}
		
		// Create a filter from the exclusion list and packages matching "java.*", which must not be exported.
		IPackageFilter filter = new IPackageFilter() {
			public boolean select(String packageName) {
				return !packageName.equals("java") && !packageName.startsWith("java.") && !excludePkgNames.contains(packageName);
			}
		};
		IFormPage page = (IFormPage) getManagedForm().getContainer();
		IWorkbenchWindow window = page.getEditorSite().getWorkbenchWindow();
		
		// Prepare the package lister from the Java project
		IJavaProject javaProject = getJavaProject();
		if(javaProject == null) {
			MessageDialog.openError(btnAdd.getShell(), "Error", "Cannot add packages: unable to find a Java project associated with the editor input.");
			return;
		}
		IJavaSearchScope searchScope = SearchEngine.createJavaSearchScope(new IJavaElement[] { javaProject });
		JavaSearchScopePackageLister packageLister = new JavaSearchScopePackageLister(searchScope, window);
		
		
		// Create and open the dialog
		ExportedPackageSelectionDialog dialog = new ExportedPackageSelectionDialog(btnAdd.getShell(), packageLister, filter, "Select new packages to export from the bundle.");
		dialog.setSourceOnly(true);
		dialog.setMultipleSelection(true);
		if(dialog.open() == Window.OK) {
			Object[] results = dialog.getResult();
			List<ExportedPackage> added = new LinkedList<ExportedPackage>();
			
			// Get the version string
			ExportVersionPolicy versionPolicy = dialog.getExportVersionPolicy();
			String version = null;
			if(versionPolicy == ExportVersionPolicy.linkWithBundle) {
				version = VERSION_LINK_BUNDLE_VERSION;
			} else if(versionPolicy == ExportVersionPolicy.specified) {
				version = dialog.getSpecifiedVersion();
			}
			
			// Select the results
			for (Object result : results) {
				String newPackageName = (String) result;
				ExportedPackage newPackage = new ExportedPackage(newPackageName, version);
				if(packages.add(newPackage)) {
					added.add(newPackage);
				}
			}
			
			// Update the model and view
			if(!added.isEmpty()) {
				model.setExportedPackages(packages);
				markDirty();
			}
		}
	}
	
	private void doRemovePackages() {
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		if(!selection.isEmpty()) {
			Collection<ExportedPackage> packages = model.getExportedPackages();
			
			@SuppressWarnings("unchecked")
			Iterator elements = selection.iterator();
			boolean changed = false;
			while(elements.hasNext()) {
				Object pkg = elements.next();
				if(packages.remove(pkg))
					changed = true;
			}
			
			if(changed) {
				model.setExportedPackages(packages);
				markDirty();
			}
		}
	}
	
	@Override
	public void refresh() {
		viewer.setInput(model.getExportedPackages());
		super.refresh();
	}
	
	@Override
	public void initialize(IManagedForm form) {
		super.initialize(form);
		
		model = (BndEditModel) form.getInput();
		model.addPropertyChangeListener(Constants.EXPORT_PACKAGE, this);
	}
	
	@Override
	public void dispose() {
		super.dispose();
		if(this.model != null)
			this.model.removePropertyChangeListener(this);
	}

	public void propertyChange(PropertyChangeEvent evt) {
		markStale();
	}
	
	private IJavaProject getJavaProject() {
		IFormPage page = (IFormPage) getManagedForm().getContainer();
		IEditorInput input = page.getEditorInput();
		if(!IFileEditorInput.class.isInstance(input)) {
			return null;
		}
		IProject project = ((IFileEditorInput) input).getFile().getProject();
		return JavaCore.create(project);
	}
}
