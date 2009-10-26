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
package name.neilbartlett.eclipse.jareditor.internal;

import java.util.jar.JarEntry;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.DetailsPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IDetailsPageProvider;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.MasterDetailsBlock;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class JARContentMasterDetailsBlock extends MasterDetailsBlock {

	private JARContentTreePart contentTreePart;
	private JARContentDetailsPage jarContentDetailsPage;
	
	private Object input = null;

	protected void createMasterPart(IManagedForm managedForm, Composite parent) {
		FormToolkit toolkit = managedForm.getToolkit();
		Composite container = toolkit.createComposite(parent);
		container.setLayout(new GridLayout(1, false));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Section section = toolkit.createSection(container, Section.TITLE_BAR | Section.EXPANDED);
		contentTreePart = new JARContentTreePart(section, managedForm);
		contentTreePart.setFormInput(input);
		managedForm.addPart(contentTreePart);
		
		section.setLayoutData(new GridData(GridData.FILL_BOTH));
	}

	protected void createToolBarActions(IManagedForm managedForm) {
		final ScrolledForm form = managedForm.getForm();
		Action haction = new Action("hor", Action.AS_RADIO_BUTTON) { //$NON-NLS-1$
			public void run() {
				sashForm.setOrientation(SWT.HORIZONTAL);
				form.reflow(true);
			}
		};
		haction.setChecked(true);
		haction.setToolTipText("Horizontal orientation");
		haction.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Constants.PLUGIN_ID, "/icons/th_horizontal.gif"));
		Action vaction = new Action("ver", Action.AS_RADIO_BUTTON) { //$NON-NLS-1$
			public void run() {
				sashForm.setOrientation(SWT.VERTICAL);
				form.reflow(true);
			}
		};
		vaction.setChecked(false);
		vaction.setToolTipText("Vertical orientation");
		vaction.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Constants.PLUGIN_ID, "/icons/th_vertical.gif"));
		form.getToolBarManager().add(haction);
		form.getToolBarManager().add(vaction);
	}

	protected void registerPages(DetailsPart detailsPart) {
		detailsPart.setPageProvider(new IDetailsPageProvider() {
			public Object getPageKey(Object object) {
				if(object instanceof JarEntry)
					return JarEntry.class;
				
				return object.getClass();
			}
			public IDetailsPage getPage(Object key) {
				return null;
			}
		});
		
		jarContentDetailsPage = new JARContentDetailsPage();
		jarContentDetailsPage.setFormInput(input);
		
		detailsPart.registerPage(JarEntry.class, jarContentDetailsPage);
		detailsPart.registerPage(ZipTreeNode.class, jarContentDetailsPage);
	}

	public void setMasterPartInput(Object input) {
		this.input = input;
		if(contentTreePart != null && !contentTreePart.getSection().isDisposed())
			contentTreePart.setFormInput(input);
		if(jarContentDetailsPage != null)
			jarContentDetailsPage.setFormInput(input);
	}

}
