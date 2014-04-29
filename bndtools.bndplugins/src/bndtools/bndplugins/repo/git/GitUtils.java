package bndtools.bndplugins.repo.git;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.FS;

public class GitUtils {

    private static final Map<File,FileRepository> localRepos = new HashMap<File,FileRepository>();

    public static synchronized FileRepository getRepository(File gitRoot, String branch, String gitUrl, String gitPushUrl) throws IOException, ConfigInvalidException, JGitInternalException {

        File dotGit;
        if (gitRoot.getName().equals(Constants.DOT_GIT)) {
            dotGit = gitRoot;
        } else {
            dotGit = new File(gitRoot, Constants.DOT_GIT);
        }

        FileRepository repository = localRepos.get(dotGit);
        if (repository != null && dotGit.exists()) {
            return repository;
        }

        if (!dotGit.exists()) {
            Git.cloneRepository().setDirectory(gitRoot).setCloneAllBranches(true).setURI(gitUrl).call();
            FileBasedConfig config = new FileBasedConfig(new File(dotGit, "config"), FS.DETECTED);
            config.load();
            if (gitPushUrl != null) {
                config.setString(ConfigConstants.CONFIG_REMOTE_SECTION, "origin", "pushurl", gitPushUrl);
            }
            config.save();
        }

        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        repository = builder.setGitDir(dotGit).readEnvironment().findGitDir().build();
        localRepos.put(dotGit, repository);
        try {
            repository.incrementOpen();
            Git git = Git.wrap(repository);

            // Check branch
            boolean pull = true;
            String currentBranch = repository.getBranch();
            if (branch != null && !branch.equals(currentBranch)) {
                CheckoutCommand checkout = git.checkout();
                if (!branchExists(git, branch)) {
                    checkout.setCreateBranch(true);
                    pull = false;
                }
                checkout.setName(branch);
                checkout.call();
            }
            if (pull) {
                git.pull().call();
            } else {
                git.fetch().call();
            }
        } catch (Exception e) {
            if (!(e.getCause() instanceof TransportException)) {
                throw new RuntimeException(e);
            }
        } finally {
            if (repository != null) {
                repository.close();
            }
        }

        return repository;
    }

    private static boolean branchExists(Git git, String branch) {
        List<Ref> refs = git.branchList().call();
        for (Ref ref : refs) {
            if (branch.equals(ref.getName())) {
                return true;
            }
        }
        return false;
    }
}
