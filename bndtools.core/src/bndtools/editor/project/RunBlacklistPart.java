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
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
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
import aQute.bnd.osgi.resource.Filters;
import aQute.bnd.version.Version;
import aQute.libg.filters.AndFilter;
import aQute.libg.filters.Filter;
import aQute.libg.filters.LiteralFilter;
import aQute.libg.filters.Operator;
import aQute.libg.filters.SimpleFilter;
import bndtools.BndConstants;
import bndtools.Plugin;
import bndtools.editor.common.BndEditorPart;
import bndtools.model.repo.DependencyPhase;
import bndtools.model.repo.ProjectBundle;
import bndtools.model.repo.RepositoryBundle;
import bndtools.model.repo.RepositoryBundleVersion;
import bndtools.wizards.repo.RepoBundleSelectionWizard;

public class RunBlacklistPart extends BndEditorPart implements PropertyChangeListener {

    private static final String[] SUBSCRIBE_PROPS = new String[] {
            BndConstants.RUNBLACKLIST
    };

    private Table table;
    private TableViewer viewer;

    private List<Requirement> blacklists;

    private ToolItem addBundleTool;
    private ToolItem removeTool;

    private boolean committing = false;

    public RunBlacklistPart(Composite parent, FormToolkit toolkit, int style) {
        super(parent, toolkit, style);
        createSection(getSection(), toolkit);
    }

    @Override
    protected String[] getProperties() {
        return SUBSCRIBE_PROPS;
    }

    private void createSection(Section section, FormToolkit tk) {
        section.setText("Run Blacklist");
        section.setDescription("The specified requirements will be excluded from the resolution.");

        // Create toolbar
        ToolBar toolbar = new ToolBar(section, SWT.FLAT);
        section.setTextClient(toolbar);
        fillToolBar(toolbar);

        // Create main panel
        Composite composite = tk.createComposite(section);
        section.setClient(composite);

        table = tk.createTable(composite, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
        viewer = new TableViewer(table);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setLabelProvider(new RequirementLabelProvider());

        // Listeners
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                removeTool.setEnabled(!viewer.getSelection().isEmpty());
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

        // Layout
        GridLayout layout;
        GridData gd;

        layout = new GridLayout(2, false);
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 5;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        composite.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
        gd.widthHint = 50;
        gd.heightHint = 50;
        table.setLayoutData(gd);

        gd = new GridData(SWT.RIGHT, SWT.CENTER, true, false);
    }

    private void fillToolBar(ToolBar toolbar) {
        // Add Bundle
        addBundleTool = new ToolItem(toolbar, SWT.PUSH);
        addBundleTool.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ADD));
        addBundleTool.setToolTipText("Add Bundle Requirement");
        addBundleTool.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doAddBundle();
            }
        });

        // Remove
        removeTool = new ToolItem(toolbar, SWT.PUSH);
        removeTool.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE));
        removeTool.setDisabledImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE_DISABLED));
        removeTool.setToolTipText("Remove");
        removeTool.setEnabled(false);
        removeTool.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doRemove();
            }
        });
    }

    private static Requirement createRequirement(Object elem) {
        String bsn = null;
        Version version = null;

        if (elem instanceof RepositoryBundle) {
            bsn = ((RepositoryBundle) elem).getBsn();
        } else if (elem instanceof RepositoryBundleVersion) {
            RepositoryBundleVersion rbv = (RepositoryBundleVersion) elem;
            bsn = rbv.getBsn();
            version = rbv.getVersion();
        } else if (elem instanceof ProjectBundle) {
            bsn = ((ProjectBundle) elem).getBsn();
        }

        if (bsn != null) {
            Filter filter = new SimpleFilter(IdentityNamespace.IDENTITY_NAMESPACE, bsn);
            if (version != null) {
                filter = new AndFilter().addChild(filter).addChild(new SimpleFilter("version", Operator.GreaterThanOrEqual, version.toString()));
            }
            Requirement req = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE).addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter.toString()).buildSyntheticRequirement();
            return req;
        }
        return null;
    }

    private void doAddBundle() {
        try {
            RepoBundleSelectionWizard wizard = new RepoBundleSelectionWizard(new ArrayList<VersionedClause>(), DependencyPhase.Run);
            wizard.setSelectionPageTitle("Add Bundle Blacklist");
            WizardDialog dialog = new WizardDialog(getSection().getShell(), wizard);

            if (Window.OK == dialog.open()) {
                List<VersionedClause> result = wizard.getSelectedBundles();
                Set<Requirement> adding = new LinkedHashSet<Requirement>(result.size());
                for (VersionedClause bundle : result) {
                    Filter filter = new SimpleFilter(IdentityNamespace.IDENTITY_NAMESPACE, bundle.getName());

                    String versionRange = bundle.getVersionRange();
                    if (versionRange != null && !"latest".equals(versionRange)) {
                        filter = new AndFilter().addChild(filter).addChild(new LiteralFilter(Filters.fromVersionRange(versionRange)));
                    }
                    Requirement req = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE).addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter.toString()).buildSyntheticRequirement();
                    adding.add(req);
                }
                updateViewerWithNewBlacklist(adding);
            }
        } catch (Exception e) {
            ErrorDialog.openError(getSection().getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error selecting bundles for blacklist.", e));
        }
    }

    private void doRemove() {
        IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
        if (!selection.isEmpty()) {
            Iterator< ? > elements = selection.iterator();
            List<Object> removed = new LinkedList<Object>();
            while (elements.hasNext()) {
                Object element = elements.next();
                if (blacklists.remove(element))
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
        try {
            committing = true;
            model.setRunBlacklist(blacklists);
        } finally {
            committing = false;
        }
    }

    @Override
    public void refreshFromModel() {
        List<Requirement> blacklistEntries = model.getRunBlacklist();

        blacklists = new ArrayList<Requirement>(blacklistEntries != null ? blacklistEntries : Collections.<Requirement> emptyList());
        viewer.setInput(blacklists);
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
     * Update the blacklists already available with new ones. Already existing blacklists will be removed from the given
     * set.
     *
     * @param adding
     *            Set with {@link Requirement}s to add
     * @return true if blacklists were added.
     */
    private boolean updateViewerWithNewBlacklist(Set<Requirement> adding) {
        // remove duplicates
        adding.removeAll(blacklists);
        if (adding.isEmpty()) {
            return false;
        }
        blacklists.addAll(adding);
        viewer.add(adding.toArray());
        markDirty();
        return true;
    }

    private class RequirementViewerDropAdapter extends AbstractViewerDropAdapter {

        public RequirementViewerDropAdapter() {
            super(viewer, EnumSet.of(SupportedTransfer.LocalSelection));
        }

        @Override
        protected boolean performSelectionDrop(ISelection data, Object target, int location) {
            Set<Requirement> adding = new LinkedHashSet<Requirement>();

            if (data instanceof IStructuredSelection) {
                IStructuredSelection structSel = (IStructuredSelection) data;

                for (Object elem : structSel.toList()) {
                    Requirement requirement = createRequirement(elem);
                    if (requirement != null)
                        adding.add(requirement);
                }
            }

            return updateViewerWithNewBlacklist(adding);
        }
    }
}
