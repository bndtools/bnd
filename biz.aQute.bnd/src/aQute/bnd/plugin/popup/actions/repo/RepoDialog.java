package aQute.bnd.plugin.popup.actions.repo;

import java.util.*;

import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

import aQute.bnd.service.*;
import aQute.lib.osgi.*;

public class RepoDialog extends Dialog {
    private Text                             bsn;
    private Text                             version;

    private Label                            lblRepository;
    private Combo                            repositories;
    private java.util.List<RepositoryPlugin> repos;
    private Jar                              jar;
    private Label                            lblBundleDescription;
    private Text                             description;
    private RepositoryPlugin                 selected;

    /**
     * Create the dialog.
     * 
     * @param parentShell
     */
    public RepoDialog(Shell parentShell, Jar jar,
            java.util.List<RepositoryPlugin> repos) {
        super(parentShell);
        setShellStyle(SWT.DIALOG_TRIM);
        this.repos = repos;
        this.jar = jar;
    }

    /**
     * Create contents of the dialog.
     * 
     * @param parent
     */
    @Override
    protected Control createDialogArea(Composite parent) {

        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(null);
        {
            lblRepository = new Label(container, SWT.NONE);
            lblRepository.setBounds(8, 8, 160, 24);
            lblRepository.setText("Repository");
        }
        {
            repositories = new Combo(container, SWT.READ_ONLY);
            repositories.setBounds(168, 3, 272, 30);
            repositories.addSelectionListener(new SelectionAdapter() {

                public void widgetSelected(SelectionEvent e) {
                    selected = repos.get(repositories.getSelectionIndex());
                }

            });
        }
        {
            bsn = new Text(container, SWT.BORDER);
            bsn.setEditable(false);
            bsn.setBounds(168, 39, 272, 28);
        }
        {
            version = new Text(container, SWT.BORDER);
            version.setEditable(false);
            version.setBounds(168, 67, 272, 28);
        }
        {
            Label lblBundleSymbolicName = new Label(container, SWT.NONE);
            lblBundleSymbolicName.setBounds(8, 40, 160, 24);
            lblBundleSymbolicName.setText("Bundle Symbolic Name");
        }
        {
            Label lblVersion = new Label(container, SWT.NONE);
            lblVersion.setBounds(8, 72, 160, 24);
            lblVersion.setText("Bundle Version");
        }
        {
            lblBundleDescription = new Label(container, SWT.NONE);
            lblBundleDescription.setBounds(8, 105, 160, 24);
            lblBundleDescription.setText("Bundle Description");
        }
        {
            description = new Text(container, SWT.BORDER | SWT.V_SCROLL
                    | SWT.MULTI);
            description.setEditable(false);
            description.setBounds(168, 101, 272, 117);
        }

        setup();
        return container;
    }

    private void setup() {
        try {
            bsn.setText(jar.getManifest().getMainAttributes().getValue(
                    Constants.BUNDLE_SYMBOLICNAME));
            String v = jar.getManifest().getMainAttributes().getValue(
                    Constants.BUNDLE_VERSION);
            if (v == null)
                v = "0.0.0";
            version.setText(v);

            v = jar.getManifest().getMainAttributes().getValue(
                    Constants.BUNDLE_DESCRIPTION);
            if (v == null)
                v = "";
            description.setText(v);

            for (Iterator<RepositoryPlugin> i = repos.iterator(); i.hasNext();) {
                RepositoryPlugin plugin = i.next();
                if (plugin.canWrite())
                    repositories.add(plugin.getName());
                else
                    i.remove();
            }
            repositories.select(0);
            selected = repos.get(0);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Create contents of the button bar.
     * 
     * @param parent
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        Button button = createButton(parent, IDialogConstants.OK_ID,
                IDialogConstants.OK_LABEL, true);
        button.setText("Deploy");
        createButton(parent, IDialogConstants.CANCEL_ID,
                IDialogConstants.CANCEL_LABEL, false);
    }

    /**
     * Return the initial size of the dialog.
     */
    @Override
    protected Point getInitialSize() {
        return new Point(450, 300);
    }

    public RepositoryPlugin getRepository() {
        return selected;
    }
}
