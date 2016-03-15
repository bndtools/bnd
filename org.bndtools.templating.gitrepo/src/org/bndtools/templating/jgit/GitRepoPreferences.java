package org.bndtools.templating.jgit;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import aQute.bnd.header.Parameters;

public class GitRepoPreferences {

    private static final String PREF_GITHUB_REPOS = "githubRepos";
    private static final String PREF_GIT_REPOS = "gitRepos";

    private final Bundle bundle = FrameworkUtil.getBundle(GitRepoPreferences.class);
    private final ScopedPreferenceStore store;

    public GitRepoPreferences() {
        store = new ScopedPreferenceStore(InstanceScope.INSTANCE, bundle.getSymbolicName());

        store.setDefault(PREF_GITHUB_REPOS, "osgi/workspace");
        store.setDefault(PREF_GIT_REPOS, "https://github.com/osgi/workspace.git");
    }

    public Parameters getGithubRepos() {
        return new Parameters(store.getString(PREF_GITHUB_REPOS));
    }

    public void setGithubRepos(Parameters params) {
        store.setValue(PREF_GITHUB_REPOS, params.toString());
    }

    public Parameters getGitRepos() {
        return new Parameters(store.getString(PREF_GIT_REPOS));
    }

    public void setGitRepos(Parameters params) {
        store.setValue(PREF_GIT_REPOS, params.toString());
    }
}
