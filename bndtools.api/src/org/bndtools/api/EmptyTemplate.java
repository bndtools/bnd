package org.bndtools.api;

import java.util.ArrayList;
import java.util.List;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;

public class EmptyTemplate implements IProjectTemplate {

    @Override
    public void modifyInitialBndModel(BndEditModel model, String projectName, ProjectPaths projectPaths) {
        // -buildpath
        List<VersionedClause> buildPath = new ArrayList<VersionedClause>();
        List<VersionedClause> tmp;
        tmp = model.getBuildPath();
        if (tmp != null)
            buildPath.addAll(tmp);
        Attrs attrs = new Attrs();
        attrs.put("packages", "*");
        buildPath.add(new VersionedClause("osgi.annotation", attrs));
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
