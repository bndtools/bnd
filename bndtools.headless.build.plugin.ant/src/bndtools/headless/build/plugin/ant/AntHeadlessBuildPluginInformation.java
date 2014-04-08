package bndtools.headless.build.plugin.ant;

import org.bndtools.api.NamedPlugin;

public class AntHeadlessBuildPluginInformation implements NamedPlugin {
    private static final String NAME = "Ant";
    private static final boolean ENABLED_BY_DEFAULT = false;

    public String getName() {
        return NAME;
    }

    public boolean isEnabledByDefault() {
        return ENABLED_BY_DEFAULT;
    }
}