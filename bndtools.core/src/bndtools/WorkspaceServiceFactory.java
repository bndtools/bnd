package bndtools;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

import bndtools.api.ILogger;

public class WorkspaceServiceFactory implements ServiceFactory {
    private static final ILogger logger = Logger.getLogger();

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
