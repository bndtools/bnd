package bndtools.templates.amdatu.mongo;

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
import aQute.bnd.build.model.clauses.ExportedPackage;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;

public class AmdatuMongoTemplate implements IProjectTemplate {
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
        
        model.setBuildPath(buildPath);

        model.setBundleActivator(projectName + ".mongo.Activator");

        model.setPrivatePackages(Arrays.asList(new String[] { projectName + ".mongo" }));
        model.setExportedPackages(Arrays.asList(new ExportedPackage(projectName + ".api", new Attrs())));
        
    }

    public void modifyInitialBndProject(IBndProject project, String projectName, ProjectPaths projectPaths) {
		String src = projectPaths.getSrc();
		String pkgPath = projectName.replaceAll("\\.", "/");

		Map<String, String> replaceRegularExpressionsApi = new LinkedHashMap<String,String>();
		replaceRegularExpressionsApi.put("@package@", projectName + ".api");

		Map<String, String> replaceRegularExpressionsMongo = new LinkedHashMap<String,String>();
		replaceRegularExpressionsMongo.put("@package@", projectName + ".mongo");
		replaceRegularExpressionsMongo.put("@package_api@", projectName + ".api");

		project.addResource(src + "/" + pkgPath + "/api/Example.java", new BndProjectResource(AmdatuMongoTemplate.class.getResource("Example.java.txt"), replaceRegularExpressionsApi));
        project.addResource(src + "/" + pkgPath + "/api/ExampleDocument.java", new BndProjectResource(AmdatuMongoTemplate.class.getResource("ExampleDocument.java.txt"), replaceRegularExpressionsApi));

        project.addResource(src + "/" + pkgPath + "/mongo/Activator.java", new BndProjectResource(AmdatuMongoTemplate.class.getResource("Activator.java.txt"), replaceRegularExpressionsMongo));
        project.addResource(src + "/" + pkgPath + "/mongo/ExampleComponent.java", new BndProjectResource(AmdatuMongoTemplate.class.getResource("ExampleComponent.java.txt"), replaceRegularExpressionsMongo));
    }

    public boolean enableTestSourceFolder() {
        return true;
    }
}
