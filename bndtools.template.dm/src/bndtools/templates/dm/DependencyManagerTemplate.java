package bndtools.templates.dm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.bndtools.api.IBndProject;
import org.bndtools.api.IProjectTemplate;
import org.osgi.framework.Constants;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.EE;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.resource.CapReqBuilder;

public class DependencyManagerTemplate implements IProjectTemplate {
    public void modifyInitialBndModel(BndEditModel model) {
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

        model.setBundleActivator("org.example.Activator");
        model.setRunRequires(requires);
        model.setRunBundles(runPath);
        model.setRunFw("org.apache.felix.framework");
        model.setEE(EE.JavaSE_1_6);

        model.setPrivatePackages(Arrays.asList(new String[] { "org.example" }));
    }

    private static void addRunBundle(String bsn, Collection<? super VersionedClause> runPath, Collection<? super Requirement> requires, boolean inferred) {
        runPath.add(new VersionedClause(bsn, new Attrs()));
        if (!inferred) {
            Requirement req = new CapReqBuilder("osgi.identity").addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, "(osgi.identity=" + bsn + ")").buildSyntheticRequirement();
            requires.add(req);
        }
    }

    public void modifyInitialBndProject(IBndProject project) {
        project.addResource("src/org/example/Activator.java", DependencyManagerTemplate.class.getResource("Activator.java.txt"));
        project.addResource("src/org/example/ExampleComponent.java", DependencyManagerTemplate.class.getResource("ExampleComponent.java.txt"));
        project.addResource("test/org/example/ExampleComponentTest.java", DependencyManagerTemplate.class.getResource("ExampleComponentTest.java.txt"));
    }

    public boolean enableTestSourceFolder() {
        return true;
    }
}
