package bndtools.dnd.gav;

import static org.eclipse.swt.dnd.DND.DROP_COPY;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.dnd.IDragAndDropService;
import org.eclipse.ui.part.EditorPart;

import aQute.bnd.exceptions.Exceptions;

public class GAVIPageListener implements IPartListener {

	private final Transfer[] transfers = new Transfer[] {
		TextTransfer.getInstance()
	};

	@Override
	public void partOpened(IWorkbenchPart part) {
		IDragAndDropService dndService = part.getSite()
			.getService(IDragAndDropService.class);

		if (dndService == null) {
			return;
		}

		Control control = part.getAdapter(Control.class);

		if ((control == null) || !(control instanceof StyledText)) {
			return;
		}

		IPath file = null;

		if (part instanceof EditorPart) {
			EditorPart editorPart = (EditorPart) part;

			IEditorInput editorInput = editorPart.getEditorInput();

			if (editorInput instanceof IFileEditorInput) {
				IFileEditorInput input = (IFileEditorInput) editorInput;

				file = input.getFile()
					.getFullPath();
			} else if (editorInput instanceof IStorageEditorInput) {
				IStorageEditorInput input = (IStorageEditorInput) editorInput;

				try {
					file = input.getStorage()
						.getFullPath();
				} catch (CoreException e) {
					throw Exceptions.duck(e);
				}
			}
		}

		if (file != null) {
			String fileName = file.lastSegment();
			String fileExtension = file.getFileExtension();

			if ("pom.xml".equals(fileName)) {
				dndService.addMergedDropTarget(control, DROP_COPY, transfers,
					new MavenDropTargetListener((StyledText) control));
			} else if ("gradle".equalsIgnoreCase(fileExtension)) {
				dndService.addMergedDropTarget(control, DROP_COPY, transfers,
					new GradleDropTargetListener((StyledText) control));
			} else if ("bnd".equalsIgnoreCase(fileExtension) || "bndrun".equalsIgnoreCase(fileExtension)) {
				dndService.addMergedDropTarget(control, DROP_COPY, transfers,
					new BndDropTargetListener((StyledText) control));
			}
		}
	}

	@Override
	public void partClosed(IWorkbenchPart part) {
		IDragAndDropService dndService = part.getSite()
			.getService(IDragAndDropService.class);

		Control control = part.getAdapter(Control.class);

		if (control != null && dndService != null) {
			dndService.removeMergedDropTarget(control);
		}
	}

	@Override
	public void partActivated(IWorkbenchPart part) {}

	@Override
	public void partDeactivated(IWorkbenchPart part) {}

	@Override
	public void partBroughtToTop(IWorkbenchPart part) {}

}
