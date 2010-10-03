package bndtools.wizards.workspace;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;
import bndtools.api.repository.RemoteRepository;
import bndtools.api.repository.RemoteRepositoryFactory;

public class RepositorySelectionPage extends WizardPage {

    private static final String ATTR_IMPLICIT = "implicit"; //$NON-NLS-1$

    private final PropertyChangeSupport propertySupport = new PropertyChangeSupport(this);

    private Collection<RemoteRepository> repositories;
    private RemoteRepository selectedRepository = null;

    public RepositorySelectionPage(String pageName) {
        super(pageName);
        loadRepositories();
        setDescription("Select an external repository from which to import bundles into the workspace.");
    }

    public RemoteRepository getSelectedRepository() {
        return selectedRepository;
    }

    public void setSelectedRepository(RemoteRepository repo) {
        RemoteRepository oldRepo = this.selectedRepository;
        this.selectedRepository = repo;
        propertySupport.firePropertyChange("selectedRepository", oldRepo, repo); //$NON-NLS-1$
        setPageComplete(this.selectedRepository != null);
    }

    private void loadRepositories() {
        IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();

        IConfigurationElement[] elements = extensionRegistry.getConfigurationElementsFor(Plugin.PLUGIN_ID, Plugin.EXTPOINT_REPO_CONTRIB);
        this.repositories = new ArrayList<RemoteRepository>(elements.length);
        for (IConfigurationElement element : elements) {
            try {
                String implicit = element.getAttribute(ATTR_IMPLICIT);
                if (!"true".equalsIgnoreCase(implicit)) {
                    RemoteRepositoryFactory repoFactory = (RemoteRepositoryFactory) element.createExecutableExtension("class");
                    if (repoFactory != null) {
                        Collection<? extends RemoteRepository> repos = repoFactory.getConfiguredRepositories();
                        this.repositories.addAll(repos);
                    }
                }
            } catch (CoreException e) {
                Plugin.logError("Error processing extension element", e);
            }
        }
        setPageComplete(this.selectedRepository != null);
    }


    public void createControl(Composite parent) {
        setTitle("Import External Repositories");

        Composite composite = new Composite(parent, SWT.NONE);

        new Label(composite, SWT.NONE).setText("External Repositories:");
        Table table = new Table(composite, SWT.FULL_SELECTION | SWT.BORDER);

        Group descGroup = new Group(composite, SWT.NONE);
        descGroup.setText("Description:");
        final Label description = new Label(descGroup, SWT.WRAP);

        final TableViewer viewer = new TableViewer(table);
        viewer.setContentProvider(new ArrayContentProvider());
        viewer.setLabelProvider(new RepositoryLabelProvider());
        viewer.setInput(repositories);

        // Listeners
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                ISelection selection = viewer.getSelection();
                if (selection.isEmpty()) {
                    setSelectedRepository(null);
                } else {
                    RemoteRepository repo = (RemoteRepository) ((IStructuredSelection) selection).getFirstElement();
                    setSelectedRepository(repo);
                }
            }
        });
        viewer.addOpenListener(new IOpenListener() {
            public void open(OpenEvent event) {
                getContainer().showPage(getNextPage());
            }
        });

        // Layout
        GridLayout layout;
        GridData gd;

        layout = new GridLayout();
        composite.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.heightHint = 150;
        table.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        descGroup.setLayoutData(gd);

        layout = new GridLayout();
        descGroup.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.TOP, true, true);
        gd.heightHint = 75;
        description.setLayoutData(gd);

        setControl(composite);
    }

    static class RepositoryLabelProvider extends StyledCellLabelProvider {

        Image repoImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/bundlefolder.png").createImage();

        @Override
        public void update(ViewerCell cell) {
            RemoteRepository remoteRepo = (RemoteRepository) cell.getElement();

            String name = remoteRepo.getName();
            // String bundleId = element.getContributor().getName();

            StyledString styledString = new StyledString(name);
            // styledString.append(" [" + bundleId + "]", StyledString.QUALIFIER_STYLER);

            cell.setText(styledString.getString());
            cell.setStyleRanges(styledString.getStyleRanges());
            cell.setImage(repoImg);
        }

        @Override
        public void dispose() {
            super.dispose();
            repoImg.dispose();
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }

}