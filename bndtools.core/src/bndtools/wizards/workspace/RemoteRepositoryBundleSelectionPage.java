package bndtools.wizards.workspace;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;

public class RemoteRepositoryBundleSelectionPage extends WizardPage implements IRepositoriesChangedCallback {

    public static final String PROP_SELECTION = "selection";
    private final PropertyChangeSupport propSupport = new PropertyChangeSupport(this);

    private final RepositoryAdmin repoAdmin;
    private final List<Resource> selectedResources = new LinkedList<Resource>();

    private TableViewer availableViewer;
    private TableViewer selectedViewer;
    private Label selectedSummaryLabel;

    private Button btnAdd;
    private Button btnRemove;

    private Job searchJob = null;

    private Label availableSummaryLabel;

    public RemoteRepositoryBundleSelectionPage(RepositoryAdmin repoAdmin) {
        super("initialSelection");
        this.repoAdmin = repoAdmin;
    }

    @Override
    public boolean isPageComplete() {
        return !selectedResources.isEmpty();
    }

    public void createControl(Composite parent) {
        setTitle("Select Bundles");
        setMessage("The selected bundles will be imported into the Local Repository.");

        Composite composite = new Composite(parent, SWT.NONE);

        Control availPanel = createAvailablePanel(composite);
        Control buttonPanel = createButtonPanel(composite);
        Control selectedPanel = createSelectedPanel(composite);

        Label lblHint = new Label(composite, SWT.WRAP);
        lblHint.setText("Hint: enter a search filter to find bundles (e.g. 'apache').");

        GridLayout layout = new GridLayout(3, false);
        composite.setLayout(layout);

        GridData gd;
        gd = new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1);
        lblHint.setLayoutData(gd);
        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        availPanel.setLayoutData(gd);
        gd = new GridData(SWT.FILL, SWT.FILL, false, true);
        buttonPanel.setLayoutData(gd);
        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        selectedPanel.setLayoutData(gd);

        setControl(composite);
        setPageComplete(false);
    }

    private static class OBRResourcesLabelProvider extends StyledCellLabelProvider {
        private Image bundleImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/brick.png").createImage();

        @Override
        public void update(ViewerCell cell) {
            Resource resource = (Resource) cell.getElement();

            StyledString string = new StyledString(resource.getSymbolicName());
            string.append(" (" + resource.getVersion() + ")", StyledString.COUNTER_STYLER);

            cell.setText(string.getString());
            cell.setStyleRanges(string.getStyleRanges());
            cell.setImage(bundleImg);
        }

        @Override
        public void dispose() {
            super.dispose();
            bundleImg.dispose();
        }
    }

    Control createAvailablePanel(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);

        new Label(composite, SWT.NONE).setText("Repository Contents:");
        final Text searchText = new Text(composite, SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
        searchText.setMessage("type filter");
        final Table availableTree = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
        availableSummaryLabel = new Label(composite, SWT.NONE);

        availableViewer = new TableViewer(availableTree);
        availableViewer.setContentProvider(new ArrayContentProvider());
        availableViewer.setLabelProvider(new OBRResourcesLabelProvider());

        // LISTENERS
        searchText.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                if (e.detail == SWT.CANCEL) {
                    cancelSearch();
                    availableViewer.setInput(Collections.<String> emptyList());
                    availableSummaryLabel.setText("");
                } else {
                    doSearch(searchText.getText(), 0L);
                }
            }
        });
        searchText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                doSearch(searchText.getText(), 1000L);
            }
        });
        searchText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.ARROW_DOWN)
                    availableTree.setFocus();
            }
        });
        availableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                btnAdd.setEnabled(!availableViewer.getSelection().isEmpty());
            }
        });
        availableViewer.addOpenListener(new IOpenListener() {
            public void open(OpenEvent event) {
                doAdd();
            }
        });

        // LAYOUT
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        composite.setLayout(layout);

        GridData gd;
        gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        searchText.setLayoutData(gd);
        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.widthHint = 250;
        availableTree.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        availableSummaryLabel.setLayoutData(gd);

        return composite;
    }

    void doAdd() {
        IStructuredSelection selection = (IStructuredSelection) availableViewer.getSelection();
        for (Iterator< ? > iterator = selection.iterator(); iterator.hasNext();) {
            Resource resource = (Resource) iterator.next();
            selectedResources.add(resource);
        }
        updateSelectedResources();
        getContainer().updateButtons();
        propSupport.firePropertyChange(PROP_SELECTION, null, selectedResources);
    }

    void doRemove() {
        IStructuredSelection selection = (IStructuredSelection) selectedViewer.getSelection();
        selectedResources.removeAll(selection.toList());
        updateSelectedResources();
        getContainer().updateButtons();
        propSupport.firePropertyChange(PROP_SELECTION, null, selectedResources);
    }

    public void changedRepositories(RepositoryAdmin repoAdmin) {
        if (this.repoAdmin == repoAdmin) {
            cancelSearch();
            if (availableViewer != null && !availableViewer.getControl().isDisposed())
                availableViewer.setInput(Collections.emptyList());
            selectedResources.clear();
            updateSelectedResources();
            propSupport.firePropertyChange(PROP_SELECTION, null, selectedResources);
        }
    }

    void doSearch(final String text, long delay) {
        cancelSearch();
        searchJob = new Job("Search Repository") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    final String filter = "(symbolicname=*" + text + "*)";
                    final Resource[] resources = repoAdmin.discoverResources(filter);

                    Arrays.sort(resources, new Comparator<Resource>() {
                        public int compare(Resource o1, Resource o2) {
                            String bsn1 = o1.getSymbolicName();
                            if (bsn1 == null)
                                bsn1 = "no-symbolic-name";

                            String bsn2 = o2.getSymbolicName();
                            if (bsn2 == null)
                                bsn2 = "no-symbolic-name";

                            return bsn1.compareToIgnoreCase(bsn2);
                        }
                    });

                    Runnable displayOp = new Runnable() {
                        public void run() {
                            if (!availableViewer.getControl().isDisposed()) {
                                availableViewer.setInput(resources != null ? resources : Collections.emptyList());
                            }
                            if (!availableSummaryLabel.isDisposed()) {
                                if (resources == null || resources.length == 0)
                                    availableSummaryLabel.setText("Nothing found.");
                                else
                                    availableSummaryLabel.setText(MessageFormat.format("{0,choice,1# One resource|1<{0} resources} found.", resources.length));
                            }
                        }
                    };

                    Display display = availableViewer.getControl().getDisplay();
                    if (display.getThread() == Thread.currentThread())
                        displayOp.run();
                    else
                        display.asyncExec(displayOp);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return Status.OK_STATUS;
            }
        };
        searchJob.schedule(delay);
    }

    void cancelSearch() {
        try {
            if (searchJob != null) {
                searchJob.cancel();
                try {
                    searchJob.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } finally {
            searchJob = null;
        }
    }

    private static class ResourceLabelProvider extends StyledCellLabelProvider {
        private final Image linkImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/link.png").createImage();

        @Override
        public void update(ViewerCell cell) {
            Resource resource = (Resource) cell.getElement();
            cell.setText(resource.getURI());
            cell.setImage(linkImg);
        }

        @Override
        public void dispose() {
            super.dispose();
            linkImg.dispose();
        }
    }

    Control createSelectedPanel(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);

        new Label(composite, SWT.NONE).setText("Selected Bundles:");
        Table table = new Table(composite, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);

        selectedViewer = new TableViewer(table);
        selectedViewer.setContentProvider(new ArrayContentProvider());
        selectedViewer.setLabelProvider(new ResourceLabelProvider());

        selectedSummaryLabel = new Label(composite, SWT.NONE);

        // LISTENERS
        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.character == SWT.DEL) {
                    doRemove();
                }
            }
        });
        selectedViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                btnRemove.setEnabled(!selectedViewer.getSelection().isEmpty());
            }
        });

        // LAYOUT
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        composite.setLayout(layout);

        GridData gd;
        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.widthHint = 250;
        table.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        selectedSummaryLabel.setLayoutData(gd);

        return composite;
    }

    private void updateSelectedResources() {
        if (selectedViewer != null && !selectedViewer.getControl().isDisposed()) {
            selectedViewer.setInput(selectedResources);
            selectedSummaryLabel.setText(MessageFormat.format("{0,choice,0#0 bundles|1#1 bundle|1<{0} bundles} to import.", selectedResources.size()));
            setPageComplete(!selectedResources.isEmpty());
        }
    }

    Control createButtonPanel(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);

        btnAdd = new Button(composite, SWT.PUSH);
        btnAdd.setText("Add -->");
        btnAdd.setEnabled(false);

        btnRemove = new Button(composite, SWT.PUSH);
        btnRemove.setText("<-- Remove");
        btnRemove.setEnabled(false);

        btnAdd.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doAdd();
            }
        });
        btnRemove.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doRemove();
            }
        });

        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        composite.setLayout(layout);

        GridData gd;
        gd = new GridData(SWT.FILL, SWT.BOTTOM, false, true);
        btnAdd.setLayoutData(gd);
        gd = new GridData(SWT.FILL, SWT.TOP, false, true);
        btnRemove.setLayoutData(gd);

        return composite;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propSupport.removePropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propSupport.removePropertyChangeListener(propertyName, listener);
    }

    public Collection<Resource> getSelectedResources() {
        return selectedResources;
    }

}