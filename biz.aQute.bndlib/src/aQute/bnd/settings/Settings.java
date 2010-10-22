package aQute.bnd.settings;

import java.util.*;
import java.util.prefs.*;

public class Settings {
	public final static String EMAIL = "email";
	public final static String NAME = "name";
	
	
	static Preferences	prefs	= Preferences.userNodeForPackage(Settings.class);

	public String globalGet(String key, String def) {
		return prefs.get(key, def);
	}
	public void globalSet(String key, String value) throws BackingStoreException {
		 prefs.put(key, value);
		 prefs.sync();
	}
	
	public Collection<String> getKeys() throws BackingStoreException {
		return Arrays.asList(prefs.keys());
	}
	
	public void globalRemove(String key) throws BackingStoreException {
		prefs.remove(key);
		 prefs.sync();
	}
}
