package org.bndtools.api;

import java.util.ArrayList;
import java.util.List;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.VersionedClause;

public class EmptyTemplate implements IProjectTemplate {

    @Override
    public void modifyInitialBndModel(BndEditModel model, String projectName, ProjectPaths projectPaths) {
        // -buildpath
        List<VersionedClause> buildPath = new ArrayList<VersionedClause>();
        List<VersionedClause> tmp;
        tmp = model.getBuildPath();
        if (tmp != null)
            buildPath.addAll(tmp);

        VersionedClause annotationLib = new VersionedClause("osgi.annotation", null);
        annotationLib.setVersionRange("6.0.1");
        buildPath.add(annotationLib);
        model.setBuildPath(buildPath);
    }

    @Override
    public void modifyInitialBndProject(IBndProject project, String projectName, ProjectPaths projectPaths) {
        // noop
    }

    @Override
    public boolean enableTestSourceFolder() {
        return true;
    }

}
