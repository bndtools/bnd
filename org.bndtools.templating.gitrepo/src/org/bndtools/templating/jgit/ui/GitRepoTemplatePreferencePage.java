package org.bndtools.templating.jgit.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.bndtools.templating.jgit.GitRepoPreferences;
import org.bndtools.utils.swt.AddRemoveButtonBarPart;
import org.bndtools.utils.swt.AddRemoveButtonBarPart.AddRemoveListener;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.libg.tuple.Pair;

public class GitRepoTemplatePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    private IWorkbench workbench;
    private GitRepoPreferences prefs;
    private List<Pair<String,Attrs>> githubRepos;

    private TableViewer vwrGithub;

    @Override
    public void init(IWorkbench workbench) {
        this.workbench = workbench;
        this.prefs = new GitRepoPreferences();

        Parameters githubRepos = prefs.getGithubRepos();
        this.githubRepos = new ArrayList<>(githubRepos.size());
        for (Entry<String,Attrs> entry : githubRepos.entrySet()) {
            this.githubRepos.add(new Pair<>(entry.getKey(), entry.getValue()));
        }
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        composite.setLayout(layout);

        // GitHub Group
        Composite grpGithub = new Composite(composite, SWT.NONE);
        grpGithub.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        grpGithub.setLayout(new GridLayout(2, false));

        Label lblGithub = new Label(grpGithub, SWT.NONE);
        lblGithub.setText("GitHub Repositories:");
        lblGithub.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
        Table tblGithubRepos = new Table(grpGithub, SWT.BORDER | SWT.MULTI);
        vwrGithub = new TableViewer(tblGithubRepos);
        vwrGithub.setContentProvider(ArrayContentProvider.getInstance());
        vwrGithub.setLabelProvider(new StyledCellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                @SuppressWarnings("unchecked")
                Pair<String,Attrs> entry = (Pair<String,Attrs>) cell.getElement();
                cell.setText(entry.getFirst());
                cell.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER));
            }
        });
        vwrGithub.setInput(githubRepos);
        GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gd.widthHint = 280;
        gd.heightHint = 80;
        tblGithubRepos.setLayoutData(gd);

        final AddRemoveButtonBarPart addRemoveButtonBarPart = new AddRemoveButtonBarPart();
        Control addRemoveControl = addRemoveButtonBarPart.createControl(grpGithub, SWT.FLAT | SWT.VERTICAL);
        addRemoveControl.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        addRemoveButtonBarPart.setRemoveEnabled(false);
        addRemoveButtonBarPart.addListener(new AddRemoveListener() {
            @Override
            public void addSelected() {
                doAddGithub();
            }

            @Override
            public void removeSelected() {
                doRemoveGithub();
            }
        });
        vwrGithub.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                addRemoveButtonBarPart.setRemoveEnabled(!vwrGithub.getSelection().isEmpty());
            }
        });
        tblGithubRepos.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.DEL && e.stateMask == 0)
                    doRemoveGithub();
            }
        });

        return composite;
    }

    protected void doAddGithub() {
        GitHubRepoDialog dialog = new GitHubRepoDialog(getShell(), "Add GitHub Repository");
        if (dialog.open() == Window.OK) {
            String repository = dialog.getRepository();
            if (repository != null) {
                Pair<String,Attrs> newEntry = new Pair<>(repository, new Attrs());
                githubRepos.add(newEntry);
                vwrGithub.add(newEntry);
            }
        }
    }

    protected void doRemoveGithub() {
        int[] selectionIndices = vwrGithub.getTable().getSelectionIndices();
        if (selectionIndices == null)
            return;
        List<Object> selected = new ArrayList<>(selectionIndices.length);
        for (int index : selectionIndices) {
            selected.add(githubRepos.get(index));
        }
        githubRepos.removeAll(selected);
        vwrGithub.remove(selected.toArray(new Object[selected.size()]));
        validate();
    }

    private void validate() {}

    @Override
    public boolean performOk() {
        Parameters params = new Parameters();
        for (Pair<String,Attrs> entry : githubRepos) {
            params.add(entry.getFirst(), entry.getSecond());
        }
        prefs.setGithubRepos(params);
        return true;
    }

}
