package org.bndtools.remoteinstall.agent;

import static org.bndtools.remoteinstall.nls.Messages.InstallerAgent_Message_InstallFailed;

import java.io.File;
import java.nio.file.Files;

import org.osgi.framework.dto.BundleDTO;
import org.osgi.service.component.annotations.Component;

import aQute.remote.api.Agent;
import aQute.remote.api.Event;
import aQute.remote.api.Supervisor;
import aQute.remote.util.AgentSupervisor;

@Component(service = InstallerAgent.class)
public final class InstallerAgent {

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
