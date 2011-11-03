package bndtools;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

public class WorkspaceServiceFactory implements ServiceFactory {

    public Object getService(Bundle bundle, ServiceRegistration registration) {
        try {
            return Central.getWorkspace();
        } catch (Exception e) {
            Plugin.logError("Unable to initialise bnd workspace.", e);
            throw new IllegalArgumentException("Unable to initialise bnd workspace.", e);
        }
    }

    public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
    }

}
