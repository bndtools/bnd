package bndtools.wizards.bndfile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.utils.workspace.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;

import aQute.bnd.build.Project;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.osgi.Constants;
import aQute.bnd.properties.Document;
import bndtools.Plugin;
import bndtools.central.Central;

public class EnableSubBundlesOperation implements IWorkspaceRunnable {

    private static final Set<String> BUNDLE_SPECIFIC_HEADERS;

    static {
        BUNDLE_SPECIFIC_HEADERS = new HashSet<String>(Arrays.asList(Constants.BUNDLE_SPECIFIC_HEADERS));
        BUNDLE_SPECIFIC_HEADERS.add(Constants.SERVICE_COMPONENT);
        BUNDLE_SPECIFIC_HEADERS.add(Constants.BUNDLE_VERSION);
    }

    private static final Set<String> PROJECT_ONLY_HEADERS = new HashSet<String>(Arrays.asList(new String[] {
            "-buildpath", "-runbundles", "-runsystempackages", "-runpath", "-runvm", "-runtrace", "-runframework", "-runfw", "-sub", "-debug"
    }));

    private final Shell parentShell;
    private final IWorkspace workspace;
    private final IPath containerPath;

    private InputStream newBundleInputStream = null;

    public EnableSubBundlesOperation(Shell parentShell, IWorkspace workspace, IPath containerPath) {
        this.parentShell = parentShell;
        this.workspace = workspace;
        this.containerPath = containerPath;
    }

    @Override
    public void run(final IProgressMonitor monitor) throws CoreException {
        IResource container = workspace.getRoot().findMember(containerPath);

        if (container == null || !container.exists())
            throw newCoreException("Container path does not exist", null);

        // Create new project model
        BndEditModel newBundleModel;
        try {
            newBundleModel = new BndEditModel(Central.getWorkspace());
        } catch (Exception e) {
            System.err.println("Unable to create BndEditModel with Workspace, defaulting to without Workspace");
            newBundleModel = new BndEditModel();
        }
        // Load project file and model
        IProject project = container.getProject();
        IFile projectFile = project.getFile(Project.BNDFILE);
        BndEditModel projectModel;
        final Document projectDocument;
        try {
            if (projectFile.exists()) {
                byte[] bytes = FileUtils.readFully(projectFile.getContents());
                projectDocument = new Document(new String(bytes, projectFile.getCharset()));
            } else {
                projectDocument = new Document("");
            }
            try {
                projectModel = new BndEditModel(Central.getWorkspace());
            } catch (Exception e) {
                System.err.println("Unable to create BndEditModel with Workspace, defaulting to without Workspace");
                projectModel = new BndEditModel();
            }
            projectModel.loadFrom(projectDocument);
        } catch (IOException e) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, e.getMessage(), e));
        }

        // Sub-bundles are meaningless outside of Bndtools
        if (project.hasNature(BndtoolsConstants.NATURE_ID)) {
            // Check if we need to enable sub-bundles on the project file
            boolean enableSubs;
            List<String> subBndFiles = projectModel.getSubBndFiles();
            List<String> availableHeaders = calculateProjectOnlyHeaders(projectModel.getAllPropertyNames());
            Collection<String> bundleSpecificHeaders = calculateBundleSpecificHeaders(availableHeaders);

            if (subBndFiles == null || subBndFiles.isEmpty()) {
                final EnableSubBundlesDialog subBundlesDialog = new EnableSubBundlesDialog(parentShell, availableHeaders, bundleSpecificHeaders);

                if (subBundlesDialog.open() != Window.OK) {
                    monitor.setCanceled(true);
                    return;
                }

                enableSubs = subBundlesDialog.isEnableSubBundles();
                bundleSpecificHeaders = subBundlesDialog.getSelectedProperties();
            } else {
                enableSubs = false;
            }

            // Enable subs and copy entries from project model to new bundle model
            if (enableSubs) {
                projectModel.setSubBndFiles(Arrays.asList(new String[] {
                        "*.bnd"
                }));
                for (String propertyName : bundleSpecificHeaders) {
                    Object value = projectModel.genericGet(propertyName);
                    projectModel.genericSet(propertyName, null);
                    newBundleModel.genericSet(propertyName, value);
                }

                // Save the project model
                projectModel.saveChangesTo(projectDocument);
                FileUtils.writeFully(projectDocument.get(), projectFile, false);
            }
        }

        // Generate the new bundle model
        Document newBundleDocument = new Document("");
        newBundleModel.saveChangesTo(newBundleDocument);

        try {
            newBundleInputStream = new ByteArrayInputStream(newBundleDocument.get().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            newBundleInputStream = null;
        }
    }

    private static List<String> calculateProjectOnlyHeaders(List<String> allPropertyNames) {
        List<String> result = new ArrayList<String>(allPropertyNames.size());

        for (String propertyName : allPropertyNames) {
            if (!PROJECT_ONLY_HEADERS.contains(propertyName))
                result.add(propertyName);
        }

        return result;
    }

    public static List<String> calculateBundleSpecificHeaders(Collection<String> propertyNames) {
        List<String> result = new ArrayList<String>(propertyNames.size());

        for (String propertyName : propertyNames) {
            if (BUNDLE_SPECIFIC_HEADERS.contains(propertyName))
                result.add(propertyName);
        }

        return result;
    }

    public static void moveBundleContentProperties(BndEditModel sourceModel, BndEditModel destModel, List<String> properties) {
        for (String property : properties) {
            Object value = sourceModel.genericGet(property);
            destModel.genericSet(property, value);
            sourceModel.genericSet(property, null);
        }
    }

    private static CoreException newCoreException(String message, Throwable cause) {
        return new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, message, cause));
    }

    public InputStream getNewBundleInputStream() {
        return newBundleInputStream;
    }
}
