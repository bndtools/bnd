package name.neilbartlett.eclipse.bndtools.editor.components;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import name.neilbartlett.eclipse.bndtools.editor.model.BndEditModel;
import name.neilbartlett.eclipse.bndtools.editor.model.ServiceComponent;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
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
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import aQute.lib.osgi.Constants;

public class ComponentListPart extends SectionPart implements PropertyChangeListener {

	private List<ServiceComponent> componentList;
	
	private IManagedForm managedForm;
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
		
		Table table = toolkit.createTable(composite, SWT.MULTI | SWT.FULL_SELECTION);
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
		btnAdd.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		btnRemove.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}
	void doAdd() {
		ServiceComponent component = new ServiceComponent("", new HashMap<String, String>());
		componentList.add(component);
		viewer.add(component);
		viewer.setSelection(new StructuredSelection(component), true);
		markDirty();
	}
	void doRemove() {
		@SuppressWarnings("unchecked") Iterator iter = ((IStructuredSelection) viewer.getSelection()).iterator();
		while(iter.hasNext()) {
			Object item = iter.next();
			componentList.remove(item);
			viewer.remove(item);
		}
		markDirty();
	}
	@Override
	public void initialize(IManagedForm form) {
		super.initialize(form);
		this.managedForm = form;
		this.model = (BndEditModel) form.getInput();
		this.model.addPropertyChangeListener(Constants.SERVICE_COMPONENT, this);
	}
	@Override
	public void dispose() {
		super.dispose();
		this.model.removePropertyChangeListener(Constants.SERVICE_COMPONENT, this);
	}
	@Override
	public void refresh() {
		super.refresh();
		
		// Deep-copy the model
		List<ServiceComponent> original= model.getServiceComponents();
		componentList = new ArrayList<ServiceComponent>(original.size());
		for (ServiceComponent component : original) {
			componentList.add(component.clone());
		}
		
		viewer.setInput(componentList);
	}
	@Override
	public void commit(boolean onSave) {
		try {
			model.removePropertyChangeListener(Constants.SERVICE_COMPONENT, this);
			model.setServiceComponents(componentList);
		} finally {
			super.commit(onSave);
			model.addPropertyChangeListener(Constants.SERVICE_COMPONENT, this);
		}
	}
	public void propertyChange(PropertyChangeEvent evt) {
		IFormPage page = (IFormPage) managedForm.getContainer();
		if(page.isActive())
			refresh();
		else
			markStale();
	}
	public void updateLabel(ServiceComponent component) {
		viewer.update(component, null);
	}
}
