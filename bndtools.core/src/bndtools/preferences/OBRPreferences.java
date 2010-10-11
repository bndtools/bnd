package bndtools.preferences;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.internal.resources.PreferenceInitializer;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;

import aQute.libg.header.OSGiHeader;
import bndtools.Plugin;
import bndtools.model.clauses.HeaderClause;
import bndtools.shared.OBRLink;

public class OBRPreferences extends PreferenceInitializer {

    private static final String PREF_OBR_URLS = "obrURLs";
    private static final String PREF_OBR_HIDE_BUILTIN = "hideBuiltInOBRs";

    public static final boolean isHideBuiltIn(IPreferenceStore prefs) {
        return prefs.getBoolean(PREF_OBR_HIDE_BUILTIN);
    }

    public static final void setHideBuiltIn(boolean hide, IPreferenceStore prefs) {
        prefs.setValue(PREF_OBR_HIDE_BUILTIN, hide);
    }

    public static final void loadBuiltInRepositories(Collection<? super OBRLink> links) {
        IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(Plugin.PLUGIN_ID, "obr");
        for (int i = 0; i < elements.length; i++) {
            links.add(new BuiltInOBRLink(elements[i]));
        }
    }

    public static final void loadConfiguredRepositories(Collection<? super OBRLink> result, IPreferenceStore prefs) {
        String urlListString = prefs.getString(PREF_OBR_URLS);
        if (urlListString == null) return;

        Map<String, Map<String, String>> header = OSGiHeader.parseHeader(urlListString);
        for (Entry<String, ?> entry : header.entrySet()) {
            result.add(new ConfiguredOBRLink(entry.getKey()));
        }
    }

    /**
     * Store a list of repositories into the preferences object; does NOT attempt to persist the preferences.
     * @param urls
     * @param prefs
     */
    public static final void storeConfiguredRepositories(Collection<? extends OBRLink> urls, IPreferenceStore prefs) {
        StringBuilder builder = new StringBuilder();

        for (Iterator<? extends OBRLink> iter = urls.iterator(); iter.hasNext(); ) {
            HeaderClause clause = new HeaderClause(iter.next().getLink(), null);
            clause.formatTo(builder);

            if (iter.hasNext()) builder.append(',');
        }

        prefs.setValue(PREF_OBR_URLS, builder.toString());
    }


    @Override
    public void initializeDefaultPreferences() {
        /*
        IPreferenceStore prefs = Plugin.getDefault().getPreferenceStore();

        List<OBRLink> urls = new ArrayList<OBRLink>();
        loadConfiguredRepositories(urls, prefs);

        if (urls.isEmpty()) {
            Bundle bundle = Plugin.getDefault().getBundleContext().getBundle();
            @SuppressWarnings("unchecked")
            Enumeration<String> paths = bundle.getEntryPaths(PATH_OBR_DIR);
            while (paths.hasMoreElements()) {
                String path = paths.nextElement();
                if (!path.endsWith("/")) {
                    URL url = bundle.getEntry(path);
                    urls.add(url.toExternalForm());
                }
            }
            storeRepoURLs(urls, prefs);

            if (prefs instanceof IPersistentPreferenceStore) {
                try {
                    ((IPersistentPreferenceStore) prefs).save();
                } catch (IOException e) {
                    Plugin.logError("Error saving initial OBR preferences.", e);
                }
            }
        }
        */
    }
}
