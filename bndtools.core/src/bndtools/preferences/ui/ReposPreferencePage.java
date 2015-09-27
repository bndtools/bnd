package bndtools.preferences.ui;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.preference.PreferencePage;
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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import bndtools.preferences.BndPreferences;

public class ReposPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    private boolean enableTemplateRepo;
    private String repoUri;

    @Override
    public void init(IWorkbench workbench) {
        BndPreferences prefs = new BndPreferences();

        enableTemplateRepo = prefs.getEnableTemplateRepo();
        repoUri = prefs.getTemplateRepoUri();
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginRight = 10;
        composite.setLayout(layout);

        Group group = new Group(composite, SWT.NONE);
        group.setText("Templates Repository");
        group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        group.setLayout(new GridLayout(2, false));

        final Button btnEnableTemplateRepo = new Button(group, SWT.CHECK);
        btnEnableTemplateRepo.setText("Enable templates repository");
        btnEnableTemplateRepo.setSelection(enableTemplateRepo);
        btnEnableTemplateRepo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));

        ControlDecoration decoration = new ControlDecoration(btnEnableTemplateRepo, SWT.RIGHT | SWT.TOP, composite);
        decoration.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION).getImage());
        decoration.setMarginWidth(3);
        decoration.setDescriptionText("This repository is used to load\ntemplates, in addition to repositories\nconfigured in the bnd workspace.");
        decoration.setShowHover(true);
        decoration.setShowOnlyOnFocus(false);

        new Label(group, SWT.NONE).setText("Repo URL:");
        final Text txtTemplateRepo = new Text(group, SWT.BORDER);
        txtTemplateRepo.setText(repoUri != null ? repoUri : "");
        GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gd.widthHint = 200;
        txtTemplateRepo.setLayoutData(gd);
        txtTemplateRepo.setEnabled(enableTemplateRepo);

        btnEnableTemplateRepo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                enableTemplateRepo = btnEnableTemplateRepo.getSelection();
                txtTemplateRepo.setEnabled(enableTemplateRepo);
                validate();
            }
        });
        txtTemplateRepo.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent ev) {
                repoUri = txtTemplateRepo.getText().trim();
                validate();
            }
        });

        return composite;
    }

    private void validate() {
        String error = null;
        if (enableTemplateRepo) {
            try {
                @SuppressWarnings("unused")
                URI uri = new URI(repoUri);
            } catch (URISyntaxException e) {
                error = "Invalid URL: " + e.getMessage();
            }
        }
        setErrorMessage(error);
        setValid(error == null);
    }

    @Override
    public boolean performOk() {
        BndPreferences prefs = new BndPreferences();

        prefs.setEnableTemplateRepo(enableTemplateRepo);
        prefs.setTemplateRepoUri(repoUri);

        return true;
    }

}
