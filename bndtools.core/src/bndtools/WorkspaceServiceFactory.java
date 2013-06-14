package bndtools;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

import bndtools.central.Central;

public class WorkspaceServiceFactory implements ServiceFactory {
    private static final ILogger logger = Logger.getLogger(WorkspaceServiceFactory.class);

    public Object getService(Bundle bundle, ServiceRegistration registration) {
        try {
            return Central.getWorkspace();
        } catch (Exception e) {
            logger.logError("Unable to initialise bnd workspace.", e);
            throw new IllegalArgumentException("Unable to initialise bnd workspace.", e);
        }
    }

    public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {}

}
