package org.bndtools.templating.jgit.ui;

import java.net.URISyntaxException;

import org.eclipse.jgit.transport.URIish;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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

public class GitCloneURLDialog extends AbstractNewEntryDialog {

    private final String title;

    private URIish cloneUri = null;
    private Text txtRepository;

    public GitCloneURLDialog(Shell parentShell, String title) {
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
        lblRepo.setText("Git Clone URL:");

        txtRepository = new Text(container, SWT.BORDER);
        txtRepository.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        txtRepository.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent ev) {
                updateFromInput();
            }
        });
        if (cloneUri != null)
            txtRepository.setText(cloneUri.toString());

        return area;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);

        Button ok = getButton(OK);
        ok.setText("Add");
        ok.setEnabled(cloneUri != null);
    }

    private void updateFromInput() {
        cloneUri = null;
        try {
            cloneUri = new URIish(txtRepository.getText().trim());
            setErrorMessage(null);
            getButton(OK).setEnabled(cloneUri != null);
        } catch (URISyntaxException e) {
            setErrorMessage(e.getMessage());
        }
    }

    @Override
    public Pair<String,Attrs> getEntry() {
        return cloneUri != null ? new Pair<>(cloneUri.toString(), new Attrs()) : null;
    }

}
