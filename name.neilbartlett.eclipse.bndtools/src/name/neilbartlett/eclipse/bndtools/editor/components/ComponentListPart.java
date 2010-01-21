package name.neilbartlett.eclipse.bndtools.editor.components;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import name.neilbartlett.eclipse.bndtools.editor.model.BndEditModel;
import name.neilbartlett.eclipse.bndtools.editor.model.ExportedPackage;
import name.neilbartlett.eclipse.bndtools.editor.model.ServiceComponent;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.ResourceUtil;

import aQute.lib.osgi.Constants;

public class ComponentListPart extends SectionPart implements PropertyChangeListener {

	private List<ServiceComponent> componentList;
	
	private IManagedForm managedForm;
	private Table table;
	private TableViewer viewer;
	private BndEditModel model;

	
	public ComponentListPart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style);
		createSection(getSection(), toolkit);
	}
	void createSection(Section section, FormToolkit toolkit) {
		section.setText("Service Components");
		
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		
		table = toolkit.createTable(composite, SWT.MULTI | SWT.FULL_SELECTION);
		viewer = new TableViewer(table);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new ServiceComponentLabelProvider());
		
		final Button btnAdd = toolkit.createButton(composite, "Add", SWT.PUSH);
		final Button btnRemove = toolkit.createButton(composite, "Remove", SWT.PUSH);
		
		// Listeners
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				managedForm.fireSelectionChanged(ComponentListPart.this, event.getSelection());
				btnRemove.setEnabled(!event.getSelection().isEmpty());
			}
		});
		viewer.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				ServiceComponent component = (ServiceComponent) ((IStructuredSelection) event.getSelection()).getFirstElement();
				if(component != null) {
					doOpenComponent(component);
				}
			}
		});
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doAdd();
			}
		});
		btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doRemove();
			}
		});
		
		// Layout
		GridData gd;
		section.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setLayout(new GridLayout(2, false));
		gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 3);
		gd.widthHint = 250;
		table.setLayoutData(gd);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
	}
	void doAdd() {
		ServiceComponent component = new ServiceComponent("", new HashMap<String, String>());
		componentList.add(component);
		viewer.add(component);
		viewer.setSelection(new StructuredSelection(component), true);
		checkComponentPackagesIncluded();
		markDirty();
	}
	void doRemove() {
		@SuppressWarnings("unchecked") Iterator iter = ((IStructuredSelection) viewer.getSelection()).iterator();
		while(iter.hasNext()) {
			Object item = iter.next();
			componentList.remove(item);
			viewer.remove(item);
		}
		checkComponentPackagesIncluded();
		markDirty();
	}
	void doOpenComponent(ServiceComponent component) {
		if(component.isPath()) {
			IFormPage page = (IFormPage) getManagedForm().getContainer();
			IResource resource = ResourceUtil.getResource(page.getEditorInput());
			IProject project = resource.getProject();
			IResource member = project.findMember(component.getName());
			if(member != null && member.getType() == IResource.FILE) {
				try {
					IDE.openEditor(page.getEditorSite().getPage(), (IFile) member);
				} catch (PartInitException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} else if(!component.getName().endsWith("*")) {
			try {
				IType type = getJavaProject().findType(component.getName());
				if(type != null) {
					JavaUI.openInEditor(type, true, true);
				}
			} catch (JavaModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (PartInitException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	void checkComponentPackagesIncluded() {
		IMessageManager msgs = getManagedForm().getMessageManager();
		msgs.setDecorationPosition(SWT.TOP | SWT.RIGHT);
		msgs.removeMessages();
		
		int index = 0;
		
		if(componentList != null) {
			for (ServiceComponent component : componentList) {
				if(component.getName().length() > 0 && !component.isPath()) {
					String classOrWildcard = component.getName();
					if(classOrWildcard != null && classOrWildcard.length() > 0) {
						int dotIndex = classOrWildcard.lastIndexOf('.');
						if(dotIndex == -1) {
							msgs.addMessage("_comp_default_pkg" + index, "Cannot use classes in the default package.", null, IMessageProvider.WARNING);
						} else {
							final String packageName = classOrWildcard.substring(0, dotIndex);
							final BndEditModel model = (BndEditModel) getManagedForm().getInput();
							if(!model.isIncludedPackage(packageName)) {
								String message = MessageFormat.format("Package \"{0}\" is not included in the bundle. It will be imported instead.", packageName);
								IAction[] fixes = new Action[] {
									new Action(MessageFormat.format("Add \"{0}\" to Private Packages.", packageName)) {
										public void run() {
											model.addPrivatePackage(packageName);
											markDirty();
										};
									},
									new Action(MessageFormat.format("Add \"{0}\" to Exported Packages.", packageName)) {
										public void run() {
											model.addExportedPackage(new ExportedPackage(packageName, null));
											markDirty();
										};
									}
								};
								msgs.addMessage("_comp_nonincluded_pkg" + index, message, fixes, IMessageProvider.WARNING);
							}
						}
					}
				}
				index++;
			}
		}
	}
	@Override
	public void initialize(IManagedForm form) {
		super.initialize(form);
		this.managedForm = form;
		this.model = (BndEditModel) form.getInput();
		this.model.addPropertyChangeListener(this);
	}
	@Override
	public void dispose() {
		super.dispose();
		this.model.removePropertyChangeListener(this);
	}
	@Override
	public void refresh() {
		super.refresh();
		
		// Deep-copy the model
		List<ServiceComponent> original= model.getServiceComponents();
		if(original != null) {
			componentList = new ArrayList<ServiceComponent>(original.size());
			for (ServiceComponent component : original) {
				componentList.add(component.clone());
			}
		} else {
			componentList = new ArrayList<ServiceComponent>();
		}
		
		viewer.setInput(componentList);
		checkComponentPackagesIncluded();
	}
	@Override
	public void commit(boolean onSave) {
		try {
			model.removePropertyChangeListener(this);
			model.setServiceComponents(componentList.isEmpty() ? null : componentList);
		} finally {
			super.commit(onSave);
			model.addPropertyChangeListener(this);
		}
	}
	public void propertyChange(PropertyChangeEvent evt) {
		if(Constants.SERVICE_COMPONENT.equals(evt.getPropertyName())) {
			// Refresh content if ServiceComponents changed
			IFormPage page = (IFormPage) managedForm.getContainer();
			if(page.isActive())
				refresh();
			else
				markStale();
		} else if(Constants.PRIVATE_PACKAGE.equals(evt.getPropertyName()) || Constants.EXPORT_PACKAGE.equals(evt.getPropertyName())) {
			// Content of bundle has changed, re-validate component's for included packages
			checkComponentPackagesIncluded();
		}
	}
	public void updateLabel(ServiceComponent component) {
		viewer.update(component, null);
		checkComponentPackagesIncluded();
	}
	IJavaProject getJavaProject() {
		IFormPage page = (IFormPage) getManagedForm().getContainer();
		IEditorInput input = page.getEditorInput();
		
		IFile file = ResourceUtil.getFile(input);
		if(file != null) {
			return JavaCore.create(file.getProject());
		} else {
			return null;
		}
	}
}
