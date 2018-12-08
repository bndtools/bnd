package org.bndtools.builder;

import org.bndtools.build.api.IProjectDecorator;
import org.bndtools.builder.decorator.ui.ProjectDecoratorImpl;
import org.osgi.framework.BundleContext;

public class BuilderPlugin extends org.eclipse.core.runtime.Plugin {

    private static BuilderPlugin instance = null;

    public static BuilderPlugin getInstance() {
        synchronized (BuilderPlugin.class) {
            return instance;
        }
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        synchronized (BuilderPlugin.class) {
            instance = this;
        }
        context.registerService(IProjectDecorator.class, new ProjectDecoratorImpl(), null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        synchronized (BuilderPlugin.class) {
            instance = null;
        }
        super.stop(context);
    }

}
