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
package bndtools.utils;



import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.forms.HyperlinkGroup;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.IMessage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;

public class MessagesPopupDialog extends PopupDialog {
	
	private final Control controlAttachment;
	private final IMessage[] messages;
	private final IWorkbenchPart part;
	private final HyperlinkGroup hyperlinkGroup;
	
	private final Image bulletImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/bullet_go.png").createImage();
	
	public MessagesPopupDialog(Control controlAttachment, IMessage[] messages, IWorkbenchPart part) {
		super(null, PopupDialog.INFOPOPUP_SHELLSTYLE, true, false, false, false, false, null, null);
		this.controlAttachment = controlAttachment;
		this.messages = messages;
		this.part = part;
		
		this.hyperlinkGroup = new HyperlinkGroup(controlAttachment.getDisplay());
		this.hyperlinkGroup.setHyperlinkUnderlineMode(HyperlinkSettings.UNDERLINE_ALWAYS);
		
	}
	@Override
	public boolean close() {
		boolean result = super.close();
		bulletImg.dispose();
		return result;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		composite.setLayout(new GridLayout(1, false));
		
		for(int i = 0; i < messages.length; i++) {
			if(i > 0) {
				Label separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
				separator.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 2, 1));
			}
			
			// Message Type Image Label
			Composite pnlTitle = new Composite(composite, SWT.NONE);
			pnlTitle.setLayout(new GridLayout(2, false));
			Label lblImage = new Label(pnlTitle, SWT.NONE);
			lblImage.setImage(getMessageImage(messages[i].getMessageType()));
			
			// Message Label
			StringBuilder builder = new StringBuilder();
			if(messages[i].getPrefix() != null) {
				builder.append(messages[i].getPrefix());
			}
			builder.append(messages[i].getMessage());
			Label lblText = new Label(pnlTitle, SWT.WRAP);
			lblText.setText(builder.toString());
			lblText.setFont(JFaceResources.getFontRegistry().getItalic(JFaceResources.DIALOG_FONT));
			lblText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			
			// Fix actions, if present
			Object data = messages[i].getData();
			IAction[] fixes;
			if(data instanceof IAction) {
				fixes = new IAction[] { (IAction) data };
			} else if(data instanceof IAction[]) {
				fixes = (IAction[]) data;
			} else {
				fixes = null;
			}
			
			if(fixes != null) {
				//new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
				Composite pnlFixes = new Composite(composite, SWT.NONE);
				pnlFixes.setLayout(new GridLayout(3, false));
				
				Label lblFixes = new Label(pnlFixes, SWT.NONE);
				lblFixes.setText("Available Fixes:");
				lblFixes.setForeground(JFaceResources.getColorRegistry().get(JFacePreferences.QUALIFIER_COLOR));
				
				for(int j = 0; j < fixes.length; j++) {
					if(j > 0) new Label(pnlFixes, SWT.NONE); // Spacer
					
					new Label(pnlFixes, SWT.NONE).setImage(bulletImg);
					
					final IAction fix = fixes[j];
					Hyperlink fixLink = new Hyperlink(pnlFixes, SWT.NONE);
					hyperlinkGroup.add(fixLink);
					fixLink.setText(fix.getText());
					fixLink.setHref(fix);
					fixLink.addHyperlinkListener(new HyperlinkAdapter() {
					    @Override
						public void linkActivated(HyperlinkEvent e) {
							fix.run();
							close();
//							part.getSite().getPage().activate(part);
							part.setFocus();
						};
					});
					fixLink.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
				}
			}
		}
		
		return composite;
	}
	@Override
	protected Point getInitialSize() {
		return super.getInitialSize();
	}
	
	@Override
	protected Point getInitialLocation(Point initialSize) {
		Rectangle linkBounds = controlAttachment.getBounds();
		linkBounds = controlAttachment.getDisplay().map(controlAttachment.getParent(), null, linkBounds);
		return new Point(linkBounds.x, linkBounds.y + linkBounds.height);
	}
	
	Image getMessageImage(int messageType) {
		switch (messageType) {
		case IMessageProvider.INFORMATION:
			return JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_INFO);
		case IMessageProvider.WARNING:
			return JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_WARNING);
		case IMessageProvider.ERROR:
			return JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_ERROR);
		default:
			return null;
		}
	}

}
