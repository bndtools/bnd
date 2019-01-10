package test.bndtools.bndplugins.repo.git;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import aQute.bnd.deployer.repository.LocalIndexedRepo;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;
import bndtools.bndplugins.repo.git.GitOBRRepo;
import junit.framework.TestCase;

public class TestGitOBRRepo extends TestCase {

    private File checkoutDir;
    private File repoDstDir;

    @Override
    protected void setUp() throws Exception {
        File tmp = IO.getFile("generated/tmp/test/" + getName());
        IO.delete(tmp);
        checkoutDir = IO.getFile(tmp, "gitcheckout");
        repoDstDir = IO.getFile(tmp, "testrepo.git");
        checkoutDir.mkdirs();
        repoDstDir.mkdirs();
    }

    public void testGitRepoGet() throws Exception {
        GitOBRRepo repo = getOBRRepo();
        File bundleFile = repo.get("osgi.core", new Version("4.2.0"), null);
        assertNotNull("Repository returned null", bundleFile);
        assertEquals(IO.getFile(checkoutDir, "jars/osgi.core/osgi.core-4.2.0.jar")
            .getAbsoluteFile(), bundleFile);
    }

    public void testGitRepoPut() throws Exception {
        GitOBRRepo repo = getOBRRepo();
        repo.put(new BufferedInputStream(new FileInputStream(IO.getFile("testdata/eclipse2/ploogins/javax.servlet_2.5.0.v200806031605.jar"))), new RepositoryPlugin.PutOptions());
        File bundleFile = repo.get("javax.servlet", new Version("2.5"), null);
        assertNotNull("Repository returned null", bundleFile);
        assertEquals(IO.getFile(checkoutDir, "jars/javax.servlet/javax.servlet-2.5.0.jar"), bundleFile);
    }

    private GitOBRRepo getOBRRepo() throws IOException {
        File srcDir = IO.getFile("testdata/testrepo.git");
        IO.copy(srcDir, repoDstDir);
        String repoUri = repoDstDir.getAbsoluteFile()
            .toURI()
            .toString();

        Map<String, String> properties = new HashMap<String, String>();
        properties.put(GitOBRRepo.PROP_GIT_URI, repoUri);
        properties.put(GitOBRRepo.PROP_GIT_PUSH_URI, repoUri);
        properties.put(GitOBRRepo.PROP_GIT_BRANCH, "master");
        properties.put(LocalIndexedRepo.PROP_LOCAL_DIR, checkoutDir.getAbsolutePath());
        properties.put(GitOBRRepo.PROP_LOCAL_SUB_DIR, "jars");

        GitOBRRepo repo = new GitOBRRepo();
        repo.setProperties(properties);

        Properties props = new Properties();
        Processor processor = new Processor();
        processor.setProperties(props);
        repo.setReporter(processor);
        repo.setRegistry(processor);
        return repo;
    }
}
