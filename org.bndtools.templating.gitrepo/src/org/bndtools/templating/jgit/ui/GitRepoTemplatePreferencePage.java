package org.bndtools.templating.jgit.ui;

import org.bndtools.templating.jgit.GitRepoPreferences;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class GitRepoTemplatePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    private final Bundle bundle = FrameworkUtil.getBundle(GitRepoTemplatePreferencePage.class);

    private final EditableParametersPart githubReposPart = new EditableParametersPart("GitHub Repositories:", ImageDescriptor.createFromURL(bundle.getEntry("icons/github.png")), new NewEntryDialogFactory() {
        @Override
        public AbstractNewEntryDialog create(Shell parentShell) {
            return new GitHubRepoDialog(parentShell, "Add Repository");
        }
    });
    private final EditableParametersPart plainGitPart = new EditableParametersPart("Raw Git Clone URLs:", ImageDescriptor.createFromURL(bundle.getEntry("icons/git-16px.png")), new NewEntryDialogFactory() {
        @Override
        public AbstractNewEntryDialog create(Shell parentShell) {
            return new GitCloneURLDialog(parentShell, "Add Clone URL");
        }
    });

    private GitRepoPreferences prefs;

    @Override
    public void init(IWorkbench workbench) {
        this.prefs = new GitRepoPreferences();

        githubReposPart.setParameters(prefs.getGithubRepos());
        plainGitPart.setParameters(prefs.getGitRepos());
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        composite.setLayout(layout);

        Control groupGithub = githubReposPart.createControl(composite);
        groupGithub.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        Control groupPlainGit = plainGitPart.createControl(composite);
        groupPlainGit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        return composite;
    }

    @Override
    public boolean performOk() {
        prefs.setGithubRepos(githubReposPart.getParameters());
        prefs.setGitRepos(plainGitPart.getParameters());
        return prefs.save();
    }

}
