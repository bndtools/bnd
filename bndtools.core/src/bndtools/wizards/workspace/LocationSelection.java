package bndtools.wizards.workspace;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import aQute.bnd.build.Project;

class LocationSelection {

    static final LocationSelection WORKSPACE = new LocationSelection(true, null);

    LocationSelection(boolean eclipseWorkspace, String externalPath) {
        this.eclipseWorkspace = eclipseWorkspace;
        this.externalPath = eclipseWorkspace ? null : externalPath;
    }

    boolean eclipseWorkspace;
    String externalPath;

    /**
     * return a non-null error string in case of error
     */
    String validate() {
        if (!eclipseWorkspace) {
            if (externalPath == null || externalPath.trim().length() == 0)
                return "Location must be specified";
            if (!Path.EMPTY.isValidPath(externalPath))
                return "Invalid location.";

            IPath path = new Path(externalPath);
            if (!Project.BNDCNF.equals(path.lastSegment()))
                return String.format("Last path segment must be '%s'.", Project.BNDCNF);

            File dir = new File(externalPath);
            if (dir.exists() && !dir.isDirectory())
                return "Location already exists and is not a directory.";
        }
        return null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (eclipseWorkspace ? 1231 : 1237);
        result = prime * result + ((externalPath == null) ? 0 : externalPath.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LocationSelection other = (LocationSelection) obj;
        if (eclipseWorkspace != other.eclipseWorkspace)
            return false;
        if (externalPath == null) {
            if (other.externalPath != null)
                return false;
        } else if (!externalPath.equals(other.externalPath))
            return false;
        return true;
    }

}
