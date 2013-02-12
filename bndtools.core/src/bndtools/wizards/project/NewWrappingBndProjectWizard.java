package bndtools.wizards.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo;
import org.osgi.framework.Constants;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.ExportedPackage;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;
import bndtools.Plugin;
import bndtools.utils.FileUtils;
import bndtools.wizards.bndfile.JarListWizardPage;

class NewWrappingBndProjectWizard extends AbstractNewBndProjectWizard {

    private final JarListWizardPage classPathPage = new JarListWizardPage("classPathPage");
    private final PackageListWizardPage packageListPage = new PackageListWizardPage("packageListPage");

    NewWrappingBndProjectWizard(final NewBndProjectWizardPageOne pageOne, final NewJavaProjectWizardPageTwo pageTwo) {
        super(pageOne, pageTwo);

        setWindowTitle("Wrap JAR as OSGi Bundle Project");

        classPathPage.addPropertyChangeListener(JarListWizardPage.PROP_PATHS, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                Collection<IPath> paths = classPathPage.getPaths();
                packageListPage.setJarPaths(paths);

                IPath firstPath = paths != null && !paths.isEmpty() ? paths.iterator().next() : null;
                String name = firstPath != null ? firstPath.lastSegment() : "";
                if (name.toLowerCase().endsWith(".jar"))
                    name = name.substring(0, name.length() - 4);

                packageListPage.setProjectName(name);
            }
        });

        packageListPage.addPropertyChangeListener("projectName", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                pageOne.setProjectName((String) evt.getNewValue());
            }
        });

        setNeedsProgressMonitor(true);
    }

    @Override
    public void addPages() {
        addPage(classPathPage);
        addPage(packageListPage);
        super.addPages();
    }

    @Override
    protected BndEditModel generateBndModel(IProgressMonitor monitor) {
        BndEditModel model = super.generateBndModel(null);

        List<ExportedPackage> exports = new ArrayList<ExportedPackage>();
        List<VersionedClause> buildPath = new ArrayList<VersionedClause>();

        // Add JARs to project build path
        IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace().getRoot();
        Collection<IPath> paths = classPathPage.getPaths();
        if (paths != null && !paths.isEmpty()) {
            int workRemaining = paths.size();
            SubMonitor progress = SubMonitor.convert(monitor, workRemaining);

            for (IPath path : paths) {
                File file = FileUtils.toFile(wsroot, path);
                if (file != null && file.isFile() && file.getName().toLowerCase().endsWith(".jar")) {
                    VersionedClause buildPathEntry = new VersionedClause("lib/" + file.getName(), new Attrs());
                    buildPathEntry.setVersionRange("file");
                    buildPath.add(buildPathEntry);
                }
                progress.setWorkRemaining(--workRemaining);
            }
        }

        // Add package exports
        List<IPath> selectedPackages = packageListPage.getSelectedPackages();
        for (IPath pkg : selectedPackages) {
            Attrs props = new Attrs();
            props.put(Constants.VERSION_ATTRIBUTE, BndEditModel.BUNDLE_VERSION_MACRO);
            ExportedPackage export = new ExportedPackage(pkg.toString().replace('/', '.'), props);

            exports.add(export);
        }

        model.setBuildPath(buildPath);
        model.setExportedPackages(exports);

        return model;
    }

    @Override
    protected void processGeneratedProject(BndEditModel bndModel, IJavaProject project, IProgressMonitor monitor) throws CoreException {
        SubMonitor progress = SubMonitor.convert(monitor, 2);
        super.processGeneratedProject(bndModel, project, progress.newChild(1));

        Collection<IPath> paths = classPathPage.getPaths();
        if (paths != null && !paths.isEmpty()) {
            int workRemaining = 1 + paths.size();
            progress.setWorkRemaining(workRemaining);

            IFolder libFolder = project.getProject().getFolder("lib");
            libFolder.create(false, true, progress.newChild(1));

            // Copy JARs into project lib folder.
            IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace().getRoot();
            for (IPath path : paths) {
                File file = FileUtils.toFile(wsroot, path);
                if (file != null && file.isFile()) {
                    IFile copy = libFolder.getFile(file.getName());
                    try {
                        FileInputStream input = new FileInputStream(file);
                        try {
                            copy.create(input, false, progress.newChild(1));
                        } finally {
                            input.close();
                        }
                    } catch (IOException e) {
                        throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error copying JAR into project.", e));
                    }
                }
                progress.setWorkRemaining(--workRemaining);
            }
        }
    }
}
