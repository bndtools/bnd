package bndtools.templates.amdatu.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bndtools.api.IBndProject;
import org.bndtools.api.IProjectTemplate;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;

public class AmdatuRestTemplate implements IProjectTemplate {
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
        buildPath.add(new VersionedClause("javax.servlet", new Attrs()));
        buildPath.add(new VersionedClause("org.amdatu.web.rest.doc", new Attrs()));
        buildPath.add(new VersionedClause("org.amdatu.web.rest.jaxrs", new Attrs()));
        buildPath.add(new VersionedClause("org.amdatu.security.tokenprovider.api", new Attrs()));

        model.setBuildPath(buildPath);

        model.setBundleActivator("org.example.rest.Activator");

        model.setPrivatePackages(Arrays.asList(new String[] { "org.example.rest" }));
    }

    public void modifyInitialBndProject(IBndProject project) {
        project.addResource("src/org/example/rest/Activator.java", AmdatuRestTemplate.class.getResource("Activator.java.txt"));
        project.addResource("src/org/example/rest/ExampleComponent.java", AmdatuRestTemplate.class.getResource("ExampleComponent.java.txt"));
    }

    public boolean enableTestSourceFolder() {
        return true;
    }
}
