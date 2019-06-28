package bndtools.editor.contents;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.namespace.PackageNamespace;

import aQute.bnd.osgi.Clazz;
import bndtools.Plugin;
import bndtools.model.resolution.CapReqMapContentProvider;
import bndtools.model.resolution.RequirementWrapper;
import bndtools.model.resolution.RequirementWrapperLabelProvider;
import bndtools.tasks.AnalyseBundleResolutionJob;
import bndtools.tasks.BndFileCapReqLoader;

public class BundleCalculatedImportsPart extends SectionPart implements IResourceChangeListener {

	private final Image		imgRefresh			= AbstractUIPlugin
		.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/arrow_refresh.png")						//$NON-NLS-1$
		.createImage();
	private final Image		imgShowSelfImports	= AbstractUIPlugin
		.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/package_folder_impexp.gif")				//$NON-NLS-1$
		.createImage();

	private Tree			tree;
	private TreeViewer		viewer;

	private ViewerFilter	hideSelfImportsFilter;
	private ViewerFilter	nonPkgFilter;

	public BundleCalculatedImportsPart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style);
		createSection(getSection(), toolkit);
	}

	private void createSection(Section section, FormToolkit toolkit) {
		// CREATE COMPONENTS
		section.setText(Messages.BundleCalculatedImportsPart_title);
		ToolBar toolbar = new ToolBar(section, SWT.FLAT);
		section.setTextClient(toolbar);

		final ToolItem showSelfImportsItem = new ToolItem(toolbar, SWT.CHECK);
		showSelfImportsItem.setImage(imgShowSelfImports);
		showSelfImportsItem.setToolTipText(Messages.BundleCalculatedImportsPart_tooltipShowSelfImports);

		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);

		// toolkit.createLabel(composite,
		// Messages.BundleCalculatedImportsPart_description, SWT.WRAP);

		tree = toolkit.createTree(composite, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);

		viewer = new TreeViewer(tree);
		viewer.setContentProvider(new CapReqMapContentProvider());
		viewer.setLabelProvider(new RequirementWrapperLabelProvider(true));
		ColumnViewerToolTipSupport.enableFor(viewer);

		nonPkgFilter = new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parent, Object element) {
				if (element instanceof RequirementWrapper)
					return PackageNamespace.PACKAGE_NAMESPACE
						.equals(((RequirementWrapper) element).requirement.getNamespace());
				return true;
			}
		};
		hideSelfImportsFilter = new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof RequirementWrapper)
					return !((RequirementWrapper) element).resolved;
				return true;
			}
		};
		viewer.setFilters(new ViewerFilter[] {
			nonPkgFilter, hideSelfImportsFilter
		});

		viewer.addSelectionChangedListener(
			event -> getManagedForm().fireSelectionChanged(BundleCalculatedImportsPart.this, event.getSelection()));
		viewer.addDragSupport(DND.DROP_MOVE | DND.DROP_COPY, new Transfer[] {
			LocalSelectionTransfer.getTransfer()
		}, new DragSourceAdapter() {
			@Override
			public void dragSetData(DragSourceEvent event) {
				LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
				if (transfer.isSupportedType(event.dataType))
					transfer.setSelection(viewer.getSelection());
			}
		});
		viewer.addOpenListener(event -> {
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
			for (Iterator<?> iter = selection.iterator(); iter.hasNext();) {
				Object item = iter.next();
				if (item instanceof Clazz) {
					Clazz importUsedBy = (Clazz) item;
					String className = importUsedBy.getFQN();
					IType type = null;

					IFile file = getEditorFile();
					if (file != null) {
						IJavaProject javaProject = JavaCore.create(file.getProject());
						try {
							type = javaProject.findType(className);
						} catch (JavaModelException e1) {
							ErrorDialog.openError(tree.getShell(), Messages.BundleCalculatedImportsPart_error,
								Messages.BundleCalculatedImportsPart_errorFindingType,
								new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat
									.format(Messages.BundleCalculatedImportsPart_errorOpeningClass, className), e1));
						}
					}
					try {
						if (type != null)
							JavaUI.openInEditor(type, true, true);
					} catch (PartInitException e2) {
						ErrorDialog.openError(tree.getShell(), Messages.BundleCalculatedImportsPart_error, null,
							new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat
								.format(Messages.BundleCalculatedImportsPart_errorOpeningJavaEditor, className), e2));
					} catch (JavaModelException e3) {
						ErrorDialog.openError(tree.getShell(), Messages.BundleCalculatedImportsPart_error, null,
							new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0,
								MessageFormat.format(Messages.BundleCalculatedImportsPart_errorOpeningClass, className),
								e3));
					}
				}
			}
		});

		// LISTENERS
		showSelfImportsItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean showSelfImports = showSelfImportsItem.getSelection();
				ViewerFilter[] filters = showSelfImports ? new ViewerFilter[] {
					nonPkgFilter
				} : new ViewerFilter[] {
					nonPkgFilter, hideSelfImportsFilter
				};
				viewer.setFilters(filters);
			}
		});

		// LAYOUT
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 2;
		composite.setLayout(layout);

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 75;
		gd.widthHint = 75;
		tree.setLayoutData(gd);
	}

	@Override
	public void initialize(IManagedForm form) {
		super.initialize(form);

		ResourcesPlugin.getWorkspace()
			.addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
	}

	@Override
	public void refresh() {
		super.refresh();

		IFile file = getEditorFile();
		if (file == null)
			return;
		IPath location = file.getLocation();
		if (location == null)
			return;

		Set<BndFileCapReqLoader> loaders = Collections.singleton(new BndFileCapReqLoader(location.toFile()));
		final AnalyseBundleResolutionJob job = new AnalyseBundleResolutionJob(
			Messages.BundleCalculatedImportsPart_jobAnalyse, loaders);
		final Display display = tree.getDisplay();
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if (job.getResult()
					.isOK()) {
					display.asyncExec(() -> {
						if (tree != null && !tree.isDisposed())
							viewer.setInput(job.getRequirements());
					});
				}
			}
		});
		job.schedule();
	}

	private IFile getEditorFile() {
		IFormPage page = (IFormPage) getManagedForm().getContainer();
		IFile file = ResourceUtil.getFile(page.getEditorInput());
		return file;
	}

	@Override
	public void dispose() {
		ResourcesPlugin.getWorkspace()
			.removeResourceChangeListener(this);
		super.dispose();
		imgRefresh.dispose();
		imgShowSelfImports.dispose();

	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IFile file = getEditorFile();
		if (file != null) {
			IResourceDelta delta = event.getDelta();
			delta = delta.findMember(file.getFullPath());
			if (delta != null) {
				IFormPage page = (IFormPage) getManagedForm().getContainer();
				if (page.isActive())
					refresh();
				else
					markStale();
			}
		}
	}
}
