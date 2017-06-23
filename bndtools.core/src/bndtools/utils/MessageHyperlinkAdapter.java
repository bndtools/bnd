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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.forms.IMessage;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.Hyperlink;

public class MessageHyperlinkAdapter implements IHyperlinkListener {

    private PopupDialog popupDialog = null;
    private final IWorkbenchPart part;

    public MessageHyperlinkAdapter(IWorkbenchPart part) {
        this.part = part;
    }

    @Override
    public void linkActivated(HyperlinkEvent e) {
        showPopup(e);
    }

    @Override
    public void linkEntered(final HyperlinkEvent e) {}

    @Override
    public void linkExited(HyperlinkEvent e) {}

    private void showPopup(final HyperlinkEvent e) {
        Hyperlink link = (Hyperlink) e.getSource();
        link.setToolTipText(null);

        if (popupDialog != null)
            popupDialog.close();

        IMessage[] messages = (IMessage[]) e.data;

        if (messages == null || messages.length == 0) {
            MessageDialog.openInformation(part.getSite().getShell(), part.getTitle(), "No further information available.");
        } else {
            popupDialog = new MessagesPopupDialog(link, (IMessage[]) e.data, part);
            popupDialog.open();
        }
    }
}