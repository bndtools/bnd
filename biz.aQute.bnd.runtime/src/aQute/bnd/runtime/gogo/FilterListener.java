package aQute.bnd.runtime.gogo;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.ListenerHook;

import aQute.lib.collections.MultiMap;

class FilterListener implements ListenerHook {
	private final static Pattern				LISTENER_INFO_PATTERN	= Pattern.compile("\\(objectClass=([^)]+)\\)");
	final MultiMap<String, BundleContext>		listenerContexts		= new MultiMap<>();

	volatile boolean							quiting;
	private ServiceRegistration<ListenerHook>	lhook;

	public FilterListener(BundleContext context) {
		lhook = context.registerService(ListenerHook.class, this, null);
	}

	@Override
	public synchronized void added(Collection<ListenerInfo> listeners) {
		if (quiting)
			return;
		for (Object o : listeners) {
			addListenerInfo((ListenerInfo) o);
		}
	}

	@Override
	public synchronized void removed(Collection<ListenerInfo> listeners) {
		if (quiting)
			return;
		for (Object o : listeners) {
			removeListenerInfo((ListenerInfo) o);
		}
	}

	private void addListenerInfo(ListenerInfo o) {
		String filter = o.getFilter();
		if (filter != null) {
			Matcher m = LISTENER_INFO_PATTERN.matcher(filter);
			while (m.find()) {
				listenerContexts.add(m.group(1), o.getBundleContext());
			}
		}
	}

	private void removeListenerInfo(ListenerInfo o) {
		String filter = o.getFilter();
		if (filter != null) {
			Matcher m = LISTENER_INFO_PATTERN.matcher(filter);
			while (m.find()) {
				listenerContexts.removeValue(m.group(1), o.getBundleContext());
			}
		}
	}

	public void close() {
		quiting = true;
		lhook.unregister();
	}
}
