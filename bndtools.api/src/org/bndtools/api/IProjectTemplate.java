package org.bndtools.api;

import aQute.bnd.build.model.BndEditModel;

public interface IProjectTemplate {

    void modifyInitialBndModel(BndEditModel model, String projectName, ProjectPaths projectPaths);

    void modifyInitialBndProject(IBndProject project, String projectName, ProjectPaths projectPaths);

    boolean enableTestSourceFolder();
}
