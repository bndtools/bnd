package org.bndtools.remoteinstall.nls;

import org.eclipse.osgi.util.NLS;

public final class Messages extends NLS {

    private static final String BASE_NAME = "org.bndtools.remoteinstall.nls.messages";

    public static String Command_Execution_Job_Name;

    public static String InstallBundleHandler_Dialog_MessageExecutionException;
    public static String InstallBundleHandler_Dialog_Title;
    public static String InstallBundleHandler_Dialog_TitleExecutionException;
    public static String InstallBundleHandler_Message_InstallFailed;
    public static String InstallBundleHandler_Message_InstallSuccess;
    public static String InstallBundleHandler_Message_ScheduledSuccess;

    public static String InstallBundleWizard_Error_NoConfigSelected;
    public static String InstallBundleWizard_Title;

    public static String InstallBundleWizardPage_ButtonAdd_Title;
    public static String InstallBundleWizardPage_ButtonEdit_Title;
    public static String InstallBundleWizardPage_ButtonRemove_Title;
    public static String InstallBundleWizardPage_Description;
    public static String InstallBundleWizardPage_Dialog_Title;
    public static String InstallBundleWizardPage_Error_NoConfigSelected;
    public static String InstallBundleWizardPage_Name;

    public static String InstallerAgent_Message_InstallFailed;

    public static String RemoteRuntimeConfigurationDialog_Description;
    public static String RemoteRuntimeConfigurationDialog_Error_InvalidHost;
    public static String RemoteRuntimeConfigurationDialog_Error_InvalidName;
    public static String RemoteRuntimeConfigurationDialog_Error_InvalidPort;
    public static String RemoteRuntimeConfigurationDialog_Error_InvalidTimeout;
    public static String RemoteRuntimeConfigurationDialog_FieldHost_Label;
    public static String RemoteRuntimeConfigurationDialog_FieldHost_Message;
    public static String RemoteRuntimeConfigurationDialog_FieldName_Label;
    public static String RemoteRuntimeConfigurationDialog_FieldName_Message;
    public static String RemoteRuntimeConfigurationDialog_FieldPort_Label;
    public static String RemoteRuntimeConfigurationDialog_FieldPort_Message;
    public static String RemoteRuntimeConfigurationDialog_FieldTimeout_Label;
    public static String RemoteRuntimeConfigurationDialog_FieldTimeout_Message;
    public static String RemoteRuntimeConfigurationDialog_Title;

    public static String TableColumn_Host;
    public static String TableColumn_Name;
    public static String TableColumn_Port;
    public static String TableColumn_Timeout;

    static {
        NLS.initializeMessages(BASE_NAME, Messages.class);
    }

    private Messages() {
        throw new IllegalAccessError("Cannot be instantiated");
    }
}
