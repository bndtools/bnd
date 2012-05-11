package test.bndtools.bndplugins.repo.git;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import aQute.bnd.service.RepositoryPlugin.Strategy;
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

	public void testGitRepo() throws Exception {
		GitOBRRepo repo = getOBRRepo();
		File bundleFile = repo.get("osgi.core", "[4.2, 4.3)", Strategy.HIGHEST, null);
		assertNotNull("Repository returned null", bundleFile);
		assertEquals(new File(checkoutDir, "jars/osgi.core/osgi.core-4.2.0.jar").getAbsoluteFile(), bundleFile);
	}

	private GitOBRRepo getOBRRepo() throws IOException {
		String repoUri = new File("testdata/testrepo.git").getAbsoluteFile().toURI().toString();
		
		Map<String, String> properties = new HashMap<String, String>();
		properties.put(GitOBRRepo.PROP_GIT_URI, repoUri);
		properties.put(GitOBRRepo.PROP_GIT_PUSH_URI, repoUri);
		properties.put(GitOBRRepo.PROP_GIT_BRANCH, "master");
		properties.put(GitOBRRepo.PROP_LOCAL_DIR, checkoutDir.getAbsolutePath());
		properties.put(GitOBRRepo.PROP_LOCAL_SUB_DIR, "jars");
		properties.put(GitOBRRepo.PROP_REPO_TYPE, GitOBRRepo.REPO_TYPE_OBR);

		GitOBRRepo repo = new GitOBRRepo();
		repo.setProperties(properties);

		return repo;
	}
}
