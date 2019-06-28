package bndtools.editor.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.bndtools.core.ui.resource.RequirementLabelProvider;
import org.bndtools.utils.dnd.AbstractViewerDropAdapter;
import org.bndtools.utils.dnd.SupportedTransfer;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;

import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.libg.filters.AndFilter;
import aQute.libg.filters.Filter;
import aQute.libg.filters.Operator;
import aQute.libg.filters.SimpleFilter;
import bndtools.Plugin;
import bndtools.editor.common.BndEditorPart;
import bndtools.model.repo.DependencyPhase;
import bndtools.model.repo.ProjectBundle;
import bndtools.model.repo.RepositoryBundle;
import bndtools.model.repo.RepositoryBundleVersion;
import bndtools.preferences.BndPreferences;
import bndtools.wizards.repo.RepoBundleSelectionWizard;

public abstract class AbstractRequirementListPart extends BndEditorPart implements PropertyChangeListener {

	public AbstractRequirementListPart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style);
	}

	private final BndPreferences	preferences	= new BndPreferences();
	private final List<Requirement>	requires	= new ArrayList<>();

	private TableViewer				viewer;
	private ToolItem				addBundleTool;
	private ToolItem				removeTool;

	private boolean					committing	= false;

	protected TableViewer createViewer(Composite parent, FormToolkit tk) {
		Table table = tk.createTable(parent, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		viewer = new TableViewer(table);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setLabelProvider(new RequirementLabelProvider());

		// Listeners
		viewer.addSelectionChangedListener(event -> removeTool.setEnabled(!viewer.getSelection()
			.isEmpty()));
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
				DependencyPhase.Run);
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
			Iterator<?> elements = selection.iterator();
			List<Object> removed = new LinkedList<>();
			while (elements.hasNext()) {
				Object element = elements.next();
				if (this.requires.remove(element))
					removed.add(element);
			}

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
		this.requires.clear();
		List<Requirement> loadedReqs = doRefreshFromModel();
		if (loadedReqs != null)
			this.requires.addAll(loadedReqs);
		viewer.setInput(this.requires);
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

	/**
	 * Update the requirements already available with new ones. Already existing
	 * requirements will be removed from the given set.
	 *
	 * @param adding Set with {@link Requirement}s to add
	 * @return true if requirements were added.
	 */
	private boolean updateViewerWithNewRequirements(Set<Requirement> adding) {
		// remove duplicates
		adding.removeAll(this.requires);
		if (adding.isEmpty()) {
			return false;
		}
		this.requires.addAll(adding);
		viewer.add(adding.toArray());
		markDirty();
		return true;
	}

	private Requirement createRequirement(Object elem) throws Exception {
		final String bsn;
		String versionRange = null;

		if (elem instanceof RepositoryBundle) {
			bsn = ((RepositoryBundle) elem).getBsn();
		} else if (elem instanceof RepositoryBundleVersion) {
			RepositoryBundleVersion rbv = (RepositoryBundleVersion) elem;
			bsn = rbv.getBsn();
			versionRange = rbv.getVersion()
				.toString();
		} else if (elem instanceof ProjectBundle) {
			bsn = ((ProjectBundle) elem).getBsn();
		} else if (elem instanceof VersionedClause) {
			VersionedClause clause = (VersionedClause) elem;
			bsn = clause.getName();
			versionRange = clause.getVersionRange();
		} else {
			throw new IllegalArgumentException("Unable to derive identity from an object of type " + elem.getClass()
				.getSimpleName());
		}

		final CapReqBuilder reqBuilder;
		if (preferences.getUseAliasRequirements()) {
			reqBuilder = new CapReqBuilder("bnd.identity").addAttribute("id", bsn);
			if (versionRange != null)
				reqBuilder.addAttribute("version", versionRange);
		} else {
			Filter filter = new SimpleFilter(IdentityNamespace.IDENTITY_NAMESPACE, bsn);
			if (versionRange != null)
				filter = new AndFilter().addChild(filter)
					.addChild(new SimpleFilter("version", Operator.GreaterThanOrEqual, versionRange));
			reqBuilder = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE)
				.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter.toString());
		}
		return reqBuilder.buildSyntheticRequirement();
	}

}
