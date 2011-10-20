package bndtools;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.eclipse.core.runtime.IPath;

import aQute.bnd.service.OBRIndexProvider;
import aQute.bnd.service.OBRResolutionMode;

public class WorkspaceObrProvider implements OBRIndexProvider {

    WorkspaceObrProvider() {
        IPath stateLocation = Plugin.getDefault().getStateLocation();
        System.out.println("my state location is " + stateLocation);
    }

    public Collection<URL> getOBRIndexes() throws IOException {
        return Collections.emptyList();
    }

    public Set<OBRResolutionMode> getSupportedModes() {
        return EnumSet.allOf(OBRResolutionMode.class);
    }

    @Override
    public String toString() {
        return "Workspace";
    }
}
