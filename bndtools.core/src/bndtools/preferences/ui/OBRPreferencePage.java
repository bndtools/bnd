package bndtools.preferences.ui;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import bndtools.Plugin;
import bndtools.preferences.ConfiguredOBRLink;
import bndtools.preferences.OBRPreferences;
import bndtools.shared.OBRLink;
import bndtools.shared.URLLabelProvider;
import bndtools.utils.FileExtensionFilter;
import bndtools.utils.URLInputValidator;

public class OBRPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    public static final String PAGE_ID = "bndtools.prefPages.obr";

    private final Set<OBRLink> repos = new LinkedHashSet<OBRLink>();
    private boolean hideBuiltIn = false;

    @SuppressWarnings("unused")
    private IWorkbench workbench;

    private Table tblConfigured;
    private TableViewer configuredViewer;
    private Button btnRemove;
    private Table tblBuiltIn;
    private TableViewer builtinViewer;
    private Button btnHidePluggedinRepositories;


    public OBRPreferencePage() {
        setTitle("OSGi Bundle Repositories");
    }

    /**
     * Create contents of the preference page.
     * @param parent
     */
    @Override
    public Control createContents(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        GridLayout gl_container = new GridLayout(1, false);
        gl_container.verticalSpacing = 10;
        container.setLayout(gl_container);

        Composite topPanel = new Composite(container, SWT.NONE);
        topPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        GridLayout gl_topPanel = new GridLayout(2, false);
        gl_topPanel.marginWidth = 0;
        gl_topPanel.marginHeight = 0;
        topPanel.setLayout(gl_topPanel);

        Label label = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));

        Label lblRepositoryUrls = new Label(topPanel, SWT.NONE);
        lblRepositoryUrls.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        lblRepositoryUrls.setText("Configured URLs (hint: drag an .xml or .zip file into the list):");

        configuredViewer = new TableViewer(topPanel, SWT.BORDER | SWT.FULL_SELECTION);
        tblConfigured = configuredViewer.getTable();
        tblConfigured.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 5));
        configuredViewer.setContentProvider(new ArrayContentProvider());
        configuredViewer.setLabelProvider(new URLLabelProvider(parent.getDisplay()));


        addDropSupport(configuredViewer);

        Button btnAddWorkspace = new Button(topPanel, SWT.NONE);
        btnAddWorkspace.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doAddWorkspace();
            }
        });
        btnAddWorkspace.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1));
        btnAddWorkspace.setText("Add");

        Button btnAddFile = new Button(topPanel, SWT.NONE);
        btnAddFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        btnAddFile.setText("Add File");
        btnAddFile.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doAddFile();
            }
        });

        Button btnAddUrl = new Button(topPanel, SWT.NONE);
        btnAddUrl.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doAddLink();
            }
        });
        btnAddUrl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        btnAddUrl.setText("Add URL");

        btnRemove = new Button(topPanel, SWT.NONE);
        btnRemove.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doRemove();
            }
        });
        btnRemove.setEnabled(false);
        btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1));
        btnRemove.setText("Remove");

        Composite bottomPanel = new Composite(container, SWT.NONE);
        bottomPanel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        GridLayout gl_bottomPanel = new GridLayout(1, false);
        gl_bottomPanel.marginWidth = 0;
        gl_bottomPanel.marginHeight = 0;
        bottomPanel.setLayout(gl_bottomPanel);

                Label lblBuiltinUrls = new Label(bottomPanel, SWT.NONE);
                lblBuiltinUrls.setText("Plugged-in URLs (provided by installed plug-ins):");

                                builtinViewer = new TableViewer(bottomPanel, SWT.BORDER | SWT.FULL_SELECTION);
                                tblBuiltIn = builtinViewer.getTable();
                                GridData gd_tblBuiltIn = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
                                gd_tblBuiltIn.heightHint = 75;
                                tblBuiltIn.setLayoutData(gd_tblBuiltIn);

                                                btnHidePluggedinRepositories = new Button(bottomPanel, SWT.CHECK);
                                                btnHidePluggedinRepositories.setText("Hide plugged-in repositories when searching or installing");
                                                btnHidePluggedinRepositories.addSelectionListener(new SelectionAdapter() {
                                                    @Override
                                                    public void widgetSelected(SelectionEvent e) {
                                                        hideBuiltIn = btnHidePluggedinRepositories.getSelection();
                                                        tblBuiltIn.setEnabled(!hideBuiltIn);
                                                    }
                                                });
                                builtinViewer.setContentProvider(new ArrayContentProvider());
                                builtinViewer.setLabelProvider(new URLLabelProvider(parent.getDisplay()));

        loadData();

        configuredViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                btnRemove.setEnabled(!configuredViewer.getSelection().isEmpty());
            }
        });

        return container;
    }

    private void addDropSupport(TableViewer viewer) {
        ViewerDropAdapter dropSupport = new ViewerDropAdapter(viewer) {
            @Override
            public boolean validateDrop(Object target, int operation, TransferData transferType) {
                return true;
            }
            @Override
            public boolean performDrop(Object data) {
                if (data instanceof String[]) {
                    String[] fileNames = (String[]) data;
                    doAddFiles(null, fileNames);
                }
                return false;
            }
            @Override
            public void dragEnter(DropTargetEvent event) {
                super.dragEnter(event);
                event.detail = DND.DROP_COPY;
            }
        };
        dropSupport.setFeedbackEnabled(false);
        viewer.addDropSupport(DND.DROP_COPY | DND.DROP_MOVE, new Transfer[] { FileTransfer.getInstance() }, dropSupport);
    }

    private void doRemove() {
        IStructuredSelection selection = (IStructuredSelection) configuredViewer.getSelection();
        repos.removeAll(selection.toList());
        configuredViewer.remove(selection.toArray());
    }

    private void doAddWorkspace() {
        ElementTreeSelectionDialog dlg = new ElementTreeSelectionDialog(getShell(), new WorkbenchLabelProvider(), new WorkbenchContentProvider());
        dlg.setTitle("Add Workspace Resource");
        dlg.setMessage("Select a .xml or .zip file in the workspace.");
        dlg.setAllowMultiple(true);
        dlg.addFilter(new FileExtensionFilter(new String[] { ".xml", ".zip" }));
        dlg.setInput(ResourcesPlugin.getWorkspace());

        // Validate non-empty selection of files
        dlg.setValidator(new ISelectionStatusValidator() {
            public IStatus validate(Object[] selection) {
                if (selection.length > 0 && selection[0] instanceof IFile) {
                    return new Status(IStatus.OK, Plugin.PLUGIN_ID, IStatus.OK, "", null); //$NON-NLS-1$
                }
                return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, IStatus.ERROR, "", null); //$NON-NLS-1$
            }
        });

        if (dlg.open() == Window.OK) {
            Object[] result = dlg.getResult();
            if (result != null) {
                List<OBRLink> adding = new ArrayList<OBRLink>(result.length);
                MultiStatus status = new MultiStatus(Plugin.PLUGIN_ID, 0, "Invalid URL(s)", null);
                for (Object file : result) {
                    IPath path = ((IResource) file).getFullPath();
                    try {
                        URL url = new URL("workspace", null, path.toString());
                        adding.add(new ConfiguredOBRLink(url.toExternalForm()));
                    } catch (MalformedURLException e) {
                        status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, e.getMessage(), e));
                    }
                }
                if (!status.isOK()) ErrorDialog.openError(getShell(), "Error", null, status);
                doAddLinks(adding);
            }
        }
    }

    private void doAddFile() {
        FileDialog dlg = new FileDialog(getShell(), SWT.OPEN | SWT.MULTI);
        dlg.setFilterExtensions(new String[] { "*.xml; *.zip" });
        dlg.setFilterNames(new String[] { "Repo. Index Files (*.xml; *.zip)" });
        dlg.setFilterPath(System.getProperty("user.home"));

        if(dlg.open() != null) {
            String[] files = dlg.getFileNames();
            doAddFiles(dlg.getFilterPath(), files);
        }
    }

    private void doAddFiles(String prefix, String[] fileNames) {
        List<OBRLink> adding = new ArrayList<OBRLink>(fileNames.length);
        MultiStatus status = new MultiStatus(Plugin.PLUGIN_ID, 0, "Invalid URL(s)", null);
        for (String fileName : fileNames) {
            try {
                File file;
                if (prefix != null) {
                    file = new File(prefix, fileName);
                } else {
                    file = new File(fileName);
                }
                URL url = file.toURI().toURL();
                adding.add(new ConfiguredOBRLink(url.toExternalForm()));
            } catch (MalformedURLException e) {
                status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, e.getMessage(), e));
            }
        }
        if (!status.isOK()) ErrorDialog.openError(getShell(), "Error", null, status);
        doAddLinks(adding);
    }

    private void doAddLink() {
        InputDialog dlg = new InputDialog(getShell(), "URL Entry", "Enter an external URL", "http://", new URLInputValidator());
        if (dlg.open() == Window.OK) {
            String value = dlg.getValue();
            try {
                URL url = new URL(value);
                doAddLink(new ConfiguredOBRLink(url.toExternalForm()));
            } catch (MalformedURLException e) {
                ErrorDialog.openError(getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, e.getMessage(), e));
            }
        }
    }

    private void doAddLink(OBRLink link) {
        if (repos.add(link))
            configuredViewer.add(link);
    }

    private void doAddLinks(List<? extends OBRLink> links) {
        if (links != null && !links.isEmpty()) {
            if (repos.addAll(links))
                configuredViewer.refresh();
        }
    }

    private void loadData() {
        IPreferenceStore prefs = getPreferenceStore();

        // Configured repos
        OBRPreferences.loadConfiguredRepositories(repos, prefs);
        configuredViewer.setInput(repos);

        // Built-in repos
        List<OBRLink> builtin = new ArrayList<OBRLink>();
        OBRPreferences.loadBuiltInRepositories(builtin);
        builtinViewer.setInput(builtin);

        // Hide built-in
        hideBuiltIn = OBRPreferences.isHideBuiltIn(prefs);
        tblBuiltIn.setEnabled(!hideBuiltIn);
        btnHidePluggedinRepositories.setSelection(hideBuiltIn);
    }

    @Override
    protected IPreferenceStore doGetPreferenceStore() {
        return Plugin.getDefault().getPreferenceStore();
    }

    public void init(IWorkbench workbench) {
        this.workbench = workbench;
    }

    @Override
    public boolean performOk() {
        IPreferenceStore prefs = getPreferenceStore();
        OBRPreferences.storeConfiguredRepositories(repos, prefs);
        OBRPreferences.setHideBuiltIn(hideBuiltIn, prefs);

        try {
            if (prefs instanceof IPersistentPreferenceStore)
                ((IPersistentPreferenceStore) prefs).save();
            return true;
        } catch (IOException e) {
            ErrorDialog.openError(getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error saving preferences.", e));
            return false;
        }
    }
}
