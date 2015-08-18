package org.bndtools.core.templates.project.standard;

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

public class OSGiSpecAPITemplate implements IProjectTemplate {

    @Override
    public void modifyInitialBndModel(BndEditModel model, String projectName, ProjectPaths projectPaths) {
        model.setExportedPackages(Arrays.asList(new ExportedPackage[] {
                new ExportedPackage(projectName, new Attrs())
        }));

        // Bundle-Version: 1.0.0
        model.setBundleVersion("1.0.0.${tstamp}");

        // -baseline
        model.setGenericString("-baseline", "*");

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
        String src = projectPaths.getSrc();
        String pkgPath = projectName.replaceAll("\\.", "/");

        Map<String,String> replaceRegularExpressions = new LinkedHashMap<String,String>();
        replaceRegularExpressions.put("@package@", projectName);

        project.addResource(src + "/" + pkgPath + "/ExampleProviderInterface.java", new BndProjectResource(OSGiSpecAPITemplate.class.getResource("ExampleProviderInterface.java.txt"), replaceRegularExpressions));
        project.addResource(src + "/" + pkgPath + "/ExampleConsumerInterface.java", new BndProjectResource(OSGiSpecAPITemplate.class.getResource("ExampleConsumerInterface.java.txt"), replaceRegularExpressions));
        project.addResource(src + "/" + pkgPath + "/package-info.java", new BndProjectResource(OSGiSpecAPITemplate.class.getResource("package-info.java.txt"), replaceRegularExpressions));
    }

    @Override
    public boolean enableTestSourceFolder() {
        return true;
    }
}
