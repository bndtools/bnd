package bndtools.editor.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.bndtools.core.ui.resource.RequirementLabelProvider;
import org.bndtools.utils.dnd.AbstractViewerDropAdapter;
import org.bndtools.utils.dnd.SupportedTransfer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.bndtools.core.ui.StickyToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.FileEditorInput;
import org.osgi.resource.Requirement;

import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.osgi.resource.CapReqBuilder;
import bndtools.Plugin;
import bndtools.editor.BndEditor;
import bndtools.editor.common.BndEditorPart;
import bndtools.model.repo.DependencyPhase;
import bndtools.model.repo.FeatureVersionNode;
import bndtools.model.repo.IncludedBundleItem;
import bndtools.model.repo.ProjectBundle;
import bndtools.model.repo.RepositoryBundle;
import bndtools.model.repo.RepositoryBundleUtils;
import bndtools.model.repo.RepositoryBundleVersion;
import bndtools.model.repo.RepositoryFeature;
import bndtools.model.repo.RepositoryResourceElement;
import bndtools.preferences.BndPreferences;
import bndtools.wizards.repo.RepoBundleSelectionWizard;

public abstract class AbstractRequirementListPart extends BndEditorPart implements PropertyChangeListener {

	public AbstractRequirementListPart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style);
	}

	private final BndPreferences	preferences				= new BndPreferences();
	/** Local requirements: written to this file's own merge key. */
	private final List<Requirement>	requires				= new ArrayList<>();
	/** Inherited requirements: from included files, shown in gray (read-only). */
	private final List<Requirement>	inheritedRequires		= new ArrayList<>();

	private TableViewer				viewer;
	private ToolItem				addBundleTool;
	private ToolItem				removeTool;

	private boolean					committing				= false;
	/** Per-requirement provenance: maps each inherited requirement to the file path that defines it. */
	private Map<Requirement, String>	inheritedProvenances	= new java.util.LinkedHashMap<>();
	/** Local entries that are a single unexpanded macro reference (e.g. {@code ${name}}), mapped to their raw definition text. */
	private Map<Requirement, String>	macroExpansions	= new java.util.LinkedHashMap<>();
	/** Maps a local macro-reference entry to the file path where the referenced macro/property is defined. */
	private Map<Requirement, String>	macroProvenances		= new java.util.LinkedHashMap<>();
	/** The key used to write local requirements (plain stem or stem.local). */
	private String					localKey				= null;
	/** Extra property key subscribed dynamically (the localKey when it's a suffix). */
	private String					subscribedLocalKey		= null;

	/** Returns the primary bnd property key this part displays (e.g. {@code -runrequires}). */
	protected abstract String getPrimaryPropertyKey();

	/** Returns the local write key (plain stem or stem.local) determined during the last refresh. */
	protected final String getLocalKey() {
		return localKey != null ? localKey : getPrimaryPropertyKey();
	}

	/** Colors inherited items gray and local items with the default foreground. */
	private class MixedRequirementLabelProvider extends RequirementLabelProvider {
		private final Color grey;

		MixedRequirementLabelProvider(Display display) {
			grey = display.getSystemColor(SWT.COLOR_DARK_GRAY);
		}

		@Override
		public void update(ViewerCell cell) {
			super.update(cell);
			if (inheritedRequires.contains(cell.getElement())) {
				cell.setForeground(grey);
				// clear styled ranges so the grey foreground is not overridden
				cell.setStyleRanges(new StyleRange[0]);
			}
		}

		@Override
		public String getToolTipText(Object element) {
			String definition = macroExpansions.get(element);
			if (definition != null) {
				StringBuilder sb = new StringBuilder(definition);
				String prov = macroProvenances.get(element);
				if (prov != null) {
					sb.append("\n\nDouble-click to open ")
						.append(BndEditModelAccessor.getDisplayProvenance(model, prov));
				}
				return sb.toString();
			}
			if (inheritedRequires.contains(element)) {
				String prov = inheritedProvenances.get(element);
				return prov != null
					? "Inherited from " + BndEditModelAccessor.getDisplayProvenance(model, prov)
						+ "\nDouble-click to open source."
					: "Inherited from an included file.";
			}
			return null;
		}
	}

	protected TableViewer createViewer(Composite parent, FormToolkit tk) {
		Table table = tk.createTable(parent, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		viewer = new TableViewer(table);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setLabelProvider(new MixedRequirementLabelProvider(table.getDisplay()));
		StickyToolTipSupport.enableFor(viewer);

		// Listeners
		viewer.addSelectionChangedListener(event -> {
			IStructuredSelection sel = (IStructuredSelection) viewer.getSelection();
			boolean hasLocalSelected = !sel.isEmpty()
				&& sel.toList().stream().anyMatch(e -> requires.contains(e));
			removeTool.setEnabled(hasLocalSelected);
		});
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection sel = (IStructuredSelection) viewer.getSelection();
				if (sel.isEmpty())
					return;
				Requirement req = (Requirement) sel.getFirstElement();
				// Macro entries navigate to where the referenced macro/property is defined;
				// plain inherited entries navigate to the file that defines the merge key.
				String prov = macroProvenances.containsKey(req) ? macroProvenances.get(req)
					: inheritedProvenances.get(req);
				if (prov != null)
					openProvenanceFile(prov);
			}
		});
		table.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.character == SWT.DEL) {
					doRemove();
				} else if (e.character == '+') {
					doAddBundle();
				}
			}
		});
		RequirementViewerDropAdapter dropper = new RequirementViewerDropAdapter();
		dropper.install(viewer);

		return viewer;
	}

	protected void createToolBar(Section section) {
		ToolBar toolbar = new ToolBar(section, SWT.FLAT);
		section.setTextClient(toolbar);

		// Add Bundle
		addBundleTool = new ToolItem(toolbar, SWT.PUSH);
		addBundleTool.setImage(PlatformUI.getWorkbench()
			.getSharedImages()
			.getImage(ISharedImages.IMG_OBJ_ADD));
		addBundleTool.setToolTipText(getAddButtonLabel());
		addBundleTool.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doAddBundle();
			}
		});

		// Remove
		removeTool = new ToolItem(toolbar, SWT.PUSH);
		removeTool.setImage(PlatformUI.getWorkbench()
			.getSharedImages()
			.getImage(ISharedImages.IMG_TOOL_DELETE));
		removeTool.setDisabledImage(PlatformUI.getWorkbench()
			.getSharedImages()
			.getImage(ISharedImages.IMG_TOOL_DELETE_DISABLED));
		removeTool.setToolTipText("Remove");
		removeTool.setEnabled(false);
		removeTool.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doRemove();
			}
		});
	}

	private class RequirementViewerDropAdapter extends AbstractViewerDropAdapter {

		public RequirementViewerDropAdapter() {
			super(viewer, EnumSet.of(SupportedTransfer.LocalSelection));
		}

		@Override
		protected boolean performSelectionDrop(ISelection data, Object target, int location) {
			Set<Requirement> adding = new LinkedHashSet<>();

			if (data instanceof IStructuredSelection) {
				IStructuredSelection structSel = (IStructuredSelection) data;
				MultiStatus status = new MultiStatus(Plugin.PLUGIN_ID, 0, "Error adding one or more requirements",
					null);
				for (Object elem : structSel.toList()) {
					try {
						Requirement requirement = createRequirement(elem);
						adding.add(requirement);
					} catch (Exception e) {
						status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error generating requirement", e));
					}
				}

				if (!status.isOK())
					ErrorDialog.openError(getSection().getShell(), "Error", null, status);
			}

			return updateViewerWithNewRequirements(adding);
		}
	}

	protected abstract String getAddButtonLabel();

	protected abstract void doCommitToModel(List<Requirement> requires);

	protected abstract List<Requirement> doRefreshFromModel();

	private void doAddBundle() {
		try {
			RepoBundleSelectionWizard wizard = new RepoBundleSelectionWizard(new ArrayList<VersionedClause>(),
				DependencyPhase.Req);
			wizard.setSelectionPageTitle(getAddButtonLabel());
			WizardDialog dialog = new WizardDialog(getSection().getShell(), wizard);

			if (Window.OK == dialog.open()) {
				List<VersionedClause> result = wizard.getSelectedBundles();
				Set<Requirement> adding = new LinkedHashSet<>(result.size());
				for (VersionedClause bundle : result) {
					Requirement req = createRequirement(bundle);
					adding.add(req);
				}
				updateViewerWithNewRequirements(adding);
			}
		} catch (Exception e) {
			ErrorDialog.openError(getSection().getShell(), "Error", null,
				new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error selecting bundles.", e));
		}
	}

	private void doRemove() {
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		if (!selection.isEmpty()) {
			// Only local items may be removed; inherited ones stay in their source file.
			@SuppressWarnings("unchecked")
			List<Object> removed = ((List<Object>) selection.toList()).stream()
				.filter(e -> this.requires.remove(e))
				.collect(Collectors.toList());
			if (!removed.isEmpty()) {
				viewer.remove(removed.toArray());
				markDirty();
			}
		}
	}

	@Override
	public final void commitToModel(boolean onSave) {
		try {
			committing = true;
			doCommitToModel(Collections.unmodifiableList(this.requires));
		} finally {
			committing = false;
		}
	}

	@Override
	protected final void refreshFromModel() {
		// Local requirements: only what is defined in this file's own merge keys.
		List<Requirement> newLocal = doRefreshFromModel();
		if (newLocal == null)
			newLocal = Collections.emptyList();

		// Inherited requirements: merged view minus local.
		String primaryKey = getPrimaryPropertyKey();
		List<Requirement> newInherited = Collections.emptyList();
		if (primaryKey != null && model != null) {
			List<Requirement> merged = BndEditModelAccessor.getMergedRequirements(model, primaryKey);
			if (merged != null && !merged.isEmpty()) {
				Set<Requirement> localSet = new HashSet<>(newLocal);
				newInherited = merged.stream()
					.filter(r -> !localSet.contains(r))
					.collect(Collectors.toList());
			}
		}

		// Local entries that are a single unexpanded macro reference (e.g. ${name}) are kept
		// collapsed as-is (whether defined locally or inherited from an included file); their
		// definition text is only shown via tooltip/double-click.
		Map<Requirement, String> newMacroExpansions = new java.util.LinkedHashMap<>();
		Map<Requirement, String> newMacroProvenances = new java.util.LinkedHashMap<>();
		if (model != null) {
			List<Requirement> candidates = new ArrayList<>(newLocal.size() + newInherited.size());
			candidates.addAll(newLocal);
			candidates.addAll(newInherited);
			for (Requirement candidate : candidates) {
				Optional<String> macroName = BndEditModelAccessor.getMacroReferenceName(candidate);
				if (macroName.isEmpty())
					continue;
				String definition = BndEditModelAccessor.getMacroDefinitionText(model, macroName.get());
				if (definition == null)
					continue;
				newMacroExpansions.put(candidate, definition);
				BndEditModelAccessor.getPropertyProvenance(model, macroName.get())
					.ifPresent(prov -> newMacroProvenances.put(candidate, prov));
			}
		}
		this.macroExpansions = newMacroExpansions;
		this.macroProvenances = newMacroProvenances;

		// Determine which key local additions should be written to.
		String newLocalKey = primaryKey;
		if (primaryKey != null && model != null) {
			String existingKey = BndEditModelAccessor.findLocalMergeKey(model, primaryKey);
			if (existingKey != null) {
				newLocalKey = existingKey;
			} else if (!newInherited.isEmpty()) {
				// No local key yet but inherited items exist: use a suffix so bnd merges them.
				newLocalKey = primaryKey + ".local";
			}
		}
		localKey = newLocalKey;

		// Keep the property-change subscription aligned with the local key.
		if (!Objects.equals(subscribedLocalKey, newLocalKey)) {
			if (subscribedLocalKey != null && model != null)
				model.removePropertyChangeListener(subscribedLocalKey, this);
			subscribedLocalKey = newLocalKey;
			if (newLocalKey != null && model != null
				&& !Arrays.asList(getProperties()).contains(newLocalKey))
				model.addPropertyChangeListener(newLocalKey, this);
		}

		// Update provenance tooltip for inherited items.
		if (!newInherited.isEmpty()) {
			inheritedProvenances = BndEditModelAccessor.getInheritedRequirementProvenances(model, primaryKey);
			String tip = inheritedProvenances.values().stream().findAny().isPresent()
				? "Some requirements are inherited from included files. Double-click an inherited item to open its source."
				: "Some requirements are inherited from included files.";
			viewer.getControl().setToolTipText(tip);
		} else {
			inheritedProvenances = Collections.emptyMap();
			viewer.getControl().setToolTipText(null);
		}

		addBundleTool.setEnabled(true);
		removeTool.setEnabled(false);

		if (newInherited.equals(this.inheritedRequires) && newLocal.equals(this.requires))
			return;

		this.inheritedRequires.clear();
		this.inheritedRequires.addAll(newInherited);
		this.requires.clear();
		this.requires.addAll(newLocal);

		List<Requirement> combined = new ArrayList<>(this.inheritedRequires.size() + this.requires.size());
		combined.addAll(this.inheritedRequires);
		combined.addAll(this.requires);
		viewer.setInput(combined);
	}

	@Override
	public void dispose() {
		if (subscribedLocalKey != null && model != null) {
			model.removePropertyChangeListener(subscribedLocalKey, this);
			subscribedLocalKey = null;
		}
		super.dispose();
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (!committing) {
			IFormPage page = (IFormPage) getManagedForm().getContainer();
			if (page.isActive()) {
				refresh();
			} else {
				markStale();
			}
		}
	}

	private void openProvenanceFile(String absolutePath) {
		File file = new File(absolutePath);
		if (!file.isFile())
			return;
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace()
			.getRoot();
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

	/** Adds new requirements as local items. Items already in inherited or local lists are skipped. */
	private boolean updateViewerWithNewRequirements(Set<Requirement> adding) {
		adding.removeAll(this.inheritedRequires);
		adding.removeAll(this.requires);
		if (adding.isEmpty())
			return false;
		this.requires.addAll(adding);
		viewer.add(adding.toArray());
		markDirty();
		return true;
	}

	private Requirement createRequirement(Object elem) throws Exception {
		final String bsn;
		String versionRange = null;
		boolean isFeature = false;

		if (elem instanceof RepositoryFeature) {
			// Check RepositoryFeature BEFORE RepositoryBundle since both extend RepositoryEntry
			RepositoryFeature rf = (RepositoryFeature) elem;
			bsn = rf.getBsn();
			String version = rf.getFeature().getVersion();
			if (version != null && !version.equals("0.0.0")) {
				versionRange = version;
			}
			isFeature = true;
		} else if (elem instanceof FeatureVersionNode) {
			FeatureVersionNode versionNode = (FeatureVersionNode) elem;
			bsn = versionNode.getFeature().getId();
			String version = versionNode.getVersion();
			if (version != null && !version.equals("0.0.0")) {
				versionRange = version;
			}
			isFeature = true;
		} else if (elem instanceof RepositoryBundle) {
			bsn = ((RepositoryBundle) elem).getBsn();
		} else if (elem instanceof RepositoryBundleVersion) {
			RepositoryBundleVersion rbv = (RepositoryBundleVersion) elem;
			bsn = rbv.getBsn();
			versionRange = RepositoryBundleUtils.toVersionRangeUpToNextMajor(rbv.getVersion())
				.toString();
		} else if (elem instanceof ProjectBundle) {
			bsn = ((ProjectBundle) elem).getBsn();
		} else if (elem instanceof VersionedClause) {
			VersionedClause clause = (VersionedClause) elem;
			bsn = clause.getName();
			versionRange = clause.getVersionRange();
		} else if (elem instanceof RepositoryResourceElement rre) {
			RepositoryBundleVersion repositoryBundleVersion = rre.getRepositoryBundleVersion();
			bsn = repositoryBundleVersion.getBsn();
			versionRange = RepositoryBundleUtils.toVersionRangeUpToNextMajor(repositoryBundleVersion.getVersion())
				.toString();
		} else if (elem instanceof IncludedBundleItem ibi) {
			bsn = ibi.getPlugin().id;
			String version = ibi.getPlugin().version;
			if (version != null && !version.equals("0.0.0")) {
				versionRange = version;
			}
		} else {
			throw new IllegalArgumentException("Unable to derive identity from an object of type " + elem.getClass()
				.getSimpleName());
		}

		final CapReqBuilder reqBuilder;
		if (preferences.getUseAliasRequirements()) {
			reqBuilder = new CapReqBuilder("bnd.identity").addAttribute("id", bsn);
			if (versionRange != null)
				reqBuilder.addAttribute("version", versionRange);
			if (isFeature)
				reqBuilder.addAttribute("type", "org.eclipse.update.feature");
		} else {
			reqBuilder = CapReqBuilder.createBundleRequirement(bsn, versionRange);
		}
		return reqBuilder.buildSyntheticRequirement();
	}

}
