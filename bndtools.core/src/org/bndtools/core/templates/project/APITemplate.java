package org.bndtools.core.templates.project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.ExportedPackage;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;
import bndtools.api.IBndProject;
import bndtools.api.IProjectTemplate;

public class APITemplate implements IProjectTemplate {

    public void modifyInitialBndModel(BndEditModel model) {
        // Export-Package: org.example.api
        model.setExportedPackages(Arrays.asList(new ExportedPackage[] {
            new ExportedPackage("org.example.api", new Attrs())
        }));

        // Bundle-Version: 1.0.0
        model.setBundleVersion("1.0.0");

        // -buildpath
        List<VersionedClause> buildPath = new ArrayList<VersionedClause>();
        List<VersionedClause> tmp;
        tmp = model.getBuildPath();
        if (tmp != null)
            buildPath.addAll(tmp);
        buildPath.add(new VersionedClause("biz.aQute.bnd.annotation", new Attrs()));
        model.setBuildPath(buildPath);
    }

    public void modifyInitialBndProject(IBndProject project) {
        project.addResource("src/org/example/api/ExampleProviderInterface.java", APITemplate.class.getResource("ExampleProviderInterface.java.txt"));
        project.addResource("src/org/example/api/ExampleConsumerInterface.java", APITemplate.class.getResource("ExampleConsumerInterface.java.txt"));
        project.addResource("src/org/example/api/packageinfo", APITemplate.class.getResource("packageinfo-template.txt"));
    }

    public boolean enableTestSourceFolder() {
        return false;
    }

}
