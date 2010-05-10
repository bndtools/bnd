package bndtools.wizards;

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
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import bndtools.Plugin;
import bndtools.utils.BundleUtils;

@SuppressWarnings("restriction")
public class BndWorkspaceConfigurationPage extends WizardPage {

	private static final String NO_DESCRIPTION = "No description available";

    private IConfigurationElement[] elements;
    private IConfigurationElement[] checkedConfigElements;

    public BndWorkspaceConfigurationPage(String pageName) {
        super(pageName);
        loadRepositories();
        setDescription("Select external repositories to copy into the Bnd workspace.");
    }

    private void loadRepositories() {
        IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();

        this.elements = extensionRegistry.getConfigurationElementsFor(Plugin.PLUGIN_ID, Plugin.EXTPOINT_REPO_CONTRIB);

        List<IConfigurationElement> checkedList = new ArrayList<IConfigurationElement>(elements.length);
        for (IConfigurationElement element : elements) {
            String selectedStr = element.getAttribute("selected");
            if ("true".equalsIgnoreCase(selectedStr)) {
                checkedList.add(element);
            }
        }
        this.checkedConfigElements = checkedList.toArray(new IConfigurationElement[checkedList.size()]);
    }

	public void createControl(Composite parent) {
		setTitle("Configure BndTools Workspace");

		Composite composite = new Composite(parent, SWT.NONE);

		new Label(composite, SWT.NONE).setText("External Repositories:");
		Table table = new Table(composite, SWT.FULL_SELECTION | SWT.BORDER | SWT.CHECK);

		final CheckboxTableViewer viewer = new CheckboxTableViewer(table);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new RepositoryContribLabelProvider());

        viewer.setInput(elements);
        viewer.setCheckedElements(checkedConfigElements);

		new Label(composite, SWT.NONE).setText("Description:");
		final Browser browser = new Browser(composite, SWT.BORDER);

		// Listeners
		viewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				Object[] checkedElements = viewer.getCheckedElements();
				checkedConfigElements = new IConfigurationElement[checkedElements.length];
				System.arraycopy(checkedElements, 0, checkedConfigElements, 0, checkedElements.length);
			}
		});
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection selection = viewer.getSelection();
				String text;
				if(selection.isEmpty()) {
					text = "";
				} else {
					IConfigurationElement element = (IConfigurationElement) ((IStructuredSelection) selection).getFirstElement();
					if(element == null) {
						text = NO_DESCRIPTION;
					} else {
						IConfigurationElement[] children = element.getChildren("description");
						if(children.length < 1) {
							text = NO_DESCRIPTION;
						} else {
							text = children[0].getValue();
						}
					}
				}
				browser.setText(text);
			}
		});

		// Layout
		GridLayout layout;
		GridData gd;

		layout = new GridLayout(1, false);
		composite.setLayout(layout);

		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		table.setLayoutData(gd);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.heightHint = 100;
		browser.setLayoutData(gd);

		setControl(composite);
	}

	static class RepositoryContribLabelProvider extends StyledCellLabelProvider {

		Image repoImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/fldr_obj.gif").createImage();

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
	public IConfigurationElement[] getCheckedConfigurationElements() {
		return checkedConfigElements;
	}
	boolean createCnfProject() {
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();

		final IWorkspaceRunnable createCnfProjectOp = new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				monitor.setTaskName("Setting up Bnd Workspace...");
				try {
					IProject cnfProject = workspace.getRoot().getProject(Project.BNDCNF);
					SubMonitor progress = SubMonitor.convert(monitor, "Setting up Bnd Workspace...", 0);
					if(cnfProject == null || !cnfProject.exists()) {
						progress.setWorkRemaining(5);

						// Create the project and configure it as a Java project
						JavaCapabilityConfigurationPage.createProject(cnfProject, (URI) null, progress.newChild(1));
						configureJavaProject(JavaCore.create(cnfProject), null, progress.newChild(1));

						// Copy build.bnd and build.xml from the template
						copyResourceToFile("template_build.bnd", cnfProject.getFile(Workspace.BUILDFILE), progress.newChild(1));
						copyResourceToFile("template_cnf_build.xml", cnfProject.getFile("build.xml"), progress.newChild(1));
					} else if(!cnfProject.isOpen()) {
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
			if(display != null) {
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
			ErrorDialog.openError(getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Error creating Bnd configuration project \"{0}\".", Project.BNDCNF), e.getTargetException()));
			return false;
		} catch (CoreException e) {
			ErrorDialog.openError(getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Error creating Bnd configuration project \"{0}\".", Project.BNDCNF), e));
			return false;
		} catch (InterruptedException e) {
		}
		return true;
	}
	void createRepository(IProject cnfProject, IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, "Copying repository", 0);

		// Create the repository folder if it doesn't already exist
		IFolder repoFolder = cnfProject.getFolder("repo");
		if(!repoFolder.exists()) {
			repoFolder.create(true, true, progress.newChild(1));
		}

        // Copy the built-in repository
        Bundle myBundle = Plugin.getDefault().getBundle();
        copyFromBundleToFolder(myBundle, new Path("repo"), repoFolder, true, progress.newChild(1));

		// Copy in the repository contributions
		IConfigurationElement[] elements = checkedConfigElements;
		if(elements != null && elements.length > 0) {
			progress.setWorkRemaining(elements.length);

			for (IConfigurationElement element : elements) {
				String path = element.getAttribute("path");
				if(path != null) {
					String bsn = element.getContributor().getName();
					Bundle bundle = BundleUtils.findBundle(bsn, null);
					if(bundle != null) {
                        copyFromBundleToFolder(bundle, new Path(path), repoFolder, false, progress.newChild(1));
					}
				}
			}
		}
	}

    void copyFromBundleToFolder(Bundle bundle, IPath path, IFolder toFolder, boolean replaceContents, IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, String.format("Copying repository contribution %s", path.toString()), IProgressMonitor.UNKNOWN);

		URL baseUrl = bundle.getEntry(path.toString());
		IPath basePath = new Path(baseUrl.getPath()).makeAbsolute();

		@SuppressWarnings("unchecked")
		Enumeration<URL> entriesEnum = bundle.findEntries(path.toString(), null, true);
		int entryCount = 0;
		List<URL> entries = new ArrayList<URL>();
		while(entriesEnum.hasMoreElements()) {
			entries.add(entriesEnum.nextElement());
			entryCount++;
		}
		progress.setWorkRemaining(entryCount);

		path = path.makeAbsolute();
		if(entryCount > 0) {
			for (URL entry : entries) {
				IPath entryPath = new Path(entry.getPath()).makeAbsolute();
				if(basePath.isPrefixOf(entryPath)) {
					entryPath = entryPath.removeFirstSegments(basePath.segmentCount());

					if(entryPath.hasTrailingSeparator()) {
						// It's a folder
						IFolder newFolder = toFolder.getFolder(entryPath);
						if(!newFolder.exists())
							newFolder.create(true, true, progress.newChild(1));
					} else {
						// It's a file
						IFile newFile = toFolder.getFile(entryPath);
						try {
							if(newFile.exists()) {
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
	@SuppressWarnings("deprecation")
	void configureJavaProject(IJavaProject javaProject, String newProjectCompliance, IProgressMonitor monitor) throws CoreException, InterruptedException {
		SubMonitor progress = SubMonitor.convert(monitor, 5);
		try {
			IProject project= javaProject.getProject();
			BuildPathsBlock.addJavaNature(project, progress.newChild(1));

			// Create the source folder
			IFolder srcFolder = project.getFolder("src");
			if(!srcFolder.exists()) {
				srcFolder.create(true, true, progress.newChild(1));
			}
			progress.setWorkRemaining(3);

			// Create the output location
			IFolder outputFolder = project.getFolder("bin");
			if(!outputFolder.exists())
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
        if(!destinationFile.exists()) {
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
}