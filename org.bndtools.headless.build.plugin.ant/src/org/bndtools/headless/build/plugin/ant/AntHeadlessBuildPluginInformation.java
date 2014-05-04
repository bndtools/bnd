package org.bndtools.headless.build.plugin.ant;

import org.bndtools.api.NamedPlugin;

public class AntHeadlessBuildPluginInformation implements NamedPlugin {
    private static final String NAME = "Ant";
    private static final boolean ENABLED_BY_DEFAULT = false;
    private static final boolean DEPRECATED = true;

    public String getName() {
        return NAME;
    }

    public boolean isEnabledByDefault() {
        return ENABLED_BY_DEFAULT;
    }

    public boolean isDeprecated() {
        return DEPRECATED;
    }
}