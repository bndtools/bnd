package bndtools.templates;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.osgi.framework.Constants;

import aQute.libg.header.Attrs;
import bndtools.api.IBndModel;
import bndtools.api.IBndProject;
import bndtools.api.IProjectTemplate;
import bndtools.model.clauses.ExportedPackage;
import bndtools.model.clauses.VersionedClause;

public class IntegrationTestingTemplate implements IProjectTemplate {

    private static final String ALL_TEST_CASES_MACRO = "${classes;CONCRETE;EXTENDS;junit.framework.TestCase}"; //$NON-NLS-1$

    public void modifyInitialBndModel(IBndModel model) {
        List<VersionedClause> newBuildPath = new ArrayList<VersionedClause>();

        List<VersionedClause> oldBuildPath = model.getBuildPath();
        if (oldBuildPath != null) newBuildPath.addAll(oldBuildPath);

        newBuildPath.add(createBundleRef("osgi.core", "[4.1,5)"));
        newBuildPath.add(createBundleRef("osgi.cmpn", null));
        newBuildPath.add(createBundleRef("junit.osgi", null));
        newBuildPath.add(createBundleRef("org.mockito.mockito-all", null));
        model.setBuildPath(newBuildPath);

        model.setTestSuites(Arrays.asList(ALL_TEST_CASES_MACRO));
        model.setRunFramework("org.apache.felix.framework");
        model.setPrivatePackages(Arrays.asList(new String[] { "org.example.tests" }));
        model.setRunBundles(Arrays.asList(new VersionedClause[] { createBundleRef("org.mockito.mockito-all", null) }));

        model.setSystemPackages(Arrays.asList(new ExportedPackage[] { new ExportedPackage("sun.reflect", new Attrs()) }));
        model.setRunVMArgs("-ea");
    }

	VersionedClause createBundleRef(String bsn, String version) {
		Attrs attribs = new Attrs();
		if (version != null)
			attribs.put(Constants.VERSION_ATTRIBUTE, version);
		return new VersionedClause(bsn, attribs);
	}

    public void modifyInitialBndProject(IBndProject project) {
        URL testSrc = IntegrationTestingTemplate.class.getResource("ExampleTest.java.txt");
        project.addResource("src/org/example/tests/ExampleTest.java", testSrc);
    }

    public boolean enableTestSourceFolder() {
        return false;
    }
}
