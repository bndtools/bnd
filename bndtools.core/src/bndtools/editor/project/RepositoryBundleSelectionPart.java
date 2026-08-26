package bndtools.editor.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.bndtools.core.ui.StickyToolTipSupport;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.dnd.URLTransfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.ResourceTransfer;
import org.osgi.framework.namespace.IdentityNamespace;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.Constants;
import bndtools.Plugin;
import bndtools.editor.BndEditor;
import bndtools.editor.common.BndEditorPart;
import bndtools.model.clauses.VersionedClauseLabelProvider;
import bndtools.model.repo.DependencyPhase;
import bndtools.model.repo.IncludedBundleItem;
import bndtools.model.repo.ProjectBundle;
import bndtools.model.repo.RepositoryBundle;
import bndtools.model.repo.RepositoryBundleUtils;
import bndtools.model.repo.RepositoryBundleVersion;
import bndtools.model.repo.RepositoryFeature;
import bndtools.model.repo.RepositoryResourceElement;
import bndtools.preferences.BndPreferences;
import bndtools.types.Pair;
import bndtools.wizards.repo.RepoBundleSelectionWizard;
import bndtools.wizards.workspace.AddFilesToRepositoryWizard;

public abstract class RepositoryBundleSelectionPart extends BndEditorPart implements PropertyChangeListener {
	private final String			propertyName;
	private final DependencyPhase	phase;

	private Table					table;
	protected TableViewer			viewer;

	protected BndEditModel			model;
	protected List<VersionedClause>	bundles;
	/** Bundles inherited from included files; shown gray, not committable. */
	protected List<VersionedClause>	inheritedBundles		= new ArrayList<>();
	/** Per-BSN provenance: maps each inherited bundle's BSN to the file path that defines it. */
	private Map<String, String>		inheritedBundleProvenances	= Collections.emptyMap();
	protected ToolItem				removeItemTool;

	protected RepositoryBundleSelectionPart(String propertyName, DependencyPhase phase, Composite parent,
		FormToolkit toolkit, int style) {
		super(parent, toolkit, style);
		this.propertyName = propertyName;
		this.phase = phase;
		createSection(getSection(), toolkit);
	}

	@Override
	protected String[] getProperties() {
		return new String[] {
			propertyName
		};
	}

	protected ToolItem createAddItemTool(ToolBar toolbar) {
		ToolItem tool = new ToolItem(toolbar, SWT.PUSH);

		tool.setImage(PlatformUI.getWorkbench()
			.getSharedImages()
			.getImage(ISharedImages.IMG_OBJ_ADD));
		tool.setToolTipText("Add Bundle");
		tool.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doAdd();
			}
		});

		return tool;
	}

	protected ToolItem createRemoveItemTool(ToolBar toolbar) {
		ToolItem tool = new ToolItem(toolbar, SWT.PUSH);

		tool.setImage(PlatformUI.getWorkbench()
			.getSharedImages()
			.getImage(ISharedImages.IMG_TOOL_DELETE));
		tool.setDisabledImage(PlatformUI.getWorkbench()
			.getSharedImages()
			.getImage(ISharedImages.IMG_TOOL_DELETE_DISABLED));
		tool.setToolTipText("Remove");
		tool.setEnabled(false);
		tool.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doRemove();
			}
		});

		return tool;
	}

	protected ToolItem getRemoveItemTool() {
		return removeItemTool;
	}

	protected void fillToolBar(ToolBar toolbar) {
		createAddItemTool(toolbar);
		this.removeItemTool = createRemoveItemTool(toolbar);
	}

	protected IBaseLabelProvider getLabelProvider() {
		return new MixedVersionedClauseLabelProvider();
	}

	/** A label provider that colors inherited bundles gray and local bundles with the default foreground. */
	protected class MixedVersionedClauseLabelProvider extends VersionedClauseLabelProvider {
		private Color grey;

		@Override
		public void update(ViewerCell cell) {
			super.update(cell);
			if (inheritedBundles.contains(cell.getElement())) {
				if (grey == null)
					grey = cell.getItem().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
				cell.setForeground(grey);
				cell.setStyleRanges(new StyleRange[0]);
			}
		}

		@Override
		public String getToolTipText(Object element) {
			if (!(element instanceof VersionedClause) || !inheritedBundles.contains(element))
				return null;
			String path = inheritedBundleProvenances.get(((VersionedClause) element).getName());
			return path != null
				? "Inherited from " + BndEditModelAccessor.getDisplayProvenance(model, path)
					+ "\nDouble-click to open source."
				: "Inherited from an included file.";
		}
	}

	void createSection(Section section, FormToolkit toolkit) {
		// Toolbar buttons
		ToolBar toolbar = new ToolBar(section, SWT.FLAT);
		section.setTextClient(toolbar);
		fillToolBar(toolbar);

		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);

		table = toolkit.createTable(composite, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER | SWT.H_SCROLL);

		viewer = new TableViewer(table);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(getLabelProvider());
		StickyToolTipSupport.enableFor(viewer);

		// Listeners
		viewer.addSelectionChangedListener(event -> {
			ToolItem remove = getRemoveItemTool();
			if (remove != null)
				remove.setEnabled(isRemovable(event.getSelection()));
		});
		// Double-click on an inherited row opens the file that defines that specific bundle.
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				IStructuredSelection sel = (IStructuredSelection) viewer.getSelection();
				if (sel.isEmpty()) return;
				Object first = sel.getFirstElement();
				if (!(first instanceof VersionedClause) || !inheritedBundles.contains(first)) return;
				String path = inheritedBundleProvenances.get(((VersionedClause) first).getName());
				if (path != null) openProvenanceByPath(path);
			}
		});
		ViewerDropAdapter dropAdapter = new ViewerDropAdapter(viewer) {
			@Override
			public void dragEnter(DropTargetEvent event) {
				super.dragEnter(event);
				event.detail = DND.DROP_COPY;
			}

			@Override
			public boolean validateDrop(Object target, int operation, TransferData transferType) {
				if (FileTransfer.getInstance()
					.isSupportedType(transferType))
					return true;

				if (ResourceTransfer.getInstance()
					.isSupportedType(transferType))
					return true;

				ISelection selection = LocalSelectionTransfer.getTransfer()
					.getSelection();
				if (selection.isEmpty() || !(selection instanceof IStructuredSelection)) {
					return false;
				}

				Iterator<?> iterator = ((IStructuredSelection) selection).iterator();
				while (iterator.hasNext()) {
					if (!selectionIsDroppable(iterator.next())) {
						return false;
					}
				}
				return true;
			}

			private boolean selectionIsDroppable(Object element) {
				return element instanceof RepositoryBundle || element instanceof RepositoryBundleVersion
					|| element instanceof ProjectBundle || element instanceof RepositoryResourceElement
					|| element instanceof RepositoryFeature || element instanceof IncludedBundleItem;
			}

			@Override
			public boolean performDrop(Object data) {
				TransferData transfer = getCurrentEvent().currentDataType;

				if (data instanceof String[]) {
					return handleFileNameDrop((String[]) data);
				} else if (data instanceof IResource[]) {
					return handleResourceDrop((IResource[]) data);
				} else {
					return handleSelectionDrop();
				}
			}

			private boolean handleResourceDrop(IResource[] resources) {
				File[] files = new File[resources.length];
				for (int i = 0; i < resources.length; i++) {
					files[i] = resources[i].getLocation()
						.toFile();
				}
				return handleFileDrop(files);
			}

			private boolean handleFileNameDrop(String[] paths) {
				File[] files = new File[paths.length];
				for (int i = 0; i < paths.length; i++) {
					files[i] = new File(paths[i]);
				}
				return handleFileDrop(files);
			}

			private boolean handleFileDrop(File[] files) {
				if (files.length > 0) {
					BndPreferences prefs = new BndPreferences();
					boolean hideWarning = prefs.getHideWarningExternalFile();
					if (!hideWarning) {
						MessageDialogWithToggle dialog = MessageDialogWithToggle.openWarning(getSection().getShell(),
							"Add External Files",
							"External files cannot be directly added to a project, they must be added to a local repository first.",
							"Do not show this warning again.", false, null, null);
						if (Window.CANCEL == dialog.getReturnCode())
							return false;
						if (dialog.getToggleState()) {
							prefs.setHideWarningExternalFile(true);
						}
					}

					AddFilesToRepositoryWizard wizard = new AddFilesToRepositoryWizard(null, files);
					WizardDialog dialog = new WizardDialog(getSection().getShell(), wizard);
					if (Window.OK == dialog.open()) {
						List<Pair<String, String>> addingBundles = wizard.getSelectedBundles();
						List<VersionedClause> addingClauses = new ArrayList<>(addingBundles.size());

						for (Pair<String, String> addingBundle : addingBundles) {
							Attrs attribs = new Attrs();
							attribs.put(Constants.VERSION_ATTRIBUTE, addingBundle.getSecond());
							addingClauses.add(new VersionedClause(addingBundle.getFirst(), attribs));
						}

						handleAdd(addingClauses);
					}
					return true;
				}
				return false;
			}

			private boolean handleSelectionDrop() {
				ISelection selection = LocalSelectionTransfer.getTransfer()
					.getSelection();
				if (selection.isEmpty() || !(selection instanceof IStructuredSelection)) {
					return false;
				}
				List<VersionedClause> adding = new LinkedList<>();
				Iterator<?> iterator = ((IStructuredSelection) selection).iterator();
				while (iterator.hasNext()) {
					Object item = iterator.next();
					if (item instanceof RepositoryBundle) {
						VersionedClause newClause = RepositoryBundleUtils.convertRepoBundle((RepositoryBundle) item,
							phase);
						adding.add(newClause);
					} else if (item instanceof RepositoryBundleVersion) {
						RepositoryBundleVersion bundleVersion = (RepositoryBundleVersion) item;
						VersionedClause newClause = RepositoryBundleUtils.convertRepoBundleVersion(bundleVersion,
							phase);
						adding.add(newClause);
					} else if (item instanceof RepositoryResourceElement) {
						RepositoryResourceElement elt = (RepositoryResourceElement) item;
						VersionedClause newClause = RepositoryBundleUtils
							.convertRepoBundleVersion(elt.getRepositoryBundleVersion(), phase);
						adding.add(newClause);
					} else if (item instanceof RepositoryFeature) {
						RepositoryFeature feature = (RepositoryFeature) item;
						// Create a clause in the canonical feature syntax:
						// id;version='V';type=org.eclipse.update.feature
						adding.add(RepositoryBundleUtils.convertRepoFeature(feature));
					} else if (item instanceof IncludedBundleItem) {
						IncludedBundleItem bundleItem = (IncludedBundleItem) item;
						VersionedClause newClause = new VersionedClause(bundleItem.getPlugin().id, new Attrs());
						// Set version if available and not default
						String version = bundleItem.getPlugin().version;
						if (version != null && !version.equals("0.0.0")) {
							newClause.setVersionRange(version);
						}
						adding.add(newClause);
					}
				}

				handleAdd(adding);
				return true;
			}

			private void handleAdd(Collection<VersionedClause> newClauses) {
				if (newClauses == null || newClauses.isEmpty())
					return;

				List<VersionedClause> toAdd = new LinkedList<>();
				for (VersionedClause newClause : newClauses) {
					boolean found = false;
					for (ListIterator<VersionedClause> iter = bundles.listIterator(); iter.hasNext();) {
						VersionedClause existing = iter.next();
						if (newClause.getName()
							.equals(existing.getName())
							&& Objects.equals(newClause.getAttribs()
								.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE),
								existing.getAttribs()
									.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE))) {
							int index = iter.previousIndex();
							iter.set(newClause);
							viewer.replace(newClause, index);

							found = true;
							break;
						}
					}
					if (!found)
						toAdd.add(newClause);
				}

				bundles.addAll(toAdd);
				viewer.add(toAdd.toArray());

				markDirty();
			}
		};
		dropAdapter.setFeedbackEnabled(false);
		dropAdapter.setExpandEnabled(false);
		viewer.addDropSupport(DND.DROP_COPY | DND.DROP_MOVE, new Transfer[] {
			LocalSelectionTransfer.getTransfer(), FileTransfer.getInstance(), ResourceTransfer.getInstance(),
			URLTransfer.getInstance()
		}, dropAdapter);

		table.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.character == SWT.DEL) {
					doRemove();
				} else if (e.character == '+') {
					doAdd();
				}
			}
		});

		// Layout
		GridLayout layout = new GridLayout(1, false);
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 50;
		gd.heightHint = getTableHeightHint();
		table.setLayoutData(gd);
	}

	private static boolean isRemovable(ISelection selection) {
		if (selection.isEmpty())
			return false;

		if (selection instanceof IStructuredSelection) {
			List<?> list = ((IStructuredSelection) selection).toList();
			for (Object object : list) {
				if (!(object instanceof VersionedClause)) {
					return false;
				}
			}
			return true;
		}

		return false;
	}

	/** Returns null to disable inherited display; subclasses may override. */
	protected List<VersionedClause> loadMergedFromModel(BndEditModel m) {
		return null;
	}

	protected int getTableHeightHint() {
		return SWT.DEFAULT;
	}

	protected List<VersionedClause> getBundles() {
		return bundles;
	}

	protected void setBundles(final List<VersionedClause> bundles) {
		this.bundles = bundles;
		Display.getDefault()
			.asyncExec(() -> viewer.setInput(buildDisplayList()));
	}

	private List<Object> buildDisplayList() {
		List<Object> combined = new ArrayList<>(inheritedBundles.size() + (bundles != null ? bundles.size() : 0));
		combined.addAll(inheritedBundles);
		if (bundles != null)
			combined.addAll(bundles);
		return combined;
	}

	private void doAdd() {
		try {
			RepoBundleSelectionWizard wizard = createBundleSelectionWizard(getBundles());
			if (wizard != null) {
				WizardDialog dialog = new WizardDialog(getSection().getShell(), wizard);
				if (dialog.open() == Window.OK) {
					setBundles(wizard.getSelectedBundles());
					markDirty();
				}
			}
		} catch (Exception e) {
			ErrorDialog.openError(getSection().getShell(), "Error", null,
				new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error opening bundle resolver wizard.", e));
		}
	}

	private void doRemove() {
		if (!isRemovable(viewer.getSelection()))
			return;

		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		if (!selection.isEmpty()) {
			Iterator<?> elements = selection.iterator();
			List<Object> removed = new LinkedList<>();
			while (elements.hasNext()) {
				Object element = elements.next();
				// Only local bundles can be removed; inherited stay in their source file.
				if (!inheritedBundles.contains(element) && bundles.remove(element))
					removed.add(element);
			}

			if (!removed.isEmpty()) {
				viewer.remove(removed.toArray());
				markDirty();
			}
		}
	}

	@Override
	public void commitToModel(boolean onSave) {
		saveToModel(model, bundles);
	}

	protected abstract void saveToModel(BndEditModel model, List<VersionedClause> bundles);

	protected abstract List<VersionedClause> loadFromModel(BndEditModel model);

	protected final RepoBundleSelectionWizard createBundleSelectionWizard(List<VersionedClause> bundles)
		throws Exception {
		RepoBundleSelectionWizard wizard = new RepoBundleSelectionWizard(bundles, phase);
		setSelectionWizardTitleAndMessage(wizard);

		return wizard;
	}

	protected abstract void setSelectionWizardTitleAndMessage(RepoBundleSelectionWizard wizard);

	@Override
	public void refreshFromModel() {
		// Compute inherited bundles (merged view minus local).
		List<VersionedClause> merged = loadMergedFromModel(model);
		List<VersionedClause> local = loadFromModel(model);
		if (local == null) local = new ArrayList<>();
		if (merged != null) {
			Set<String> localNames = new HashSet<>();
			for (VersionedClause vc : local)
				localNames.add(vc.getName());
			inheritedBundles.clear();
			for (VersionedClause vc : merged)
				if (!localNames.contains(vc.getName()))
					inheritedBundles.add(vc);
		} else {
			inheritedBundles.clear();
		}

		// Per-bundle provenance for the double-click handler.
		inheritedBundleProvenances = inheritedBundles.isEmpty() ? Collections.emptyMap()
			: BndEditModelAccessor.getInheritedBundleProvenances(model, propertyName);

		table.setToolTipText(inheritedBundles.isEmpty() ? null
			: "Some bundles are inherited from included files. Double-click an inherited item to open its source.");

		setBundles(new ArrayList<>(local));
	}

	private void openProvenanceByPath(String absolutePath) {
		File file = new File(absolutePath);
		if (!file.isFile())
			return;
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IFile iFile = root.getFileForLocation(new Path(absolutePath));
		if (iFile == null || !iFile.exists())
			return;
		try {
			PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow()
				.getActivePage()
				.openEditor(new FileEditorInput(iFile), BndEditor.WORKSPACE_EDITOR);
		} catch (PartInitException e) {
			ErrorDialog.openError(getSection().getShell(), "Error", null,
				new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Failed to open source file.", e));
		}
	}

	@Override
	public void initialize(IManagedForm form) {
		super.initialize(form);

		model = (BndEditModel) form.getInput();
		model.addPropertyChangeListener(propertyName, this);
	}

	@Override
	public void dispose() {
		super.dispose();
		if (model != null)
			model.removePropertyChangeListener(propertyName, this);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		IFormPage page = (IFormPage) getManagedForm().getContainer();
		if (page.isActive()) {
			refresh();
		} else {
			markStale();
		}
	}
}
