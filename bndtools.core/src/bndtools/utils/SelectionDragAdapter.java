package bndtools.utils;

import java.util.Iterator;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.TextTransfer;

import bndtools.model.repo.RepositoryBundle;
import bndtools.model.repo.RepositoryBundleVersion;
import bndtools.model.repo.RepositoryFeature;
import bndtools.model.repo.RepositoryResourceElement;

public class SelectionDragAdapter implements DragSourceListener {

	private final LocalSelectionTransfer	selectionTransfer	= LocalSelectionTransfer.getTransfer();
	private final TextTransfer				textTransfer		= TextTransfer.getInstance();

	private final Viewer					viewer;

	public SelectionDragAdapter(Viewer viewer) {
		this.viewer = viewer;
	}

	@Override
	public void dragStart(DragSourceEvent event) {
		ISelection selection = viewer.getSelection();
		selectionTransfer.setSelection(selection);
		selectionTransfer.setSelectionSetTime(event.time & 0xFFFFFFFFL);
		event.doit = !selection.isEmpty();
	}

	@Override
	public void dragSetData(DragSourceEvent event) {
		if (textTransfer.isSupportedType(event.dataType)) {
			ISelection selection = selectionTransfer.getSelection();
			Iterator<?> iterator = ((IStructuredSelection) selection).iterator();
			while (iterator.hasNext()) {
				Object item = iterator.next();
				if (item instanceof RepositoryBundle) {
					RepositoryBundle rb = (RepositoryBundle) item;
					event.data = rb.getResource()
						.toString();
					break;
				} else if (item instanceof RepositoryBundleVersion) {
					RepositoryBundleVersion rbv = (RepositoryBundleVersion) item;
					event.data = rbv.getResource()
						.toString();
					break;
				} else if (item instanceof RepositoryResourceElement) {
					RepositoryResourceElement rbe = (RepositoryResourceElement) item;
					event.data = rbe.getResource()
						.toString();
					break;
				} else if (item instanceof RepositoryFeature) {
					RepositoryFeature rf = (RepositoryFeature) item;
					// Create drag data as "feature:id:version" string
					event.data = "feature:" + rf.getFeature()
						.getId() + ":" + rf.getFeature()
							.getVersion();
					break;
				}
			}
			return;
		}
		// For consistency set the data to the selection even though
		// the selection is provided by the LocalSelectionTransfer
		// to the drop target adapter.
		event.data = selectionTransfer.getSelection();
	}

	@Override
	public void dragFinished(DragSourceEvent event) {
		selectionTransfer.setSelection(null);
		selectionTransfer.setSelectionSetTime(0);
	}
}
