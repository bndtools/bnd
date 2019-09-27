package bndtools.wizards.project;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import bndtools.internal.pkgselection.PackageNameLabelProvider;

public class PackageListWizardPage extends WizardPage {

	private boolean						loaded				= false;
	private boolean						programmaticChange	= false;
	private boolean						uiChange			= false;

	private Collection<IPath>			paths;

	private final PropertyChangeSupport	propSupport			= new PropertyChangeSupport(this);
	private final List<IPath>			availablePackages	= new ArrayList<>();
	private final List<IPath>			selectedPackages	= new ArrayList<>();
	private String						projectName			= null;

	private Table						tblAvailable;
	private TableViewer					selectedViewer;
	private Table						tblSelected;
	private TableViewer					availableViewer;
	private Button						btnAdd;
	private Button						btnAddAll;
	private Button						btnRemove;
	private Button						btnRemoveAll;
	private Text						txtProjectName;

	protected PackageListWizardPage(String pageName) {
		super(pageName);
		setTitle("Select Packages");
		setMessage("The selected packages will be included in the bundle and provided as Exports.");
	}

	@Override
	@SuppressWarnings("unused")
	public void createControl(Composite parent) {

		Composite composite = new Composite(parent, SWT.NONE);
		setControl(composite);
		composite.setLayout(new GridLayout(3, false));

		Label lblAvailablePackages = new Label(composite, SWT.NONE);
		lblAvailablePackages.setText("Available Packages:");
		new Label(composite, SWT.NONE);

		Label lblSelectedPackages = new Label(composite, SWT.NONE);
		lblSelectedPackages.setText("Selected Packages:");

		tblAvailable = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION);
		tblAvailable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		availableViewer = new TableViewer(tblAvailable);
		availableViewer.setContentProvider(ArrayContentProvider.getInstance());
		availableViewer.setLabelProvider(new PackageNameLabelProvider());
		availableViewer.addSelectionChangedListener(event -> updateUI());
		availableViewer.addDoubleClickListener(event -> doAddSelection());

		Composite composite_1 = new Composite(composite, SWT.NONE);
		GridLayout gl_composite_1 = new GridLayout(1, false);
		gl_composite_1.marginWidth = 0;
		gl_composite_1.marginHeight = 0;
		composite_1.setLayout(gl_composite_1);

		btnAdd = new Button(composite_1, SWT.NONE);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doAddSelection();
			}
		});
		btnAdd.setText("Add ->");
		btnAdd.setEnabled(false);

		btnAddAll = new Button(composite_1, SWT.NONE);
		btnAddAll.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnAddAll.setText("Add All ->");
		btnAddAll.setEnabled(false);
		btnAddAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doAddAll();
			}
		});

		btnRemove = new Button(composite_1, SWT.NONE);
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doRemoveSelection();
			}
		});

		btnRemove.setText("<- Remove");
		btnRemove.setEnabled(false);

		btnRemoveAll = new Button(composite_1, SWT.NONE);
		btnRemoveAll.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnRemoveAll.setText("<- Remove All");
		btnRemoveAll.setEnabled(false);
		btnRemoveAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doRemoveAll();
			}
		});

		tblSelected = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION);
		tblSelected.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		selectedViewer = new TableViewer(tblSelected);
		selectedViewer.setContentProvider(ArrayContentProvider.getInstance());
		selectedViewer.setLabelProvider(new PackageNameLabelProvider());
		selectedViewer.addSelectionChangedListener(event -> updateUI());
		selectedViewer.addDoubleClickListener(event -> doRemoveSelection());

		Composite composite_2 = new Composite(composite, SWT.NONE);
		composite_2.setLayout(new GridLayout(2, false));
		composite_2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

		Label lblNewLabel = new Label(composite_2, SWT.NONE);
		lblNewLabel.setText("Project Name:");

		txtProjectName = new Text(composite_2, SWT.BORDER);
		txtProjectName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		txtProjectName.setText(projectName != null ? projectName : "");
		txtProjectName.addModifyListener(e -> {
			if (!programmaticChange) {
				try {
					uiChange = true;
					setProjectName(txtProjectName.getText());
				} finally {
					uiChange = false;
				}
			}
		});
	}

	void setJarPaths(Collection<IPath> paths) {
		loaded = false;
		this.paths = paths;
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);

		if (!loaded) {
			try {
				getContainer().run(false, false, monitor -> {
					availablePackages.clear();
					selectedPackages.clear();
					for (IPath path : paths) {
						Jar jar = null;
						try {
							if (path.isAbsolute())
								jar = new Jar(path.toFile());
							else
								jar = new Jar(ResourcesPlugin.getWorkspace()
									.getRoot()
									.getLocation()
									.append(path)
									.toFile());

							Map<String, Map<String, Resource>> dirs = jar.getDirectories();
							for (Entry<String, Map<String, Resource>> entry : dirs.entrySet()) {
								if (entry.getValue() == null)
									continue;

								IPath packagePath = new Path(entry.getKey());
								if (packagePath.segmentCount() < 1)
									continue;
								if ("META-INF".equalsIgnoreCase(packagePath.segment(0)))
									continue;

								availablePackages.add(packagePath);
							}
						} catch (Exception e) {} finally {
							if (jar != null)
								jar.close();
						}
					}
				});
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				loaded = true;
			}
			availableViewer.setInput(availablePackages);
			selectedViewer.setInput(selectedPackages);
		}
		updateUI();
	}

	private void updateUI() {
		btnAdd.setEnabled(!availableViewer.getSelection()
			.isEmpty());
		btnAddAll.setEnabled(!availablePackages.isEmpty());
		btnRemove.setEnabled(!selectedViewer.getSelection()
			.isEmpty());
		btnRemoveAll.setEnabled(!selectedPackages.isEmpty());

		getContainer().updateButtons();
		getContainer().updateMessage();
	}

	@Override
	public boolean isPageComplete() {
		return !selectedPackages.isEmpty() && projectName != null && projectName.length() > 0;
	}

	private String getWarning() {
		String warning = null;
		if (projectName == null || projectName.length() == 0)
			warning = "Project name must be specified.";
		return warning;
	}

	@Override
	public String getMessage() {
		String message = getWarning();
		if (message == null)
			message = "The selected packages will be included in the generated bundle, and exported as API packages.";
		return message;
	}

	@Override
	public int getMessageType() {
		return getWarning() != null ? IMessageProvider.WARNING : IMessageProvider.NONE;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String name) {
		try {
			programmaticChange = true;
			String oldName = this.projectName;

			this.projectName = name;
			if (txtProjectName != null && !txtProjectName.isDisposed()) {
				if (!uiChange)
					txtProjectName.setText(projectName);
				updateUI();
			}
			propSupport.firePropertyChange("projectName", oldName, name);
		} finally {
			programmaticChange = false;
		}
	}

	public List<IPath> getSelectedPackages() {
		return selectedPackages;
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propSupport.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propSupport.removePropertyChangeListener(listener);
	}

	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propSupport.addPropertyChangeListener(propertyName, listener);
	}

	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propSupport.removePropertyChangeListener(propertyName, listener);
	}

	@SuppressWarnings("unchecked")
	void doAddSelection() {
		IStructuredSelection sel = (IStructuredSelection) availableViewer.getSelection();
		availablePackages.removeAll(sel.toList());
		availableViewer.remove(sel.toArray());

		selectedPackages.addAll(sel.toList());
		selectedViewer.add(sel.toArray());
		updateUI();
	}

	void doAddAll() {
		selectedPackages.addAll(availablePackages);
		selectedViewer.add(availablePackages.toArray());

		availablePackages.clear();
		availableViewer.refresh();
		updateUI();
	}

	@SuppressWarnings("unchecked")
	void doRemoveSelection() {
		IStructuredSelection sel = (IStructuredSelection) selectedViewer.getSelection();
		selectedPackages.removeAll(sel.toList());
		selectedViewer.remove(sel.toArray());

		availablePackages.addAll(sel.toList());
		availableViewer.add(sel.toArray());
		updateUI();
	}

	void doRemoveAll() {
		availablePackages.addAll(selectedPackages);
		availableViewer.add(selectedPackages.toArray());

		selectedPackages.clear();
		selectedViewer.refresh();
		updateUI();
	}

}
