package bndtools.preferences;

import org.eclipse.core.internal.resources.PreferenceInitializer;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import bndtools.Plugin;

public class OBRPreferenceInitializer extends PreferenceInitializer {
    @Override
    public void initializeDefaultPreferences() {
        IEclipsePreferences prefs = new InstanceScope().getNode(Plugin.PLUGIN_ID);
    }
}
