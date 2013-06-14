package bndtools.templates.amdatu.mixed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bndtools.api.IBndProject;
import org.bndtools.api.IProjectTemplate;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;
import bndtools.templates.amdatu.mongo.AmdatuMongoTemplate;
import bndtools.templates.amdatu.rest.AmdatuRestTemplate;

public class AmdatuMixedTemplate implements IProjectTemplate {
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
        buildPath.add(new VersionedClause("javax.servlet", new Attrs()));
        buildPath.add(new VersionedClause("org.amdatu.web.rest.doc", new Attrs()));
        buildPath.add(new VersionedClause("org.amdatu.web.rest.jaxrs", new Attrs()));
        buildPath.add(new VersionedClause("org.amdatu.security.tokenprovider.api", new Attrs()));
        
        model.setBuildPath(buildPath);
        model.setSubBndFiles(Arrays.asList("mongo.bnd", "rest.bnd", "api.bnd"));
    }

    public void modifyInitialBndProject(IBndProject project) {
    	project.addResource("api.bnd", AmdatuMixedTemplate.class.getResource("api.bnd.txt"));
    	project.addResource("mongo.bnd", AmdatuMixedTemplate.class.getResource("mongo.bnd.txt"));
    	project.addResource("rest.bnd", AmdatuMixedTemplate.class.getResource("rest.bnd.txt"));
    	
    	project.addResource("src/org/example/api/packageinfo", AmdatuMixedTemplate.class.getResource("packageinfo.txt"));
    	project.addResource("src/org/example/api/Example.java", AmdatuMongoTemplate.class.getResource("Example.java.txt"));
        project.addResource("src/org/example/api/ExampleDocument.java", AmdatuMongoTemplate.class.getResource("ExampleDocument.java.txt"));
        
        project.addResource("src/org/example/mongo/Activator.java", AmdatuMongoTemplate.class.getResource("Activator.java.txt"));
        project.addResource("src/org/example/mongo/ExampleComponent.java", AmdatuMongoTemplate.class.getResource("ExampleComponent.java.txt"));
        
        project.addResource("src/org/example/rest/Activator.java", AmdatuRestTemplate.class.getResource("Activator.java.txt"));
        project.addResource("src/org/example/rest/ExampleComponent.java", AmdatuRestTemplate.class.getResource("ExampleComponent.java.txt"));
    }

    public boolean enableTestSourceFolder() {
        return true;
    }
}
