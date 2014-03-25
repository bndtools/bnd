package bndtools.templates.dm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bndtools.api.BndProjectResource;
import org.bndtools.api.IBndProject;
import org.bndtools.api.IProjectTemplate;
import org.bndtools.api.ProjectPaths;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.EE;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.resource.CapReqBuilder;

public class DependencyManagerTemplate implements IProjectTemplate {
    public void modifyInitialBndModel(BndEditModel model, String projectName, ProjectPaths projectPaths) {
        List<VersionedClause> buildPath = new ArrayList<VersionedClause>();
        List<VersionedClause> tmp;

        tmp = model.getBuildPath();
        if (tmp != null) {
            buildPath.addAll(tmp);
        }
        buildPath.add(new VersionedClause("osgi.core", new Attrs()));
        buildPath.add(new VersionedClause("osgi.cmpn", new Attrs()));
        buildPath.add(new VersionedClause("org.apache.felix.dependencymanager", new Attrs()));
        buildPath.add(new VersionedClause("${junit}", new Attrs()));

        model.setBuildPath(buildPath);

        List<VersionedClause> runPath = new ArrayList<VersionedClause>();
        List<Requirement> requires = new ArrayList<Requirement>();

        tmp = model.getRunBundles();
        if (tmp != null) {
            runPath.addAll(tmp);
        }

        addRunBundle("osgi.cmpn", runPath, requires, true);
        addRunBundle("org.apache.felix.dependencymanager", runPath, requires, true);
        addRunBundle("org.apache.felix.dependencymanager.shell", runPath, requires, false);
        addRunBundle("org.apache.felix.gogo.shell", runPath, requires, false);
        addRunBundle("org.apache.felix.gogo.command", runPath, requires, false);
        addRunBundle("org.apache.felix.gogo.runtime", runPath, requires, true);
        addRunBundle("org.apache.felix.log", runPath, requires, false);

        model.setBundleActivator(projectName + ".Activator");
        model.setRunRequires(requires);
        model.setRunBundles(runPath);
        model.setRunFw("org.apache.felix.framework");
        model.setEE(EE.JavaSE_1_6);

        model.setPrivatePackages(Arrays.asList(new String[] {
            projectName
        }));
    }

    private static void addRunBundle(String bsn, Collection< ? super VersionedClause> runPath, Collection< ? super Requirement> requires, boolean inferred) {
        runPath.add(new VersionedClause(bsn, new Attrs()));
        if (!inferred) {
            Requirement req = new CapReqBuilder("osgi.identity").addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, "(osgi.identity=" + bsn + ")").buildSyntheticRequirement();
            requires.add(req);
        }
    }

    public void modifyInitialBndProject(IBndProject project, String projectName, ProjectPaths projectPaths) {
        String src = projectPaths.getSrc();
        String testsrc = projectPaths.getTestSrc();
        String pkgPath = projectName.replaceAll("\\.", "/");

        Map<String,String> replaceRegularExpressions = new LinkedHashMap<String,String>();
        replaceRegularExpressions.put("@package@", projectName);

        project.addResource(src + "/" + pkgPath + "/Activator.java", new BndProjectResource(DependencyManagerTemplate.class.getResource("Activator.java.txt"), replaceRegularExpressions));
        project.addResource(src + "/" + pkgPath + "/ExampleComponent.java", new BndProjectResource(DependencyManagerTemplate.class.getResource("ExampleComponent.java.txt"), replaceRegularExpressions));
        project.addResource(testsrc + "/" + pkgPath + "/ExampleComponentTest.java", new BndProjectResource(DependencyManagerTemplate.class.getResource("ExampleComponentTest.java.txt"), replaceRegularExpressions));
    }

    public boolean enableTestSourceFolder() {
        return true;
    }
}
