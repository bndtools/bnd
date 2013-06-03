package bndtools.builder;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.bndtools.build.api.BuildErrorDetailsHandler;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import aQute.service.reporter.Report.Location;
import bndtools.Logger;
import bndtools.Plugin;

class BuildErrorDetailsHandlers {

    private final ConcurrentMap<String,BuildErrorDetailsHandler> cache = new ConcurrentHashMap<String,BuildErrorDetailsHandler>();

    BuildErrorDetailsHandler findHandler(Location location) {
        if (location == null || location.details == null)
            return DefaultBuildErrorDetailsHandler.INSTANCE;

        String type = location.details.getClass().getName();
        return findHandler(type);
    }

    BuildErrorDetailsHandler findHandler(String type) {
        BuildErrorDetailsHandler handler = cache.get(type);
        if (handler != null)
            return handler;

        handler = DefaultBuildErrorDetailsHandler.INSTANCE;
        IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(Plugin.PLUGIN_ID, "buildErrorDetailsHandlers");
        if (elements != null) {
            for (IConfigurationElement element : elements) {
                if (type.equals(element.getAttribute("typeMatch"))) {
                    try {
                        handler = (BuildErrorDetailsHandler) element.createExecutableExtension("class");
                        break;
                    } catch (Exception e) {
                        Logger.getLogger().logError("Error instantiating build error handler for type " + type, e);
                    }
                }
            }
        }

        BuildErrorDetailsHandler mapped = cache.putIfAbsent(type, handler);
        if (mapped != null)
            return mapped;
        return handler;
    }

}
