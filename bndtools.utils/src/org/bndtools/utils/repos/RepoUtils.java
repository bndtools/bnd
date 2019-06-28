package org.bndtools.utils.repos;

import aQute.bnd.build.WorkspaceRepository;
import aQute.bnd.service.RepositoryPlugin;

public class RepoUtils {

	public static boolean isWorkspaceRepo(RepositoryPlugin repo) {
		if (repo.getClass() == WorkspaceRepository.class)
			return true;
		return false;
	}

}
