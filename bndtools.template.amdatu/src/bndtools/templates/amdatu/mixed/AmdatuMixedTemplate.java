package bndtools.templates.amdatu.mixed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bndtools.api.BndProjectResource;
import org.bndtools.api.IBndProject;
import org.bndtools.api.IProjectTemplate;
import org.bndtools.api.ProjectPaths;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;
import bndtools.templates.amdatu.mongo.AmdatuMongoTemplate;
import bndtools.templates.amdatu.rest.AmdatuRestTemplate;

public class AmdatuMixedTemplate implements IProjectTemplate {
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

    public void modifyInitialBndProject(IBndProject project, String projectName, ProjectPaths projectPaths) {
    	String pkgPath = projectName.replaceAll("\\.", "/");
    	
    	Map<String, String> replaceRegularExpressionsApi = new LinkedHashMap<String,String>();
    	replaceRegularExpressionsApi.put("@package@", projectName + ".api");

    	Map<String, String> replaceRegularExpressionsMongo = new LinkedHashMap<String,String>();
    	replaceRegularExpressionsMongo.put("@package@", projectName + ".mongo");
    	replaceRegularExpressionsMongo.put("@package_api@", projectName + ".api");

    	Map<String, String> replaceRegularExpressionsRest = new LinkedHashMap<String,String>();
    	replaceRegularExpressionsRest.put("@package@", projectName + ".rest");

		String src = projectPaths.getSrc();
    	
    	project.addResource(src + "/" + pkgPath + "/api/packageinfo", new BndProjectResource(AmdatuMixedTemplate.class.getResource("packageinfo.txt"), null));
    	project.addResource(src + "/" + pkgPath + "/api/Example.java", new BndProjectResource(AmdatuMongoTemplate.class.getResource("Example.java.txt"), replaceRegularExpressionsApi));
        project.addResource(src + "/" + pkgPath + "/api/ExampleDocument.java", new BndProjectResource(AmdatuMongoTemplate.class.getResource("ExampleDocument.java.txt"), replaceRegularExpressionsApi));
        
        project.addResource(src + "/" + pkgPath + "/mongo/Activator.java", new BndProjectResource(AmdatuMongoTemplate.class.getResource("Activator.java.txt"), replaceRegularExpressionsMongo));
        project.addResource(src + "/" + pkgPath + "/mongo/ExampleComponent.java", new BndProjectResource(AmdatuMongoTemplate.class.getResource("ExampleComponent.java.txt"), replaceRegularExpressionsMongo));
        
        project.addResource(src + "/" + pkgPath + "/rest/Activator.java", new BndProjectResource(AmdatuRestTemplate.class.getResource("Activator.java.txt"), replaceRegularExpressionsRest));
        project.addResource(src + "/" + pkgPath + "/rest/ExampleComponent.java", new BndProjectResource(AmdatuRestTemplate.class.getResource("ExampleComponent.java.txt"), replaceRegularExpressionsRest));
        
        project.addResource("api.bnd", new BndProjectResource(AmdatuMixedTemplate.class.getResource("api.bnd.txt"), replaceRegularExpressionsApi));
        project.addResource("mongo.bnd", new BndProjectResource(AmdatuMixedTemplate.class.getResource("mongo.bnd.txt"), replaceRegularExpressionsMongo));
        project.addResource("rest.bnd", new BndProjectResource(AmdatuMixedTemplate.class.getResource("rest.bnd.txt"), replaceRegularExpressionsRest));
    }

    public boolean enableTestSourceFolder() {
        return true;
    }
}
