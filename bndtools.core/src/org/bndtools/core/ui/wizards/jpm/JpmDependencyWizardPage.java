package org.bndtools.core.ui.wizards.jpm;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;
import bndtools.central.Central;
import aQute.bnd.build.Workspace;
import aQute.bnd.service.repository.SearchableRepository;
import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;

public class JpmDependencyWizardPage extends WizardPage {

    private static final String DEFAULT_MESSAGE = "Review the following dependencies supplied by JPM4J before adding them.";

    private final URI originUri;

    private TableViewer viewerDirect;
    private CheckboxTableViewer viewerIndirect;
    private boolean queried = false;

    private SearchableRepository repository;

    private String errorText;
    private Set<ResourceDescriptor> directResources;
    private Set<ResourceDescriptor> indirectResources;
    private Set<ResourceDescriptor> selectedIndirectResources;

    private ResourceDescriptor selection;

    public JpmDependencyWizardPage(URI originUri) {
        super("jpmDependencies");
        this.originUri = originUri;

        setTitle("Add Dependencies from JPM4J");
    }

    @SuppressWarnings("unused")
    public void createControl(Composite parent) {
        // CREATE CONTROLS
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);

        Label lblDirect = new Label(container, SWT.NONE);
        lblDirect.setFont(JFaceResources.getBannerFont());
        lblDirect.setText("Direct Dependencies:");

        Table tblDirect = new Table(container, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER | SWT.V_SCROLL);
        viewerDirect = new TableViewer(tblDirect);
        viewerDirect.setContentProvider(ArrayContentProvider.getInstance());
        viewerDirect.setLabelProvider(new ResourceDescriptorLabelProvider());

        createHelpLabel(container, "The above dependencies will be added to the project and, if necessary, to the JPM4J local index.");

        Label separator = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);

        Label lblIndirect = new Label(container, SWT.NONE);
        lblIndirect.setFont(JFaceResources.getBannerFont());
        lblIndirect.setText("Transitive Dependencies:");

        Composite cmpIndirect = new Composite(container, SWT.NONE);
        Table tblIndirect = new Table(cmpIndirect, SWT.CHECK | SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER | SWT.V_SCROLL);
        viewerIndirect = new CheckboxTableViewer(tblIndirect);
        viewerIndirect.setContentProvider(ArrayContentProvider.getInstance());
        viewerIndirect.setLabelProvider(new ResourceDescriptorLabelProvider());

        Button btnSelectAll = new Button(cmpIndirect, SWT.PUSH);
        btnSelectAll.setText("All");

        Button btnSelectNone = new Button(cmpIndirect, SWT.PUSH);
        btnSelectNone.setText("None");

        new Label(cmpIndirect, SWT.NONE);

        createHelpLabel(container, "The above dependencies will be added to the JPM4J local index. Checked dependencies will also be added directly to the project.");

        // LISTENERS

        // Query JPM and show results *after* dialog is shown. This ensures progress is visible in the dialog's
        // progress bar
        getContainer().getShell().addShellListener(new ShellAdapter() {
            @Override
            public void shellActivated(ShellEvent e) {
                runQuery();
            }
        });
        viewerIndirect.addCheckStateListener(new ICheckStateListener() {
            public void checkStateChanged(CheckStateChangedEvent ev) {
                if (selectedIndirectResources == null)
                    selectedIndirectResources = new HashSet<ResourceDescriptor>();

                ResourceDescriptor resource = (ResourceDescriptor) ev.getElement();
                if (ev.getChecked())
                    selectedIndirectResources.add(resource);
                else
                    selectedIndirectResources.remove(resource);
            }
        });
        btnSelectAll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                selectedIndirectResources.addAll(indirectResources);
                updateSelectedCheckboxes();
            }
        });
        btnSelectNone.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                selectedIndirectResources.clear();
                updateSelectedCheckboxes();
            }
        });

        viewerDirect.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                ISelection sel = event.getSelection();
                if (sel.isEmpty())
                    selection = (ResourceDescriptor) ((IStructuredSelection) viewerIndirect.getSelection()).getFirstElement();
                else
                    selection = (ResourceDescriptor) ((IStructuredSelection) sel).getFirstElement();
                getContainer().updateMessage();
            }
        });
        viewerIndirect.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                ISelection sel = event.getSelection();
                if (sel.isEmpty())
                    selection = (ResourceDescriptor) ((IStructuredSelection) viewerDirect.getSelection()).getFirstElement();
                else
                    selection = (ResourceDescriptor) ((IStructuredSelection) sel).getFirstElement();
                getContainer().updateMessage();
            }
        });

        // LAYOUT
        GridLayout layout;
        GridData gd;

        layout = new GridLayout(1, true);
        container.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        gd.heightHint = 30;
        tblDirect.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        separator.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        cmpIndirect.setLayoutData(gd);

        layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.horizontalSpacing = 2;
        layout.verticalSpacing = 2;
        cmpIndirect.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 3);
        gd.heightHint = 100;
        tblIndirect.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.FILL, false, false);
        btnSelectAll.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.FILL, false, false);
        btnSelectNone.setLayoutData(gd);
    }

    private Control createHelpLabel(Composite container, String text) {
        Label label = new Label(container, SWT.WRAP);
        label.setText(text);

        ControlDecoration decoration = new ControlDecoration(label, SWT.LEFT, container);
        Image imgInfo = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION).getImage();
        decoration.setImage(imgInfo);

        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        gd.horizontalIndent = imgInfo.getBounds().width;
        gd.widthHint = 100;
        label.setLayoutData(gd);

        return label;
    }

    private void runQuery() {
        if (!queried) {
            errorText = null;
            try {
                Workspace workspace = Central.getWorkspace();
                repository = workspace.getPlugin(SearchableRepository.class);
                if (repository == null)
                    throw new Exception("No searchable repository is configured in the workspace. Try adding the JPM4J plugin.");

                QueryJpmDependenciesRunnable query = new QueryJpmDependenciesRunnable(originUri, repository);
                getContainer().run(true, true, query);
                queried = true;
                errorText = query.getError();
                directResources = query.getDirectResources();
                indirectResources = query.getIndirectResources();
                selectedIndirectResources = new HashSet<ResourceDescriptor>();
            } catch (InvocationTargetException e) {
                errorText = e.getCause().getMessage();
            } catch (InterruptedException e) {
                // ignore
            } catch (Exception e) {
                errorText = e.getMessage();
            } finally {
                updateUi();
            }
        }
    }

    private void updateUi() {
        viewerDirect.setInput(directResources);
        viewerIndirect.setInput(indirectResources);
        updateSelectedCheckboxes();
        getContainer().updateButtons();
        getContainer().updateMessage();
    }

    private void updateSelectedCheckboxes() {
        Object[] selectedArray = selectedIndirectResources != null ? selectedIndirectResources.toArray(new Object[selectedIndirectResources.size()]) : new Object[0];
        viewerIndirect.setCheckedElements(selectedArray);
    }

    @Override
    public boolean isPageComplete() {
        return queried && errorText == null;
    }

    @Override
    public String getErrorMessage() {
        return errorText;
    }

    @Override
    public String getMessage() {
        if (selection != null) {
            return selection.description;
        }

        return DEFAULT_MESSAGE;
    }

    public Set<ResourceDescriptor> getDirectResources() {
        return Collections.unmodifiableSet(directResources);
    }

    public Set<ResourceDescriptor> getIndirectResources() {
        return Collections.unmodifiableSet(indirectResources);
    }

    public Set<ResourceDescriptor> getSelectedIndirectResources() {
        return Collections.unmodifiableSet(selectedIndirectResources);
    }

    public SearchableRepository getRepository() {
        return repository;
    }

    public URI getOriginUri() {
        return originUri;
    }

    private static class ResourceDescriptorLabelProvider extends StyledCellLabelProvider {

        private final Image imgJar = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/jar_obj.gif").createImage();

        @Override
        public void update(ViewerCell cell) {
            ResourceDescriptor descriptor = (ResourceDescriptor) cell.getElement();
            StyledString label = new StyledString(descriptor.bsn + " ");
            if (descriptor.version != null)
                label.append(descriptor.version.toString(), StyledString.COUNTER_STYLER);

            cell.setText(label.toString());
            cell.setStyleRanges(label.getStyleRanges());

            cell.setImage(imgJar);
        }

        @Override
        public void dispose() {
            imgJar.dispose();
        }
    }

}
