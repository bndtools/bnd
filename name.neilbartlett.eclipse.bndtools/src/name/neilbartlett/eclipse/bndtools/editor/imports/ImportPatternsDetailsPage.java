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
package name.neilbartlett.eclipse.bndtools.editor.imports;

import name.neilbartlett.eclipse.bndtools.editor.model.HeaderClause;
import name.neilbartlett.eclipse.bndtools.editor.pkgpatterns.PkgPatternsDetailsPage;
import name.neilbartlett.eclipse.bndtools.editor.pkgpatterns.PkgPatternsListPart;
import name.neilbartlett.eclipse.bndtools.utils.ModificationLock;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.osgi.framework.Constants;

public class ImportPatternsDetailsPage extends PkgPatternsDetailsPage {
	
	private final PkgPatternsListPart listPart;
	
	private final ModificationLock modifyLock = new ModificationLock();
	
	private Button btnOptional;
	
	public ImportPatternsDetailsPage(PkgPatternsListPart listPart) {
		super(listPart, "Import Pattern Details");
		this.listPart = listPart;
	}
	
	@Override
	public void createContents(Composite parent) {
		super.createContents(parent);
		
		Composite mainComposite = getMainComposite();
		
		FormToolkit toolkit = getManagedForm().getToolkit();
		toolkit.createLabel(mainComposite, ""); // Spacer
		btnOptional = toolkit.createButton(mainComposite, "Optional Resolution", SWT.CHECK);
		
		btnOptional.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(!modifyLock.isUnderModification()) {
					String resolution = btnOptional.getSelection() ? Constants.RESOLUTION_OPTIONAL : null;
					for (HeaderClause clause : selectedClauses) {
						clause.getAttribs().put(aQute.lib.osgi.Constants.RESOLUTION_DIRECTIVE, resolution);
					}
					listPart.updateLabels(selectedClauses);
					listPart.validate();
					markDirty();
				}
			}
		});
	}
	@Override
	public void refresh() {
		super.refresh();
		modifyLock.modifyOperation(new Runnable() {
			public void run() {
				if(selectedClauses.length == 0) {
					btnOptional.setEnabled(false);
					btnOptional.setGrayed(true);
				} else if(selectedClauses.length == 1) {
					btnOptional.setEnabled(true);
					btnOptional.setGrayed(false);
					btnOptional.setSelection(isOptional(selectedClauses[0]));
				} else {
					btnOptional.setEnabled(true);
					
					boolean differs = false;
					boolean first = isOptional(selectedClauses[0]);
					for(int i = 1; i < selectedClauses.length; i++) {
						if(first != isOptional(selectedClauses[i])) {
							differs = true;
							break;
						}
					}
					if(differs) {
						btnOptional.setGrayed(true);
					} else {
						btnOptional.setSelection(first);
					}
				}
			}
		});
	}
	private static boolean isOptional(HeaderClause clause) {
		String resolution = clause.getAttribs().get(aQute.lib.osgi.Constants.RESOLUTION_DIRECTIVE);
		return Constants.RESOLUTION_OPTIONAL.equals(resolution);
	}
}
