package org.bndtools.core.templates.project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bndtools.api.BndProjectResource;
import org.bndtools.api.IBndProject;
import org.bndtools.api.IProjectTemplate;
import org.bndtools.api.ProjectPaths;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.ExportedPackage;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;

public class APITemplate implements IProjectTemplate {

    @Override
    public void modifyInitialBndModel(BndEditModel model, String projectName, ProjectPaths projectPaths) {
        model.setExportedPackages(Arrays.asList(new ExportedPackage[] {
            new ExportedPackage(projectName, new Attrs())
        }));

        // Bundle-Version: 1.0.0
        model.setBundleVersion("1.0.0.${tstamp}");

        // -buildpath
        List<VersionedClause> buildPath = new ArrayList<VersionedClause>();
        List<VersionedClause> tmp;
        tmp = model.getBuildPath();
        if (tmp != null)
            buildPath.addAll(tmp);
        buildPath.add(new VersionedClause("biz.aQute.bnd.annotation", new Attrs()));
        model.setBuildPath(buildPath);
    }

    @Override
    public void modifyInitialBndProject(IBndProject project, String projectName, ProjectPaths projectPaths) {
        String src = projectPaths.getSrc();
        String pkgPath = projectName.replaceAll("\\.", "/");

        Map<String,String> replaceRegularExpressions = new LinkedHashMap<String,String>();
        replaceRegularExpressions.put("@package@", projectName);

        project.addResource(src + "/" + pkgPath + "/ExampleProviderInterface.java", new BndProjectResource(APITemplate.class.getResource("ExampleProviderInterface.java.txt"), replaceRegularExpressions));
        project.addResource(src + "/" + pkgPath + "/ExampleConsumerInterface.java", new BndProjectResource(APITemplate.class.getResource("ExampleConsumerInterface.java.txt"), replaceRegularExpressions));
        project.addResource(src + "/" + pkgPath + "/packageinfo", new BndProjectResource(APITemplate.class.getResource("packageinfo-template.txt"), null));
    }

    @Override
    public boolean enableTestSourceFolder() {
        return true;
    }
}
