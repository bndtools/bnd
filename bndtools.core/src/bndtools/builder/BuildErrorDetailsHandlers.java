package bndtools.builder;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.bndtools.build.api.BuildErrorDetailsHandler;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import bndtools.Logger;
import bndtools.Plugin;

public class BuildErrorDetailsHandlers {

    public static final BuildErrorDetailsHandlers INSTANCE = new BuildErrorDetailsHandlers();

    private final ConcurrentMap<String,BuildErrorDetailsHandler> cache = new ConcurrentHashMap<String,BuildErrorDetailsHandler>();

    private BuildErrorDetailsHandlers() {}

    public BuildErrorDetailsHandler findHandler(String type) {
        if (type == null)
            return DefaultBuildErrorDetailsHandler.INSTANCE;

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
