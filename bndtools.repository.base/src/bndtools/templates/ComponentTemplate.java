package bndtools.templates;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import bndtools.api.IBndModel;
import bndtools.api.IBndProject;
import bndtools.api.IProjectTemplate;
import bndtools.editor.model.ServiceComponent;
import bndtools.model.clauses.VersionedClause;

public class ComponentTemplate implements IProjectTemplate {

    public void modifyInitialBndModel(IBndModel model) {
        List<VersionedClause> buildPath = new ArrayList<VersionedClause>();
        List<VersionedClause> tmp;

        tmp = model.getBuildPath();
        if (tmp != null)
            buildPath.addAll(tmp);
        buildPath.add(new VersionedClause("osgi.core", new HashMap<String, String>()));
        buildPath.add(new VersionedClause("osgi.cmpn", new HashMap<String, String>()));
        buildPath.add(new VersionedClause("biz.aQute.bnd.annotation", new HashMap<String, String>()));
        buildPath.add(new VersionedClause("junit.osgi", new HashMap<String, String>()));

        model.setBuildPath(buildPath);

        List<VersionedClause> runPath = new ArrayList<VersionedClause>();
        tmp = model.getRunBundles();
        if (tmp != null)
            runPath.addAll(tmp);
        runPath.add(new VersionedClause("osgi.cmpn", new HashMap<String, String>()));
        runPath.add(new VersionedClause("org.apache.felix.scr", new HashMap<String, String>()));
        runPath.add(new VersionedClause("org.apache.felix.shell", new HashMap<String, String>()));
        runPath.add(new VersionedClause("org.apache.felix.shell.tui", new HashMap<String, String>()));
        model.setRunBundles(runPath);

        model.setRunFramework("org.apache.felix.framework");

        model.setServiceComponents(Arrays.asList(new ServiceComponent[] { new ServiceComponent("*", null) }));
        model.setPrivatePackages(Arrays.asList(new String[] { "org.example" }));
    }

    @Override
    public void modifyInitialBndProject(IBndProject project) {
        URL componentSrc = IntegrationTestingTemplate.class.getResource("ExampleComponent.java.txt");
        project.addResource("src/org/example/ExampleComponent.java", componentSrc);
    }

}
