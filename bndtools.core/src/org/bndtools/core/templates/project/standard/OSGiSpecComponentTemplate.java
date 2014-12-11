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
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;

public class OSGiSpecComponentTemplate implements IProjectTemplate {

    @Override
    public void modifyInitialBndModel(BndEditModel model, String projectName, ProjectPaths projectPaths) {
        model.setPrivatePackages(Arrays.asList(new String[] {
            projectName
        }));

        // -dsannotation: *
        model.setDSAnnotationPatterns(Arrays.asList(new String[] {
            "*"
        }));

        // -buildpath
        List<VersionedClause> buildPath = new ArrayList<VersionedClause>();
        List<VersionedClause> tmp;
        tmp = model.getBuildPath();
        if (tmp != null)
            buildPath.addAll(tmp);
        buildPath.add(new VersionedClause("osgi.core", new Attrs()));

        VersionedClause cmpnClause = new VersionedClause("osgi.cmpn", new Attrs());
        cmpnClause.setVersionRange("5.0.0");
        buildPath.add(cmpnClause);

        model.setBuildPath(buildPath);
    }

    @Override
    public void modifyInitialBndProject(IBndProject project, String projectName, ProjectPaths projectPaths) {
        String src = projectPaths.getSrc();
        String pkgPath = projectName.replaceAll("\\.", "/");

        Map<String,String> replaceRegularExpressions = new LinkedHashMap<String,String>();
        replaceRegularExpressions.put("@package@", projectName);

        project.addResource(src + "/" + pkgPath + "/ExampleComponent.java", new BndProjectResource(OSGiSpecComponentTemplate.class.getResource("ExampleComponent.java.txt"), replaceRegularExpressions));
        project.addResource("launch.bndrun", new BndProjectResource(OSGiSpecComponentTemplate.class.getResource("/launchTemplates/felix4+shell.bndrun"), null));
    }

    @Override
    public boolean enableTestSourceFolder() {
        return true;
    }

}
