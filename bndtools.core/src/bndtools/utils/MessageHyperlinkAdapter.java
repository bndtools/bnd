package bndtools.utils;

import java.util.Objects;
import java.util.stream.Stream;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.forms.IMessage;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.Hyperlink;

public class MessageHyperlinkAdapter implements IHyperlinkListener {

	private PopupDialog				popupDialog	= null;
	private final IWorkbenchPart	part;

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

		if (messages == null) {
			messages = new IMessage[0];
		} else {
			messages = Stream.of(messages)
				.filter(Objects::nonNull)
				.toArray(IMessage[]::new);
		}

		if (messages.length == 0) {
			MessageDialog.openInformation(part.getSite()
				.getShell(), part.getTitle(), "No further information available.");
		} else {
			popupDialog = new MessagesPopupDialog(link, (IMessage[]) e.data, part);
			popupDialog.open();
		}
	}

}
