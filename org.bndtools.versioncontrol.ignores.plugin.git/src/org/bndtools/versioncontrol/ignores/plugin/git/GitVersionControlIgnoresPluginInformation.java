package org.bndtools.versioncontrol.ignores.plugin.git;

import org.bndtools.api.NamedPlugin;

public class GitVersionControlIgnoresPluginInformation implements NamedPlugin {
    private static final String NAME = "Git";
    private static final boolean ENABLED_BY_DEFAULT = true;
    private static final boolean DEPRECATED = false;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isEnabledByDefault() {
        return ENABLED_BY_DEFAULT;
    }

    @Override
    public boolean isDeprecated() {
        return DEPRECATED;
    }
}