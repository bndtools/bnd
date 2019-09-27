package org.bndtools.core.resolve.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.bndtools.core.resolve.ResolutionResult;
import org.bndtools.utils.resources.ResourceUtils;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.Filters;
import aQute.libg.filters.AndFilter;
import aQute.libg.filters.LiteralFilter;
import aQute.libg.filters.SimpleFilter;

public class ResolutionSuccessPanel {

	private final BndEditModel					model;
	private final ResolutionResultPresenter		presenter;

	private final ResolutionTreeContentProvider	reasonsContentProvider	= new ResolutionTreeContentProvider();
	private final List<Resource>				checkedOptional			= new ArrayList<>();
	private final Map<Resource, Requirement>	addedOptionals			= new HashMap<>();

	private Composite							composite;
	private TableViewer							requiredViewer;
	private CheckboxTableViewer					optionalViewer;
	private TreeViewer							reasonsViewer;
	private Button								btnAddResolveOptional;
	private ResolutionResult					result;
	private Section								sectOptional;

	public ResolutionSuccessPanel(BndEditModel model, ResolutionResultPresenter presenter) {
		this.model = model;
		this.presenter = presenter;
	}

	public void createControl(Composite parent) {
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		composite = toolkit.createComposite(parent);

		GridLayout layout = new GridLayout(1, false);
		composite.setLayout(layout);

		SashForm form = new SashForm(composite, SWT.VERTICAL);
		form.setLayout(new GridLayout(1, false));
		form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		GridData gd;

		Section sectRequired = toolkit.createSection(form,
			ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED);
		sectRequired.setText("Required Resources");

		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 200;
		gd.heightHint = 150;
		sectRequired.setLayoutData(gd);

		Table tblRequired = toolkit.createTable(sectRequired,
			SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
		sectRequired.setClient(tblRequired);

		requiredViewer = new TableViewer(tblRequired);
		requiredViewer.setContentProvider(ArrayContentProvider.getInstance());
		requiredViewer.setLabelProvider(new ResourceLabelProvider());
		requiredViewer.setComparator(new BundleSorter());

		requiredViewer.addSelectionChangedListener(event -> {
			IStructuredSelection sel = (IStructuredSelection) event.getSelection();
			Resource resource = (Resource) sel.getFirstElement();

			reasonsContentProvider.setOptional(false);
			if (result != null)
				reasonsContentProvider.setResolution(result.getResourceWirings());

			reasonsViewer.setInput(resource);
			reasonsViewer.expandToLevel(2);
		});

		sectOptional = toolkit.createSection(form,
			ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED);
		sectOptional.setText("Optional Resources");

		Composite cmpOptional = toolkit.createComposite(sectOptional);
		sectOptional.setClient(cmpOptional);
		cmpOptional.setLayout(new GridLayout(2, false));

		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 200;
		gd.heightHint = 150;
		sectOptional.setLayoutData(gd);

		Table tblOptional = toolkit.createTable(cmpOptional,
			SWT.BORDER | SWT.CHECK | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		tblOptional.setLayoutData(gd);

		optionalViewer = new CheckboxTableViewer(tblOptional);
		optionalViewer.setContentProvider(ArrayContentProvider.getInstance());
		optionalViewer.setLabelProvider(new ResourceLabelProvider());
		optionalViewer.setComparator(new BundleSorter());

		optionalViewer.addSelectionChangedListener(event -> {
			IStructuredSelection sel = (IStructuredSelection) event.getSelection();
			doOptionalReasonUpdate((Resource) sel.getFirstElement());
		});

		optionalViewer.addCheckStateListener(event -> {
			Resource resource = (Resource) event.getElement();
			if (!addedOptionals.containsKey(resource)) {
				if (event.getChecked()) {
					checkedOptional.add(resource);
				} else {
					checkedOptional.remove(resource);
				}
			}
			presenter.updateButtons();
			updateResolveOptionalButton();

			optionalViewer.setSelection(() -> true);

			doOptionalReasonUpdate(resource);
		});

		Composite cmpOptionalButtons = toolkit.createComposite(cmpOptional);
		cmpOptionalButtons.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		GridLayout gl_cmpOptionalButtons = new GridLayout(3, false);
		gl_cmpOptionalButtons.marginHeight = 0;
		gl_cmpOptionalButtons.marginWidth = 0;
		cmpOptionalButtons.setLayout(gl_cmpOptionalButtons);

		btnAddResolveOptional = toolkit.createButton(cmpOptionalButtons, "Update and Resolve", SWT.NONE);
		btnAddResolveOptional.setEnabled(false);
		btnAddResolveOptional.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doAddResolve();
			}
		});
		btnAddResolveOptional.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false));

		Button btnAllOptional = toolkit.createButton(cmpOptionalButtons, "Select All", SWT.NONE);
		btnAllOptional.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				optionalViewer.setAllChecked(true);
				checkedOptional.clear();
				for (Object object : optionalViewer.getCheckedElements()) {
					if (!addedOptionals.containsKey(object)) {
						checkedOptional.add((Resource) object);
					}
				}
				presenter.updateButtons();
				updateResolveOptionalButton();
			}
		});
		btnAllOptional.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		Button btnClearOptional = toolkit.createButton(cmpOptionalButtons, "Clear All", SWT.NONE);
		btnClearOptional.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				optionalViewer.setAllChecked(false);
				checkedOptional.clear();
				presenter.updateButtons();
				updateResolveOptionalButton();
			}
		});
		btnClearOptional.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		Section sectReason = toolkit.createSection(form,
			ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED);
		sectReason.setText("Reasons");

		Tree tblReasons = new Tree(sectReason, SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
		sectReason.setClient(tblReasons);

		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 200;
		gd.heightHint = 150;
		sectReason.setLayoutData(gd);

		reasonsViewer = new TreeViewer(tblReasons);
		reasonsViewer.setContentProvider(reasonsContentProvider);
		reasonsViewer.setLabelProvider(new ResolutionTreeLabelProvider());
	}

	public Control getControl() {
		return composite;
	}

	private void doAddResolve() {
		List<Requirement> oldRequires = model.getRunRequires();
		if (oldRequires == null)
			oldRequires = Collections.emptyList();

		ArrayList<Requirement> newRequires = new ArrayList<>(oldRequires.size() + checkedOptional.size());
		newRequires.addAll(oldRequires);

		for (Iterator<Entry<Resource, Requirement>> it = addedOptionals.entrySet()
			.iterator(); it.hasNext();) {
			Entry<Resource, Requirement> entry = it.next();
			if (!optionalViewer.getChecked(entry.getKey())) {
				newRequires.remove(entry.getValue());
				it.remove();
			}
		}

		for (Resource resource : checkedOptional) {
			Requirement req = resourceToRequirement(resource);
			addedOptionals.put(resource, req);
			newRequires.add(req);
		}

		model.setRunRequires(newRequires);
		checkedOptional.clear();
		presenter.recalculate();
	}

	public void setInput(ResolutionResult result) {
		this.result = result;
		checkedOptional.clear();

		Collection<Resource> wirings = null;
		if (result != null && result.getResolution() != null && result.getResolution()
			.isOK() && result.getResolution().required != null) {
			wirings = result.getResolution()
				.getOrderedResources();
		}

		requiredViewer.setInput(wirings != null ? wirings : null);
		wirings = (result != null && result.getOptionalResources() != null)
			? new HashSet<>(result.getOptionalResources()
				.keySet())
			: new HashSet<>();

		wirings.addAll(addedOptionals.keySet());

		if (wirings.isEmpty()) {
			sectOptional.setExpanded(false);
		} else {
			sectOptional.setExpanded(true);
		}
		optionalViewer.setInput(wirings.isEmpty() ? null : wirings);
		optionalViewer.setGrayedElements(addedOptionals.keySet()
			.toArray());
		optionalViewer.setCheckedElements(addedOptionals.keySet()
			.toArray());

		for (TableItem tableItem : optionalViewer.getTable()
			.getItems()) {
			Display display = Display.getCurrent();
			Color addedColor = display.getSystemColor(SWT.COLOR_GRAY);
			if (tableItem.getGrayed()) {
				tableItem.setBackground(addedColor);
			} else {
				tableItem.setBackground(null);
			}
		}

		updateResolveOptionalButton();
	}

	private void updateResolveOptionalButton() {
		if (!checkedOptional.isEmpty() || previouslyAddedNowRemoved()) {
			btnAddResolveOptional.setEnabled(true);
			presenter.setMessage(
				"Click 'Update and Resolve' to add the checked optional bundles to requirements and re-resolve.",
				IMessageProvider.INFORMATION);
		} else {
			btnAddResolveOptional.setEnabled(false);
			presenter.setMessage(null, IMessageProvider.INFORMATION);
		}
	}

	private boolean previouslyAddedNowRemoved() {
		for (Resource resource : addedOptionals.keySet()) {
			if (!optionalViewer.getChecked(resource)) {
				return true;
			}
		}
		return false;
	}

	public boolean isComplete() {
		return checkedOptional.isEmpty();
	}

	public void dispose() {}

	private void doOptionalReasonUpdate(Resource resource) {
		reasonsContentProvider.setOptional(true);
		if (result != null) {
			Map<Resource, List<Wire>> combined = new HashMap<>(result.getResourceWirings());
			combined.putAll(result.getOptionalResources());
			reasonsContentProvider.setResolution(combined);
		}

		reasonsViewer.setInput(resource);
		reasonsViewer.expandToLevel(2);
	}

	private static Requirement resourceToRequirement(Resource resource) {
		Capability identity = ResourceUtils.getIdentityCapability(resource);
		String id = ResourceUtils.getIdentity(identity);
		Version version = ResourceUtils.getVersion(identity);
		Version dropQualifier = new Version(version.getMajor(), version.getMinor(), version.getMicro());

		AndFilter filter = new AndFilter();
		filter.addChild(new SimpleFilter(IdentityNamespace.IDENTITY_NAMESPACE, id));
		filter.addChild(new LiteralFilter(Filters.fromVersionRange(dropQualifier.toString())));

		Requirement req = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE)
			.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter.toString())
			.buildSyntheticRequirement();
		return req;
	}
}
