package bndtools.jareditor.internal;

import java.net.URI;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.PlatformUI;

public class URIHelper {

    static URI retrieveFileURI(final IEditorInput input) {
        URI uri = null;
        if (input instanceof IFileEditorInput) {
            uri = ((IFileEditorInput) input).getFile().getLocationURI();
            if (!uri.getScheme().equals("file")) {
                // we have a file on the local machine and can get the file URI from it
                uri = ((IFileEditorInput) input).getFile().getLocation().toFile().toURI();
            }
        } else if (input instanceof IURIEditorInput) {
            uri = ((IURIEditorInput) input).getURI();
            if (!uri.getScheme().equals("file")) {
                // unrecoverable error java.io.File(URI) is only supporting file protocol
                Status status = new Status(IStatus.ERROR, PluginConstants.PLUGIN_ID, "Only file uri protocol is supported.");
                ErrorDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Error", null, status);
            }
        }

        return uri;
    }

}
