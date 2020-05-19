package bndtools.explorer;

import static org.eclipse.jface.layout.GridLayoutFactory.fillDefaults;

import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Objects;

import org.bndtools.build.api.AbstractBuildListener;
import org.bndtools.build.api.BuildListener;
import org.bndtools.core.ui.icons.Icons;
import org.bndtools.utils.swt.FilterPanelPart;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

import aQute.lib.exceptions.Exceptions;
import aQute.lib.exceptions.FunctionWithException;
import aQute.lib.strings.Strings;
import aQute.libg.glob.Glob;
import bndtools.Plugin;
import bndtools.central.Central;
import bndtools.preferences.BndPreferences;

public class BndtoolsExplorer extends PackageExplorerPart {
	public static final String					VIEW_ID					= "bndtools.PackageExplorer";

	private final FilterPanelPart				filterPart				= new FilterPanelPart(Plugin.getDefault()
		.getScheduler());
	private Glob								glob;
	private IProject							selectedProject;

	private IPartListener						partListener			= new IPartListener() {

																			@Override
																			public void partOpened(
																				IWorkbenchPart part) {}

																			@Override
																			public void partDeactivated(
																				IWorkbenchPart part) {}

																			@Override
																			public void partClosed(
																				IWorkbenchPart part) {
																				if (part instanceof IEditorPart) {
																					IProject selectedProject = getProject(
																						((IEditorPart) part)
																							.getEditorInput());
																					if (selectedProject == BndtoolsExplorer.this.selectedProject)
																						setSelection(null);
																				}

																			}

																			@Override
																			public void partBroughtToTop(
																				IWorkbenchPart part) {}

																			@Override
																			public void partActivated(
																				IWorkbenchPart part) {
																				if (part instanceof IEditorPart) {
																					IProject selectedProject = getProject(
																						((IEditorPart) part)
																							.getEditorInput());
																					setSelection(selectedProject);
																				}
																			}
																		};
	private PropertyChangeListener				filterListener			= e -> {
																			setFilterText((String) e.getNewValue());
																		};

	boolean										installed;

	static ImageDescriptor						warnImage				= Icons.desc("warnings");
	static ImageDescriptor						errorImage				= Icons.desc("errors");
	static ImageDescriptor						okImage					= Icons.desc("allok");
	static IWorkbench							workbench				= PlatformUI.getWorkbench();
	private ToolBarManager						toolBarManager			= new ToolBarManager(SWT.HORIZONTAL);
	private IWorkbenchWindow					activeWorkbenchWindow	= workbench.getActiveWorkbenchWindow();
	private IWorkbenchPage						activePage				= activeWorkbenchWindow.getActivePage();

	private ServiceRegistration<BuildListener>	buildListenerRegistration;
	private Label								message;
	private BndPreferences						preferences				= new BndPreferences();

	/**
	 * <pre>
	 *
	 * 		parent
	 * 			explorer
	 * 				header
	 * 					toolbar
	 * 						status
	 * 						reload
	 * 					message
	 * 				filterControl
	 * 				original
	 * </pre>
	 */
	@Override
	public void createPartControl(Composite parent) {

		Composite explorer = new Composite(parent, SWT.NONE);
		fillDefaults().spacing(2, 2)
			.margins(4, 4)
			.applyTo(explorer);

		Composite header = doHeader(explorer);

		Control filterControl = filterPart.createControl(explorer);
		filterPart.setHint("Filter for projects (glob)");

		super.createPartControl(explorer);

		Control[] children = explorer.getChildren();
		assert children.length > 2 : "Package explorer must have changed and added more than one control :-(";

		Control original = children[2];

		GridDataFactory.fillDefaults()
			.grab(true, false)
			.applyTo(filterControl);

		GridDataFactory.fillDefaults()
			.grab(true, true)
			.applyTo(original);

		GridDataFactory.fillDefaults()
			.grab(true, false)
			.applyTo(header);

		fixupRefactoringPasteAction();

		filterPart.addPropertyChangeListener(filterListener);
		activePage.addPartListener(partListener);
	}

	private Composite doHeader(Composite parent) {
		Composite header = new Composite(parent, SWT.NONE);
		ToolBar toolbar = toolBarManager.createControl(header);
		message = new Label(header, SWT.NONE);
		message.setBackground(null);
		message.setText(getPrompt());

		fillDefaults().numColumns(2)
			.equalWidth(false)
			.applyTo(header);

		GridDataFactory.fillDefaults()
			.grab(true, false)
			.indent(0, 4)
			.applyTo(message);

		Action status = statusAction();
		toolBarManager.add(status);
		buildListener(status);

		Action reloadAction = reloadAction();
		toolBarManager.add(reloadAction);
		toolBarManager.update(true);
		return header;
	}

	@Override
	public void dispose() {
		filterPart.removePropertyChangeListener(filterListener);
		buildListenerRegistration.unregister();
		activePage.removePartListener(partListener);
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
			if (firstElement instanceof IResource) {
				IProject project = getProject((IResource) firstElement);
				if (project != null) {
					setSelection(project);
				}
			}
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

	private void setFilterText(String newValue) {
		Glob old = glob;
		glob = null;

		if (Strings.nonNullOrEmpty(newValue))
			glob = new Glob(newValue);
		if (!Objects.equals(old, glob))
			update();

	}

	private ImageDescriptor getImageDescriptor() {
		int maxStatus = getMaxSeverity();

		switch (maxStatus) {
			case IMarker.SEVERITY_INFO :
				return okImage;

			case IMarker.SEVERITY_WARNING :
				return warnImage;

			case IMarker.SEVERITY_ERROR :
				return errorImage;
		}
		return null;
	}

	private int getMaxSeverity() {
		int maxStatus = Arrays.stream(ResourcesPlugin.getWorkspace()
			.getRoot()
			.getProjects())
			.filter(IProject::isOpen)
			.map(FunctionWithException.asFunction(p -> p.findMaxProblemSeverity(null, false, IResource.DEPTH_INFINITE)))
			.reduce(IMarker.SEVERITY_INFO, Integer::max);
		return maxStatus;
	}

	private void update() {
		if (!installed) {
			installed = true;
			installFilter();
		}

		getTreeViewer().refresh();
	}

	private void installFilter() {
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

	private Action reloadAction() {
		Action rebuild = new Action("Reload workspace", Icons.desc("refresh")) {
			@Override
			public void run() {
				try {
					setImageDescriptor(Icons.desc("refresh.disable"));
					setEnabled(false);
					IFile workspaceBuildFile = Central.getWorkspaceBuildFile();
					Job.create("Reload", (monitor) -> {
						workspaceBuildFile.touch(monitor);
						Display.getDefault()
							.asyncExec(() -> {
								setEnabled(true);
								setImageDescriptor(Icons.desc("refresh"));
							});
					})
						.schedule();
				} catch (Exception e) {
					throw Exceptions.duck(e);
				}
			}
		};
		return rebuild;
	}

	private void fixupRefactoringPasteAction() {
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
	}

	private void buildListener(Action buildStatusAction) {
		BundleContext bundleContext = FrameworkUtil.getBundle(BndtoolsExplorer.class)
			.getBundleContext();

		BuildListener buildListener = new AbstractBuildListener() {
			@Override
			public void released(IProject project) {
				String info = getPrompt();

				Display.getDefault()
					.asyncExec(() -> {
						message.setText(info == null ? "" : info);
						ImageDescriptor descriptor = getImageDescriptor();
						buildStatusAction.setImageDescriptor(descriptor);
						toolBarManager.update(true);
					});
			}

		};

		buildListenerRegistration = bundleContext.registerService(BuildListener.class, buildListener, null);
	}

	private String getPrompt() {
		try {
			String prompt = preferences.getPrompt();
			String info = prompt == null ? null
				: Central.getWorkspace()
					.getReplacer()
					.process(prompt);
			return info;
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	private Action statusAction() {
		Action buildStatusAction = new Action("Build status", getImageDescriptor()) {
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

		return buildStatusAction;
	}

}
