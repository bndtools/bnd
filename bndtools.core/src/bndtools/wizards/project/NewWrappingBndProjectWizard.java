package bndtools.wizards.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo;
import org.osgi.framework.Constants;

import aQute.lib.osgi.Jar;
import bndtools.Plugin;
import bndtools.editor.model.BndEditModel;
import bndtools.model.clauses.ExportedPackage;
import bndtools.model.clauses.VersionedClause;
import bndtools.utils.FileUtils;
import bndtools.wizards.bndfile.JarListWizardPage;


class NewWrappingBndProjectWizard extends AbstractNewBndProjectWizard {

    final JarListWizardPage classPathPage = new JarListWizardPage("classPathPage");

    NewWrappingBndProjectWizard(final NewBndProjectWizardPageOne pageOne, final NewJavaProjectWizardPageTwo pageTwo) {
        super(pageOne, pageTwo);

        setWindowTitle("Wrap JAR as OSGi Bundle Project");

        classPathPage.addPropertyChangeListener(JarListWizardPage.PROP_PATHS, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                Collection<IPath> paths = classPathPage.getPaths();
                IPath firstPath = paths != null && !paths.isEmpty() ? paths.iterator().next() : null;
                String name = firstPath != null ? firstPath.lastSegment() : "";
                if(name.toLowerCase().endsWith(".jar"))
                    name = name.substring(0, name.length() - 4);
                pageOne.setProjectName(name);
            }
        });
    }

    @Override
    public void addPages() {
        addPage(classPathPage);
        super.addPages();
    }

    @Override
    protected BndEditModel generateBndModel(IProgressMonitor monitor) {
        BndEditModel model = super.generateBndModel(null);

        List<ExportedPackage> exports = new ArrayList<ExportedPackage>();
        List<VersionedClause> buildPath = new ArrayList<VersionedClause>();

        IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace().getRoot();
        Collection<IPath> paths = classPathPage.getPaths();
        if (paths != null && !paths.isEmpty()) {
            int workRemaining = paths.size();

            SubMonitor progress = SubMonitor.convert(monitor, workRemaining);

            for (IPath path : paths) {
                File file = FileUtils.toFile(wsroot, path);
                if (file != null && file.isFile() && file.getName().toLowerCase().endsWith(".jar")) {
                    try {
                        addExportsForJar(exports, file, progress.newChild(1));
                        progress.worked(1);
                    } catch (IOException e) {
                        Plugin.logError("Error reading JAR: " + file.getAbsolutePath(), e);
                    }

                    VersionedClause buildPathEntry = new VersionedClause("lib/" + file.getName(), new HashMap<String, String>());
                    buildPathEntry.setVersionRange("file");
                    buildPath.add(buildPathEntry);
                }
                progress.setWorkRemaining(--workRemaining);
            }
        }

        model.setBuildPath(buildPath);
        model.setExportedPackages(exports);

        return model;
    }

    private void addExportsForJar(List<ExportedPackage> exports, File jarFile, IProgressMonitor monitor) throws IOException {
        SubMonitor progress = SubMonitor.convert(monitor, 3);

        Jar jar = null;
        List<String> packages;
        try {
            jar = new Jar(jarFile);
            packages = jar.getPackages();
            progress.worked(2);
        } finally {
            if (jar != null) jar.close();
        }

        if(packages != null && !packages.isEmpty()) {
            progress.setWorkRemaining(packages.size());
            for (String packageName : packages) {
                if (!"meta-inf".equalsIgnoreCase(packageName)) {
                    Map<String, String> props = new HashMap<String, String>();
                    props.put(Constants.VERSION_ATTRIBUTE, BndEditModel.BUNDLE_VERSION_MACRO);
                    ExportedPackage export = new ExportedPackage(packageName, props);

                    exports.add(export);
                }
                progress.worked(1);
            }
        }
    }

    @Override
    protected void processGeneratedProject(BndEditModel bndModel, IProject project, IProgressMonitor monitor) throws CoreException {
        SubMonitor progress = SubMonitor.convert(monitor, 2);
        super.processGeneratedProject(bndModel, project, progress.newChild(1));

        Collection<IPath> paths = classPathPage.getPaths();
        if (paths != null && !paths.isEmpty()) {
            int workRemaining = 1 + paths.size();
            progress.setWorkRemaining(workRemaining);

            IFolder libFolder = project.getFolder("lib");
            libFolder.create(false, true, progress.newChild(1));

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
