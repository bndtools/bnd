package aQute.launcher;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

class LauncherSystemProperties extends Properties {
	private static final long			serialVersionUID	= 1L;
	private final Map<Object, Object>	userSet				= new HashMap<>();

	LauncherSystemProperties(Properties... properties) {
		for (Properties p : properties) {
			for (Map.Entry<Object, Object> e : p.entrySet()) {
				super.put(e.getKey(), e.getValue());
			}
		}
		Properties sysProps = System.getProperties();
		if (sysProps instanceof LauncherSystemProperties) {
			synchronized (sysProps) {
				for (Map.Entry<Object, Object> e : ((LauncherSystemProperties) sysProps).userSet.entrySet()) {
					Object key = e.getKey();
					Object value = e.getValue();
					if (value != null) {
						put(key, value);
					} else {
						remove(key);
					}
				}
			}
		}
	}

	@Override
	public synchronized Object put(Object key, Object value) {
		Object existing = super.put(key, value);
		userSet.put(key, value);
		return existing;
	}

	@Override
	public synchronized Object remove(Object key) {
		Object existing = super.remove(key);
		if (existing != null) {
			userSet.put(key, null);
		}
		return existing;
	}

	@Override
	public synchronized void clear() {
		super.clear();
		userSet.clear();
	}
}
