package org.bndtools.api;

import aQute.bnd.build.model.BndEditModel;

public class EmptyTemplate implements IProjectTemplate {

    public void modifyInitialBndModel(BndEditModel model, ProjectPaths projectPaths) {
        // noop
    }

    public void modifyInitialBndProject(IBndProject project, ProjectPaths projectPaths) {
        // noop
    }

    public boolean enableTestSourceFolder() {
        return true;
    }

}
