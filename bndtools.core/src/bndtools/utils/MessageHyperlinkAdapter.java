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

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.ui.forms.IMessage;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.Hyperlink;

public class MessageHyperlinkAdapter implements IHyperlinkListener {
	
	private PopupDialog popupDialog = null;

	public void linkActivated(HyperlinkEvent e) {
		showPopup(e);
	}

	public void linkEntered(final HyperlinkEvent e) {
		/*
		// Ignore keyboard focus, only show on mouse-over
		Hyperlink hyperlink = (Hyperlink) e.getSource();
		
		// Calculate location of the hyperlink relative to the Display
		Rectangle linkBounds = hyperlink.getBounds();
		linkBounds = hyperlink.getDisplay().map(hyperlink.getParent(), null, linkBounds);
		
		// Check if the mouse is inside the hyperlink
		Point mouse = hyperlink.getDisplay().getCursorLocation();
		if(linkBounds.contains(mouse)) {
			showPopup(e);
		}
		*/
	}
	
	private void showPopup(final HyperlinkEvent e) {
		Hyperlink link = (Hyperlink) e.getSource();
		link.setToolTipText(null);
		
		if(popupDialog != null) 
			popupDialog.close();
		popupDialog = new MessagesPopupDialog(link, (IMessage[]) e.data);
		popupDialog.open();
	}

	public void linkExited(HyperlinkEvent e) {
	}
}
