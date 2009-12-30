/*******************************************************************************
 * Copyright (c) 2009 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 ******************************************************************************/
package name.neilbartlett.eclipse.bndtools.editor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.osgi.framework.Constants;

public class GeneralInfoSectionPart extends SectionPart implements PropertyChangeListener {

	private BndEditModel model;
	
	private Text txtBSN;
	private Text txtVersion;
	
	private AtomicBoolean inRefresh = new AtomicBoolean(false);

	public GeneralInfoSectionPart(Composite parent, FormToolkit toolkit) {
		super(parent, toolkit, ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED);
		createSection(getSection(), toolkit);
	}

	private void createSection(Section section, FormToolkit toolkit) {
		section.setText("General Information");
		
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		
		toolkit.createLabel(composite, Constants.BUNDLE_SYMBOLICNAME);
		txtBSN = toolkit.createText(composite, "");
		
		toolkit.createLabel(composite, Constants.BUNDLE_VERSION);
		txtVersion = toolkit.createText(composite, "");
		
		// Listeners
		Listener markDirtyListener = new Listener() {
			public void handleEvent(Event event) {
				if(!inRefresh.get())
					markDirty();
			}
		};
		txtBSN.addListener(SWT.Modify, markDirtyListener);
		txtVersion.addListener(SWT.Modify, markDirtyListener);
		
		// Layout
		GridData gd;
		
		composite.setLayout(new GridLayout(2, false));
		gd = new GridData(SWT.FILL, SWT.TOP, true, false);
		gd.widthHint = 100;
		txtBSN.setLayoutData(gd);

		gd = new GridData(SWT.FILL, SWT.TOP, true, false);
		gd.widthHint = 100;
		txtVersion.setLayoutData(gd);
	}
	
	@Override
	public void commit(boolean onSave) {
		// TODO Auto-generated method stub
		super.commit(onSave);
		
		String bsn = txtBSN.getText();
		if(bsn != null && bsn.length() == 0) bsn = null;
		model.setBundleSymbolicName(bsn);
		
		String version = txtVersion.getText();
		if(version != null && version.length() == 0) version = null;
		model.setBundleVersion(version);
	}
	
	@Override
	public void refresh() {
		super.refresh();
		
		if(inRefresh.compareAndSet(false, true)) {
			try {
				
				String bsn = model.getBundleSymbolicName();
				txtBSN.setText(bsn != null ? bsn : ""); //$NON-NLS-1$
				
				String bundleVersion = model.getBundleVersionString();
				txtVersion.setText(bundleVersion != null ? bundleVersion : ""); //$NON-NLS-1$
			} finally {
				inRefresh.set(false);
			}
		}
	}
	
	@Override
	public void initialize(IManagedForm form) {
		super.initialize(form);
		this.model = (BndEditModel) form.getInput();
		model.addPropertyChangeListener(this);
	}
	
	@Override
	public boolean setFormInput(Object input) {
		super.setFormInput(input);
		
		if(this.model != null) {
			this.model.removePropertyChangeListener(this);
		}
		this.model = (BndEditModel) input;
		this.model.addPropertyChangeListener(this);
		return false;
	}

	public void propertyChange(PropertyChangeEvent evt) {
		markStale();
	}
	
	@Override
	public void dispose() {
		super.dispose();
		if(this.model != null)
			this.model.removePropertyChangeListener(this);
	}
}
