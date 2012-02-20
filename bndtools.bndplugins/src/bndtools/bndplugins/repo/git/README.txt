A repository plugin for deploying to a git repository

Example:

-plugins:		bndtools.bndplugins.repo.git.GitOBRRepo;name='Bundles';path:=${build}/repo/bndtools.bndplugins.repos/bndtools.bndplugins.repos-0.0.0.jar;\
                git-uri=git://github.com/bndtools/repo.git;\
                git-push-uri=git@github.com:bndtools/repo.git;\
                git-branch=master;\
				sub=bundles
