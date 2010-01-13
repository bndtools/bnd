package name.neilbartlett.eclipse.bndtools.editor.components;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import name.neilbartlett.eclipse.bndtools.UIConstants;
import name.neilbartlett.eclipse.bndtools.editor.FormPartJavaSearchContext;
import name.neilbartlett.eclipse.bndtools.editor.IJavaSearchContext;
import name.neilbartlett.eclipse.bndtools.editor.model.ComponentSvcReference;
import name.neilbartlett.eclipse.bndtools.editor.model.ServiceComponent;
import name.neilbartlett.eclipse.bndtools.editor.model.ServiceComponentConfigurationPolicy;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.ResourceUtil;

public class ComponentDetailsPage extends AbstractFormPart implements IDetailsPage {

	private static final String PROP_COMPONENT_NAME = "_COMPONENT_NAME";
	private static final String PROP_REFERENCES = "_COMPONENT_REFS";
	
	private final ComponentListPart listPart;
	
	private ServiceComponent selected;
	
	private Section provideSection;
	private Section referenceSection;
	private Section lifecycleSection;
	private Section configPolicySection;
	
	private Text txtName;
	private Button btnEnabled;

	private Text txtActivate;
	private Text txtDeactivate;
	private Text txtModified;
	private Button btnImmediate;
	private Button btnSvcFactory;
	private Text txtFactoryId;

	private List<String> provides;
	private Table tableProvide;
	private TableViewer viewerProvide;
	private Button btnAddProvide;
	private Button btnRemoveProvide;
	
	private Button btnConfigPolicyOptional;
	private Button btnConfigPolicyRequire;
	private Button btnConfigPolicyIgnore;

	private List<ComponentSvcReference> references;
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
	public void createContents(Composite parent) {
		FormToolkit toolkit = getManagedForm().getToolkit();
		
		// Create Controls
		Section mainSection = toolkit.createSection(parent, Section.TITLE_BAR);
		mainSection.setText("Component Details");
		fillMainSection(toolkit, mainSection);

		provideSection = toolkit.createSection(parent, Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED);
		provideSection.setText("Provided Services");
		fillProvideSection(toolkit, provideSection);
		
		referenceSection = toolkit.createSection(parent, Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED);
		referenceSection.setText("Service References");
		fillReferenceSection(toolkit, referenceSection);
		
		lifecycleSection = toolkit.createSection(parent, Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED);
		lifecycleSection.setText("Lifecycle");
		fillLifecycleSection(toolkit, lifecycleSection);
		
		configPolicySection = toolkit.createSection(parent, Section.TITLE_BAR | Section.TWISTIE);
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
		ControlDecoration decor;
		
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		
		Hyperlink lnkName = toolkit.createHyperlink(composite, "Name:", SWT.NONE);
		txtName = toolkit.createText(composite, "");
		decor = new ControlDecoration(txtName, SWT.LEFT | SWT.TOP, composite);
		decor.setImage(contentProposalDecoration.getImage());
		decor.setDescriptionText("Content assist available"); // TODO: keystrokes
		decor.setShowHover(true);
		decor.setShowOnlyOnFocus(true);
		
		KeyStroke assistKeyStroke = null;
		try {
			assistKeyStroke = KeyStroke.getInstance("Ctrl+Space");
		} catch (ParseException x) {
			// Ignore
		}
		ComponentNameProposalProvider proposalProvider = new ComponentNameProposalProvider(new FormPartJavaSearchContext(this));
		ContentProposalAdapter proposalAdapter = new ContentProposalAdapter(txtName, new TextContentAdapter(), proposalProvider, assistKeyStroke, UIConstants.AUTO_ACTIVATION_CLASSNAME);
		proposalAdapter.addContentProposalListener(proposalProvider);
		proposalAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
		proposalAdapter.setAutoActivationDelay(1500);
		proposalAdapter.setLabelProvider(proposalProvider.createLabelProvider());
		
		toolkit.createLabel(composite, ""); // Spacer
		btnEnabled = toolkit.createButton(composite, "Enabled", SWT.CHECK);
		
		// Listeners
		lnkName.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				doOpenPattern();
			}
		});
		txtName.addListener(SWT.Modify, new Listener() {
			public void handleEvent(Event event) {
				if(refreshers.get() == 0) {
					selected.setName(txtName.getText());
					listPart.updateLabel(selected);
					markDirty(PROP_COMPONENT_NAME);
					updateVisibility();
				}
			}
		});
		btnEnabled.addListener(SWT.Modify, new MarkDirtyListener(ServiceComponent.COMPONENT_ENABLED));
		
		// Layout
		GridData gd;
		composite.setLayout(new GridLayout(2, false));

		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent = 5;
		txtName.setLayoutData(gd);

		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent = 5;
		txtName.setLayoutData(gd);
	}
	void doOpenPattern() {
		if(selected.isPath()) {
			IFormPage page = (IFormPage) getManagedForm().getContainer();
			IResource resource = ResourceUtil.getResource(page.getEditorInput());
			IProject project = resource.getProject();
			IResource member = project.findMember(selected.getName());
			if(member != null && member.getType() == IResource.FILE) {
				try {
					IDE.openEditor(page.getEditorSite().getPage(), (IFile) member);
				} catch (PartInitException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} else if(!selected.getName().endsWith("*")) {
			try {
				IType type = getJavaProject().findType(selected.getName());
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
	void fillProvideSection(FormToolkit toolkit, Section section) {
		// Create controls
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		
		tableProvide = toolkit.createTable(composite, SWT.FULL_SELECTION | SWT.MULTI);
		viewerProvide = new TableViewer(tableProvide);
		viewerProvide.setContentProvider(new ArrayContentProvider());
		viewerProvide.setLabelProvider(new ProvideLabelProvider());
		
		Composite pnlButtons = toolkit.createComposite(composite);
		btnAddProvide = toolkit.createButton(pnlButtons, "Add", SWT.PUSH);
		btnRemoveProvide = toolkit.createButton(pnlButtons, "Remove", SWT.PUSH);
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
		GridLayout layout;
		
		layout = new GridLayout(1, false);
		layout.verticalSpacing = 5;
		layout.horizontalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);
		
		gd = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 3);
		gd.widthHint = 200;
		gd.heightHint = 60;
		tableProvide.setLayoutData(gd);
		pnlButtons.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		
		layout = new GridLayout(2, true);
		layout.verticalSpacing = 0;
		layout.horizontalSpacing = 5;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		pnlButtons.setLayout(layout);
		btnAddProvide.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		btnRemoveProvide.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
	}
	void doAddProvide() {
		IJavaSearchContext searchContext = new FormPartJavaSearchContext(this);
		SvcInterfaceSelectionDialog dialog = new SvcInterfaceSelectionDialog(getManagedForm().getForm().getShell(), "Add Service Interface", "Interface:", searchContext);
		if(dialog.open() == Window.OK) {
			String svcName = dialog.getValue();
			if(!provides.contains(svcName)) {
				provides.add(svcName);
				viewerProvide.add(svcName);
				markDirty(ServiceComponent.COMPONENT_PROVIDE);
			}
		}
	}
	void doRemoveProvide() {
		@SuppressWarnings("unchecked") Iterator iter = ((IStructuredSelection) viewerProvide.getSelection()).iterator();
		while(iter.hasNext()) {
			Object item = iter.next();
			provides.remove(item);
			viewerProvide.remove(item);
		}
		markDirty(ServiceComponent.COMPONENT_PROVIDE);
	}
	void fillReferenceSection(FormToolkit toolkit, Section section) {
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		
		tableReferences = toolkit.createTable(composite, SWT.FULL_SELECTION | SWT.MULTI);
		tableReferences.setHeaderVisible(true);
		tableReferences.setLinesVisible(true);
		
		TableColumn col;
		col = new TableColumn(tableReferences, SWT.NONE);
		col.setWidth(100);
		col.setText("Name");
		
		col = new TableColumn(tableReferences, SWT.NONE);
		col.setWidth(200);
		col.setText("Interface");
		
		col = new TableColumn(tableReferences, SWT.NONE);
		col.setWidth(35);
		col.setText("Card.");
		
		col = new TableColumn(tableReferences, SWT.NONE);
		col.setWidth(100);
		col.setText("Target");
		
		viewerReferences = new TableViewer(tableReferences);
		viewerReferences.setContentProvider(new ArrayContentProvider());
		viewerReferences.setLabelProvider(new ComponentSvcRefTableLabelProvider());
		
		Composite pnlButtons = toolkit.createComposite(composite, SWT.NONE);
		btnAddReference = toolkit.createButton(pnlButtons, "Add", SWT.PUSH);
		btnEditReference = toolkit.createButton(pnlButtons, "Edit", SWT.PUSH);
		btnRemoveReference = toolkit.createButton(pnlButtons, "Remove", SWT.PUSH);
		btnRemoveReference.setEnabled(false);
		
		// Listeners
		viewerReferences.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) viewerReferences.getSelection();
				btnEditReference.setEnabled(selection.size() == 1);
				btnRemoveReference.setEnabled(!selection.isEmpty());
			}
		});
		btnAddReference.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doAddReference();
			};
		});
		btnEditReference.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doEditReference();
			};
		});
		btnRemoveReference.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doRemoveReference();
			}
		});
		
		// Layout
		GridData gd;
		GridLayout layout;
		
		layout = new GridLayout(1, false);
		layout.verticalSpacing = 5;
		layout.horizontalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);
		
		gd = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 3);
		gd.widthHint = 250;
		gd.heightHint = 70;
		tableReferences.setLayoutData(gd);
		pnlButtons.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));

		layout = new GridLayout(3, true);
		layout.verticalSpacing = 0;
		layout.horizontalSpacing = 5;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		pnlButtons.setLayout(layout);
		btnAddReference.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		btnEditReference.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		btnRemoveReference.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
	}
	Set<String> getExistingReferenceNames() {
		Set<String> result = new HashSet<String>();
		for (ComponentSvcReference ref : references) {
			String name = ref.getName();
			result.add(name);
		}
		return result;
	}
	void doAddReference() {
		ComponentSvcRefWizard wizard = new ComponentSvcRefWizard(getExistingReferenceNames(), getJavaProject(), txtName.getText());
		
		Shell shell = getManagedForm().getForm().getShell();
		WizardDialog dialog = new WizardDialog(shell, wizard);
		if(dialog.open() == Window.OK) {
			ComponentSvcReference newSvcRef = wizard.getResult();
			references.add(newSvcRef);
			viewerReferences.add(newSvcRef);
			markDirty(PROP_REFERENCES);
		}
	}
	void doEditReference() {
		ComponentSvcReference selected = (ComponentSvcReference) ((IStructuredSelection) viewerReferences.getSelection()).getFirstElement();
		Set<String> existingNames = getExistingReferenceNames();
		existingNames.remove(selected.getName());
		
		ComponentSvcRefWizard wizard = new ComponentSvcRefWizard(selected, existingNames, getJavaProject(), txtName.getText());
		Shell shell = getManagedForm().getForm().getShell();
		WizardDialog dialog = new WizardDialog(shell, wizard);
		if(dialog.open() == Window.OK) {
			viewerReferences.update(selected, null);
			markDirty(PROP_REFERENCES);
		}
	}
	void doRemoveReference() {
		@SuppressWarnings("unchecked") Iterator iter = ((IStructuredSelection) viewerReferences.getSelection()).iterator();
		while(iter.hasNext()) {
			ComponentSvcReference svcRef = (ComponentSvcReference) iter.next();
			references.remove(svcRef);
			viewerReferences.remove(svcRef);
			markDirty(PROP_REFERENCES);
		}
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
		txtActivate.addListener(SWT.Modify, new MarkDirtyListener(ServiceComponent.COMPONENT_ACTIVATE));
		txtDeactivate.addListener(SWT.Modify, new MarkDirtyListener(ServiceComponent.COMPONENT_DEACTIVATE));
		txtModified.addListener(SWT.Modify, new MarkDirtyListener(ServiceComponent.COMPONENT_MODIFIED));
		txtFactoryId.addListener(SWT.Modify, new MarkDirtyListener(ServiceComponent.COMPONENT_FACTORY));
		
		// Layout
		composite.setLayout(new GridLayout(2, false));
		
		GridData gd;
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent = 5;
		txtActivate.setLayoutData(gd);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent = 5;
		txtDeactivate.setLayoutData(gd);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent = 5;
		txtModified.setLayoutData(gd);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent = 5;
		txtFactoryId.setLayoutData(gd);
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
		
		// Listeners
		MarkDirtyListener configPolicyDirtyListener = new MarkDirtyListener(ServiceComponent.COMPONENT_CONFIGURATION_POLICY);
		btnConfigPolicyOptional.addListener(SWT.Selection, configPolicyDirtyListener);
		btnConfigPolicyRequire.addListener(SWT.Selection, configPolicyDirtyListener);
		btnConfigPolicyIgnore.addListener(SWT.Selection, configPolicyDirtyListener);
		
		// Layout
		composite.setLayout(new GridLayout(1, false));
	}
	public void selectionChanged(IFormPart part, ISelection selection) {
		Object element = ((IStructuredSelection) selection).getFirstElement();
		this.selected = ((ServiceComponent) element);
		
		loadSelection();
		setFocus();
	}
	@Override
	public void commit(boolean onSave) {
		// Main section
		setBooleanAttribIfDirty(ServiceComponent.COMPONENT_ENABLED, btnEnabled.getSelection());
		
		// Lifecycle section
		setStringAttribIfDirty(ServiceComponent.COMPONENT_ACTIVATE, txtActivate.getText());
		setStringAttribIfDirty(ServiceComponent.COMPONENT_DEACTIVATE, txtDeactivate.getText());
		setStringAttribIfDirty(ServiceComponent.COMPONENT_MODIFIED, txtModified.getText());
		setBooleanAttribIfDirty(ServiceComponent.COMPONENT_IMMEDIATE, btnImmediate.getSelection());
		setBooleanAttribIfDirty(ServiceComponent.COMPONENT_SERVICEFACTORY, btnSvcFactory.getSelection());
		setStringAttribIfDirty(ServiceComponent.COMPONENT_FACTORY, txtFactoryId.getText());

		// Provides section
		if(dirtySet.contains(ServiceComponent.COMPONENT_PROVIDE)) {
			selected.setListAttrib(ServiceComponent.COMPONENT_PROVIDE, provides);
		}
		setStringAttribIfDirty(ServiceComponent.COMPONENT_FACTORY, txtFactoryId.getText());
		
		// References section
		if(dirtySet.contains(PROP_REFERENCES)) {
			selected.setSvcRefs(references);
		}
		
		// Config Policy Section
		String configPolicy;
		if(btnConfigPolicyOptional.getSelection()) {
			configPolicy = ServiceComponentConfigurationPolicy.optional.toString();
		} else if(btnConfigPolicyRequire.getSelection()) {
			configPolicy = ServiceComponentConfigurationPolicy.require.toString();
		} else if(btnConfigPolicyIgnore.getSelection()) {
			configPolicy = ServiceComponentConfigurationPolicy.ignore.toString();
		} else {
			configPolicy = null;
		}
		selected.getAttribs().put(ServiceComponent.COMPONENT_CONFIGURATION_POLICY, configPolicy);
		
		dirtySet.clear();
		listPart.commit(onSave);
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
	private void updateVisibility() {
		boolean enable = !selected.isPath();
		btnEnabled.setVisible(enable);
		provideSection.setVisible(enable);
		referenceSection.setVisible(enable);
		configPolicySection.setVisible(enable);
		lifecycleSection.setVisible(enable);
	}
	private void loadSelection() {
		try {
			refreshers.incrementAndGet();
			
			// Main Section
			txtName.setText(selected != null ? selected.getName() : "");
			loadCheckBox(btnEnabled, ServiceComponent.COMPONENT_ENABLED, true);
			updateVisibility();
			
			// Lifecycle Section
			loadTextField(txtActivate, ServiceComponent.COMPONENT_ACTIVATE, "");
			loadTextField(txtDeactivate, ServiceComponent.COMPONENT_DEACTIVATE, "");
			loadTextField(txtModified, ServiceComponent.COMPONENT_MODIFIED, "");
			loadCheckBox(btnImmediate, ServiceComponent.COMPONENT_IMMEDIATE, false);
			loadCheckBox(btnSvcFactory, ServiceComponent.COMPONENT_SERVICEFACTORY, false);
			loadTextField(txtFactoryId, ServiceComponent.COMPONENT_FACTORY, "");
			
			// Provides Section
			provides = (selected != null) ? selected.getListAttrib(ServiceComponent.COMPONENT_PROVIDE) : Collections.<String>emptyList();
			viewerProvide.setInput(provides);
			
			// References section
			references = selected.getSvcRefs();
			viewerReferences.setInput(references);
			
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
		selected.getAttribs().put(attrib, value);
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
	@Override
	public void setFocus() {
		txtName.setFocus();
		txtName.setSelection(txtName.getText().length());
	}
}
