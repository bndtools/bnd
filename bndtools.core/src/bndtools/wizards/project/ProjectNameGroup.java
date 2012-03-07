package bndtools.wizards.project;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class ProjectNameGroup {

    private static final String PROP_PROJECT_NAME = "projectName";
    private final PropertyChangeSupport propSupport = new PropertyChangeSupport(this);

    private String projectName = "";

    private Text txtProjectName;

    private AtomicBoolean programmaticChange = new AtomicBoolean(false);

    /**
     * @wbp.parser.entryPoint
     */
    public Control createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(2, false));

        Label lblProjectName = new Label(composite, SWT.NONE);
        lblProjectName.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblProjectName.setText("Project name:");

        txtProjectName = new Text(composite, SWT.BORDER);
        txtProjectName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        txtProjectName.setText(projectName);
        txtProjectName.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                if (programmaticChange.compareAndSet(false, true)) {
                    try {
                        String old = projectName;
                        projectName = txtProjectName.getText().trim();
                        propSupport.firePropertyChange(PROP_PROJECT_NAME, old, projectName);
                    } finally {
                        programmaticChange.set(false);
                    }
                }
            }
        });

        return composite;
    }

    public String getProjectName() {
        return projectName;
    }

    private void updateUI() {
        if (txtProjectName != null && !txtProjectName.isDisposed())
            txtProjectName.setText(projectName);
    }

    public void addPropertyChangeListener(PropertyChangeListener var0) {
        propSupport.addPropertyChangeListener(var0);
    }

    public void removePropertyChangeListener(PropertyChangeListener var0) {
        propSupport.removePropertyChangeListener(var0);
    }


}
