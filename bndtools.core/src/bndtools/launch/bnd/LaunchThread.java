package bndtools.launch.bnd;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;

import aQute.bnd.build.ProjectLauncher;
import bndtools.central.Central;
import bndtools.launch.OSGiRunLaunchDelegate;

class LaunchThread extends Thread implements IResourceChangeListener {
    private static final ILogger logger = Logger.getLogger(OSGiRunLaunchDelegate.class);
    final ProjectLauncher launcher;
    List<String> older;
    final static Timer timer = new Timer(true);
    volatile boolean closed;

    TimerTask timerTask;

    LaunchThread(ProjectLauncher pl) {
        super("bnd::launch-" + pl.getProject());
        super.setDaemon(true);
        this.launcher = pl;
        older = new ArrayList<String>(launcher.getRunBundles());
    }

    @Override
    public void run() {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        try {
            workspace.addResourceChangeListener(this);
            launcher.launch();
        } catch (Exception e) {
            logger.logError("bnd " + getName() + " launcher failed", e);
        } finally {
            closed = true;
            workspace.removeResourceChangeListener(this);
        }
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        try {
            Collection<String> newer = launcher.getRunBundles();
            if (!newer.equals(older) || hasChanged(newer, event.getDelta())) {
                update(newer);
            }

        } catch (Exception e) {
            logger.logError("Error updating launch properties file for " + this + ".", e);
        }
    }

    private void update(final Collection<String> newer) throws Exception {
        if (timerTask != null)
            timerTask.cancel();

        timerTask = new TimerTask() {
            @Override
            public void run() {
                if (!closed)
                    try {
                        older = new ArrayList<String>(newer);
                        launcher.update();
                    } catch (Exception e) {
                        logger.logError("Error updating launch properties file for " + this + ".", e);
                    }
            }
        };
        timer.schedule(timerTask, 500);
    }

    private boolean hasChanged(Collection<String> files, IResourceDelta delta) throws Exception {
        for (String path : files) {
            if (hasChanged(path, delta))
                return true;
        }
        return false;
    }

    private boolean hasChanged(String path, IResourceDelta delta) throws Exception {
        File file = new File(path);
        return hasChanged(file, delta);
    }

    private boolean hasChanged(File file, IResourceDelta delta) throws Exception {
        IPath ipath = Central.toPath(file);
        return delta.findMember(ipath) != null;
    }

    void close() throws InterruptedException {
        launcher.cancel();
        join(5000);
    }

}
