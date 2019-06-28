package org.bndtools.core.ui.wizards.blueprint;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class WizardBndFileSelector extends WizardPage {

	private static final ILogger	logger	= Logger.getLogger(WizardBndFileSelector.class);

	private CheckboxTreeViewer		checkboxTreeViewer;

	protected WizardBndFileSelector() {
		super("selectBndPage", "Select Bnd files to update", null);
		setPageComplete(false);
	}

	@Override
	public void setVisible(boolean visible) {
		setPageComplete(true);
		super.setVisible(visible);
	}

	@Override
	public void createControl(Composite composite) {
		checkboxTreeViewer = new CheckboxTreeViewer(composite);

		checkboxTreeViewer.addCheckStateListener(new ICheckStateListener() {

			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				Object element = event.getElement();
				ITreeContentProvider contentProvider = (ITreeContentProvider) checkboxTreeViewer.getContentProvider();

				alterChildren(element, contentProvider, event.getChecked());

				alterParents(element, contentProvider);
			}

			private void alterChildren(Object element, ITreeContentProvider contentProvider, boolean checked) {
				Queue<Object> toRecurse = new LinkedList<>();
				toRecurse.offer(element);
				while (!toRecurse.isEmpty()) {
					Object o = toRecurse.poll();
					for (Object child : contentProvider.getChildren(o)) {
						if (contentProvider.hasChildren(child))
							toRecurse.offer(child);
						checkboxTreeViewer.setChecked(child, checked);
						checkboxTreeViewer.setGrayed(child, false);
					}
				}
			}

			private void alterParents(Object element, ITreeContentProvider contentProvider) {
				Object parent = contentProvider.getParent(element);
				while (parent != null) {
					boolean allChecked = true;
					boolean noneChecked = true;
					for (Object child : contentProvider.getChildren(parent)) {
						boolean checked = checkboxTreeViewer.getChecked(child);
						boolean grayed = checkboxTreeViewer.getGrayed(child);
						allChecked &= checked & !grayed;
						noneChecked &= !checked;
					}

					if (allChecked) {
						checkboxTreeViewer.setChecked(parent, true);
						checkboxTreeViewer.setGrayed(parent, false);
					} else if (noneChecked) {
						checkboxTreeViewer.setGrayChecked(parent, false);
					} else {
						checkboxTreeViewer.setGrayChecked(parent, true);
					}
					parent = contentProvider.getParent(parent);
				}
			}
		});

		setControl(checkboxTreeViewer.getControl());
	}

	public IFile[] getSelectedBndFiles() {
		List<IFile> files = new ArrayList<>();
		for (Object o : checkboxTreeViewer.getCheckedElements()) {
			if (o instanceof IFile)
				files.add((IFile) o);
		}
		return files.toArray(new IFile[0]);
	}

	public void updateControls(IPath containerFullPath) {

		String selectedProjectName = containerFullPath.segment(0);
		final IProject project = ResourcesPlugin.getWorkspace()
			.getRoot()
			.getProject(selectedProjectName);

		final List<IResource> bndFiles = new ArrayList<>();

		try {
			project.accept(res -> {
				if (res.getType() == IResource.FILE && res.getName()
					.endsWith(".bnd")) {
					bndFiles.add(res.requestResource());
				}
				return true;
			}, 0);
		} catch (CoreException e) {
			logger.logError(String.format("Unable to locate bnd files for project: %s", project.getName()), e);
		}

		setupTree(bndFiles);
		checkboxTreeViewer.setInput(project);

		if (bndFiles.size() == 1)
			checkboxTreeViewer.setCheckedElements(bndFiles.toArray());

		checkboxTreeViewer.refresh(true);
		checkboxTreeViewer.getControl()
			.redraw();
	}

	private void setupTree(final List<IResource> bndFiles) {
		checkboxTreeViewer.setContentProvider(new WorkbenchContentProvider() {

			@Override
			public Object[] getChildren(Object element) {
				Object[] children = super.getChildren(element);
				List<IResource> resources = new ArrayList<>();
				for (Object o : children) {
					IPath child = ((IResource) o).getProjectRelativePath();
					for (IResource bndFile : bndFiles) {
						if (child.isPrefixOf(bndFile.getProjectRelativePath())
							|| child.equals(bndFile.getProjectRelativePath())) {
							resources.add((IResource) o);
							break;
						}
					}
				}
				return resources.toArray();
			}
		});
		checkboxTreeViewer.setLabelProvider(WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider());
		checkboxTreeViewer.expandAll();
	}

}
