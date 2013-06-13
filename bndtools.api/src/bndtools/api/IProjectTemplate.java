package bndtools.api;

import aQute.bnd.build.model.BndEditModel;

public interface IProjectTemplate {

    void modifyInitialBndModel(BndEditModel model);

    void modifyInitialBndProject(IBndProject project);

    boolean enableTestSourceFolder();
}
