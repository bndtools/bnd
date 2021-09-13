package org.bndtools.remoteinstall.command;

import static org.bndtools.remoteinstall.helper.MessageDialogHelper.showError;
import static org.bndtools.remoteinstall.helper.MessageDialogHelper.showMessage;
import static org.bndtools.remoteinstall.nls.Messages.Command_Execution_Job_Name;
import static org.bndtools.remoteinstall.nls.Messages.InstallBundleHandler_Dialog_Title;
import static org.bndtools.remoteinstall.nls.Messages.InstallBundleHandler_Message_InstallFailed;
import static org.bndtools.remoteinstall.nls.Messages.InstallBundleHandler_Message_InstallSuccess;
import static org.bndtools.remoteinstall.nls.Messages.InstallBundleHandler_Message_ScheduledSuccess;
import static org.eclipse.core.runtime.Status.CANCEL_STATUS;
import static org.eclipse.core.runtime.Status.OK_STATUS;

import java.io.File;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.remoteinstall.agent.InstallerAgent;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = InstallCommand.class)
public final class InstallCommand {

    private final ILogger logger = Logger.getLogger(getClass());

    @Reference
    private InstallerAgent agent;

    public void execute(final String host, final int port, final File file, final int timeout) {
        final Job job = Job.create(Command_Execution_Job_Name, monitor -> {
            final SubMonitor subMonitor = SubMonitor.convert(monitor, 1);
            try {
                if (subMonitor.isCanceled()) {
                    return CANCEL_STATUS;
                }
                agent.install(host, port, file, timeout);
                return OK_STATUS;
            } catch (final Exception ex) {
                logger.logError(InstallBundleHandler_Message_InstallFailed, ex);
                Display.getDefault().asyncExec(() -> showError(InstallBundleHandler_Message_InstallFailed,
                        InstallBundleHandler_Dialog_Title, ex));
                return CANCEL_STATUS;
            }
        });
        job.addJobChangeListener(new JobChangeAdapter() {

            @Override
            public void scheduled(final IJobChangeEvent event) {
                logger.logInfo(InstallBundleHandler_Message_ScheduledSuccess, null);
                showMessage(InstallBundleHandler_Message_ScheduledSuccess, InstallBundleHandler_Dialog_Title);
            }

            @Override
            public void done(final IJobChangeEvent event) {
                if (event.getResult().isOK()) {
                    logger.logInfo(InstallBundleHandler_Message_InstallSuccess, null);
                    Display.getDefault().asyncExec(() -> showMessage(InstallBundleHandler_Message_InstallSuccess,
                            InstallBundleHandler_Dialog_Title));
                }
            }
        });
        job.setUser(true);
        job.schedule();
    }

}
