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

    private String cloneUri = null;
    private String name = null;
    private Text txtRepository;

    private Text txtName;

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
        lblRepo.setText("Clone URL:");

        txtRepository = new Text(container, SWT.BORDER);
        txtRepository.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        if (cloneUri != null)
            txtRepository.setText(cloneUri.toString());

        Label lblName = new Label(container, SWT.NONE);
        lblName.setText("Name:");
        txtName = new Text(container, SWT.BORDER);
        txtName.setMessage("optional");
        txtName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        if (name != null)
            txtName.setText(name);

        ModifyListener modifyListener = new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent ev) {
                updateFromInput();
                updateButtons();
            }
        };
        txtRepository.addModifyListener(modifyListener);
        txtName.addModifyListener(modifyListener);

        return area;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);

        Button ok = getButton(OK);
        ok.setText("Save");
        ok.setEnabled(cloneUri != null);
    }

    private void updateFromInput() {
        try {
            cloneUri = txtRepository.getText().trim();
            name = txtName.getText().trim();
            setErrorMessage(null);

            @SuppressWarnings("unused") // for validation
            URIish uriish = new URIish(cloneUri);
        } catch (URISyntaxException e) {
            setErrorMessage(e.getMessage());
        }
    }

    private void updateButtons() {
        getButton(OK).setEnabled(cloneUri != null);
    }

    @Override
    public void setEntry(Pair<String,Attrs> entry) {
        cloneUri = entry.getFirst();
        name = entry.getSecond().get("name");

        if (txtRepository != null && !txtRepository.isDisposed())
            txtRepository.setText(cloneUri);
        if (txtName != null && !txtName.isDisposed())
            txtName.setText(name);
    }

    @Override
    public Pair<String,Attrs> getEntry() {
        Attrs attrs = new Attrs();
        if (name != null && !name.isEmpty())
            attrs.put("name", name);
        return cloneUri != null ? new Pair<>(cloneUri.toString(), attrs) : null;
    }

}
