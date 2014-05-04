package org.bndtools.headless.build.plugin.gradle;

import org.bndtools.api.NamedPlugin;

public class GradleHeadlessBuildPluginInformation implements NamedPlugin {
    private static final String NAME = "Gradle";
    private static final boolean ENABLED_BY_DEFAULT = true;
    private static final boolean DEPRECATED = false;

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