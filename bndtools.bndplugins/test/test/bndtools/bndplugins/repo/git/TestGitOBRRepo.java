package test.bndtools.bndplugins.repo.git;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;
import bndtools.bndplugins.repo.git.GitOBRRepo;

public class TestGitOBRRepo extends TestCase {

    private File checkoutDir = new File("generated/test-gitcheckout-tmp");

    @Override
    protected void setUp() throws Exception {
        IO.delete(checkoutDir);
        checkoutDir.mkdirs();
    }

    @Override
    protected void tearDown() throws Exception {
        IO.delete(checkoutDir);
    }

    public void testGitRepoGet() throws Exception {
        GitOBRRepo repo = getOBRRepo();
        File bundleFile = repo.get("osgi.core", new Version("4.2.0"), null);
        assertNotNull("Repository returned null", bundleFile);
        assertEquals(new File(checkoutDir, "jars/osgi.core/osgi.core-4.2.0.jar").getAbsoluteFile(), bundleFile);
    }

    public void testGitRepoPut() throws Exception {
        GitOBRRepo repo = getOBRRepo();
        repo.put(new BufferedInputStream(new FileInputStream("testdata/eclipse2/ploogins/javax.servlet_2.5.0.v200806031605.jar")), new RepositoryPlugin.PutOptions());
        File bundleFile = repo.get("javax.servlet", new Version("2.5"), null);
        assertNotNull("Repository returned null", bundleFile);
        assertEquals(new File(checkoutDir, "jars/javax.servlet/javax.servlet-2.5.0.jar").getAbsoluteFile(), bundleFile);
    }

    private GitOBRRepo getOBRRepo() throws IOException {
        String repoUri = new File("testdata/testrepo.git").getAbsoluteFile().toURI().toString();

        Map<String,String> properties = new HashMap<String,String>();
        properties.put(GitOBRRepo.PROP_GIT_URI, repoUri);
        properties.put(GitOBRRepo.PROP_GIT_PUSH_URI, repoUri);
        properties.put(GitOBRRepo.PROP_GIT_BRANCH, "master");
        properties.put(GitOBRRepo.PROP_LOCAL_DIR, checkoutDir.getAbsolutePath());
        properties.put(GitOBRRepo.PROP_LOCAL_SUB_DIR, "jars");
        properties.put(GitOBRRepo.PROP_REPO_TYPE, GitOBRRepo.REPO_TYPE_OBR);

        GitOBRRepo repo = new GitOBRRepo();
        repo.setProperties(properties);

        Properties props = new Properties();
        props.put(Processor.PLUGIN, org.osgi.impl.bundle.bindex.BundleIndexerImpl.class.getName());
        Processor processor = new Processor();
        processor.setProperties(props);
        repo.setReporter(processor);
        repo.setRegistry(processor);
        return repo;
    }
}
