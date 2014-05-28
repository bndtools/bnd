package org.bndtools.core.templates.enroute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bndtools.api.BndProjectResource;
import org.bndtools.api.IBndProject;
import org.bndtools.api.IProjectTemplate;
import org.bndtools.api.ProjectPaths;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.ExportedPackage;
import aQute.bnd.build.model.clauses.ServiceComponent;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;

public class ProjectTemplate implements IProjectTemplate {
    static Pattern LAST_PART = Pattern.compile(".*\\.([^.]+)");

    @Override
    public void modifyInitialBndModel(BndEditModel model, String projectName, ProjectPaths projectPaths) {
        // Service-Component: *
        model.setServiceComponents(Arrays.asList(new ServiceComponent[] {
            new ServiceComponent("*", null)
        }));

        model.setPrivatePackages(Arrays.asList(new String[] {
            projectName + ".provider"
        }));

        model.setExportedPackages(Arrays.asList(new ExportedPackage(projectName + ".api", new Attrs())));

        model.setBundleDescription("${warning:please explain what this bundle does}");
        model.setBundleVersion("1.0.0.${tstamp}");

        List<VersionedClause> buildPath = new ArrayList<VersionedClause>();
        List<VersionedClause> tmp;
        tmp = model.getBuildPath();
        if (tmp != null)
            buildPath.addAll(tmp);
        buildPath.add(new VersionedClause("osgi.enroute.base.api", new Attrs()));
        buildPath.add(new VersionedClause("osgi.enroute.base.junit", new Attrs()));

    }

    @Override
    public void modifyInitialBndProject(IBndProject project, String projectName, ProjectPaths projectPaths) {
        String impl = "ExampleImpl";
        String api = "Example";

        Matcher m = LAST_PART.matcher(projectName);
        if (m.matches()) {
            String stem = m.group(1).substring(0, 1).toUpperCase() + m.group(1).substring(1);
            impl = stem + "Impl";
            api = stem;
        }

        String src = projectPaths.getSrc();
        String testsrc = projectPaths.getTestSrc();
        String pkgPath = projectName.replaceAll("\\.", "/");

        Map<String,String> replaceRegularExpressions = new LinkedHashMap<String,String>();
        replaceRegularExpressions.put("@package@", projectName);
        replaceRegularExpressions.put("@implementation@", impl);
        replaceRegularExpressions.put("@api@", api);

        project.addResource(src + "/" + pkgPath + "/api/" + api + ".java", getResource("/enroute/ExampleApi.java", replaceRegularExpressions));
        project.addResource(src + "/" + pkgPath + "/provider/" + impl + ".java", getResource("/enroute/ExampleImpl.java", replaceRegularExpressions));
        project.addResource(testsrc + "/" + pkgPath + "/provider/" + impl + "Test.java", getResource("/enroute/ExampleImplTest.java", replaceRegularExpressions));
        project.addResource("launch.bndrun", getResource("/enroute/launch.bndrun", replaceRegularExpressions));
    }

    private BndProjectResource getResource(String path, Map<String,String> regexs) {
        return new BndProjectResource(ProjectTemplate.class.getResource(path), regexs);
    }

    @Override
    public boolean enableTestSourceFolder() {
        return true;
    }
}
