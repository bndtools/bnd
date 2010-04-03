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

import java.text.MessageFormat;


import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import bndtools.UIConstants;
import bndtools.javamodel.IJavaSearchContext;
import bndtools.utils.JavaContentProposalLabelProvider;

public class SvcInterfaceSelectionDialog extends InputDialog {
	
	private final IJavaSearchContext searchContext;
	
	public SvcInterfaceSelectionDialog(Shell parentShell, String dialogTitle, String dialogMessage, IJavaSearchContext searchContext) {
		super(parentShell, dialogTitle, dialogMessage, "", null);
		this.searchContext = searchContext;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Control dialogArea = super.createDialogArea(parent);
		
		FieldDecoration proposalDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL);

		KeyStroke assistKeyStroke = null;
		try {
			assistKeyStroke = KeyStroke.getInstance("Ctrl+Space");
		} catch (ParseException x) {
			// Ignore
		}
		
		Text textField = getText();
		ControlDecoration decor = new ControlDecoration(textField, SWT.LEFT | SWT.TOP);
		decor.setImage(proposalDecoration.getImage());
		decor.setDescriptionText(MessageFormat.format("Content Assist is available. Press {0} or start typing to activate", assistKeyStroke.format()));
		decor.setShowHover(true);
		decor.setShowOnlyOnFocus(true);
		
		SvcInterfaceProposalProvider proposalProvider = new SvcInterfaceProposalProvider(searchContext);
		ContentProposalAdapter proposalAdapter = new ContentProposalAdapter(textField, new TextContentAdapter(), proposalProvider, assistKeyStroke, UIConstants.AUTO_ACTIVATION_CLASSNAME);
		proposalAdapter.addContentProposalListener(proposalProvider);
		proposalAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
		proposalAdapter.setLabelProvider(new JavaContentProposalLabelProvider());
		proposalAdapter.setAutoActivationDelay(1500);
		
		return dialogArea;
	}
}
