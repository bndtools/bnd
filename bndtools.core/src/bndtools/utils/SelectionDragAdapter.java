package bndtools.utils;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;

public class SelectionDragAdapter implements DragSourceListener {

	private final Viewer viewer;

	public SelectionDragAdapter(Viewer viewer) {
		this.viewer = viewer;
	}

	@Override
	public void dragStart(DragSourceEvent event) {
		ISelection selection = viewer.getSelection();
		LocalSelectionTransfer.getTransfer()
			.setSelection(selection);
		LocalSelectionTransfer.getTransfer()
			.setSelectionSetTime(event.time & 0xFFFFFFFFL);
		event.doit = !selection.isEmpty();
	}

	@Override
	public void dragSetData(DragSourceEvent event) {
		// For consistency set the data to the selection even though
		// the selection is provided by the LocalSelectionTransfer
		// to the drop target adapter.
		event.data = LocalSelectionTransfer.getTransfer()
			.getSelection();
	}

	@Override
	public void dragFinished(DragSourceEvent event) {
		LocalSelectionTransfer.getTransfer()
			.setSelection(null);
		LocalSelectionTransfer.getTransfer()
			.setSelectionSetTime(0);
	}
}
