package org.bndtools.core.jobs.newproject;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import bndtools.BndConstants;
import bndtools.Central;
import bndtools.Plugin;

import aQute.bnd.build.Project;
import aQute.bnd.service.OBRIndexProvider;

public class RequiredObrCheckingJob extends WorkspaceJob {

    private static final int DEFAULT_RETRY_COUNT = 4;
    private static final long RESCHEDULE_TIME = 2000;

    private final IProject project;
    private final int retryCount;

    public RequiredObrCheckingJob(IProject project) {
        this(project, DEFAULT_RETRY_COUNT);
    }

    public RequiredObrCheckingJob(IProject project, int retryCount) {
        super("requiredObrCheck");
        this.project = project;
        this.retryCount = retryCount;
    }

    @Override
    public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
        @SuppressWarnings("unused")
        SubMonitor progress = SubMonitor.convert(monitor);

        if (!project.exists() || !project.isOpen())
            return Status.CANCEL_STATUS;

        IFile bndFile = project.getFile(Project.BNDFILE);
        if (!bndFile.exists()) {
            if (retryCount > 0) new RequiredObrCheckingJob(project, retryCount - 1).schedule(RESCHEDULE_TIME);
            return Status.CANCEL_STATUS;
        }

        IJavaProject javaProject = JavaCore.create(project);
        Project model = Plugin.getDefault().getCentral().getModel(javaProject);

        if (model != null) {
            String requireObr;
            try {
                model.prepare();
                requireObr = model.getProperty(BndConstants.REQUIRE_OBR);
            } catch (Exception e) {
                return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Failed to check project required OBR status", e);
            }

            if (requireObr != null) {
                Set<String> installedUrls;
                try {
                    installedUrls = getInstalledUrls();
                } catch (Exception e) {
                    return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Failed to checking installed OBR indexes.", e);
                }

                List<String> missingUrls = new LinkedList<String>();

                StringTokenizer tokenizer = new StringTokenizer(requireObr, ",");
                while (tokenizer.hasMoreTokens()) {
                    String url = tokenizer.nextToken();

                    if (!installedUrls.contains(url)) {
                        missingUrls.add(url);
                    }
                }

                if (!missingUrls.isEmpty()) {
                    new RequireObrPromptJob(project, missingUrls).schedule();
                }

            }
        }
        return Status.OK_STATUS;
    }

    private static Set<String> getInstalledUrls() throws Exception {
        Set<String> urls = new HashSet<String>();

        List<OBRIndexProvider> providers = Central.getWorkspace().getPlugins(OBRIndexProvider.class);
        for (OBRIndexProvider provider : providers) {
            Collection<URI> indexes = provider.getOBRIndexes();
            for (URI index : indexes) {
                urls.add(index.toString());
            }
        }

        return urls;
    }

}
