package bndtools.team;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.team.core.RepositoryProvider;

public class TeamUtils {
    /**
     * Get the id of the repository provider that is managing the project
     * 
     * @param project
     *            the project
     * @return null when project is null or when the project is not managed by a version control plugin
     */
    static public String getProjectRepositoryProviderId(IJavaProject project) {
        if (project == null) {
            return null;
        }

        RepositoryProvider repositoryProvider = RepositoryProvider.getProvider(project.getProject());
        if (repositoryProvider != null) {
            return repositoryProvider.getID();
        }

        return null;
    }
}