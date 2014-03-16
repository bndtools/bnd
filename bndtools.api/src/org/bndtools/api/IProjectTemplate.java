package org.bndtools.api;

import aQute.bnd.build.model.BndEditModel;

public interface IProjectTemplate {

    void modifyInitialBndModel(BndEditModel model, ProjectPaths projectPaths);

    void modifyInitialBndProject(IBndProject project, ProjectPaths projectPaths);

    boolean enableTestSourceFolder();
}
