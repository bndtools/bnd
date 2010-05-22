package bndtools;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;

import aQute.bnd.plugin.Central;
import aQute.bnd.service.RepositoryPlugin;
import bndtools.utils.Pair;

public class InitialRepositoryScanner extends Job {

    public InitialRepositoryScanner(String name) {
        super(name);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        MultiStatus status = new MultiStatus(Plugin.PLUGIN_ID, 0, "Error(s) occurred while analysing repository bundles.", null);

        List<Pair<RepositoryPlugin, File>> queue = new LinkedList<Pair<RepositoryPlugin,File>>();
        int bundleCount = 0;

        // Process repositories
        List<RepositoryPlugin> repos = null;
        try {
            repos = Central.getWorkspace().getPlugins(RepositoryPlugin.class);
        } catch (Exception e) {
            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error querying repositories", e));
            return status;
        }
        if(repos != null) for (RepositoryPlugin repo : repos) {
            List<String> bsns = repo.list(null);
            for (String bsn : bsns) {
                if(monitor.isCanceled())
                    return Status.CANCEL_STATUS;
                try {
                    File[] files = repo.get(bsn, null);
                    if(files != null) for (File file : files) {
                        queue.add(new Pair<RepositoryPlugin, File>(repo, file));
                        bundleCount ++;
                    }
                } catch (Exception e) {
                    status.add(new Status(IStatus.WARNING, Plugin.PLUGIN_ID, MessageFormat.format("Error getting files for BSN \"{0}\" from repository {1}.", bsn, repo.getName()), e));
                }
            }
        }

        SubMonitor progress = SubMonitor.convert(monitor, bundleCount);
        RepositoryModel model = Plugin.getDefault().getRepositoryModel();

        for (Pair<RepositoryPlugin,File> pair : queue) {
            try {
                model.updateRepositoryBundle(pair.getFirst(), pair.getSecond(), progress.newChild(1));
            } catch (IOException e) {
                status.add(new Status(IStatus.WARNING, Plugin.PLUGIN_ID, MessageFormat.format("Error updating bundle file \"{0}\".", pair.getSecond()), e));
            } catch (CoreException e) {
                status.add(new Status(IStatus.WARNING, Plugin.PLUGIN_ID, MessageFormat.format("Error updating bundle file \"{0}\".", pair.getSecond()), e));
            }
        }

        return status;
    }
}