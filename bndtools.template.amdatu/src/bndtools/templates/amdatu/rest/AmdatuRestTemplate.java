package bndtools.templates.amdatu.rest;

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

public class AmdatuRestTemplate implements IProjectTemplate {
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
        buildPath.add(new VersionedClause("javax.servlet", new Attrs()));
        buildPath.add(new VersionedClause("org.amdatu.web.rest.doc", new Attrs()));
        buildPath.add(new VersionedClause("org.amdatu.web.rest.jaxrs", new Attrs()));
        buildPath.add(new VersionedClause("org.amdatu.security.tokenprovider.api", new Attrs()));

        model.setBuildPath(buildPath);

        model.setBundleActivator(projectName + ".Activator");

        model.setPrivatePackages(Arrays.asList(new String[] { projectName }));
    }

    public void modifyInitialBndProject(IBndProject project, String projectName, ProjectPaths projectPaths) {
		String src = projectPaths.getSrc();
		String pkgPath = projectName.replaceAll("\\.", "/");

		Map<String, String> replaceRegularExpressions = new LinkedHashMap<String,String>();
		replaceRegularExpressions.put("@package@", projectName);

		project.addResource(src + "/" + pkgPath + "/Activator.java", new BndProjectResource(AmdatuRestTemplate.class.getResource("Activator.java.txt"), replaceRegularExpressions));
        project.addResource(src + "/" + pkgPath + "/ExampleComponent.java", new BndProjectResource(AmdatuRestTemplate.class.getResource("ExampleComponent.java.txt"), replaceRegularExpressions));
    }

    public boolean enableTestSourceFolder() {
        return true;
    }
}
