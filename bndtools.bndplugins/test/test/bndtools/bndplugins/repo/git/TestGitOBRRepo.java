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

    private final File checkoutDir = IO.getFile("generated/test-gitcheckout-tmp");

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
        assertEquals(IO.getFile(checkoutDir, "jars/osgi.core/osgi.core-4.2.0.jar").getAbsoluteFile(), bundleFile);
        removeOBRRepo();
    }

    public void testGitRepoPut() throws Exception {
        GitOBRRepo repo = getOBRRepo();
        repo.put(new BufferedInputStream(new FileInputStream(IO.getFile("testdata/eclipse2/ploogins/javax.servlet_2.5.0.v200806031605.jar"))), new RepositoryPlugin.PutOptions());
        File bundleFile = repo.get("javax.servlet", new Version("2.5"), null);
        assertNotNull("Repository returned null", bundleFile);
        assertEquals(IO.getFile(checkoutDir, "jars/javax.servlet/javax.servlet-2.5.0.jar"), bundleFile);
        removeOBRRepo();
    }

    private static File getOBRRepoDstDir() {
        return IO.getFile("testdata/tmp/testrepo.git");
    }

    private static void removeOBRRepo() throws IOException {
        IO.deleteWithException(getOBRRepoDstDir());
    }

    private GitOBRRepo getOBRRepo() throws IOException {
        File srcDir = IO.getFile("testdata/testrepo.git");
        File dstDir = getOBRRepoDstDir();
        IO.copy(srcDir, dstDir);

        String repoUri = dstDir.getAbsoluteFile().toURI().toString();

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
