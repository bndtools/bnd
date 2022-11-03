package bndtools.explorer;

import static org.eclipse.jface.layout.GridLayoutFactory.fillDefaults;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.build.api.AbstractBuildListener;
import org.bndtools.build.api.BuildListener;
import org.bndtools.core.ui.icons.Icons;
import org.bndtools.utils.swt.FilterPanelPart;
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
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
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
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormText;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

import aQute.bnd.build.Workspace;
import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.exceptions.FunctionWithException;
import aQute.lib.io.IO;
import bndtools.Plugin;
import bndtools.central.Central;
import bndtools.central.sync.WorkspaceSynchronizer;
import bndtools.preferences.BndPreferences;
import bndtools.preferences.ui.BndPreferencePage;

public class BndtoolsExplorer extends PackageExplorerPart {
	public static final String			VIEW_ID		= "bndtools.PackageExplorer";
	static final ImageDescriptor		warnImage	= Icons.desc("warnings");
	static final ImageDescriptor		errorImage	= Icons.desc("errors");
	static final ImageDescriptor		okImage		= Icons.desc("allok");
	static final BndPreferences			preferences	= new BndPreferences();
	static final IWorkbench				workbench	= PlatformUI.getWorkbench();
	private Model						model		= new Model();
	private boolean						installed;
	private final List<AutoCloseable>	closeables	= new ArrayList<>();

	/**
	 * The GUI:
	 *
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

		FilterPanelPart filterPart = new FilterPanelPart(Plugin.getDefault()
			.getScheduler());
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

		fixupRefactoringPasteAction(filterPart);

		PropertyChangeListener filterListener = e -> {
			model.setFilterText((String) e.getNewValue());
		};
		filterPart.addPropertyChangeListener(filterListener);
		model.onUpdate(() -> filterPart.setFilter(model.filterText));
		closeables.add(() -> filterPart.removePropertyChangeListener(filterListener));

		IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
		IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();

		IPartListener partListener = getPartListener();
		activePage.addPartListener(partListener);
		closeables.add(() -> activePage.removePartListener(partListener));

		closeables.add(preferences.onPrompt(model::setPrompt));

		ISelectionChangedListener evListener = (event) -> {
			selected(event.getSelection());
		};
		getSite().getSelectionProvider()
			.addSelectionChangedListener(evListener);
		closeables.add(() -> getSite().getSelectionProvider()
			.removeSelectionChangedListener(evListener));

		model.onUpdate(this::updateTreeViewer);
		model.update();
	}

	private Composite doHeader(Composite parent) {
		ToolBarManager toolBarManager = new ToolBarManager(SWT.HORIZONTAL);

		Composite header = new Composite(parent, SWT.NONE);
		ToolBar toolbar = toolBarManager.createControl(header);
		FormText message = new FormText(header, SWT.NONE);
		message.setBackground(null);
		message.addHyperlinkListener(new IHyperlinkListener() {

			@Override
			public void linkExited(HyperlinkEvent e) {}

			@Override
			public void linkEntered(HyperlinkEvent e) {}

			@Override
			public void linkActivated(HyperlinkEvent e) {
				if ("prefs".equals(e.getHref())) {
					PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(null, BndPreferencePage.PAGE_ID,
						new String[] {}, null);
					dialog.open();
				} else {
					try {
						PlatformUI.getWorkbench()
							.getBrowserSupport()
							.getExternalBrowser()
							.openURL(new URL((String) e.getHref()));
					} catch (IOException | PartInitException e1) {
						// ignore
					}
				}
			}
		});
		model.onUpdate(() -> {
			try {
				message.setText(asForm(model.message), true, false);
			} catch (Exception e) {
				message.setText(asForm(model.message), false, false);
			}
		});

		fillDefaults().numColumns(2)
			.equalWidth(false)
			.applyTo(header);

		GridDataFactory.fillDefaults()
			.grab(true, false)
			.indent(0, 4)
			.applyTo(message);

		Action severity = severityAction();
		toolBarManager.add(severity);
		model.onUpdate(() -> {
			ImageDescriptor descriptor = getImageDescriptor(model.severity);
			severity.setImageDescriptor(descriptor);
			toolBarManager.update(true);
		});

		buildListener();

		Action reloadAction = reloadAction();
		toolBarManager.add(reloadAction);
		toolBarManager.update(true);

		Action pin = pinAction();
		toolBarManager.add(pin);
		toolBarManager.update(true);

		return header;
	}

	@Override
	public void dispose() {
		closeables.forEach(IO::close);
		super.dispose();
	}

	@Override
	public int tryToReveal(Object element) {
		int result = super.tryToReveal(element);
		afterReveal(element);
		return result;
	}

	private void afterReveal(Object element) {
		if (element instanceof IResource) {
			model.setActualSelection(element);
			model.setSelectedProject(getProject((IResource) element));
		} else {
			model.setActualSelection(null);
			model.setSelectedProject(null);
		}
	}

	@Override
	public void selectAndReveal(Object element) {
		super.selectAndReveal(element);
		afterReveal(element);
	}

	@Override
	public void selectReveal(ISelection selection) {
		selected(selection);
		super.selectReveal(selection);
	}

	private void selected(ISelection selection) {

		if (selection instanceof IStructuredSelection) {
			Object firstElement = ((IStructuredSelection) selection).getFirstElement();
			if (firstElement instanceof IResource) {
				model.setActualSelection(firstElement);
				IProject project = getProject((IResource) firstElement);
				if (project != null) {
					model.setSelectedProject(project);
				}
			} else if (firstElement instanceof JavaProject) {
				model.setActualSelection(((JavaProject) firstElement).getProject());
			} else
				model.setActualSelection(null);
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

	private ImageDescriptor getImageDescriptor(int severity) {
		switch (severity) {
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
		return Arrays.stream(ResourcesPlugin.getWorkspace()
			.getRoot()
			.getProjects())
			.filter(IProject::isOpen)
			.filter(p -> !Objects.equals(p.getName(), BndtoolsConstants.BNDTOOLS_JAREDITOR_TEMP_PROJECT_NAME))
			.map(FunctionWithException.asFunction(p -> p.findMaxProblemSeverity(null, false, IResource.DEPTH_INFINITE)))
			.reduce(IMarker.SEVERITY_INFO, Integer::max);
	}

	private void updateTreeViewer() {
		if (!installed) {
			installed = true;
			installFilter();
			model.filterDirty.set(true);
		}

		if (model.filterDirty.getAndSet(false))
			getTreeViewer().refresh();
	}

	private void installFilter() {
		getTreeViewer().addFilter(new ViewerFilter() {

			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (model.glob == null)
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
				if (project == model.selectedProject)
					return true;
				if (model.glob.finds(name) >= 0)
					return true;

				if (model.isPinned(project))
					return true;

				try {
					int maxSeverity = project.findMaxProblemSeverity(null, false, IResource.DEPTH_INFINITE);

					switch (maxSeverity) {
						case 0 :
							return false;
						case IMarker.SEVERITY_ERROR :
							return model.glob.finds(":error") >= 0;

						case IMarker.SEVERITY_WARNING :
							return model.glob.finds(":warning") >= 0;
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
			WorkspaceSynchronizer s;

			@Override
			public void run() {
				try {
					Workspace workspace = Central.getWorkspace();
					if (workspace.isDefaultWorkspace())
						return;

					setImageDescriptor(Icons.desc("refresh.disabled"));
					setEnabled(false);

					s = new WorkspaceSynchronizer();

					Job syncjob = Job.create("Sync bnd workspace", monitor -> {
						s.synchronize(true, monitor, this::done);
					});
					syncjob.setRule(ResourcesPlugin.getWorkspace()
						.getRoot());
					syncjob.schedule();
				} catch (Exception e) {
					throw Exceptions.duck(e);
				}
			}

			private void done() {
				Display.getDefault()
					.asyncExec(() -> {
						setEnabled(true);
						setImageDescriptor(Icons.desc("refresh"));
					});
			}
		};
		return rebuild;
	}

	private Action pinAction() {
		Action pin = new Action("Pin project", Icons.desc("pin.plus")) {

			@Override
			public void run() {
				model.doPin();
			}
		};
		Runnable runnable = () -> {

			if (!(model.selection instanceof IProject)) {
				pin.setImageDescriptor(Icons.desc("pin.disabled"));
				pin.setEnabled(false);
				return;
			}

			pin.setEnabled(true);
			if (model.isPinned((IProject) model.selection)) {
				pin.setImageDescriptor(Icons.desc("pin.minus"));
			} else {
				pin.setImageDescriptor(Icons.desc("pin.plus"));
			}
		};
		runnable.run();
		model.onUpdate(runnable);
		return pin;
	}

	private void fixupRefactoringPasteAction(FilterPanelPart filterPart) {
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

	private void buildListener() {
		BundleContext bundleContext = FrameworkUtil.getBundle(BndtoolsExplorer.class)
			.getBundleContext();

		BuildListener buildListener = new AbstractBuildListener() {
			@Override
			public void released(IProject project) {
				model.setSeverity(getMaxSeverity());
				model.updateMessage();
			}

		};

		ServiceRegistration<BuildListener> buildListenerRegistration = bundleContext
			.registerService(BuildListener.class, buildListener, null);
		closeables.add(buildListenerRegistration::unregister);
	}

	private Action severityAction() {
		Action action = new Action("Build status", getImageDescriptor(IMarker.SEVERITY_INFO)) {
			@Override
			public void runWithEvent(Event event) {
				switch (model.severity) {
					case IMarker.SEVERITY_ERROR :
						model.setFilterText(":error");
						break;

					case IMarker.SEVERITY_WARNING :
						model.setFilterText(":warning");
						break;

					default :
				}
			}
		};
		return action;
	}

	private String asForm(String s) {
		StringBuilder prompt = new StringBuilder();
		prompt.append("<form><p>")
			.append(s)
			.append("</p></form>");
		return prompt.toString();
	}

	private IPartListener getPartListener() {
		return new IPartListener() {
			@Override
			public void partOpened(IWorkbenchPart part) {}

			@Override
			public void partDeactivated(IWorkbenchPart part) {}

			@Override
			public void partClosed(IWorkbenchPart part) {
				if (part instanceof IEditorPart) {
					IProject selectedProject = getProject(((IEditorPart) part).getEditorInput());
					model.closeProject(selectedProject);
				}

			}

			@Override
			public void partBroughtToTop(IWorkbenchPart part) {}

			@Override
			public void partActivated(IWorkbenchPart part) {
				if (part instanceof IEditorPart) {
					IProject selectedProject = getProject(((IEditorPart) part).getEditorInput());
					model.setSelectedProject(selectedProject);
				}
			}
		};
	}

}
