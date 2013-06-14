package bndtools.templates.amdatu.mongo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bndtools.api.IBndProject;
import org.bndtools.api.IProjectTemplate;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.ExportedPackage;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;

public class AmdatuMongoTemplate implements IProjectTemplate {
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
        buildPath.add(new VersionedClause("org.amdatu.mongo", new Attrs()));
        buildPath.add(new VersionedClause("jackson-core-asl", new Attrs()));
        buildPath.add(new VersionedClause("jackson-mapper-asl", new Attrs()));
        buildPath.add(new VersionedClause("de.undercouch.bson4jackson", new Attrs()));
        buildPath.add(new VersionedClause("net.vz.mongodb.jackson.mongo-jackson-mapper", new Attrs()));
        buildPath.add(new VersionedClause("org.mongodb.mongo-java-driver", new Attrs()));
        
        model.setBuildPath(buildPath);

        model.setBundleActivator("org.example.mongo.Activator");

        model.setPrivatePackages(Arrays.asList(new String[] { "org.example.mongo" }));
        model.setExportedPackages(Arrays.asList(new ExportedPackage("org.example.api", new Attrs())));
        
    }

    public void modifyInitialBndProject(IBndProject project) {
    	project.addResource("src/org/example/api/Example.java", AmdatuMongoTemplate.class.getResource("Example.java.txt"));
        project.addResource("src/org/example/api/ExampleDocument.java", AmdatuMongoTemplate.class.getResource("ExampleDocument.java.txt"));
        project.addResource("src/org/example/mongo/Activator.java", AmdatuMongoTemplate.class.getResource("Activator.java.txt"));
        project.addResource("src/org/example/mongo/ExampleComponent.java", AmdatuMongoTemplate.class.getResource("ExampleComponent.java.txt"));
    }

    public boolean enableTestSourceFolder() {
        return true;
    }
}
