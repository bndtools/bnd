package bndtools.launch.bnd;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy2;

import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.build.RunSession;

public class BndStreamsProxy implements IStreamsProxy2 {
    final ProjectLauncher projectLauncher;
    final RunSession session;
    Job job;
    StreamMonitor stdout;
    StreamMonitor stderr;

    public BndStreamsProxy(ProjectLauncher pl, RunSession session) {
        this.projectLauncher = pl;
        this.session = session;

        try {
            session.stdout(stdout = new StreamMonitor());
            session.stderr(stderr = new StreamMonitor());
        } catch (Exception e) {
            e.printStackTrace();
            // ignore
        }
    }

    class StreamMonitor implements IStreamMonitor, Appendable {
        final StringBuffer sb = new StringBuffer();
        final ConcurrentHashMap<IStreamListener, Integer> listeners = new ConcurrentHashMap<IStreamListener, Integer>();

        @Override
        public void addListener(IStreamListener listener) {
            listeners.put(listener, 0);
        }

        @Override
        public void removeListener(IStreamListener listener) {
            listeners.remove(listener);
        }

        @Override
        public String getContents() {
            return sb.toString();
        }

        @Override
        public Appendable append(char ch) throws IOException {
            sb.append(ch);
            trigger();
            return this;
        }

        @Override
        public Appendable append(CharSequence text) throws IOException {
            sb.append(text);
            trigger();
            return this;
        }

        @Override
        public Appendable append(CharSequence text, int start, int length) throws IOException {
            sb.append(text, start, length);
            trigger();
            return this;
        }

        public void flush() {
            for (Entry<IStreamListener, Integer> listener : listeners.entrySet()) {
                int start = listener.getValue();
                int end = sb.length();
                listener.setValue(end);
                listener.getKey()
                    .streamAppended(sb.substring(start, end), this);
            }
        }
    }

    synchronized void trigger() {
        if (job == null) {
            job = new Job("Stream trigger") {

                @Override
                protected IStatus run(IProgressMonitor arg0) {
                    flush();
                    return Status.OK_STATUS;
                }
            };
        }
        job.schedule(200);
    }

    void flush() {
        job = null;
        if (stdout != null) {
            stdout.flush();
            stderr.flush();
        }
    }

    @Override
    public IStreamMonitor getErrorStreamMonitor() {
        return stderr;
    }

    @Override
    public IStreamMonitor getOutputStreamMonitor() {
        return stdout;
    }

    @Override
    public void write(String input) throws IOException {
        try {
            session.stdin(input);
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public void closeInputStream() throws IOException {
        // ignore
    }

    public void close() {
        stdout = null;
        stderr = null;
    }

}
