package bndtools.wizards.workspace;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;
import org.eclipse.jdt.ui.wizards.JavaCapabilityConfigurationPage;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import bndtools.Plugin;
import bndtools.api.repository.RemoteRepository;
import bndtools.utils.BundleUtils;

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

    boolean createCnfProject() {
        final IWorkspace workspace = ResourcesPlugin.getWorkspace();

        final IWorkspaceRunnable createCnfProjectOp = new IWorkspaceRunnable() {
            public void run(IProgressMonitor monitor) throws CoreException {
                monitor.setTaskName("Setting up Bnd Workspace...");
                try {
                    IProject cnfProject = workspace.getRoot().getProject(Project.BNDCNF);
                    SubMonitor progress = SubMonitor.convert(monitor, "Setting up Bnd Workspace...", 0);
                    if (cnfProject == null || !cnfProject.exists()) {
                        progress.setWorkRemaining(5);

                        // Create the project and configure it as a Java project
                        JavaCapabilityConfigurationPage.createProject(cnfProject, (URI) null, progress.newChild(1));
                        configureJavaProject(JavaCore.create(cnfProject), null, progress.newChild(1));

                        // Copy build.bnd and build.xml from the template
                        copyResourceToFile("template_build.bnd", cnfProject.getFile(Workspace.BUILDFILE), progress.newChild(1));
                        copyResourceToFile("template_cnf_build.xml", cnfProject.getFile("build.xml"), progress.newChild(1));
                    } else if (!cnfProject.isOpen()) {
                        progress.setWorkRemaining(2);

                        cnfProject.open(progress.newChild(1));
                    } else {
                        progress.setWorkRemaining(1);
                    }

                    // Copy the bundle repository
                    createRepository(cnfProject, progress.newChild(1));
                } catch (InterruptedException e) {
                }
            }
        };

        try {
            // Check whether we can report on the GUI thread or not
            Display display = Display.getCurrent();
            if (display != null) {
                getContainer().run(false, false, new IRunnableWithProgress() {
                    public void run(IProgressMonitor monitor) throws InvocationTargetException {
                        try {
                            workspace.run(createCnfProjectOp, monitor);
                        } catch (CoreException e) {
                            throw new InvocationTargetException(e);
                        }
                    }
                });
            } else {
                workspace.run(createCnfProjectOp, null);
            }
        } catch (InvocationTargetException e) {
            ErrorDialog.openError(
                    getShell(),
                    "Error",
                    null,
                    new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Error creating Bnd configuration project \"{0}\".", Project.BNDCNF), e
                            .getTargetException()));
            return false;
        } catch (CoreException e) {
            ErrorDialog
                    .openError(
                            getShell(),
                            "Error",
                            null,
                            new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Error creating Bnd configuration project \"{0}\".",
                                    Project.BNDCNF), e));
            return false;
        } catch (InterruptedException e) {
        }
        return true;
    }

    private void createRepository(IProject cnfProject, IProgressMonitor monitor) throws CoreException {
        SubMonitor progress = SubMonitor.convert(monitor, "Copying repository", 0);

        // Create the repository folder if it doesn't already exist
        IFolder repoFolder = cnfProject.getFolder("repo");
        if (!repoFolder.exists()) {
            repoFolder.create(true, true, progress.newChild(1));
        }

        // Copy the built-in repository
        Bundle myBundle = Plugin.getDefault().getBundle();
        copyFromBundleToFolder(myBundle, new Path("repo"), repoFolder, true, progress.newChild(1));

        // Copy in the repository contributions
        if (elements != null && elements.length > 0) {
            progress.setWorkRemaining(elements.length);

            for (IConfigurationElement element : elements) {
                String path = element.getAttribute("path");
                if (path != null) {
                    String bsn = element.getContributor().getName();
                    Bundle bundle = BundleUtils.findBundle(bsn, null);
                    if (bundle != null) {
                        copyFromBundleToFolder(bundle, new Path(path), repoFolder, false, progress.newChild(1));
                    }
                }
            }
        }
    }

    private void copyFromBundleToFolder(Bundle bundle, IPath path, IFolder toFolder, boolean replaceContents, IProgressMonitor monitor) throws CoreException {

        SubMonitor progress = SubMonitor.convert(monitor, String.format("Copying repository contribution %s", path.toString()), IProgressMonitor.UNKNOWN);

        URL baseUrl = bundle.getEntry(path.toString());
        IPath basePath = new Path(baseUrl.getPath()).makeAbsolute();

        @SuppressWarnings("unchecked")
        Enumeration<URL> entriesEnum = bundle.findEntries(path.toString(), null, true);
        int entryCount = 0;
        List<URL> entries = new ArrayList<URL>();
        while (entriesEnum.hasMoreElements()) {
            entries.add(entriesEnum.nextElement());
            entryCount++;
        }
        progress.setWorkRemaining(entryCount);

        path = path.makeAbsolute();
        if (entryCount > 0) {
            for (URL entry : entries) {
                IPath entryPath = new Path(entry.getPath()).makeAbsolute();
                if (basePath.isPrefixOf(entryPath)) {
                    entryPath = entryPath.removeFirstSegments(basePath.segmentCount());

                    if (entryPath.hasTrailingSeparator()) {
                        // It's a folder
                        IFolder newFolder = toFolder.getFolder(entryPath);
                        if (!newFolder.exists())
                            newFolder.create(true, true, progress.newChild(1));
                    } else {
                        // It's a file
                        IFile newFile = toFolder.getFile(entryPath);
                        try {
                            if (newFile.exists()) {
                                if (replaceContents)
                                    newFile.setContents(entry.openStream(), true, true, progress.newChild(1));
                            } else {
                                newFile.create(entry.openStream(), true, progress.newChild(1));
                            }
                        } catch (IOException e) {
                            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error reading file from repository contributor bundle.", e));
                        }
                    }
                }
            }
        }
    }

    private void configureJavaProject(IJavaProject javaProject, String newProjectCompliance, IProgressMonitor monitor) throws CoreException,
            InterruptedException {
        SubMonitor progress = SubMonitor.convert(monitor, 5);
        try {
            IProject project = javaProject.getProject();
            BuildPathsBlock.addJavaNature(project, progress.newChild(1));

            // Create the source folder
            IFolder srcFolder = project.getFolder("src");
            if (!srcFolder.exists()) {
                srcFolder.create(true, true, progress.newChild(1));
            }
            progress.setWorkRemaining(3);

            // Create the output location
            IFolder outputFolder = project.getFolder("bin");
            if (!outputFolder.exists())
                outputFolder.create(true, true, progress.newChild(1));
            outputFolder.setDerived(true);
            progress.setWorkRemaining(2);

            // Set the output location
            javaProject.setOutputLocation(outputFolder.getFullPath(), progress.newChild(1));

            // Create classpath entries
            IClasspathEntry[] classpath = new IClasspathEntry[2];
            classpath[0] = JavaCore.newSourceEntry(srcFolder.getFullPath());
            classpath[1] = JavaCore.newContainerEntry(new Path("org.eclipse.jdt.launching.JRE_CONTAINER"));

            javaProject.setRawClasspath(classpath, progress.newChild(1));
        } catch (OperationCanceledException e) {
            throw new InterruptedException();
        }
    }

    private void copyResourceToFile(String resourceName, IFile destinationFile, IProgressMonitor monitor) throws CoreException {
        SubMonitor progress = SubMonitor.convert(monitor, 1);
        if (!destinationFile.exists()) {
            InputStream templateStream = getClass().getResourceAsStream(resourceName);
            try {
                destinationFile.create(templateStream, true, progress.newChild(1));
            } finally {
                try {
                    templateStream.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        } else {
            progress.worked(1);
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }

}