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
package bndtools.editor.imports;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicInteger;


import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;

import bndtools.editor.model.BndEditModel;
import bndtools.editor.model.LowerVersionMatchType;
import bndtools.editor.model.UpperVersionMatchType;
import bndtools.editor.model.VersionPolicy;

import aQute.lib.osgi.Constants;

public class VersionPolicyPart extends SectionPart implements
		PropertyChangeListener {
	
	private BndEditModel model;
	private AtomicInteger refreshers = new AtomicInteger(0);
	
	private Composite mainComposite;
	private StackLayout stack;
	private Composite editComposite;
	private Composite errorComposite;
	
	private Composite groupFrom;
	private Button btnCustomize;
	private Button btnFromExact;
	private Button btnFromMicro;
	private Button btnFromMinor;
	private Button btnFromMajor;
	
	private Composite groupTo;
	private Button btnUpperInclusive;
	private Button btnToUnspecified;
	private Button btnToExact;
	private Button btnToMicro;
	private Button btnToMinor;
	private Button btnToMajor;
	
	
	public VersionPolicyPart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style);
		createSection(getSection(), toolkit);
	}

	void createSection(Section section, FormToolkit toolkit) {
		section.setText("Import Version Policy");
		
		mainComposite = toolkit.createComposite(section);
		section.setClient(mainComposite);
		
		stack = new StackLayout();
		mainComposite.setLayout(stack);
		editComposite = toolkit.createComposite(mainComposite);
		errorComposite = toolkit.createComposite(mainComposite);
		
		btnCustomize = toolkit.createButton(editComposite, "Customise the version policy", SWT.CHECK);
		
		groupFrom = toolkit.createComposite(editComposite, SWT.BORDER);
		toolkit.createLabel(groupFrom, "Lower Bound:");
		btnFromExact = toolkit.createButton(groupFrom, "Exact (e.g. 1.2.3.qual)", SWT.RADIO);
		btnFromMicro = toolkit.createButton(groupFrom, "Micro (e.g. 1.2.3)", SWT.RADIO);
		btnFromMinor = toolkit.createButton(groupFrom, "Minor (e.g. 1.2)", SWT.RADIO);
		btnFromMajor = toolkit.createButton(groupFrom, "Major (e.g. 1)", SWT.RADIO);

		groupTo = toolkit.createComposite(editComposite, SWT.BORDER);
		toolkit.createLabel(groupTo, "Upper Bound:");
		btnToUnspecified = toolkit.createButton(groupTo, "Unspecified", SWT.RADIO);
		btnToExact = toolkit.createButton(groupTo, "Exact (e.g. 1.2.3.qual)", SWT.RADIO);
		btnToMicro = toolkit.createButton(groupTo, "Next Micro (e.g. 1.2.4)", SWT.RADIO);
		btnToMinor = toolkit.createButton(groupTo, "Next Minor (e.g. 1.3)", SWT.RADIO);
		btnToMajor = toolkit.createButton(groupTo, "Next Major (e.g. 2)", SWT.RADIO);
		btnUpperInclusive = toolkit.createButton(groupTo, "Inclusive", SWT.CHECK);
		
		Label lblUnparseable = toolkit.createLabel(errorComposite, "Unable to parse the version policy specified in the text. It is either corrupted, or too complex for a GUI representation.", SWT.WRAP);
		Hyperlink linkUnparseable = toolkit.createHyperlink(errorComposite, "Override the text representation.", SWT.WRAP);
		
		// Listeners
		btnCustomize.addSelectionListener(new SelectionAdapter() {
			@Override
            public void widgetSelected(SelectionEvent ev) {
				if(btnCustomize.getSelection()) {
					VersionPolicy policy = createDefaultVersionPolicy();
					updatePolicyButtons(policy);
					updateEnablement();
				} else {
					updatePolicyButtons(null);
					updateEnablement();
				}
				markDirty();
			};
		});
		SelectionAdapter markDirtyListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				markDirty();
			}
		};
		btnFromExact.addSelectionListener(markDirtyListener);
		btnFromMicro.addSelectionListener(markDirtyListener);
		btnFromMinor.addSelectionListener(markDirtyListener);
		btnFromMajor.addSelectionListener(markDirtyListener);
		btnUpperInclusive.addSelectionListener(markDirtyListener);
		btnToUnspecified.addSelectionListener(markDirtyListener);
		btnToExact.addSelectionListener(markDirtyListener);
		btnToMicro.addSelectionListener(markDirtyListener);
		btnToMinor.addSelectionListener(markDirtyListener);
		btnToMajor.addSelectionListener(markDirtyListener);
		linkUnparseable.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
            public void linkActivated(HyperlinkEvent e) {
				btnCustomize.setSelection(true);
				VersionPolicy policy = createDefaultVersionPolicy();
				updatePolicyButtons(policy);
				updateEnablement();
				markDirty();
				stack.topControl = editComposite;
				mainComposite.layout();
			}
		});
		 
		// Layout
		GridLayout layout;
		GridData gd;
		
		mainComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		layout = new GridLayout(2, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		editComposite.setLayout(layout);
		btnCustomize.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1));
		groupFrom.setLayoutData(new GridData(GridData.FILL_BOTH));
		groupFrom.setLayout(new GridLayout(1, false));
		
		groupTo.setLayoutData(new GridData(GridData.FILL_BOTH));
		groupTo.setLayout(new GridLayout(2, false));
		btnToUnspecified.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1));
		btnToExact.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1));
		btnToMicro.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1));
		btnToMinor.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1));
		btnToMajor.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1));
		
		errorComposite.setLayout(new GridLayout(1, false));
		gd = new GridData(SWT.LEFT, SWT.TOP, false, false);
		gd.widthHint = 300;
		lblUnparseable.setLayoutData(gd);
	}
	
	@Override
	public void initialize(IManagedForm form) {
		super.initialize(form);
		model = (BndEditModel) form.getInput();
		model.addPropertyChangeListener(Constants.VERSIONPOLICY, this);
	}
	@Override
	public void dispose() {
		super.dispose();
		model.removePropertyChangeListener(Constants.VERSIONPOLICY, this);
	}
	
	@Override
	public void refresh() {
		super.refresh();
		try {
			refreshers.incrementAndGet();
			VersionPolicy policy = model.getVersionPolicy();
			
			btnCustomize.setSelection(policy == null);
			updatePolicyButtons(policy);
			updateEnablement();
			
			stack.topControl = editComposite;
			mainComposite.layout();
		} catch (IllegalArgumentException e) {
			stack.topControl = errorComposite;
			mainComposite.layout();
		} finally {
			refreshers.decrementAndGet();
		}
	}
	
	void updatePolicyButtons(VersionPolicy policy) {
		LowerVersionMatchType lowerMatch = policy != null ? policy.getLowerMatch() : LowerVersionMatchType.Minor;
		btnFromExact.setSelection(lowerMatch == LowerVersionMatchType.Exact);
		btnFromMicro.setSelection(lowerMatch == LowerVersionMatchType.Micro);
		btnFromMinor.setSelection(lowerMatch == LowerVersionMatchType.Minor);
		btnFromMajor.setSelection(lowerMatch == LowerVersionMatchType.Major);
		
		UpperVersionMatchType upperMatch = policy != null ? policy.getUpperMatch() : null;
		btnUpperInclusive.setSelection(policy != null && policy.isUpperInclusive());
		btnToUnspecified.setSelection(upperMatch == null);
		btnToExact.setSelection(upperMatch == UpperVersionMatchType.Exact);
		btnToMicro.setSelection(upperMatch == UpperVersionMatchType.NextMicro);
		btnToMinor.setSelection(upperMatch == UpperVersionMatchType.NextMinor);
		btnToMajor.setSelection(upperMatch == UpperVersionMatchType.NextMajor);
		btnCustomize.setSelection(policy != null);
	}
	
	VersionPolicy createDefaultVersionPolicy() {
		return new VersionPolicy(LowerVersionMatchType.Minor, UpperVersionMatchType.NextMajor, false);
	}
	
	@Override
	public void commit(boolean onSave) {
		super.commit(onSave);
		try {
			model.removePropertyChangeListener(Constants.VERSIONPOLICY, this);
			VersionPolicy policy;
			if(!btnCustomize.getSelection()) {
				policy = null;
			} else {
				LowerVersionMatchType lower = LowerVersionMatchType.Minor;
				if(btnFromExact.getSelection())
					lower = LowerVersionMatchType.Exact;
				else if(btnFromMicro.getSelection())
					lower = LowerVersionMatchType.Micro;
				else if(btnFromMinor.getSelection())
					lower = LowerVersionMatchType.Minor;
				else if(btnFromMajor.getSelection())
					lower = LowerVersionMatchType.Major;
				
				UpperVersionMatchType upper = UpperVersionMatchType.NextMajor;
				if(btnToUnspecified.getSelection())
					upper = null;
				else if(btnToMicro.getSelection())
					upper = UpperVersionMatchType.NextMicro;
				else if(btnToMinor.getSelection())
					upper = UpperVersionMatchType.NextMinor;
				else if(btnToMajor.getSelection())
					upper = UpperVersionMatchType.NextMajor;
				
				policy = new VersionPolicy(lower, upper, btnUpperInclusive.getSelection());
			}
			model.setVersionPolicy(policy);
		} finally {
			model.addPropertyChangeListener(Constants.VERSIONPOLICY, this);
		}
	}
	
	void updateEnablement() {
		Control[] children;
		
		boolean enable = btnCustomize.getSelection();
		groupFrom.setEnabled(enable);
		children = groupFrom.getChildren();
		for (Control child : children) {
			child.setEnabled(enable);
		}
		groupTo.setEnabled(enable);
		children = groupTo.getChildren();
		for (Control child : children) {
			child.setEnabled(enable);
		}
	}

	@Override
	public void markDirty() {
		if(refreshers.get() == 0)
			super.markDirty();
	}

	public void propertyChange(PropertyChangeEvent evt) {
		if(Constants.VERSIONPOLICY.equals(evt.getPropertyName())) {
			IFormPage page = (IFormPage) getManagedForm().getContainer();
			if(page.isActive()) {
				refresh();
			} else {
				markStale();
			}
		}
	}

}
