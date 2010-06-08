package bndtools.wizards.workspace;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

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
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerFilter;
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

public class RepositorySelectionPage extends WizardPage {

    private static final String ATTR_IMPLICIT = "implicit"; //$NON-NLS-1$
    private static final String NO_DESCRIPTION = "No description available";

    private final PropertyChangeSupport propertySupport = new PropertyChangeSupport(this);

    private IConfigurationElement[] elements;
    private RemoteRepository selectedRepository = null;

    public RepositorySelectionPage(String pageName) {
        super(pageName);
        loadRepositories();
        setDescription("Select external repositories to import into the Bnd workspace.");
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

        this.elements = extensionRegistry.getConfigurationElementsFor(Plugin.PLUGIN_ID, Plugin.EXTPOINT_REPO_CONTRIB);

        List<IConfigurationElement> checkedList = new ArrayList<IConfigurationElement>(elements.length);
        for (IConfigurationElement element : elements) {
            String implicitStr = element.getAttribute(ATTR_IMPLICIT);
            if ("true".equalsIgnoreCase(implicitStr)) {
                checkedList.add(element);
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
        viewer.setLabelProvider(new RepositoryContribLabelProvider());

        viewer.setFilters(new ViewerFilter[] { new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                IConfigurationElement configElement = (IConfigurationElement) element;
                String implicit = configElement.getAttribute(ATTR_IMPLICIT);
                return !("true".equalsIgnoreCase(implicit));
            }
        } });
        viewer.setInput(elements);

        // Listeners
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                ISelection selection = viewer.getSelection();
                String text;
                if (selection.isEmpty()) {
                    text = "";
                    setSelectedRepository(null);
                } else {
                    IConfigurationElement element = (IConfigurationElement) ((IStructuredSelection) selection).getFirstElement();
                    if (element == null) {
                        text = NO_DESCRIPTION;
                        setSelectedRepository(null);
                    } else {
                        IConfigurationElement[] children = element.getChildren("description");
                        if (children.length < 1) {
                            text = NO_DESCRIPTION;
                        } else {
                            text = children[0].getValue();
                        }
                        setSelectedRepository(new LazyInitialisedRemoteRepository(element));
                    }
                }
                description.setText(text);
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

    static class RepositoryContribLabelProvider extends StyledCellLabelProvider {

        Image repoImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/repository_alt.png").createImage();

        @Override
        public void update(ViewerCell cell) {
            IConfigurationElement element = (IConfigurationElement) cell.getElement();

            String name = element.getAttribute("name");
            String bundleId = element.getContributor().getName();

            StyledString styledString = new StyledString(name);
            styledString.append(" [" + bundleId + "]", StyledString.QUALIFIER_STYLER);

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