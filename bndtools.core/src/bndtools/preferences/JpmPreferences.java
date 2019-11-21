package bndtools.preferences;

import org.eclipse.jface.preference.IPreferenceStore;

import bndtools.Plugin;

public class JpmPreferences {

	public static final String		PREF_BROWSER_SELECTION			= "browserSelection";

	public static final int			PREF_BROWSER_WEBKIT				= 0;
	public static final int			PREF_BROWSER_MOZILLA			= 1;
	public static final int			PREF_BROWSER_EXTERNAL			= 2;
	public static final int			PREF_BROWSER_PLATFORM_DEFAULT	= 3;

	public static final String[]	PREF_BROWSER_SELECTION_CHOICES	= new String[] {
		"Internal WebKit", "Internal Mozilla (Depcreated, will be WebKit instead)", "External", "Platform Default"
	};

	private final IPreferenceStore	store;

	public JpmPreferences() {
		store = Plugin.getDefault()
			.getPreferenceStore();
		store.setDefault(PREF_BROWSER_SELECTION, PREF_BROWSER_PLATFORM_DEFAULT);
	}

	public int getBrowserSelection() {
		return store.getInt(PREF_BROWSER_SELECTION);
	}

	public void setBrowserSelection(int browserSelection) {
		store.setValue(PREF_BROWSER_SELECTION, browserSelection);
	}

}
