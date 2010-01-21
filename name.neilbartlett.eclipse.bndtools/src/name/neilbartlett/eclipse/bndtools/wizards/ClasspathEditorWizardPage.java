package name.neilbartlett.eclipse.bndtools.wizards;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.utils.FileExtensionFilter;
import name.neilbartlett.eclipse.bndtools.utils.JarPathLabelProvider;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class ClasspathEditorWizardPage extends WizardPage {

	private final WizardNewFileCreationPage newFilePage;
	private Collection<IPath> paths = new LinkedList<IPath>();
	
	private TableViewer viewer;
	private Button btnAdd;
	private Button btnAddExternal;
	private Button btnRemove;

	public ClasspathEditorWizardPage(final String pageName, WizardNewFileCreationPage newFilePage) {
		super(pageName);
		this.newFilePage = newFilePage;
	}

	public void createControl(final Composite parent) {
		setTitle("Wrap JAR Files as Bundles");
		final Composite composite = new Composite(parent, SWT.NONE);
		
		final Table table = new Table(composite, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		viewer = new TableViewer(table);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new JarPathLabelProvider());
		
		btnAdd = new Button(composite, SWT.PUSH);
		btnAdd.setText("Add");
		btnAddExternal = new Button(composite, SWT.PUSH);
		btnAddExternal.setText("Add External");
		btnRemove = new Button(composite, SWT.PUSH);
		btnRemove.setText("Remove");
		
		viewer.setInput(paths);
		update();
	
		// Listeners
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				update();
			}
		});
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IPath containerPath = newFilePage.getContainerFullPath();
				if(containerPath != null) {
					IResource newFile = ResourcesPlugin.getWorkspace().getRoot().findMember(containerPath);
					if(newFile != null) {
						ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getShell(), new WorkbenchLabelProvider(), new WorkbenchContentProvider());
						dialog.setValidator(new ISelectionStatusValidator() {
							public IStatus validate(Object[] selection) {
								if (selection.length > 0 && selection[0] instanceof IFile) {
									return new Status(IStatus.OK, Plugin.PLUGIN_ID, IStatus.OK, "", null); //$NON-NLS-1$
								}
								return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, IStatus.ERROR, "", null); //$NON-NLS-1$
							}
						});
						dialog.setAllowMultiple(true);
						dialog.setTitle("JAR File Selection");
						dialog.setMessage("Select the JAR files to wrap as an OSGi bundle.");
						dialog.addFilter(new FileExtensionFilter("jar")); //$NON-NLS-1$
						dialog.setInput(newFile.getProject());
						
						if(dialog.open() == Window.OK) {
							Object[] files = dialog.getResult();
							List<IPath> added = new ArrayList<IPath>(files.length);
							for (Object file : files) {
								added.add(((IResource) file).getFullPath().makeRelative());
							}
							if(!added.isEmpty()) {
								paths.addAll(added);
								viewer.add(added.toArray(new IPath[added.size()]));
							}
						}
					}
				}
				update();
			}
		});
		btnAddExternal.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(getShell(), SWT.OPEN | SWT.MULTI);
				dialog.setFilterExtensions(new String[] {"*.jar"}); //$NON-NLS-1$
				String res = dialog.open();
				if (res != null) {
					IPath filterPath = new Path(dialog.getFilterPath());
					
					String[] fileNames = dialog.getFileNames();
					List<IPath> added = new ArrayList<IPath>(fileNames.length);
					for (String fileName : fileNames) {
						added.add(filterPath.append(fileName));
					}
					if(!added.isEmpty()) {
						paths.addAll(added);
						viewer.add(added.toArray(new IPath[added.size()]));
					}
				}
				update();
			}
		});
		btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				paths.removeAll(((IStructuredSelection) viewer.getSelection()).toList());
				viewer.remove(((IStructuredSelection) viewer.getSelection()).toArray());
				update();
			}
		});
		
		// Layout
		composite.setLayout(new GridLayout(2, false));
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 4));
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		btnAddExternal.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		setControl(composite);
	}
	@Override
	public boolean isPageComplete() {
		return !paths.isEmpty();
	}
	
	private void update() {
		btnRemove.setEnabled(!viewer.getSelection().isEmpty());
		getContainer().updateButtons();
		getContainer().updateMessage();
	}

	public Collection<IPath> getPaths() {
		return paths;
	}
	
	public void setPaths(Collection<IPath> paths) {
		this.paths = paths;
	}
}
