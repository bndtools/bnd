package bndtools.wizards.workspace;

import org.eclipse.core.runtime.IPath;

public class CnfInfo {

    static enum Existence {
        None, Exists, ImportedClosed, ImportedOpen
    };

    private final Existence existence;
    private final IPath location;

    public CnfInfo(Existence existence, IPath location) {
        this.existence = existence;
        this.location = location;
    }

    public Existence getExistence() {
        return existence;
    }

    public IPath getLocation() {
        return location;
    }

}
