package bndtools;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

import aQute.bnd.build.Workspace;
import bndtools.central.Central;

public class WorkspaceServiceFactory implements ServiceFactory<Workspace> {
	private static final ILogger logger = Logger.getLogger(WorkspaceServiceFactory.class);

	@Override
	public Workspace getService(Bundle bundle, ServiceRegistration<Workspace> registration) {
		try {
			return Central.getWorkspace();
		} catch (Exception e) {
			logger.logError("Unable to initialise Bnd workspace.", e);
			throw new IllegalArgumentException("Unable to initialise Bnd workspace.", e);
		}
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<Workspace> registration, Workspace service) {}
}
