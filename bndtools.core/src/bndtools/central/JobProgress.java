package bndtools.central;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import aQute.bnd.service.progress.ProgressPlugin;
import bndtools.Plugin;

public class JobProgress implements ProgressPlugin {

    @Override
    public Task startTask(final String name, final int size) {
        final ProgressTask task = new ProgressTask(name, size);

        new Job("Bnd workspace build") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                task.setProgressMonitor(monitor);
                monitor.beginTask(name, size);

                while (!task.isDone() && !task.isCanceled()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {}
                }

                Throwable error = task.getError();

                if (error != null) {
                    return new Status(Status.ERROR, Plugin.PLUGIN_ID, error.getMessage(), error);
                }

                return Status.OK_STATUS;
            }
        }.schedule();

        return task;
    }

    private static class ProgressTask implements Task {
        private final String _name;
        private final int _size;
        private IProgressMonitor _progressMonitor;
        private boolean _done;
        private Throwable _error;

        public ProgressTask(String name, int size) {
            _name = name;
            _size = size;
        }

        @Override
        public void worked(int units) {
            if (!isDone() && !_progressMonitor.isCanceled()) {
                _progressMonitor.worked(units);
            }
        }

        @Override
        public void done(String message, Throwable e) {
            _done = true;
            _progressMonitor.done();
            _error = e;
        }

        @Override
        public boolean isCanceled() {
            return _progressMonitor.isCanceled();
        }

        public boolean isDone() {
            return _done;
        }

        public void setProgressMonitor(IProgressMonitor monitor) {
            _progressMonitor = monitor;
            _progressMonitor.beginTask(_name, _size);
        }

        public Throwable getError() {
            return _error;
        }
    }

}
