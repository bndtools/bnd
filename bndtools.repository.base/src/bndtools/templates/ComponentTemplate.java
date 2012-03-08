package bndtools.templates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import aQute.libg.header.Attrs;
import bndtools.api.IBndModel;
import bndtools.api.IBndProject;
import bndtools.api.IProjectTemplate;
import bndtools.api.Requirement;
import bndtools.model.clauses.ServiceComponent;
import bndtools.model.clauses.VersionedClause;

public class ComponentTemplate implements IProjectTemplate {

    public void modifyInitialBndModel(IBndModel model) {
        List<VersionedClause> buildPath = new ArrayList<VersionedClause>();
        List<VersionedClause> tmp;

        tmp = model.getBuildPath();
        if (tmp != null)
            buildPath.addAll(tmp);
        buildPath.add(new VersionedClause("osgi.core", new Attrs()));
        buildPath.add(new VersionedClause("osgi.cmpn", new Attrs()));
        buildPath.add(new VersionedClause("biz.aQute.bnd.annotation", new Attrs()));
        buildPath.add(new VersionedClause("junit.osgi", new Attrs()));

        model.setBuildPath(buildPath);

        List<VersionedClause> runPath = new ArrayList<VersionedClause>();
        List<Requirement> requires = new ArrayList<Requirement>();

        tmp = model.getRunBundles();
        if (tmp != null)
            runPath.addAll(tmp);

        addRunBundle("osgi.cmpn", runPath, requires, true);
        addRunBundle("org.apache.felix.scr", runPath, requires, false);
        addRunBundle("org.apache.felix.shell", runPath, requires, true);
        addRunBundle("org.apache.felix.shell.tui", runPath, requires, false);

        model.setRunRequire(requires);
        model.setRunBundles(runPath);
        model.setRunFramework("org.apache.felix.framework");

        model.setServiceComponents(Arrays.asList(new ServiceComponent[] { new ServiceComponent("*", null) }));
        model.setPrivatePackages(Arrays.asList(new String[] { "org.example" }));
    }

    private void addRunBundle(String bsn, Collection<? super VersionedClause> runPath, Collection<? super Requirement> requires, boolean inferred) {
        runPath.add(new VersionedClause(bsn, new Attrs()));
        if (!inferred)
            requires.add(new Requirement("bundle", "(symbolicname=" + bsn + ")"));
    }

    public void modifyInitialBndProject(IBndProject project) {
        project.addResource("src/org/example/ExampleComponent.java", ComponentTemplate.class.getResource("ExampleComponent.java.txt"));
        project.addResource("test/org/example/ExampleComponentTest.java", ComponentTemplate.class.getResource("ExampleComponentTest.java.txt"));
    }

    public boolean enableTestSourceFolder() {
        return true;
    }

}
