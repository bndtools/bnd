package bndtools.wizards.project;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
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
import org.eclipse.swt.widgets.Text;

import bndtools.Plugin;

public class ProjectNameGroup {

    private static final String PROP_PROJECT_NAME = "projectName";
    private static final String PROP_PACKAGE_NAME = "packageName";
    private static final String PROP_STATUS = "status";

    private final PropertyChangeSupport propSupport = new PropertyChangeSupport(this);

    private String projectName = "";
    private String packageName = "";
    private IStatus status = Status.OK_STATUS;

    private Text txtProjectName;
    private Text txtBasePackageName;
    private Button btnInferBasePackageName;

    /**
     * @wbp.parser.entryPoint
     */
    public Control createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(2, false));

        Label lblProjectName = new Label(composite, SWT.NONE);
        lblProjectName.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        lblProjectName.setText("Project Name:");

        txtProjectName = new Text(composite, SWT.BORDER);
        txtProjectName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        txtProjectName.setText(projectName);
        txtProjectName.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                String old = projectName;
                projectName = txtProjectName.getText().trim();
                propSupport.firePropertyChange(PROP_PROJECT_NAME, old, projectName);

                if (btnInferBasePackageName.getSelection()) {
                    txtBasePackageName.setText(toLegalPackageName(projectName));
                }
            }
        });

        Label lblBasePackageName = new Label(composite, SWT.NONE);
        lblBasePackageName.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        lblBasePackageName.setText("Java Package:");

        txtBasePackageName = new Text(composite, SWT.BORDER);
        txtBasePackageName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        txtBasePackageName.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent ev) {
                String old = packageName;
                packageName = txtBasePackageName.getText().trim();
                propSupport.firePropertyChange(PROP_PACKAGE_NAME, old, packageName);
                updateStatus();
            }
        });

        @SuppressWarnings("unused")
        Label lblSpacer1 = new Label(composite, SWT.NONE);

        btnInferBasePackageName = new Button(composite, SWT.CHECK);
        btnInferBasePackageName.setText("Derive from project name");
        btnInferBasePackageName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        btnInferBasePackageName.setSelection(true);
        txtBasePackageName.setEnabled(false);
        btnInferBasePackageName.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean infer = btnInferBasePackageName.getSelection();
                txtBasePackageName.setEnabled(!infer);
                if (infer) {
                    txtBasePackageName.setText(toLegalPackageName(projectName));
                } else {
                    txtBasePackageName.setFocus();
                }
            }
        });

        return composite;
    }

    private void updateStatus() {
        String error = validatePackageName(packageName);

        IStatus oldStatus = status;
        status = error != null ? new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, error, null) : Status.OK_STATUS;
        if (oldStatus != status)
            propSupport.firePropertyChange(PROP_STATUS, oldStatus, status);
    }

    /**
     * Validate a Java package name.
     *
     * @return null if the package name is legal; an error message if illegal
     */
    static String validatePackageName(String packageName) {
        if (packageName.isEmpty())
            return "Package name may not be empty";

        String[] parts = packageName.split("\\.", -1);
        for (String part : parts) {
            if (part.isEmpty())
                return "Package name part may not be empty";

            char firstChar = part.charAt(0);
            if (!Character.isJavaIdentifierStart(firstChar))
                return "Illegal character at start of package name part: " + firstChar;

            for (int i = 1; i < part.length(); i++) {
                char ch = part.charAt(i);
                if (!Character.isJavaIdentifierPart(ch))
                    return "Illegal character in package name part: " + ch;
            }
        }

        // Orl Korrect
        return null;
    }

    /**
     * Convert a string to a legal package name
     */
    static String toLegalPackageName(String input) throws IllegalArgumentException {
        if (input.isEmpty())
            return "";

        String[] parts = input.split("\\.", -1);
        List<String> newParts = new ArrayList<>(parts.length);

        for (String part : parts) {
            StringBuilder builder = new StringBuilder();

            if (!part.isEmpty()) {
                char firstChar = part.charAt(0);
                if (Character.isJavaIdentifierStart(firstChar))
                    builder.append(firstChar);
                else if (Character.isJavaIdentifierPart(firstChar))
                    builder.append('_').append(firstChar);
                else if ('-' == firstChar)
                    builder.append('-');

                for (int i = 1; i < part.length(); i++) {
                    char ch = part.charAt(i);
                    if (Character.isJavaIdentifierPart(ch)) {
                        builder.append(ch);
                    } else if ('-' == ch) {
                        builder.append('_');
                    }
                }
            }

            String newPart = builder.toString();
            if (!newPart.isEmpty())
                newParts.add(newPart);
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < newParts.size(); i++) {
            if (i > 0)
                builder.append('.');
            builder.append(newParts.get(i));
        }
        return builder.toString();
    }

    public String getProjectName() {
        return projectName;
    }

    public String getPackageName() {
        return packageName;
    }

    public IStatus getStatus() {
        return status;
    }

    public void addPropertyChangeListener(PropertyChangeListener var0) {
        propSupport.addPropertyChangeListener(var0);
    }

    public void removePropertyChangeListener(PropertyChangeListener var0) {
        propSupport.removePropertyChangeListener(var0);
    }

}
