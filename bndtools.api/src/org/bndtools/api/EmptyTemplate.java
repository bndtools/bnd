package org.bndtools.api;

import aQute.bnd.build.model.BndEditModel;

public class EmptyTemplate implements IProjectTemplate {

    public void modifyInitialBndModel(BndEditModel model) {
        // noop
    }

    public void modifyInitialBndProject(IBndProject project) {
        // noop
    }

    public boolean enableTestSourceFolder() {
        return true;
    }

}
