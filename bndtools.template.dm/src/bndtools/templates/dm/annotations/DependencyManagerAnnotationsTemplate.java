package bndtools.templates.dm.annotations;

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
import aQute.bnd.build.model.clauses.HeaderClause;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.resource.CapReqBuilder;

public class DependencyManagerAnnotationsTemplate implements IProjectTemplate {
    public void modifyInitialBndModel(BndEditModel model, String projectName, ProjectPaths projectPaths) {
        List<VersionedClause> buildPath = new ArrayList<VersionedClause>();
        List<VersionedClause> tmp;

        tmp = model.getBuildPath();
        if (tmp != null) {
            buildPath.addAll(tmp);
        }
        buildPath.add(new VersionedClause("osgi.core", new Attrs()));
        buildPath.add(new VersionedClause("osgi.cmpn", new Attrs()));
        buildPath.add(new VersionedClause("${build}/plugins/org.apache.felix.dependencymanager.annotation-3.1.1-SNAPSHOT.jar;version=file", new Attrs()));
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
        addRunBundle("org.apache.felix.dependencymanager.runtime", runPath, requires, false);
        addRunBundle("org.apache.felix.dependencymanager.shell", runPath, requires, false);
        addRunBundle("org.apache.felix.gogo.shell", runPath, requires, false);
        addRunBundle("org.apache.felix.gogo.command", runPath, requires, false);
        addRunBundle("org.apache.felix.gogo.runtime", runPath, requires, true);
        addRunBundle("org.apache.felix.log", runPath, requires, false);

        model.setRunRequires(requires);
        model.setRunBundles(runPath);
        model.setRunFw("org.apache.felix.framework;version='[4.0.3,4.0.3]'");
        model.setEE(EE.JavaSE_1_6);

        model.setPrivatePackages(Arrays.asList(new String[] {
            projectName
        }));

        List<HeaderClause> plugins = new ArrayList<HeaderClause>();
        plugins.add(new HeaderClause("org.apache.felix.dm.annotation.plugin.bnd.AnnotationPlugin;path:=../cnf/plugins/org.apache.felix.dependencymanager.annotation-3.1.1-snapshot.jar", new Attrs()));
        model.setPlugins(plugins);
    }

    private static void addRunBundle(String bsn, Collection< ? super VersionedClause> runPath, Collection< ? super Requirement> requires, boolean inferred) {
        runPath.add(new VersionedClause(bsn, new Attrs()));
        if (!inferred) {
            Requirement r = new CapReqBuilder("osgi.identity").addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, "(osgi.identity=" + bsn + ")").buildSyntheticRequirement();
            requires.add(r);
        }
    }

    public void modifyInitialBndProject(IBndProject project, String projectName, ProjectPaths projectPaths) {
        String src = projectPaths.getSrc();
        String testsrc = projectPaths.getTestSrc();
        String pkgPath = projectName.replaceAll("\\.", "/");

        Map<String,String> replaceRegularExpressions = new LinkedHashMap<String,String>();
        replaceRegularExpressions.put("@package@", projectName);

        project.addResource(src + "/" + pkgPath + "/ExampleComponent.java", new BndProjectResource(DependencyManagerAnnotationsTemplate.class.getResource("ExampleComponent.java.txt"), replaceRegularExpressions));
        project.addResource(testsrc + "/" + pkgPath + "/ExampleComponentTest.java", new BndProjectResource(DependencyManagerAnnotationsTemplate.class.getResource("ExampleComponentTest.java.txt"), replaceRegularExpressions));
    }

    public boolean enableTestSourceFolder() {
        return true;
    }
}
