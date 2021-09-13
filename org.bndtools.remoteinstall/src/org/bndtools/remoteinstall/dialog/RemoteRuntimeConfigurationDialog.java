package org.bndtools.remoteinstall.dialog;

import static org.bndtools.remoteinstall.nls.Messages.RemoteRuntimeConfigurationDialog_Description;
import static org.bndtools.remoteinstall.nls.Messages.RemoteRuntimeConfigurationDialog_Error_InvalidHost;
import static org.bndtools.remoteinstall.nls.Messages.RemoteRuntimeConfigurationDialog_Error_InvalidName;
import static org.bndtools.remoteinstall.nls.Messages.RemoteRuntimeConfigurationDialog_Error_InvalidPort;
import static org.bndtools.remoteinstall.nls.Messages.RemoteRuntimeConfigurationDialog_Error_InvalidTimeout;
import static org.bndtools.remoteinstall.nls.Messages.RemoteRuntimeConfigurationDialog_FieldHost_Label;
import static org.bndtools.remoteinstall.nls.Messages.RemoteRuntimeConfigurationDialog_FieldHost_Message;
import static org.bndtools.remoteinstall.nls.Messages.RemoteRuntimeConfigurationDialog_FieldName_Label;
import static org.bndtools.remoteinstall.nls.Messages.RemoteRuntimeConfigurationDialog_FieldName_Message;
import static org.bndtools.remoteinstall.nls.Messages.RemoteRuntimeConfigurationDialog_FieldPort_Label;
import static org.bndtools.remoteinstall.nls.Messages.RemoteRuntimeConfigurationDialog_FieldPort_Message;
import static org.bndtools.remoteinstall.nls.Messages.RemoteRuntimeConfigurationDialog_FieldTimeout_Label;
import static org.bndtools.remoteinstall.nls.Messages.RemoteRuntimeConfigurationDialog_FieldTimeout_Message;
import static org.bndtools.remoteinstall.nls.Messages.RemoteRuntimeConfigurationDialog_Title;
import static org.eclipse.jface.dialogs.IMessageProvider.INFORMATION;

import org.bndtools.remoteinstall.dto.RemoteRuntimeConfiguration;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public final class RemoteRuntimeConfigurationDialog extends TitleAreaDialog {

    private static final String DEFAULT_PORT    = "1450";
    private static final String DEFAULT_TIMEOUT = "5000";

    private String name;
    private String host;
    private String port;
    private String timeout;

    private Text textHost;
    private Text textName;
    private Text textPort;
    private Text textTimeout;

    public RemoteRuntimeConfigurationDialog(final Shell parent) {
        super(parent);
    }

    @Override
    public void create() {
        super.create();
        setTitle(RemoteRuntimeConfigurationDialog_Title);
        setMessage(RemoteRuntimeConfigurationDialog_Description, INFORMATION);
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        final Composite area = (Composite) super.createDialogArea(parent);

        final GridLayout layout    = new GridLayout(2, false);
        final Composite  container = new Composite(area, SWT.NONE);

        container.setLayoutData(new GridData(GridData.FILL_BOTH));
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        container.setLayout(layout);

        createName(container);
        createHost(container);
        createPort(container);
        createTimeout(container);

        return area;
    }

    private void createName(final Composite container) {
        final Label labelName = new Label(container, SWT.NONE);
        labelName.setText(RemoteRuntimeConfigurationDialog_FieldName_Label);

        final GridData dataName = new GridData();

        dataName.grabExcessHorizontalSpace = true;
        dataName.horizontalAlignment       = GridData.FILL;

        textName = new Text(container, SWT.BORDER);
        textName.setLayoutData(dataName);
        textName.setMessage(RemoteRuntimeConfigurationDialog_FieldName_Message);

        if (name != null) {
            textName.setText(name);
        }
    }

    private void createHost(final Composite container) {
        final Label labelHost = new Label(container, SWT.NONE);
        labelHost.setText(RemoteRuntimeConfigurationDialog_FieldHost_Label);

        final GridData dataHost = new GridData();

        dataHost.grabExcessHorizontalSpace = true;
        dataHost.horizontalAlignment       = GridData.FILL;

        textHost = new Text(container, SWT.BORDER);
        textHost.setLayoutData(dataHost);
        textHost.setMessage(RemoteRuntimeConfigurationDialog_FieldHost_Message);

        if (host != null) {
            textHost.setText(host);
        }
    }

    private void createPort(final Composite container) {
        final Label labelPort = new Label(container, SWT.NONE);
        labelPort.setText(RemoteRuntimeConfigurationDialog_FieldPort_Label);

        final GridData dataPort = new GridData();

        dataPort.grabExcessHorizontalSpace = true;
        dataPort.horizontalAlignment       = GridData.FILL;

        textPort = new Text(container, SWT.BORDER);

        textPort.setText(DEFAULT_PORT);
        textPort.setMessage(RemoteRuntimeConfigurationDialog_FieldPort_Message);
        textPort.setLayoutData(dataPort);
        textPort.addVerifyListener(e -> {
            final String currentText = ((Text) e.widget).getText();
            final String portValue   = currentText.substring(0, e.start) + e.text + currentText.substring(e.end);
            try {
                final int portNum = Integer.parseInt(portValue);
                if (portNum < 0 || portNum > 65535) {
                    e.doit = false;
                }
            } catch (final NumberFormatException ex) {
                if (!"".equals(portValue)) {
                    e.doit = false;
                }
            }
        });
        if (port != null) {
            textPort.setText(port);
        }
    }

    private void createTimeout(final Composite container) {
        final Label labelTimeout = new Label(container, SWT.NONE);
        labelTimeout.setText(RemoteRuntimeConfigurationDialog_FieldTimeout_Label);

        final GridData dataTimeout = new GridData();

        dataTimeout.grabExcessHorizontalSpace = true;
        dataTimeout.horizontalAlignment       = GridData.FILL;

        textTimeout = new Text(container, SWT.BORDER);

        textTimeout.setText(DEFAULT_TIMEOUT);
        textTimeout.setMessage(RemoteRuntimeConfigurationDialog_FieldTimeout_Message);
        textTimeout.setLayoutData(dataTimeout);
        textTimeout.addVerifyListener(e -> {
            final String currentText = ((Text) e.widget).getText();
            final String timeout     = currentText.substring(0, e.start) + e.text + currentText.substring(e.end);
            try {
                final int timeoutNum = Integer.parseInt(timeout);
                if (timeoutNum < 0 || timeoutNum > 600000) {
                    e.doit = false;
                }
            } catch (final NumberFormatException ex) {
                if (!"".equals(timeout)) {
                    e.doit = false;
                }
            }
        });
        if (timeout != null) {
            textTimeout.setText(timeout);
        }
    }

    public RemoteRuntimeConfiguration getConfiguration() {
        final RemoteRuntimeConfiguration configuration = new RemoteRuntimeConfiguration();

        configuration.name    = name;
        configuration.host    = host;
        configuration.port    = Integer.parseInt(port);
        configuration.timeout = Integer.parseInt(timeout);

        return configuration;
    }

    @Override
    protected boolean isResizable() {
        return false;
    }

    @Override
    protected void okPressed() {
        if (!checkInput(textName.getText())) {
            setErrorMessage(RemoteRuntimeConfigurationDialog_Error_InvalidName);
        } else if (!checkInput(textHost.getText())) {
            setErrorMessage(RemoteRuntimeConfigurationDialog_Error_InvalidHost);
        } else if (!checkInput(textPort.getText())) {
            setErrorMessage(RemoteRuntimeConfigurationDialog_Error_InvalidPort);
        } else if (!checkInput(textTimeout.getText())) {
            setErrorMessage(RemoteRuntimeConfigurationDialog_Error_InvalidTimeout);
        } else {
            saveInput();
            super.okPressed();
        }
    }

    private void saveInput() {
        name    = textName.getText();
        host    = textHost.getText();
        port    = textPort.getText();
        timeout = textTimeout.getText();
    }

    public void setConfiguration(final RemoteRuntimeConfiguration configuration) {
        name    = configuration.name;
        host    = configuration.host;
        port    = String.valueOf(configuration.port);
        timeout = String.valueOf(configuration.timeout);
    }

    private boolean checkInput(final String input) {
        return input != null && !input.trim().isEmpty();
    }

}