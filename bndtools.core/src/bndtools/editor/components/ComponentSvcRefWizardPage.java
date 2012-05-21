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

import java.util.Set;


import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import bndtools.UIConstants;
import bndtools.javamodel.IJavaMethodSearchContext;
import bndtools.model.clauses.ComponentSvcReference;
import bndtools.utils.JavaContentProposalLabelProvider;

public class ComponentSvcRefWizardPage extends WizardPage {

	private final Set<String> existingNames;
	private final ComponentSvcReference serviceReference;
	private final IJavaProject javaProject;
	private final String componentClassName;
	
	private final IJavaMethodSearchContext searchContext = new IJavaMethodSearchContext() {
		public IRunnableContext getRunContext() {
			return getContainer();
		}
		public IJavaProject getJavaProject() {
			return javaProject;
		}
		public String getTargetTypeName() {
			return componentClassName;
		}
		public String[] getParameterTypeNames() {
			// TODO Auto-generated method stub
			return null;
		}
	};

	private Text txtName;
	private Text txtInterface;
	private Text txtBind;
	private Text txtUnbind;
	private Button btnOptional;
	private Button btnMultiple;
	private Button btnDynamic;
	private Text txtTargetFilter;

	public ComponentSvcRefWizardPage(ComponentSvcReference serviceRef, String pageName, Set<String> existingNames, IJavaProject javaProject, String componentClassName) {
		super(pageName);
		assert serviceRef != null;
		
		this.javaProject = javaProject;
		this.componentClassName = componentClassName;
		this.serviceReference = serviceRef;
		this.existingNames = existingNames;
	}
	
	public void createControl(Composite parent) {
		setTitle("Edit Service Reference");
		
		KeyStroke assistKeyStroke = null;
		try {
			assistKeyStroke = KeyStroke.getInstance("Ctrl+Space");
		} catch (ParseException x) {
			// Ignore
		}
		FieldDecoration assistDecor = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL);
		
		Composite composite = new Composite(parent, SWT.NONE);
		
		new Label(composite, SWT.NONE).setText("Name:");
		txtName = new Text(composite, SWT.BORDER);
		
		new Label(composite, SWT.NONE).setText("Interface:");
		txtInterface = new Text(composite, SWT.BORDER);
		ControlDecoration decorInterface = new ControlDecoration(txtInterface, SWT.LEFT | SWT.TOP, composite);
		decorInterface.setImage(assistDecor.getImage());
		decorInterface.setDescriptionText("Content assist available");
		decorInterface.setShowHover(true);
		decorInterface.setShowOnlyOnFocus(true);
		
		// Add content proposal to svc interface field
		SvcInterfaceProposalProvider proposalProvider = new SvcInterfaceProposalProvider(searchContext);
		ContentProposalAdapter interfaceProposalAdapter = new ContentProposalAdapter(txtInterface, new TextContentAdapter(), proposalProvider, assistKeyStroke, UIConstants.autoActivationCharacters());
		interfaceProposalAdapter.addContentProposalListener(proposalProvider);
		interfaceProposalAdapter.setAutoActivationDelay(1500);
		interfaceProposalAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
		interfaceProposalAdapter.setLabelProvider(new JavaContentProposalLabelProvider());
		
		new Label(composite, SWT.NONE).setText("Bind:");
		txtBind = new Text(composite, SWT.BORDER);
		ControlDecoration decorBind = new ControlDecoration(txtBind, SWT.LEFT | SWT.TOP, composite);
		decorBind.setImage(assistDecor.getImage());
		decorBind.setDescriptionText("Content assist available");
		decorBind.setShowHover(true);
		decorBind.setShowOnlyOnFocus(true);
		
		MethodProposalProvider bindProposalProvider = new MethodProposalProvider(searchContext);
		ContentProposalAdapter bindProposalAdapter = new ContentProposalAdapter(txtBind, new TextContentAdapter(), bindProposalProvider, assistKeyStroke, UIConstants.autoActivationCharacters());
		bindProposalAdapter.addContentProposalListener(bindProposalProvider);
		bindProposalAdapter.setAutoActivationDelay(1500);
		bindProposalAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
		bindProposalAdapter.setLabelProvider(new MethodProposalLabelProvider());
		
		new Label(composite, SWT.NONE).setText("Unbind:");
		txtUnbind = new Text(composite, SWT.BORDER);
		ControlDecoration decorUnbind = new ControlDecoration(txtUnbind, SWT.LEFT | SWT.TOP, composite);
		decorUnbind.setImage(assistDecor.getImage());
		decorUnbind.setDescriptionText("Content assist available");
		decorUnbind.setShowHover(true);
		decorUnbind.setShowOnlyOnFocus(true);
		ContentProposalAdapter unbindProposalAdapter = new ContentProposalAdapter(txtUnbind, new TextContentAdapter(), bindProposalProvider, assistKeyStroke, UIConstants.autoActivationCharacters());
		unbindProposalAdapter.addContentProposalListener(bindProposalProvider);
		unbindProposalAdapter.setAutoActivationDelay(1500);
		unbindProposalAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
		unbindProposalAdapter.setLabelProvider(new MethodProposalLabelProvider());
		
		new Label(composite, SWT.NONE); // Spacer
		Composite pnlButtons = new Composite(composite, SWT.NONE);
		btnOptional = new Button(pnlButtons, SWT.CHECK);
		btnOptional.setText("Optional");
		btnMultiple = new Button(pnlButtons, SWT.CHECK);
		btnMultiple.setText("Multiple");
		btnDynamic = new Button(pnlButtons, SWT.CHECK);
		btnDynamic.setText("Dynamic");
		
		new Label(composite, SWT.NONE).setText("Target Filter:");
		txtTargetFilter = new Text(composite, SWT.BORDER);
		
		// Initialise
		initialiseFields();
		validate();
		
		// Listeners
		ModifyListener nameAndInterfaceModifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String name = emptyToNull(txtName.getText());
				String clazz = emptyToNull(txtInterface.getText());
				if(name == null)
					name = clazz;
				
				serviceReference.setName(name);
				serviceReference.setServiceClass(clazz);
				validate();
			}
		};
		txtName.addModifyListener(nameAndInterfaceModifyListener);
		txtInterface.addModifyListener(nameAndInterfaceModifyListener);
		txtBind.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				serviceReference.setBind(emptyToNull(txtBind.getText()));
				validate();
			}
		});
		txtUnbind.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				serviceReference.setUnbind(emptyToNull(txtUnbind.getText()));
				validate();
			}
		});
		btnOptional.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				serviceReference.setOptional(btnOptional.getSelection());
				validate();
			}
		});
		btnMultiple.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				serviceReference.setMultiple(btnMultiple.getSelection());
				validate();
			}
		});
		btnDynamic.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				serviceReference.setDynamic(btnDynamic.getSelection());
				validate();
			}
		});
		txtTargetFilter.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				serviceReference.setTargetFilter(emptyToNull(txtTargetFilter.getText()));
			}
		});

		// Layout
		GridLayout layout;
		
		layout = new GridLayout(4, false);
		composite.setLayout(layout);
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		txtInterface.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		txtBind.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		txtUnbind.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		txtTargetFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		
		pnlButtons.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false, 3, 1));
		layout = new GridLayout(1, true);
//		layout.horizontalSpacing = 0;
//		layout.verticalSpacing = 0;
//		layout.marginHeight = 0;
//		layout.marginWidth = 0;
		pnlButtons.setLayout(layout);
		
		setControl(composite);
	}
	static String nullCheck(String string, String defaultValue) {
		return string != null ? string : defaultValue;
	}
	static String emptyToNull(String string) {
		if(string == null || string.length() == 0)
			return null;
		return string;
	}
	void initialiseFields() {
		txtName.setText(nullCheck(serviceReference.getName(), ""));
		txtInterface.setText(nullCheck(serviceReference.getServiceClass(), ""));
		txtBind.setText(nullCheck(serviceReference.getBind(), ""));
		txtUnbind.setText(nullCheck(serviceReference.getUnbind(), ""));
		
		btnOptional.setSelection(serviceReference.isOptional());
		btnMultiple.setSelection(serviceReference.isMultiple());
		btnDynamic.setSelection(serviceReference.isDynamic());
		
		txtTargetFilter.setText(nullCheck(serviceReference.getTargetFilter(), ""));
	}
	void validate() {
		String error = null;
		String warning = null;
		
		// Interface must not be null
		if(serviceReference.getServiceClass() == null)
			error = "A service interface name must be specified.";
		
		// Name must not already be used
		else if(existingNames.contains(serviceReference.getName())) {
			error = String.format("A reference with the name \"%s\" already exists for this component.", serviceReference.getName());
		}
		
		// Multiple/optional references SHOULD be dynamic
		if((serviceReference.isMultiple() || serviceReference.isOptional()) && !serviceReference.isDynamic()) {
			warning = "It is recommended to make optional or multiple references dynamic.";
		}
		
		// Unbind without bind is pointless
		else if(serviceReference.getBind() == null && serviceReference.getUnbind() != null) {
			warning = "Unbind cannot be set when bind is not set.";
		}
		
		setErrorMessage(error);
		setMessage(warning, IMessageProvider.WARNING);
		
		setPageComplete(error == null);
		getContainer().updateButtons();
	}
}
