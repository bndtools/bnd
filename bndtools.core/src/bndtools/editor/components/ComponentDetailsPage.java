/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 *******************************************************************************/
package bndtools.editor.components;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
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
import org.eclipse.swt.graphics.Image;
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
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;
import bndtools.UIConstants;
import bndtools.editor.common.MapContentProvider;
import bndtools.editor.common.MapEntryCellModifier;
import bndtools.editor.common.PropertiesTableLabelProvider;
import bndtools.editor.model.ComponentSvcReference;
import bndtools.editor.model.ServiceComponent;
import bndtools.editor.model.ServiceComponentConfigurationPolicy;
import bndtools.javamodel.FormPartJavaSearchContext;
import bndtools.javamodel.IJavaSearchContext;

public class ComponentDetailsPage extends AbstractFormPart implements IDetailsPage {

	private static final String PROP_COMPONENT_NAME = "_COMPONENT_NAME";
	private static final String PROP_REFERENCES = "_COMPONENT_REFS";

	private final Image imgEdit = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/pencil.png").createImage();
	private final ComponentListPart listPart;

	private ServiceComponent selected;

	private Section propertiesSection;
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

	private Map<String,String> properties;
	private Table tableProperties;
	private TableViewer viewerProperties;
	private MapEntryCellModifier<String, String> modifierProperties;

	private List<String> provides;
	private Table tableProvide;
	private TableViewer viewerProvide;

	private Button btnConfigPolicyOptional;
	private Button btnConfigPolicyRequire;
	private Button btnConfigPolicyIgnore;

	private List<ComponentSvcReference> references;
	private Table tableReferences;
	private TableViewer viewerReferences;

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

		propertiesSection = toolkit.createSection(parent, Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED);
		propertiesSection.setText("Properties");
		fillPropertiesSection(toolkit, propertiesSection);

		provideSection = toolkit.createSection(parent, Section.TITLE_BAR | Section.TWISTIE);
		provideSection.setText("Provided Services");
		fillProvideSection(toolkit, provideSection);

		referenceSection = toolkit.createSection(parent, Section.TITLE_BAR | Section.TWISTIE);
		referenceSection.setText("Service References");
		fillReferenceSection(toolkit, referenceSection);

		lifecycleSection = toolkit.createSection(parent, Section.TITLE_BAR | Section.TWISTIE);
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
		propertiesSection.setLayoutData(gd);

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
		txtName = toolkit.createText(composite, "", SWT.BORDER);
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
				listPart.doOpenComponent(selected.getName());
			}
		});
		txtName.addListener(SWT.Modify, new Listener() {
			public void handleEvent(Event event) {
				if(refreshers.get() == 0) {
					String oldName = selected.getName();
					String newName = txtName.getText();
					selected.setName(newName);

					listPart.updateLabel(oldName, newName);
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

	void fillPropertiesSection(FormToolkit toolkit, Section section) {
		// Create controls
		ToolBar toolbar = new ToolBar(section, SWT.FLAT | SWT.HORIZONTAL);
		section.setTextClient(toolbar);

		final ToolItem addItem = new ToolItem(toolbar, SWT.NULL);
		addItem.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ADD));
		addItem.setToolTipText("Add");

		final ToolItem removeItem = new ToolItem(toolbar, SWT.PUSH);
		removeItem.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE));
		removeItem.setDisabledImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE_DISABLED));
		removeItem.setToolTipText("Remove");
		removeItem.setEnabled(false);

		Composite composite = toolkit.createComposite(section, SWT.NONE);
		section.setClient(composite);
		tableProperties = toolkit.createTable(composite, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		viewerProperties = new TableViewer(tableProperties);
		modifierProperties = new MapEntryCellModifier<String, String>(viewerProperties);

		tableProperties.setHeaderVisible(true);
		tableProperties.setLinesVisible(false);

		modifierProperties.addColumnsToTable();

		viewerProperties.setUseHashlookup(true);
		viewerProperties.setColumnProperties(modifierProperties.getColumnProperties());
		modifierProperties.addCellEditorsToViewer();
		viewerProperties.setCellModifier(modifierProperties);

		viewerProperties.setContentProvider(new MapContentProvider());
		viewerProperties.setLabelProvider(new PropertiesTableLabelProvider());

		// Layout
		GridLayout layout = new GridLayout(1, false);
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.heightHint = 70;
		tableProperties.setLayoutData(gd);

		// Listeners
		viewerProperties.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				removeItem.setEnabled(!viewerProperties.getSelection().isEmpty());
			}
		});
		addItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doAddProperty();
			}
		});
		removeItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doRemoveProperty();
			}
		});
	}
	void doAddProperty() {
		properties.put("name", "value");
		viewerProperties.add("name");
		markDirty(ServiceComponent.COMPONENT_PROPERTIES);

		viewerProperties.editElement("name", 0);
	}
	void doRemoveProperty() {
		Iterator iter = ((IStructuredSelection) viewerProperties.getSelection()).iterator();
		while(iter.hasNext()) {
			Object item = iter.next();
			properties.remove(item);
			viewerProperties.remove(item);
		}
		markDirty(ServiceComponent.COMPONENT_PROPERTIES);
	}
	void fillProvideSection(FormToolkit toolkit, Section section) {
		// Create controls
		ToolBar toolbar = new ToolBar(section, SWT.FLAT | SWT.HORIZONTAL);
		section.setTextClient(toolbar);

		final ToolItem addItem = new ToolItem(toolbar, SWT.NULL);
		addItem.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ADD));
		addItem.setToolTipText("Add");

		final ToolItem removeItem = new ToolItem(toolbar, SWT.PUSH);
		removeItem.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE));
		removeItem.setDisabledImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE_DISABLED));
		removeItem.setToolTipText("Remove");
		removeItem.setEnabled(false);

		Composite composite = toolkit.createComposite(section, SWT.NONE);
		section.setClient(composite);
		tableProvide = toolkit.createTable(composite, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);

		viewerProvide = new TableViewer(tableProvide);
		viewerProvide.setContentProvider(new ArrayContentProvider());
		viewerProvide.setLabelProvider(new ProvideLabelProvider());

		// Layout
		GridLayout layout = new GridLayout(1, false);
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.heightHint = 70;
		tableProvide.setLayoutData(gd);

		// Listeners
		viewerProvide.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				removeItem.setEnabled(!viewerProvide.getSelection().isEmpty());
			}
		});
		addItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doAddProvide();
			}
		});
		removeItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doRemoveProvide();
			}
		});
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

		ToolBar toolbar = new ToolBar(section, SWT.FLAT);
		section.setTextClient(toolbar);

		final ToolItem addItem = new ToolItem(toolbar, SWT.PUSH);
		addItem.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ADD));

		final ToolItem editItem = new ToolItem(toolbar, SWT.PUSH);
		editItem.setImage(imgEdit);
		editItem.setToolTipText("Edit");
		editItem.setEnabled(false);

		final ToolItem removeItem = new ToolItem(toolbar, SWT.PUSH);
		removeItem.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE));
		removeItem.setDisabledImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE_DISABLED));
		removeItem.setToolTipText("Remove");
		removeItem.setEnabled(false);


		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);

		tableReferences = toolkit.createTable(composite, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		tableReferences.setHeaderVisible(true);
		tableReferences.setLinesVisible(false);

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

		// Listeners
		viewerReferences.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) viewerReferences.getSelection();
				editItem.setEnabled(selection.size() == 1);
				removeItem.setEnabled(!selection.isEmpty());
			}
		});
		addItem.addSelectionListener(new SelectionAdapter() {
			@Override
            public void widgetSelected(SelectionEvent e) {
				doAddReference();
			};
		});
		editItem.addSelectionListener(new SelectionAdapter() {
			@Override
            public void widgetSelected(SelectionEvent e) {
				doEditReference();
			};
		});
		removeItem.addSelectionListener(new SelectionAdapter() {
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
		txtActivate = toolkit.createText(composite, "", SWT.BORDER);
		toolkit.createLabel(composite, "Deactivate method:");
		txtDeactivate = toolkit.createText(composite, "", SWT.BORDER);
		toolkit.createLabel(composite, "Modified method:");
		txtModified = toolkit.createText(composite, "", SWT.BORDER);

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
		txtFactoryId = toolkit.createText(composite, "", SWT.BORDER);
		decor = new ControlDecoration(txtFactoryId, SWT.LEFT | SWT.BOTTOM, composite);
		decor.setImage(infoDecoration.getImage());
		decor.setDescriptionText("Makes the component a 'factory component', published\nunder the ComponentFactory service, with the specified ID.");

		// Listeners
		txtActivate.addListener(SWT.Modify, new MarkDirtyListener(ServiceComponent.COMPONENT_ACTIVATE));
		txtDeactivate.addListener(SWT.Modify, new MarkDirtyListener(ServiceComponent.COMPONENT_DEACTIVATE));
		txtModified.addListener(SWT.Modify, new MarkDirtyListener(ServiceComponent.COMPONENT_MODIFIED));
		txtFactoryId.addListener(SWT.Modify, new MarkDirtyListener(ServiceComponent.COMPONENT_FACTORY));
		btnImmediate.addListener(SWT.Selection, new MarkDirtyListener(ServiceComponent.COMPONENT_IMMEDIATE));
		btnSvcFactory.addListener(SWT.Selection, new MarkDirtyListener(ServiceComponent.COMPONENT_SERVICEFACTORY));

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
		GridLayout layout = new GridLayout(3, false);
		layout.horizontalSpacing = 15;
		composite.setLayout(layout);
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

		// Properties section
		if(dirtySet.contains(ServiceComponent.COMPONENT_PROPERTIES)) {
			selected.setPropertiesMap(properties);
		}

		// Provides section
		if(dirtySet.contains(ServiceComponent.COMPONENT_PROVIDE)) {
			selected.setListAttrib(ServiceComponent.COMPONENT_PROVIDE, provides);
		}

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

			// Properties section
			properties = (selected != null) ? selected.getPropertiesMap() : new LinkedHashMap<String, String>();
			viewerProperties.setInput(properties);

			// Provides Section
			provides = (selected != null) ? selected.getListAttrib(ServiceComponent.COMPONENT_PROVIDE) : new LinkedList<String>();
			if (provides == null)
			    provides = new LinkedList<String>();
			viewerProvide.setInput(provides);

			// References section
			references = (selected != null) ? selected.getSvcRefs() : new LinkedList<ComponentSvcReference>();
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
		txtName.setSelection(0, txtName.getText().length());
	}

	@Override
	public void dispose() {
		super.dispose();
		imgEdit.dispose();
	}
}
