package biz.aQute.ai.assistant.eclipse.ui;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

class SelectionManager implements ISelectionListener {
	final static ISelectionService	selectionService	= PlatformUI.getWorkbench()
		.getActiveWorkbenchWindow()
		.getSelectionService();
	final List<Selection>			history				= new ArrayList<>();
	final AIView					owner;

	SelectionManager(AIView owner) {
		this.owner = owner;
	}

	record Selection(IWorkbenchPart part, String project, String relativePath, int start, int end, String text) {}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {

		if (part == owner)
			return;

		List<String> projects = new ArrayList<>();
		List<String> files = new ArrayList<>();

		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			for (Object o : structuredSelection) {
				if (o instanceof IFile file) {
					IPath path = file.getFullPath();
					String project = path.segment(0);
					projects.add(project);
					Path osFile = path.toFile()
						.toPath();

				}
				if (o instanceof IProject project) {
					projects.add(project.getName());
				}
			}

		} else if (selection instanceof ITextSelection) {
			ITextSelection textSelection = (ITextSelection) selection;

			// Retrieve selection start and end positions
			int start = textSelection.getOffset();
			int end = start + textSelection.getLength();

			System.out.println("Selection start: " + start);
			System.out.println("Selection end: " + end);
		}
	}

}
