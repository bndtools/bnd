package name.neilbartlett.eclipse.bndtools.editor.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import name.neilbartlett.eclipse.bndtools.editor.model.BndEditModel;
import name.neilbartlett.eclipse.bndtools.editor.model.ServiceComponent;
import name.neilbartlett.eclipse.bndtools.editor.model.ServiceComponentConfigurationPolicy;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.ResourceUtil;

public class ComponentDetailsPage extends AbstractFormPart implements IDetailsPage {

	private static final String PROP_COMPONENT_PATH = "COMPONENT_PATH";
	
	private final ComponentListPart listPart;
	
	private ServiceComponent selected;
	private ServiceComponent editClone;
	
	private Text txtPath;
	private Text txtName;
	private Button btnEnabled;

	private Text txtActivate;
	private Text txtDeactivate;
	private Text txtModified;
	private Button btnImmediate;
	private Button btnSvcFactory;
	private Text txtFactoryId;

	private Table tableProvide;
	private TableViewer viewerProvide;
	private Button btnAddProvide;
	private Button btnRemoveProvide;
	
	private Button btnConfigPolicyOptional;
	private Button btnConfigPolicyRequire;
	private Button btnConfigPolicyIgnore;

	private Table tableReferences;
	private TableViewer viewerReferences;
	private Button btnAddReference;
	private Button btnEditReference;
	private Button btnRemoveReference;
	
	// Used to ignore events that occur in response to programmatic changes
	private final AtomicInteger refreshers = new AtomicInteger(0);
	
	private class MarkDirtyListener implements Listener {
		private final String attrib;
		public MarkDirtyListener(String attrib) {
			this.attrib = attrib;
		}
		public void handleEvent(Event event) {
			if(refreshers.get() == 0)
				markDirty(attrib);
		}
	}
	
	public ComponentDetailsPage(ComponentListPart listPart) {
		this.listPart = listPart;
	}
	
	// Override the default dirtiness behaviour to track individual properties, not just a single flag for everything.
	private final Set<String> dirtySet = new HashSet<String>();

	@Override
	public boolean isDirty() {
		return !dirtySet.isEmpty();
	}
	@Override
	public void markDirty() {
		throw new UnsupportedOperationException("Do not call the no-argument version of markDirty.");
	}
	protected void markDirty(String property) {
		dirtySet.add(property);
		getManagedForm().dirtyStateChanged();
	}
	ServiceComponent getEditClone() {
		ServiceComponent result;
		if(editClone != null) {
			result = editClone;
		} else {
			editClone = (selected != null) ? selected.clone() : null;
			result = editClone;
		}
		return result;
	}
	public void createContents(Composite parent) {
		FormToolkit toolkit = getManagedForm().getToolkit();
		
		// Create Controls
		Section mainSection = toolkit.createSection(parent, Section.TITLE_BAR);
		mainSection.setText("Component Details");
		fillMainSection(toolkit, mainSection);

		Section provideSection = toolkit.createSection(parent, Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED);
		provideSection.setText("Provided Services");
		fillProvideSection(toolkit, provideSection);
		
		Section referenceSection = toolkit.createSection(parent, Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED);
		referenceSection.setText("Service References");
		fillReferenceSection(toolkit, referenceSection);
		
		Section lifecycleSection = toolkit.createSection(parent, Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED);
		lifecycleSection.setText("Lifecycle");
		fillLifecycleSection(toolkit, lifecycleSection);
		
		Section configPolicySection = toolkit.createSection(parent, Section.TITLE_BAR | Section.TWISTIE);
		configPolicySection.setText("Configuration Policy");
		fillConfigPolicySection(toolkit, configPolicySection);
		
		// Layout
		GridData gd;
		
		parent.setLayout(new GridLayout(1, false));
		
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 100;
		mainSection.setLayoutData(gd);
		
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 100;
		provideSection.setLayoutData(gd);

		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 100;
		referenceSection.setLayoutData(gd);

		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 100;
		lifecycleSection.setLayoutData(gd);

		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 200;
		configPolicySection.setLayoutData(gd);
	}
	void fillMainSection(FormToolkit toolkit, Section section) {
		FieldDecoration contentProposalDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL);
		FieldDecoration infoDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION);
		ControlDecoration decor;
		
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		
		toolkit.createLabel(composite, "Path:");
		txtPath = toolkit.createText(composite, "");
		decor = new ControlDecoration(txtPath, SWT.LEFT | SWT.TOP, composite);
		decor.setImage(contentProposalDecoration.getImage());
		decor.setDescriptionText("Content assist available"); // TODO: keystrokes
		decor.setShowHover(true);
		decor.setShowOnlyOnFocus(true);
		
		toolkit.createLabel(composite, "Name:");
		txtName = toolkit.createText(composite, "");
		toolkit.createLabel(composite, ""); // Spacer
		btnEnabled = toolkit.createButton(composite, "Enabled", SWT.CHECK);
		
		// Listeners
		txtPath.addListener(SWT.Modify, new Listener() {
			public void handleEvent(Event event) {
				if(refreshers.get() == 0) {
					selected.setPattern(txtPath.getText());
					getEditClone().setPattern(txtPath.getText());
					listPart.updateLabel(selected);
					markDirty(PROP_COMPONENT_PATH);
				}
			}
		});
		txtName.addListener(SWT.Modify, new MarkDirtyListener(ServiceComponent.COMPONENT_NAME));
		btnEnabled.addListener(SWT.Modify, new MarkDirtyListener(ServiceComponent.COMPONENT_ENABLED));
		
		// Layout
		GridData gd;
		composite.setLayout(new GridLayout(2, false));

		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent = 5;
		txtPath.setLayoutData(gd);

		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent = 5;
		txtName.setLayoutData(gd);

		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent = 5;
		txtFactoryId.setLayoutData(gd);
	}
	void fillProvideSection(FormToolkit toolkit, Section section) {
		// Create controls
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		
		tableProvide = toolkit.createTable(composite, SWT.FULL_SELECTION | SWT.MULTI);
		viewerProvide = new TableViewer(tableProvide);
		viewerProvide.setContentProvider(new ArrayContentProvider());
		viewerProvide.setLabelProvider(new ProvideLabelProvider());
		
		btnAddProvide = toolkit.createButton(composite, "Add", SWT.PUSH);
		btnRemoveProvide = toolkit.createButton(composite, "Remove", SWT.PUSH);
		btnRemoveProvide.setEnabled(false);
		
		// Listeners
		viewerProvide.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				btnRemoveProvide.setEnabled(!viewerProvide.getSelection().isEmpty());
			}
		});
		btnAddProvide.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doAddProvide();
			}
		});
		btnRemoveProvide.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doRemoveProvide();
			}
		});
		
		// Layout
		GridData gd;
		
		composite.setLayout(new GridLayout(2, false));
		gd = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 3);
		gd.widthHint = 200;
		gd.heightHint = 75;
		tableProvide.setLayoutData(gd);
		btnAddProvide.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		btnRemoveProvide.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
	}
	void doAddProvide() {
		IFormPage formPage = (IFormPage) getManagedForm().getContainer();
		IWorkbenchWindow window = formPage.getEditorSite().getWorkbenchWindow();
		
		SvcInterfaceSelectionDialog dialog = new SvcInterfaceSelectionDialog(formPage.getEditorSite().getShell(), "Add Service Interface", "Interface:", getJavaProject(), window);
		if(dialog.open() == Window.OK) {
			String svcName = dialog.getValue();
			
			ServiceComponent sc = getEditClone();
			List<String> list = sc.getListAttrib(ServiceComponent.COMPONENT_PROVIDE);
			if(!list.contains(svcName)) {
				list.add(svcName);
				viewerProvide.add(svcName);
				sc.setListAttrib(ServiceComponent.COMPONENT_PROVIDE, list);
				markDirty(ServiceComponent.COMPONENT_PROVIDE);
			}
		}
	}
	void doRemoveProvide() {
		ServiceComponent sc = getEditClone();
		List<String> list = sc.getListAttrib(ServiceComponent.COMPONENT_PROVIDE);
		
		@SuppressWarnings("unchecked") Iterator iter = ((IStructuredSelection) viewerProvide.getSelection()).iterator();
		while(iter.hasNext()) {
			Object item = iter.next();
			list.remove(item);
			viewerProvide.remove(item);
		}
		sc.setListAttrib(ServiceComponent.COMPONENT_PROVIDE, list);
		markDirty(ServiceComponent.COMPONENT_PROVIDE);
	}
	void fillReferenceSection(FormToolkit toolkit, Section section) {
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		
		tableReferences = toolkit.createTable(composite, SWT.FULL_SELECTION | SWT.MULTI);
		viewerReferences = new TableViewer(tableReferences);
		
		btnAddReference = toolkit.createButton(composite, "Add", SWT.PUSH);
		btnEditReference = toolkit.createButton(composite, "Edit", SWT.PUSH);
		btnRemoveReference = toolkit.createButton(composite, "Remove", SWT.PUSH);
		btnRemoveReference.setEnabled(false);
		
		// Listeners
		viewerReferences.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) viewerReferences.getSelection();
				btnEditReference.setEnabled(selection.size() == 1);
				btnRemoveReference.setEnabled(!selection.isEmpty());
			}
		});
		
		// Layout
		GridData gd;
		composite.setLayout(new GridLayout(2, false));
		
		gd = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 3);
		gd.widthHint = 200;
		gd.heightHint = 75;
		tableReferences.setLayoutData(gd);
		btnAddReference.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		btnEditReference.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		btnRemoveReference.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
	}
	void fillLifecycleSection(FormToolkit toolkit, Section section) {
		FieldDecoration infoDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION);
		ControlDecoration decor;
		
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		
		// Create controls
		toolkit.createLabel(composite, "Activate method:");
		txtActivate = toolkit.createText(composite, "");
		toolkit.createLabel(composite, "Deactivate method:");
		txtDeactivate = toolkit.createText(composite, "");
		toolkit.createLabel(composite, "Modified method:");
		txtModified = toolkit.createText(composite, "");
		
		toolkit.createLabel(composite, ""); // Spacer
		btnImmediate = toolkit.createButton(composite, "Immediate", SWT.CHECK);
		decor = new ControlDecoration(btnImmediate, SWT.RIGHT, composite);
		decor.setImage(infoDecoration.getImage());
		decor.setDescriptionText("The component will be activated immediately,\n" +
		                         "even when it provides a service and no consumers\n" +
		                         "of the service exist.");
		decor.setShowHover(true);
		
		toolkit.createLabel(composite, ""); // Spacer
		btnSvcFactory = toolkit.createButton(composite, "Service Factory", SWT.CHECK);
		decor = new ControlDecoration(btnSvcFactory, SWT.RIGHT, composite);
		decor.setImage(infoDecoration.getImage());
		decor.setDescriptionText("An instance of the component will be created\nfor each service consumer.");
		decor.setShowHover(true);
		
		toolkit.createLabel(composite, "Factory ID:");
		txtFactoryId = toolkit.createText(composite, "");
		decor = new ControlDecoration(txtFactoryId, SWT.LEFT | SWT.BOTTOM, composite);
		decor.setImage(infoDecoration.getImage());
		decor.setDescriptionText("Makes the component a 'factory component', published\nunder the ComponentFactory service, with the specified ID.");
		
		// Listeners
		txtFactoryId.addListener(SWT.Modify, new MarkDirtyListener(ServiceComponent.COMPONENT_FACTORY));

		
		// Layout
		composite.setLayout(new GridLayout(2, false));
		txtActivate.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		txtDeactivate.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		txtModified.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}
	void fillConfigPolicySection(FormToolkit toolkit, Section section) {
		FieldDecoration infoDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION);
		ControlDecoration decor;
		
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		btnConfigPolicyOptional = toolkit.createButton(composite, "Optional", SWT.RADIO);
		decor = new ControlDecoration(btnConfigPolicyOptional, SWT.RIGHT, composite);
		decor.setImage(infoDecoration.getImage());
		decor.setDescriptionText("The component will be activated whether or not\nmatching configuration data is available.");
		decor.setShowHover(true);
		btnConfigPolicyRequire = toolkit.createButton(composite, "Require", SWT.RADIO);
		decor = new ControlDecoration(btnConfigPolicyRequire, SWT.RIGHT, composite);
		decor.setImage(infoDecoration.getImage());
		decor.setDescriptionText("The component will be activated ONLY if matching\nconfiguration data is available.");
		decor.setShowHover(true);
		btnConfigPolicyIgnore = toolkit.createButton(composite, "Ignore", SWT.RADIO);
		decor = new ControlDecoration(btnConfigPolicyIgnore, SWT.RIGHT, composite);
		decor.setImage(infoDecoration.getImage());
		decor.setDescriptionText("The component will not receive configuration\ndata from Configuration Admin.");
		decor.setShowHover(true);
		
		// Layout
		composite.setLayout(new GridLayout(1, false));
	}
	public void selectionChanged(IFormPart part, ISelection selection) {
		System.out.println("ComponentDetailsPage#selectionChanged");
		
		// Clone the service component before we edit it.
		Object element = ((IStructuredSelection) selection).getFirstElement();
		this.selected = ((ServiceComponent) element);
		this.editClone = null;
		
		loadSelection();
	}
	@Override
	public void commit(boolean onSave) {
		setStringAttribIfDirty(ServiceComponent.COMPONENT_NAME, txtName.getText());
		setBooleanAttribIfDirty(ServiceComponent.COMPONENT_ENABLED, btnEnabled.getSelection());
		setStringAttribIfDirty(ServiceComponent.COMPONENT_FACTORY, txtFactoryId.getText());
		
		BndEditModel model = (BndEditModel) getManagedForm().getInput();
		List<ServiceComponent> list = new ArrayList<ServiceComponent>(model.getServiceComponents());
		int index = list.indexOf(selected);
		if(index > -1 && editClone != null) {
			list.set(index, editClone);
			model.setServiceComponents(list);
		}
		dirtySet.clear();
		editClone = null;
		super.commit(onSave);
	}
	@Override
	public void refresh() {
		super.refresh();
	}
	private void setStringAttribIfDirty(String attrib, String value) {
		if(dirtySet.contains(attrib)) {
			setStringAttrib(attrib, value);
		}
	}
	private void setBooleanAttribIfDirty(String attrib, boolean value) {
		if(dirtySet.contains(attrib)) {
			setBooleanAttrib(attrib, value);
		}
	}
	private void loadSelection() {
		try {
			refreshers.incrementAndGet();
			
			// Main Section
			txtPath.setText(selected != null ? selected.getPattern() : "");
			loadTextField(txtName, ServiceComponent.COMPONENT_NAME, "");
			loadCheckBox(btnEnabled, ServiceComponent.COMPONENT_ENABLED, true);
			loadTextField(txtFactoryId, ServiceComponent.COMPONENT_FACTORY, "");
			
			// Lifecycle Section
			loadTextField(txtActivate, ServiceComponent.COMPONENT_ACTIVATE, "");
			loadTextField(txtDeactivate, ServiceComponent.COMPONENT_DEACTIVATE, "");
			loadTextField(txtModified, ServiceComponent.COMPONENT_MODIFIED, "");
			loadCheckBox(btnImmediate, ServiceComponent.COMPONENT_IMMEDIATE, false);
			loadCheckBox(btnSvcFactory, ServiceComponent.COMPONENT_SERVICEFACTORY, false);
			
			// Provides Section
			viewerProvide.setInput(selected == null ? Collections.emptyList() : selected.getListAttrib(ServiceComponent.COMPONENT_PROVIDE));
			
			// Config Policy Section
			ServiceComponentConfigurationPolicy configPolicy;
			try {
				String configPolicyStr = getStringAttrib(ServiceComponent.COMPONENT_CONFIGURATION_POLICY, null);
				configPolicy = configPolicyStr != null ? Enum.valueOf(ServiceComponentConfigurationPolicy.class, configPolicyStr) : null;
			} catch (IllegalArgumentException e) {
				configPolicy = null;
			}
			btnConfigPolicyOptional.setSelection(ServiceComponentConfigurationPolicy.optional == configPolicy);
			btnConfigPolicyRequire.setSelection(ServiceComponentConfigurationPolicy.require == configPolicy);
			btnConfigPolicyIgnore.setSelection(ServiceComponentConfigurationPolicy.ignore == configPolicy);
		} finally {
			refreshers.decrementAndGet();
		}
	}
	private void loadTextField(Text field, String attrib, String defaultValue) {
		try {
			refreshers.incrementAndGet();
			String value = getStringAttrib(attrib, defaultValue);
			field.setText(value);
		} finally {
			refreshers.decrementAndGet();
		}
	}
	private void loadCheckBox(Button checkBox, String attrib, boolean defaultValue) {
		try {
			refreshers.incrementAndGet();
			boolean value = getBooleanAttrib(attrib, defaultValue);
			checkBox.setSelection(value);
		} finally {
			refreshers.decrementAndGet();
		}
	}
	private String getStringAttrib(String attrib, String defaultValue) {
		Map<String, String> attribs = selected.getAttribs();
		String value = attribs.get(attrib);
		return value != null ? value : defaultValue;
	}
	private void setStringAttrib(String attrib, String value) {
		getEditClone().getAttribs().put(attrib, value);
	}
	private boolean getBooleanAttrib(String attrib, boolean defaultValue) {
		String string = getStringAttrib(attrib, Boolean.toString(defaultValue));
		if(string.equalsIgnoreCase(Boolean.toString(true))) {
			return true;
		} else if(string.equalsIgnoreCase(Boolean.toString(false))) {
			return false;
		} else {
			return defaultValue;
		}
	}
	private void setBooleanAttrib(String attrib, boolean value) {
		setStringAttrib(attrib, Boolean.toString(value));
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
