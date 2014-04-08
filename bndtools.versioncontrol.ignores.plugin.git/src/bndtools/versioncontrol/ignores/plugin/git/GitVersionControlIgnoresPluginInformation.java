package bndtools.versioncontrol.ignores.plugin.git;

import org.bndtools.api.NamedPlugin;

public class GitVersionControlIgnoresPluginInformation implements NamedPlugin {
    private static final String NAME = "Git";
    private static final boolean ENABLED_BY_DEFAULT = true;

    public String getName() {
        return NAME;
    }

    public boolean isEnabledByDefault() {
        return ENABLED_BY_DEFAULT;
    }
}