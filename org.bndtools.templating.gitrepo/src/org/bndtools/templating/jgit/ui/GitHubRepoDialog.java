package org.bndtools.templating.jgit.ui;

import java.net.URI;

import org.bndtools.templating.jgit.Cache;
import org.bndtools.templating.jgit.GitHub;
import org.bndtools.templating.jgit.GithubRepoDetailsDTO;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import aQute.bnd.header.Attrs;
import aQute.libg.tuple.Pair;

public class GitHubRepoDialog extends AbstractNewEntryDialog {

    private final Cache cache = new Cache();
    private final String title;

    private String repository = null;
    private Text txtRepository;

    public GitHubRepoDialog(Shell parentShell, String title) {
        super(parentShell);
        this.title = title;
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle(title);
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        container.setLayout(new GridLayout(2, false));

        Label lblRepo = new Label(container, SWT.NONE);
        lblRepo.setText("Repository Name:");

        txtRepository = new Text(container, SWT.BORDER);
        txtRepository.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        if (repository != null)
            txtRepository.setText(repository);
        txtRepository.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent ev) {
                repository = txtRepository.getText().trim();
                updateButtons();
            }
        });

        Button btnValidate = new Button(container, SWT.PUSH);
        btnValidate.setText("Validate");
        btnValidate.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1));
        btnValidate.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    if (repository == null || repository.isEmpty())
                        throw new Exception("No repository name specified");
                    GithubRepoDetailsDTO dto = new GitHub(cache).loadRepoDetails(repository);
                    URI cloneUri = URI.create(dto.clone_url);
                    setErrorMessage(null);
                    setMessage("Validated, clone URL is " + cloneUri, IMessageProvider.INFORMATION);
                } catch (Exception ex) {
                    setErrorMessage(ex.getMessage());
                }
            }
        });
        return area;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);

        Button ok = getButton(OK);
        ok.setText("Add");
        ok.setEnabled(repository != null);
    }

    private void updateButtons() {
        getButton(OK).setEnabled(repository != null && repository.trim().length() > 0);
    }

    @Override
    public void setEntry(Pair<String,Attrs> entry) {
        repository = entry.getFirst();
        if (txtRepository != null && !txtRepository.isDisposed())
            txtRepository.setText(repository);
    }

    @Override
    public Pair<String,Attrs> getEntry() {
        return repository != null ? new Pair<String,Attrs>(repository.trim(), new Attrs()) : null;
    }

}
