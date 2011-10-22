package bndtools.preferences.obr;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;

import aQute.bnd.service.OBRIndexProvider;
import bndtools.Central;
import bndtools.Plugin;
import bndtools.editor.model.conversions.CollectionFormatter;
import bndtools.editor.model.conversions.SimpleListConverter;
import bndtools.types.Pair;

public class ObrPreferences extends AbstractPreferenceInitializer {

    private static final String PREF_EXCLUDED_REPOS = "excludedRepos";

    public static final Pair<List<OBRIndexProvider>, Set<String>> loadAvailableReposAndExclusions() throws Exception {
        // Get all repos...
        List<OBRIndexProvider> plugins = Central.getWorkspace().getPlugins(OBRIndexProvider.class);

        // Load exclusions from prefs
        IPreferenceStore prefs = Plugin.getDefault().getPreferenceStore();
        List<String> list = SimpleListConverter.create().convert(prefs.getString(PREF_EXCLUDED_REPOS));
        Set<String> excludes = new HashSet<String>(list);

        return Pair.newInstance(plugins, excludes);
    }

    public static final void saveExclusions(Collection<? extends String> excludes) throws IOException {
        IPreferenceStore prefs = Plugin.getDefault().getPreferenceStore();
        String formatted = new CollectionFormatter<String>(",").convert(excludes);
        prefs.setValue(PREF_EXCLUDED_REPOS, formatted);

        if (prefs instanceof IPersistentPreferenceStore) {
            ((IPersistentPreferenceStore) prefs).save();
        }
    }

    @Override
    public void initializeDefaultPreferences() {
    }

}
