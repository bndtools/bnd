package org.bndtools.templating.jgit;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import aQute.bnd.header.Parameters;

public class GitRepoPreferences {
	
	private static final String PREF_GITHUB_REPOS = "githubRepos";

	private final Bundle bundle = FrameworkUtil.getBundle(GitRepoPreferences.class);
	private final ScopedPreferenceStore store;

	public GitRepoPreferences() {
		store = new ScopedPreferenceStore(InstanceScope.INSTANCE, bundle.getSymbolicName());
		
		store.setDefault(PREF_GITHUB_REPOS, "osgi/workspace; name=\"OSGi enRoute\"");
	}
	
	public Parameters getGithubRepos() {
		String string = store.getString(PREF_GITHUB_REPOS);
		Parameters params = new Parameters(string);
		return params;
	}
	
	public void setGithubRepos(Parameters params) {
		store.setValue(PREF_GITHUB_REPOS, params.toString());
	}
}
