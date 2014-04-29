package bndtools.services;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.osgi.util.tracker.ServiceTracker;

public class WorkspaceURLStreamHandlerService extends AbstractURLStreamHandlerService {

    public static final String PROTOCOL = "workspace";

    private final ServiceTracker<IWorkspace,IWorkspace> workspaceTracker;

    public WorkspaceURLStreamHandlerService(ServiceTracker<IWorkspace,IWorkspace> workspaceTracker) {
        this.workspaceTracker = workspaceTracker;
    }

    @Override
    public URLConnection openConnection(URL url) throws IOException {
        String protocol = url.getProtocol();
        if (!PROTOCOL.equals(protocol))
            throw new MalformedURLException("Unsupported protocol");

        IPath path = new Path(url.getPath());

        IWorkspace workspace = workspaceTracker.getService();
        if (workspace == null)
            throw new IOException("Workspace is not available");

        IPath workspaceLocation = workspace.getRoot().getLocation();
        if (workspaceLocation == null)
            throw new IOException("Cannot determine workspace location.");

        IPath location = workspaceLocation.append(path);

        return new URL("file", null, location.toOSString()).openConnection();
    }

}
