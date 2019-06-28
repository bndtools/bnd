package org.bndtools.utils.dnd;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.part.ResourceTransfer;

public class AbstractViewerDropAdapter extends ViewerDropAdapter {

	private final EnumSet<SupportedTransfer> supportedTransfers;

	public AbstractViewerDropAdapter(Viewer viewer, EnumSet<SupportedTransfer> supportedTransfers) {
		super(viewer);
		this.supportedTransfers = supportedTransfers;
	}

	public void install(StructuredViewer viewer) {
		List<Transfer> transfers = new ArrayList<>();

		if (supportedTransfers.contains(SupportedTransfer.File))
			transfers.add(FileTransfer.getInstance());
		if (supportedTransfers.contains(SupportedTransfer.Resource))
			transfers.add(ResourceTransfer.getInstance());
		if (supportedTransfers.contains(SupportedTransfer.Text))
			transfers.add(TextTransfer.getInstance());
		if (supportedTransfers.contains(SupportedTransfer.LocalSelection))
			transfers.add(LocalSelectionTransfer.getTransfer());

		viewer.addDropSupport(DND.DROP_COPY | DND.DROP_MOVE, transfers.toArray(new Transfer[0]), this);
	}

	// Turn all move events into copy events
	@Override
	public void dragEnter(DropTargetEvent event) {
		super.dragEnter(event);
		event.detail = DND.DROP_COPY;
	}

	@Override
	public boolean performDrop(Object data) {
		Object target = getCurrentTarget();
		int location = getCurrentLocation();

		boolean result = false;
		if (data instanceof String) {
			result = performTextDrop((String) data, target, location);
		} else if (data instanceof String[]) {
			result = performFileDrop((String[]) data, target, location);
		} else if (data instanceof IResource[]) {
			result = performResourceDrop((IResource[]) data, target, location);
		} else if (data instanceof ISelection) {
			result = performSelectionDrop((ISelection) data, target, location);
		}
		return result;
	}

	@SuppressWarnings("unused")
	protected boolean performTextDrop(String data, Object target, int location) {
		return false;
	}

	@SuppressWarnings("unused")
	protected boolean performFileDrop(String[] data, Object target, int location) {
		return false;
	}

	@SuppressWarnings("unused")
	protected boolean performResourceDrop(IResource[] data, Object target, int location) {
		return false;
	}

	@SuppressWarnings("unused")
	protected boolean performSelectionDrop(ISelection data, Object target, int location) {
		return false;
	}

	@Override
	public boolean validateDrop(Object target, int operation, TransferData transferType) {
		if (supportedTransfers.contains(SupportedTransfer.File) && FileTransfer.getInstance()
			.isSupportedType(transferType)) {
			return validateFileDrop(target);
		} else if (supportedTransfers.contains(SupportedTransfer.Resource) && ResourceTransfer.getInstance()
			.isSupportedType(transferType)) {
			return validateResourceDrop(target);
		} else if (supportedTransfers.contains(SupportedTransfer.Text) && TextTransfer.getInstance()
			.isSupportedType(transferType)) {
			return validateTextDrop(target);
		} else if (supportedTransfers.contains(SupportedTransfer.LocalSelection) && LocalSelectionTransfer.getTransfer()
			.isSupportedType(transferType)) {
			return validateLocalSelectionDrop(target);
		} else {
			return false;
		}
	}

	@SuppressWarnings("unused")
	protected boolean validateFileDrop(Object target) {
		return true;
	}

	@SuppressWarnings("unused")
	protected boolean validateResourceDrop(Object target) {
		return true;
	}

	@SuppressWarnings("unused")
	protected boolean validateTextDrop(Object target) {
		return true;
	}

	@SuppressWarnings("unused")
	protected boolean validateLocalSelectionDrop(Object target) {
		return true;
	}

}
