package bndtools.explorer;

import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Objects;

import org.bndtools.build.api.AbstractBuildListener;
import org.bndtools.build.api.BuildListener;
import org.bndtools.utils.swt.FilterPanelPart;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import aQute.lib.exceptions.FunctionWithException;
import aQute.lib.strings.Strings;
import aQute.libg.glob.Glob;
import bndtools.Plugin;

public class BndtoolsExplorer extends PackageExplorerPart {
	public static final String		VIEW_ID			= "bndtools.PackageExplorer";

	private final FilterPanelPart	filterPart		= new FilterPanelPart(Plugin.getDefault()
		.getScheduler());
	private PropertyChangeListener	filterListener;
	private Glob					glob;
	boolean							installed;
	private IWorkbench				workbench		= PlatformUI.getWorkbench();
	private IProject				selectedProject;
	private IPartListener			partListener	= new IPartListener() {

														@Override
														public void partOpened(IWorkbenchPart part) {}

														@Override
														public void partDeactivated(IWorkbenchPart part) {}

														@Override
														public void partClosed(IWorkbenchPart part) {}

														@Override
														public void partBroughtToTop(IWorkbenchPart part) {}

														@Override
														public void partActivated(IWorkbenchPart part) {
															if (part instanceof IEditorPart) {
																IProject selectedProject = getProject(
																	((IEditorPart) part).getEditorInput());
																setSelection(selectedProject);
															}
														}
													};

	static ISharedImages			sharedImages	= PlatformUI.getWorkbench()
		.getSharedImages();
	static ImageDescriptor			warnImage		= sharedImages.getImageDescriptor(ISharedImages.IMG_OBJS_WARN_TSK);
	static ImageDescriptor			errorImage		= sharedImages.getImageDescriptor(ISharedImages.IMG_OBJS_ERROR_TSK);
	static ImageDescriptor			okImage			= Plugin.getDefault()
		.getImageRegistry()
		.getDescriptor(Plugin.IMG_OK);

	@Override
	public void createPartControl(Composite parent) {
		Composite c = new Composite(parent, SWT.NONE);
		GridLayout compactLayout = GridLayoutFactory.fillDefaults()
			.spacing(0, 0)
			.margins(0, 0)
			.create();
		c.setLayout(compactLayout);

		Composite header = new Composite(c, SWT.NONE);
		header.setLayout(GridLayoutFactory.createFrom(compactLayout)
			.numColumns(2)
			.equalWidth(false)
			.create());
		GridData fillData = GridDataFactory.fillDefaults()
			.grab(true, true)
			.create();
		header.setLayoutData(GridDataFactory.createFrom(fillData)
			.grab(true, false)
			.create());

		ToolBarManager toolBarManager = new ToolBarManager(SWT.HORIZONTAL);
		toolBarManager.createControl(header);

		Control filterControl = filterPart.createControl(header);
		filterControl.setLayoutData(fillData);

		super.createPartControl(c);

		Control[] children = c.getChildren();
		if (children.length > 1) {
			children[1].setLayoutData(fillData);
		}
		c.layout();

		filterPart.setHint("Filter for projects (glob)");
		filterListener = e -> {
			Glob old = glob;
			glob = null;
			String filterText = (String) e.getNewValue();
			if (Strings.nonNullOrEmpty(filterText))
				glob = new Glob(filterText);
			if (!Objects.equals(old, glob))
				update();
		};
		filterPart.addPropertyChangeListener(filterListener);

		IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
		if (activeWorkbenchWindow != null) {
			IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
			activePage.addPartListener(partListener);
		}

		IActionBars actionBars = getViewSite().getActionBars();

		IAction originalPaste = actionBars.getGlobalActionHandler(ActionFactory.PASTE.getId());

		actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(), new Action() {
			@Override
			public void runWithEvent(Event event) {
				Text filterText = filterPart.getFilterControl();

				if (Objects.equals(event.widget, filterText)) {
					filterText.paste();
				} else {
					originalPaste.runWithEvent(event);
				}
			}
		});

		Action action = new Action("Build status", okImage) {
			@Override
			public void runWithEvent(Event event) {
				ImageDescriptor desc = getImageDescriptor();

				if (desc.equals(errorImage)) {
					filterPart.getFilterControl()
						.setText(":error");
				} else if (desc.equals(warnImage)) {
					filterPart.getFilterControl()
						.setText(":warning");
				}
			}
		};

		toolBarManager.add(action);
		toolBarManager.update(true);
		header.layout(true);

		BundleContext bundleContext = FrameworkUtil.getBundle(BndtoolsExplorer.class)
			.getBundleContext();

		BuildListener buildListener = new AbstractBuildListener() {
			@Override
			public void released(IProject project) {
				int maxStatus = Arrays.stream(ResourcesPlugin.getWorkspace()
					.getRoot()
					.getProjects())
					.filter(IProject::isOpen)
					.map(FunctionWithException
						.asFunction(p -> p.findMaxProblemSeverity(null, false, IResource.DEPTH_INFINITE)))
					.reduce(IMarker.SEVERITY_INFO, Integer::max);

				switch (maxStatus) {
					case IMarker.SEVERITY_INFO :
						action.setImageDescriptor(okImage);
						break;
					case IMarker.SEVERITY_WARNING :
						action.setImageDescriptor(warnImage);
						break;
					case IMarker.SEVERITY_ERROR :
						action.setImageDescriptor(errorImage);
						break;
				}
				toolBarManager.update(true);
			}
		};

		bundleContext.registerService(BuildListener.class, buildListener, null);
	}

	private void update() {
		if (!installed) {
			installed = true;
			getTreeViewer().addFilter(new ViewerFilter() {

				@Override
				public boolean select(Viewer viewer, Object parentElement, Object element) {
					if (glob == null)
						return true;

					if (element instanceof JavaProject) {
						IJavaProject project = (JavaProject) element;
						String name = project.getElementName();
						return isSelected(project.getProject(), name);

					} else if (element instanceof IProject) {
						IProject project = (IProject) element;
						String name = project.getName();
						return isSelected(project, name);
					} else
						return true;
				}

				private boolean isSelected(IProject project, String name) {
					if (project == selectedProject)
						return true;
					if (glob.finds(name) >= 0)
						return true;

					try {
						int maxSeverity = project.findMaxProblemSeverity(null, false, IResource.DEPTH_INFINITE);

						switch (maxSeverity) {
							case 0 :
								return false;
							case IMarker.SEVERITY_ERROR :
								return glob.finds(":error") >= 0;

							case IMarker.SEVERITY_WARNING :
								return glob.finds(":warning") >= 0;
						}
					} catch (CoreException e) {
						// ignore
					}

					return false;

				}
			});
		}

		getTreeViewer().refresh();
	}

	@Override
	public void dispose() {
		filterPart.removePropertyChangeListener(filterListener);
		super.dispose();
	}

	@Override
	public int tryToReveal(Object element) {
		if (element instanceof IResource) {
			setSelection(getProject((IResource) element));
		} else
			setSelection(null);
		return super.tryToReveal(element);
	}

	@Override
	public void selectAndReveal(Object element) {
		tryToReveal(element);
		super.selectAndReveal(element);
	}

	@Override
	public void selectReveal(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			Object firstElement = ((IStructuredSelection) selection).getFirstElement();
		}
		super.selectReveal(selection);
	}

	private void setSelection(IProject project) {
		if (project != selectedProject) {
			this.selectedProject = project;
			update();
		}
	}

	private IProject getProject(IEditorInput iEditorInput) {
		if (iEditorInput == null) {
			return null;
		}

		if (iEditorInput instanceof IFileEditorInput) {
			IResource resource = ((IFileEditorInput) iEditorInput).getFile();
			return getProject(resource);
		}
		return null;
	}

	private IProject getProject(IResource resource) {
		while (resource != null) {
			if (resource instanceof IProject)
				return (IProject) resource;

			resource = resource.getParent();
		}
		return null;
	}
}
