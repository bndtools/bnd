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
import aQute.bnd.build.model.clauses.ServiceComponent;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;

public class ComponentTemplate implements IProjectTemplate {

    public void modifyInitialBndModel(BndEditModel model, String projectName, ProjectPaths projectPaths) {
        model.setPrivatePackages(Arrays.asList(new String[] {
            projectName
        }));

        // Service-Component: *
        model.setServiceComponents(Arrays.asList(new ServiceComponent[] {
            new ServiceComponent("*", null)
        }));

        // -buildpath
        List<VersionedClause> buildPath = new ArrayList<VersionedClause>();
        List<VersionedClause> tmp;
        tmp = model.getBuildPath();
        if (tmp != null)
            buildPath.addAll(tmp);
        buildPath.add(new VersionedClause("osgi.core", new Attrs()));
        buildPath.add(new VersionedClause("osgi.cmpn", new Attrs()));
        buildPath.add(new VersionedClause("biz.aQute.bnd.annotation", new Attrs()));
        buildPath.add(new VersionedClause("${junit}", new Attrs()));

        model.setBuildPath(buildPath);
    }

    public void modifyInitialBndProject(IBndProject project, String projectName, ProjectPaths projectPaths) {
        String src = projectPaths.getSrc();
        String testsrc = projectPaths.getTestSrc();
        String pkgPath = projectName.replaceAll("\\.", "/");

        Map<String,String> replaceRegularExpressions = new LinkedHashMap<String,String>();
        replaceRegularExpressions.put("@package@", projectName);

        project.addResource(src + "/" + pkgPath + "/ExampleComponent.java", new BndProjectResource(ComponentTemplate.class.getResource("ExampleComponent.java.txt"), replaceRegularExpressions));
        project.addResource(testsrc + "/" + pkgPath + "/ExampleComponentTest.java", new BndProjectResource(ComponentTemplate.class.getResource("ExampleComponentTest.java.txt"), replaceRegularExpressions));
        project.addResource("launch.bndrun", new BndProjectResource(ComponentTemplate.class.getResource("/launchTemplates/felix4+shell.bndrun"), null));
    }

    public boolean enableTestSourceFolder() {
        return true;
    }

}
