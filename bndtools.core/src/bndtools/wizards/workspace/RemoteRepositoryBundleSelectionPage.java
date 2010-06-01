package bndtools.wizards.workspace;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
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

import aQute.libg.version.Version;
import bndtools.Plugin;
import bndtools.api.repository.RemoteRepository;
import bndtools.types.Pair;

public class RemoteRepositoryBundleSelectionPage extends WizardPage {

    private RemoteRepository repository;

    private final Set<URL> selectedURLs = new LinkedHashSet<URL>();

    private TableViewer availableViewer;
    private TableViewer selectedViewer;
    private Label selectedSummaryLabel;

    private Button btnAdd;
    private Button btnRemove;

    private Job searchJob = null;

    public RemoteRepositoryBundleSelectionPage(String pageName) {
        super(pageName);
    }

    public void setRepository(RemoteRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean isPageComplete() {
        return !selectedURLs.isEmpty();
    }

    public Collection<URL> getSelectedURLs() {
        return Collections.unmodifiableCollection(selectedURLs);
    }

    public void createControl(Composite parent) {
        setTitle("Select Bundles");
        setMessage("The selected bundles will be imported into the local repository.");

        Composite composite = new Composite(parent, SWT.NONE);

        Control availPanel = createAvailablePanel(composite);
        Control buttonPanel = createButtonPanel(composite);
        Control selectedPanel = createSelectedPanel(composite);

        GridLayout layout = new GridLayout(3, false);
        composite.setLayout(layout);

        GridData gd;
        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        availPanel.setLayoutData(gd);
        gd = new GridData(SWT.FILL, SWT.FILL, false, true);
        buttonPanel.setLayoutData(gd);
        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        selectedPanel.setLayoutData(gd);

        setControl(composite);
        setPageComplete(false);
    }
    private class RepositorySearchResultsContentProvider implements IStructuredContentProvider {
        public void dispose() {
        }
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }
        public Object[] getElements(Object inputElement) {
            List<Pair<String, Version>> result = new ArrayList<Pair<String,Version>>();

            @SuppressWarnings("unchecked")
            Collection<String> bsns = (Collection<String>) inputElement;
            for (String bsn : bsns) {
                Collection<Version> versions = repository.versions(bsn);
                if(versions != null) for (Version version : versions) {
                    result.add(new Pair<String, Version>(bsn, version));
                }
            }

            return result.toArray(new Object[result.size()]);
        }
    }

    private static class RepositorySearchResultsLabelProvider extends StyledCellLabelProvider {
        private Image bundleImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/brick.png").createImage();
        @Override
        public void update(ViewerCell cell) {
            @SuppressWarnings("unchecked")
            Pair<String, Version> id = (Pair<String, Version>) cell.getElement();
            StyledString string = new StyledString(id.getFirst());
            string.append(" (" + id.getSecond() + ")", StyledString.COUNTER_STYLER);
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
        final Table availableTree = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
        final Label summaryLabel = new Label(composite, SWT.NONE);

        availableViewer = new TableViewer(availableTree);
        availableViewer.setContentProvider(new RepositorySearchResultsContentProvider());
        availableViewer.setLabelProvider(new RepositorySearchResultsLabelProvider());

        // LISTENERS
        searchText.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                if(e.detail == SWT.CANCEL) {
                    cancelSearch();
                    availableViewer.setInput(Collections.<String>emptyList());
                    summaryLabel.setText("");
                } else {
                    doSearch(searchText.getText(), 0L, availableViewer, summaryLabel);
                }
            }
        });
        searchText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                doSearch(searchText.getText(), 500L, availableViewer, summaryLabel);
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
        summaryLabel.setLayoutData(gd);

        return composite;
    }

    void doAdd() {
        IStructuredSelection selection = (IStructuredSelection) availableViewer.getSelection();
        for(Iterator<?> iterator = selection.iterator(); iterator.hasNext(); ) {
            @SuppressWarnings("unchecked")
            Pair<String, Version> id = (Pair<String, Version>) iterator.next();
            URL[] urls = repository.get(id.getFirst(), "[" + id.getSecond() + "," + id.getSecond() + "]");
            if(urls != null && urls.length > 0) {
                selectedURLs.add(urls[urls.length - 1]);
            }
        }
        updateSelectedURLs();
    }

    void doRemove() {
        IStructuredSelection selection = (IStructuredSelection) selectedViewer.getSelection();
        selectedURLs.removeAll(selection.toList());
        updateSelectedURLs();
    }

    void doSearch(final String text, long delay, final Viewer viewer, final Label label) {
        cancelSearch();
        searchJob = new Job("Search Repository") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                final Collection<String> list = repository.list(text);
                if(!viewer.getControl().isDisposed()) {
                    Runnable op = new Runnable() {
                        public void run() {
                            if(!viewer.getControl().isDisposed())
                                viewer.setInput(list);
                            if(!label.isDisposed()) {
                                if(list.isEmpty())
                                    label.setText("Nothing found, try wildcards (\"*\").");
                                else
                                    label.setText(MessageFormat.format("{0,choice,1# bundle|1<{0} bundles} found.", list.size()));
                            }
                        }
                    };
                    Display display = viewer.getControl().getDisplay();
                    if(display.getThread() == Thread.currentThread())
                        op.run();
                    else
                        display.asyncExec(op);
                }
                return Status.OK_STATUS;
            }
        };
        searchJob.schedule(delay);
    }

    void cancelSearch() {
        try {
            if(searchJob != null) {
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

    private static class URLLabelProvider extends StyledCellLabelProvider {
        private final Image linkImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/link.png").createImage();
        @Override
        public void update(ViewerCell cell) {
            URL url = (URL) cell.getElement();

            cell.setText(url.toString());
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
        selectedViewer.setLabelProvider(new URLLabelProvider());

        selectedSummaryLabel = new Label(composite, SWT.NONE);

        // LISTENERS
        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if(e.character == SWT.DEL) {
                    doRemove();
                }
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

    private void updateSelectedURLs() {
        selectedViewer.setInput(selectedURLs);
        selectedSummaryLabel.setText(MessageFormat.format("{0,choice,0#0 bundles|1#1 bundle|1<{0} bundles} to import.", selectedURLs.size()));
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

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        if(visible) {
            try {
                getContainer().run(true, false, new IRunnableWithProgress() {
                    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                        try {
                            repository.initialise(monitor);
                        } catch (CoreException e) {
                            throw new InvocationTargetException(e);
                        }
                    }
                });
            } catch (InvocationTargetException e) {
                CoreException x = (CoreException) e.getCause();
                ErrorDialog.openError(getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error initialising repository.", x));
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}