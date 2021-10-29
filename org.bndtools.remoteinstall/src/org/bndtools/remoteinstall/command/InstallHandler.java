package org.bndtools.remoteinstall.command;

import static org.bndtools.remoteinstall.helper.MessageDialogHelper.showMessage;
import static org.bndtools.remoteinstall.nls.Messages.Command_Execution_Job_Name;
import static org.bndtools.remoteinstall.nls.Messages.InstallBundleHandler_Dialog_MessageExecutionException;
import static org.bndtools.remoteinstall.nls.Messages.InstallBundleHandler_Dialog_TitleExecutionException;
import static org.bndtools.remoteinstall.nls.Messages.InstallBundleHandler_Message_InstallFailed;
import static org.bndtools.remoteinstall.nls.Messages.InstallerAgent_Message_InstallFailed;
import static org.eclipse.core.runtime.Status.CANCEL_STATUS;
import static org.eclipse.core.runtime.Status.OK_STATUS;
import static org.eclipse.jface.window.Window.CANCEL;

import java.io.File;
import java.nio.file.Files;
import java.time.Instant;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.remoteinstall.dto.RemoteRuntimeConfiguration;
import org.bndtools.remoteinstall.wizard.InstallBundleWizard;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler2;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import aQute.remote.api.Agent;
import aQute.remote.api.Event;
import aQute.remote.api.Supervisor;
import aQute.remote.util.AgentSupervisor;

@Component(service = IHandler2.class)
public final class InstallHandler extends AbstractHandler {
	private final ILogger	logger	= Logger.getLogger(getClass());

    @Reference
    private InstallBundleWizard wizard;

    @Override
    public Object execute(final ExecutionEvent execEvent) throws ExecutionException {
        final Shell        shell        = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        final WizardDialog dialog       = new WizardDialog(shell, wizard);
        final int          dialogResult = dialog.open();
        if (dialogResult == CANCEL) {
            return false;
        }
        final IFile                       jarFile = getJarFile();
        final RemoteRuntimeConfiguration config  = wizard.getConfiguration();
        if (jarFile == null) {
            showMessage(InstallBundleHandler_Dialog_MessageExecutionException,
                    InstallBundleHandler_Dialog_TitleExecutionException);
            return false;
        }
        execute(config.host, config.port, jarFile, config.timeout);
        return null;
    }

    private IFile getJarFile() {
        final ISelectionService service   = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService();
        final ISelection        selection = service.getSelection();

        if (selection instanceof IStructuredSelection) {
            final Object selected = ((IStructuredSelection) selection).getFirstElement();
            return Platform.getAdapterManager().getAdapter(selected, IFile.class);
        }
        return null;
    }
    
	public void execute(final String host, final int port, final IFile resource, final int timeout) {
		File file = resource.getLocation().toFile();
			final Job job = Job.create(Command_Execution_Job_Name, monitor -> {
				final SubMonitor subMonitor = SubMonitor.convert(monitor, 1);
				IMarker m= null;
				try {
					if (subMonitor.isCanceled()) {
						return CANCEL_STATUS;
					}
					m = resource.createMarker(IMarker.PROBLEM);
					m.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
					m.setAttribute(IMarker.MESSAGE, "Installing on " + host + ":" + port);
					install(host, port, file, timeout);
					m.setAttribute(IMarker.MESSAGE, "Installed on " + host + ":" + port + " at " + Instant.now());
					return OK_STATUS;
				} catch (final Exception ex) {
					logger.logError(InstallBundleHandler_Message_InstallFailed, ex);
					try {
						m.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
						m.setAttribute(IMarker.MESSAGE, "Installing on " + host + ":" + port + " " + ex.getMessage());
					} catch (CoreException e) {
						e.printStackTrace();
					}
					
					return CANCEL_STATUS;
				}
			});
			job.setUser(true);
			job.setRule(resource);
			job.schedule();
	}

    public void install(final String host, final int port, final File file, final int timeout) throws Exception {
        try (final InstallerSupervisor supervisor = new InstallerSupervisor()) {
            supervisor.connect(host, port, timeout);

            final Agent     agent     = supervisor.getAgent();
            final BundleDTO bundleDTO = agent.installWithData(null, Files.readAllBytes(file.toPath()));

            if (bundleDTO == null) {
                throw new RuntimeException(InstallerAgent_Message_InstallFailed);
            }
            agent.start(bundleDTO.id);
        }
    }

    private static class InstallerSupervisor extends AgentSupervisor<Supervisor, Agent>
            implements Supervisor, AutoCloseable {

        public void connect(final String host, final int port, final int timeout) throws Exception {
            super.connect(Agent.class, this, host, port, timeout);
        }

        @Override
        public boolean stdout(final String out) throws Exception {
            return true;
        }

        @Override
        public boolean stderr(final String out) throws Exception {
            return true;
        }

        @Override
        public void event(final Event e) throws Exception {
        }

    }


}
