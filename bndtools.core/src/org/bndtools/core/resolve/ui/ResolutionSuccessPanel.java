package org.bndtools.core.resolve.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.bndtools.core.resolve.ResolutionResult;
import org.bndtools.utils.resources.ResourceUtils;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;
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

    private final BndEditModel model;
    private final ResolutionResultPresenter presenter;

    private final ResolutionTreeContentProvider reasonsContentProvider = new ResolutionTreeContentProvider();
    private final List<Resource> checkedOptional = new ArrayList<Resource>();

    private Composite composite;
    private TableViewer requiredViewer;
    private CheckboxTableViewer optionalViewer;
    private TreeViewer reasonsViewer;
    private Button btnAddResolveOptional;
    private ResolutionResult result;

    public ResolutionSuccessPanel(BndEditModel model, ResolutionResultPresenter presenter) {
        this.model = model;
        this.presenter = presenter;
    }

    public void createControl(Composite parent) {
        FormToolkit toolkit = new FormToolkit(parent.getDisplay());
        composite = toolkit.createComposite(parent);

        GridLayout layout = new GridLayout(1, false);
        composite.setLayout(layout);

        GridData gd;

        Section sectRequired = toolkit.createSection(composite, Section.TITLE_BAR | Section.EXPANDED);
        sectRequired.setText("Required Resources");

        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.widthHint = 200;
        gd.heightHint = 150;
        sectRequired.setLayoutData(gd);

        Table tblRequired = toolkit.createTable(sectRequired, SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL);
        sectRequired.setClient(tblRequired);

        requiredViewer = new TableViewer(tblRequired);
        requiredViewer.setContentProvider(ArrayContentProvider.getInstance());
        requiredViewer.setLabelProvider(new ResourceLabelProvider());
        requiredViewer.setSorter(new BundleSorter());

        requiredViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                IStructuredSelection sel = (IStructuredSelection) event.getSelection();
                Resource resource = (Resource) sel.getFirstElement();

                reasonsContentProvider.setOptional(false);
                if (result != null)
                    reasonsContentProvider.setResolution(result.getResourceWirings());

                reasonsViewer.setInput(resource);
                reasonsViewer.expandToLevel(2);
            }
        });

        Section sectOptional = toolkit.createSection(composite, Section.TITLE_BAR | Section.TWISTIE);
        sectOptional.setText("Optional Resources");

        Composite cmpOptional = toolkit.createComposite(sectOptional);
        sectOptional.setClient(cmpOptional);
        cmpOptional.setLayout(new GridLayout(2, false));

        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        gd.widthHint = 200;
        sectOptional.setLayoutData(gd);

        Table tblOptional = toolkit.createTable(cmpOptional, SWT.BORDER | SWT.CHECK | SWT.FULL_SELECTION | SWT.H_SCROLL);
        tblOptional.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

        optionalViewer = new CheckboxTableViewer(tblOptional);
        optionalViewer.setContentProvider(ArrayContentProvider.getInstance());
        optionalViewer.setLabelProvider(new ResourceLabelProvider());
        optionalViewer.setSorter(new BundleSorter());

        optionalViewer.addCheckStateListener(new ICheckStateListener() {
            @Override
            public void checkStateChanged(CheckStateChangedEvent event) {
                Resource resource = (Resource) event.getElement();
                if (event.getChecked()) {
                    checkedOptional.add(resource);
                } else {
                    checkedOptional.remove(resource);
                }
                presenter.updateButtons();
            }
        });

        Composite cmpOptionalButtons = toolkit.createComposite(cmpOptional);
        cmpOptionalButtons.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        GridLayout gl_cmpOptionalButtons = new GridLayout(3, false);
        gl_cmpOptionalButtons.marginHeight = 0;
        gl_cmpOptionalButtons.marginWidth = 0;
        cmpOptionalButtons.setLayout(gl_cmpOptionalButtons);

        btnAddResolveOptional = toolkit.createButton(cmpOptionalButtons, "Add and Resolve", SWT.NONE);
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
                // TODO
            }
        });
        btnAllOptional.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        Button btnClearOptional = toolkit.createButton(cmpOptionalButtons, "Clear All", SWT.NONE);
        btnClearOptional.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                checkedOptional.clear();
                optionalViewer.setCheckedElements(checkedOptional.toArray());
                presenter.updateButtons();
            }
        });
        btnClearOptional.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        Section sectReason = toolkit.createSection(composite, Section.TITLE_BAR | Section.EXPANDED);
        sectReason.setText("Reasons");

        Tree tblReasons = new Tree(sectReason, SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL);
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

        ArrayList<Requirement> newRequires = new ArrayList<Requirement>(oldRequires.size() + checkedOptional.size());
        newRequires.addAll(oldRequires);

        for (Resource resource : checkedOptional) {
            Requirement req = resourceToRequirement(resource);
            newRequires.add(req);
        }

        model.setRunRequires(newRequires);
        checkedOptional.clear();
        presenter.recalculate();
    }

    public void setInput(ResolutionResult result) {
        this.result = result;
        checkedOptional.clear();

        Map<Resource,List<Wire>> wirings = (result != null) ? result.getResourceWirings() : null;
        requiredViewer.setInput(wirings != null ? wirings.keySet() : null);

        if (!checkedOptional.isEmpty()) {
            btnAddResolveOptional.setEnabled(true);
            presenter.setMessage("Click 'Add and Resolve' to add the checked optional bundles to requirements and re-resolve.", IMessageProvider.INFORMATION);
        } else {
            btnAddResolveOptional.setEnabled(false);
            presenter.setMessage(null, IMessageProvider.INFORMATION);
        }
    }

    public boolean isComplete() {
        return checkedOptional.isEmpty();
    }

    public void dispose() {}

    private static Requirement resourceToRequirement(Resource resource) {
        Capability identity = ResourceUtils.getIdentityCapability(resource);
        String id = ResourceUtils.getIdentity(identity);
        Version version = ResourceUtils.getVersion(identity);
        Version dropQualifier = new Version(version.getMajor(), version.getMinor(), version.getMicro());

        AndFilter filter = new AndFilter();
        filter.addChild(new SimpleFilter(IdentityNamespace.IDENTITY_NAMESPACE, id));
        filter.addChild(new LiteralFilter(Filters.fromVersionRange(dropQualifier.toString())));

        Requirement req = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE).addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter.toString()).buildSyntheticRequirement();
        return req;
    }
}
