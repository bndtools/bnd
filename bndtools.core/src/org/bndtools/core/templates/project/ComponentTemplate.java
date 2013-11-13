package org.bndtools.core.templates.project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bndtools.api.IBndProject;
import org.bndtools.api.IProjectTemplate;
import org.osgi.framework.Constants;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.ServiceComponent;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;

public class ComponentTemplate implements IProjectTemplate {

    public void modifyInitialBndModel(BndEditModel model) {
        // Private-Package: org.example
        model.setPrivatePackages(Arrays.asList(new String[] {
            "org.example"
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
        Attrs attrs = new Attrs();
        attrs.put(Constants.VERSION_ATTRIBUTE, "file");
        buildPath.add(new VersionedClause("${workspace}/cnf/buildrepo/org.junit/junit-" + bndtools.repository.base.Constants.JUNIT_VERSION + ".jar", attrs));
        buildPath.add(new VersionedClause("${workspace}/cnf/buildrepo/org.hamcrest/hamcrest-core-" + bndtools.repository.base.Constants.HAMCREST_CORE_VERSION + ".jar", attrs));

        model.setBuildPath(buildPath);
    }

    public void modifyInitialBndProject(IBndProject project) {
        project.addResource("src/org/example/ExampleComponent.java", ComponentTemplate.class.getResource("ExampleComponent.java.txt"));
        project.addResource("test/org/example/ExampleComponentTest.java", ComponentTemplate.class.getResource("ExampleComponentTest.java.txt"));
        project.addResource("launch.bndrun", ComponentTemplate.class.getResource("/launchTemplates/felix4+shell.bndrun"));
    }

    public boolean enableTestSourceFolder() {
        return true;
    }

}
