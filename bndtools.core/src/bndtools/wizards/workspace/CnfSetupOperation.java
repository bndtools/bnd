package bndtools.wizards.workspace;

import org.eclipse.core.runtime.IPath;

public class CnfSetupOperation {

    public static enum Type {
        Nothing, Open, Import, Create
    }
    public static final CnfSetupOperation NOTHING = new CnfSetupOperation(Type.Nothing, null);

    private final Type type;
    private final IPath location;

    CnfSetupOperation(Type type, IPath location) {
        this.type = type;
        this.location = location;
    }

    public Type getType() {
        return type;
    }

    public IPath getLocation() {
        return location;
    }

}
