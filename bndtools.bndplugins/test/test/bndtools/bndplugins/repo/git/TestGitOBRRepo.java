package test.bndtools.bndplugins.repo.git;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import aQute.bnd.service.RepositoryPlugin.Strategy;
import bndtools.bndplugins.repo.git.GitOBRRepo;

public class TestGitOBRRepo extends TestCase {

	public void testGitRepo() throws Exception {

		GitOBRRepo repo = getOBRRepo();
		File bundleFile = repo.get("osgi.core", "[4.2, 4.3)", Strategy.HIGHEST, null);
		assertNotNull("Repository returned null", bundleFile);

	}

	private static GitOBRRepo getOBRRepo() throws IOException {

		Map<String, String> properties = new HashMap<String, String>();
		properties.put(GitOBRRepo.PROP_GIT_URI, "git://github.com/bndtools/repo.git");
		properties.put(GitOBRRepo.PROP_GIT_PUSH_URI, "git@github.com:bndtools/repo.git");
		properties.put(GitOBRRepo.PROP_GIT_BRANCH, "master");
		properties.put(GitOBRRepo.PROP_LOCAL_SUB_DIR, "jars");
		properties.put(GitOBRRepo.PROP_REPO_TYPE, GitOBRRepo.REPO_TYPE_OBR);

		GitOBRRepo repo = new GitOBRRepo();
		repo.setProperties(properties);

		return repo;
	}
}
