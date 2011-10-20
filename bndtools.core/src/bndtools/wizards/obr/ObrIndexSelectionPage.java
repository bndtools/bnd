package bndtools.wizards.obr;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import aQute.bnd.service.OBRIndexProvider;
import aQute.bnd.service.OBRResolutionMode;
import aQute.bnd.service.RepositoryPlugin;
import bndtools.Central;
import bndtools.Plugin;

public class ObrIndexSelectionPage extends WizardPage {

    public static final String PROP_SELECTED_REPOS = "selectedRepos";

    private final PropertyChangeSupport propSupport = new PropertyChangeSupport(this);

    private final List<OBRIndexProvider> selectedRepos = new ArrayList<OBRIndexProvider>();
    private final List<OBRIndexProvider> availableRepos = new ArrayList<OBRIndexProvider>();

    private final Image repositoryImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/bundlefolder.png").createImage();
    private final Image projectImg = PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);

    private Table table;
    private CheckboxTableViewer viewer;

    /**
     * Create the wizard.
     */
    public ObrIndexSelectionPage() {
        super("wizardPage");
        setTitle("Select Repositories");
        setDescription("The requirements will be resolved against the selected OBR repositories.");

        loadAvailableRepos();
        selectedRepos.addAll(availableRepos);
    }

    /**
     * Create contents of the wizard.
     * @param parent
     */
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);

        setControl(container);
        container.setLayout(new GridLayout(2, false));

        Label lblAvailableRepositories = new Label(container, SWT.NONE);
        lblAvailableRepositories.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        lblAvailableRepositories.setText("Available Repositories:");

        table = new Table(container, SWT.BORDER | SWT.CHECK | SWT.FULL_SELECTION);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2));

        Button btnSelectAll = new Button(container, SWT.NONE);
        btnSelectAll.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1));
        btnSelectAll.setText("All");

        Button btnSelectNone = new Button(container, SWT.NONE);
        btnSelectNone.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1));
        btnSelectNone.setText("Clear");

        viewer = new CheckboxTableViewer(table);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setLabelProvider(new LabelProvider() {
            @Override
            public Image getImage(Object element) {
                if (element == Central.getWorkspaceObrProvider())
                    return projectImg;
                return repositoryImg;
            }
            @Override
            public String getText(Object element) {
                if (element instanceof RepositoryPlugin)
                    return ((RepositoryPlugin) element).getName();
                return element.toString();
            }
        });

        // Listeners
        viewer.addCheckStateListener(new ICheckStateListener() {
            public void checkStateChanged(CheckStateChangedEvent event) {
                List<OBRIndexProvider> old = new ArrayList<OBRIndexProvider>(selectedRepos);
                if (event.getChecked()) {
                    selectedRepos.add((OBRIndexProvider) event.getElement());
                } else {
                    selectedRepos.remove(event.getElement());
                }
                updateUi();
                propSupport.firePropertyChange(PROP_SELECTED_REPOS, old, selectedRepos);
            }
        });
        btnSelectAll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                List<OBRIndexProvider> old = new ArrayList<OBRIndexProvider>(selectedRepos);
                selectedRepos.clear();
                selectedRepos.addAll(availableRepos);
                viewer.setCheckedElements(selectedRepos.toArray());
                updateUi();
                propSupport.firePropertyChange(PROP_SELECTED_REPOS, old, selectedRepos);
            }
        });
        btnSelectNone.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                List<OBRIndexProvider> old = new ArrayList<OBRIndexProvider>(selectedRepos);
                selectedRepos.clear();
                viewer.setCheckedElements(selectedRepos.toArray());
                updateUi();
                propSupport.firePropertyChange(PROP_SELECTED_REPOS, old, selectedRepos);
            }
        });

        // Load data
        viewer.setInput(availableRepos);
        viewer.setCheckedElements(selectedRepos.toArray());
    }

    private void loadAvailableRepos() {
        availableRepos.clear();
        try {
            List<OBRIndexProvider> plugins = Central.getWorkspace().getPlugins(OBRIndexProvider.class);

            for (OBRIndexProvider plugin : plugins) {
                if (plugin.getSupportedModes().contains(OBRResolutionMode.runtime))
                    availableRepos.add(plugin);
            }
        } catch (Exception e) {
            Plugin.logError("Unable to load repositories", e);
        }
    }

    private void updateUi() {
        getContainer().updateButtons();
        getContainer().updateMessage();
    }

    @Override
    public void dispose() {
        super.dispose();
        repositoryImg.dispose();
    }

    @Override
    public boolean isPageComplete() {
        return !selectedRepos.isEmpty();
    }

    // DELEGATE METHODS

    public List<OBRIndexProvider> getSelectedRepos() {
        return selectedRepos;
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
}
