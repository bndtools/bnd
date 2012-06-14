package bndtools.templates.dm.annotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import aQute.libg.header.Attrs;
import bndtools.api.EE;
import bndtools.api.IBndModel;
import bndtools.api.IBndProject;
import bndtools.api.IProjectTemplate;
import bndtools.api.Requirement;
import bndtools.model.clauses.HeaderClause;
import bndtools.model.clauses.VersionedClause;

public class DependencyManagerAnnotationsTemplate implements IProjectTemplate {
    public void modifyInitialBndModel(IBndModel model) {
        List<VersionedClause> buildPath = new ArrayList<VersionedClause>();
        List<VersionedClause> tmp;

        tmp = model.getBuildPath();
        if (tmp != null) {
            buildPath.addAll(tmp);
        }
        buildPath.add(new VersionedClause("osgi.core", new Attrs()));
        buildPath.add(new VersionedClause("osgi.cmpn", new Attrs()));
        buildPath.add(new VersionedClause("org.apache.felix.dependencymanager.annotation", new Attrs()));
        buildPath.add(new VersionedClause("junit.osgi", new Attrs()));

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

        model.setBundleActivator("org.example.Activator");
        model.setRunRequire(requires);
        model.setRunBundles(runPath);
        model.setRunFramework("org.apache.felix.framework");
        model.setEE(EE.JavaSE_1_6);

        model.setPrivatePackages(Arrays.asList(new String[] { "org.example" }));
        
        List<HeaderClause> plugins = new ArrayList<HeaderClause>();
        plugins.add(new HeaderClause("org.apache.felix.dm.annotation.plugin.bnd.AnnotationPlugin;path:=../cnf/localrepo/org.apache.felix.dependencymanager.annotation/org.apache.felix.dependencymanager.annotation-3.1.0.jar", new Attrs()));
        model.setPlugins(plugins);
    }

    private static void addRunBundle(String bsn, Collection<? super VersionedClause> runPath, Collection<? super Requirement> requires, boolean inferred) {
        runPath.add(new VersionedClause(bsn, new Attrs()));
        if (!inferred) {
            requires.add(new Requirement("bundle", "(symbolicname=" + bsn + ")"));
        }
    }

    public void modifyInitialBndProject(IBndProject project) {
        project.addResource("src/org/example/ExampleComponent.java", DependencyManagerAnnotationsTemplate.class.getResource("ExampleComponent.java.txt"));
        project.addResource("test/org/example/ExampleComponentTest.java", DependencyManagerAnnotationsTemplate.class.getResource("ExampleComponentTest.java.txt"));
    }

    public boolean enableTestSourceFolder() {
        return true;
    }
}
