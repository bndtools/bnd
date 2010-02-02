package name.neilbartlett.eclipse.bndtools.project.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.project.BndProjectProperties;
import name.neilbartlett.eclipse.bndtools.utils.CollectionUtils;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class BndProjectPropertyPage extends PropertyPage implements IWorkbenchPropertyPage {

	private CheckboxTreeViewer viewer;
	private BndProjectProperties projectProperties;

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		
		new Label(composite, SWT.NONE).setText("Exported Bundle Directories:");
		
		Tree tree = new Tree(composite, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.CHECK);
		
		viewer = new ContainerCheckedTreeViewer(tree);
		viewer.setContentProvider(new WorkbenchContentProvider());
		viewer.setLabelProvider(new WorkbenchLabelProvider());
		viewer.addFilter(new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				return element instanceof IContainer;
			}
		});
		
		// Initialise
		IProject project = getProject();
		viewer.setInput(project);
		
		projectProperties = new BndProjectProperties(project);
		try {
			projectProperties.load();
			List<IResource> exportDirs = projectProperties.getExportedBundleDirs();
			viewer.setCheckedElements(exportDirs.toArray(new IResource[exportDirs.size()]));
			viewer.setExpandedElements(viewer.getCheckedElements());
		} catch (CoreException e) {
			ErrorDialog.openError(getShell(), "Bnd Project Properties", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error loading Bnd Project properties.", e));
		} catch (IOException e) {
			ErrorDialog.openError(getShell(), "Bnd Project Properties", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error loading Bnd Project properties.", e));
		}
		
		// Layout
		composite.setLayout(new GridLayout(1, false));
		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		return composite;
	}
	


	@Override
	public boolean performOk() {
		List<IResource> resources = CollectionUtils.newArrayList(viewer.getCheckedElements());
		for (Iterator<IResource> iter = resources.iterator(); iter.hasNext();) {
			IResource resource = iter.next();
			if(viewer.getGrayed(resource)) {
				iter.remove();
			}
		}
		resources = dedupeResourcePaths(resources);
		projectProperties.setExportedBundleDirs(resources);
		try {
			projectProperties.save();
			return true;
		} catch (CoreException e) {
			ErrorDialog.openError(getShell(), "Bnd Project Properties", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error saving Bnd Project properties.", e));
		} catch (IOException e) {
			ErrorDialog.openError(getShell(), "Bnd Project Properties", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error saving Bnd Project properties.", e));
		}
		return false;
	}
	
	private List<IResource> dedupeResourcePaths(List<IResource> resources) {
		List<IResource> result = new ArrayList<IResource>(resources);
		for (IResource resource : resources) {
			for(Iterator<IResource> iter = result.iterator(); iter.hasNext(); ) {
				IResource element = iter.next();
				if(resource != element && resource.getProjectRelativePath().isPrefixOf(element.getProjectRelativePath())) {
					iter.remove();
				}
			}
		}
		return result;
	}
	

	private IProject getProject() {
		IAdaptable element = getElement();
		if(element instanceof IProject) {
			return (IProject) element;
		}
		return (IProject) element.getAdapter(IProject.class);
	}
}
