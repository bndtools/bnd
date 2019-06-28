package bndtools.central;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.BundleTracker;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.About;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;

/**
 * This class extends the dynamic imports of bndlib with any exported package
 * from OSGi that specifies a 'bnd-plugins' attribute. Its value is either true
 * or a version range on the bnd version.
 */
class Auxiliary implements Closeable, WeavingHook {
	private final BundleContext						context;
	private final AtomicBoolean						closed	= new AtomicBoolean(false);
	private final ServiceRegistration<WeavingHook>	hook;
	private final BundleTracker<Bundle>				tracker;
	private Bundle									bndlib;
	private List<String>							delta	= new ArrayList<>();

	Auxiliary(BundleContext context, Bundle bndlib) {
		this.bndlib = bndlib;
		this.context = context;
		this.tracker = new BundleTracker<Bundle>(context, Bundle.RESOLVED + Bundle.ACTIVE + Bundle.STARTING, null) {
			@Override
			public Bundle addingBundle(Bundle bundle, BundleEvent event) {
				if (!doImport(bundle.getHeaders()
					.get(Constants.EXPORT_PACKAGE)))
					return null;

				return super.addingBundle(bundle, event);
			}

		};
		this.tracker.open();
		this.hook = this.context.registerService(WeavingHook.class, this, null);
	}

	/*
	 * Parse the exports and see
	 */
	private boolean doImport(String exports) {
		if (closed.get() || exports == null || exports.isEmpty())
			return false;

		Parameters out = new Parameters();

		Parameters p = new Parameters(exports);
		for (Entry<String, Attrs> e : p.entrySet()) {
			Attrs attrs = e.getValue();
			if (attrs == null)
				continue;

			String plugins = attrs.get("bnd-plugins");
			if (plugins == null)
				continue;

			if (!(plugins.isEmpty() || "true".equalsIgnoreCase(plugins))) {
				if (Verifier.isVersionRange(plugins)) {
					VersionRange range = new VersionRange(plugins);
					if (!range.includes(About._3_0)) // TODO
						continue;
				}
			}

			//
			// Ok, matched
			//

			String v = attrs.getVersion();
			if (v == null)
				v = "0";

			for (Iterator<String> i = attrs.keySet()
				.iterator(); i.hasNext();) {
				String key = i.next();
				if (key.endsWith(":"))
					i.remove();
			}

			if (Verifier.isVersion(v)) {
				Version version = new Version(v);
				attrs.put("version", new VersionRange(true, version, version, true).toString());
			}
			out.put(e.getKey(), attrs);
		}
		if (out.isEmpty())
			return false;

		String imports = out.toString();
		synchronized (this) {

			if (delta == null)
				delta = new ArrayList<>();

			delta.add(imports);
		}
		return true;
	}

	@Override
	public void weave(WovenClass wovenClass) {
		if (delta == null || delta.isEmpty())
			return;

		BundleWiring wiring = wovenClass.getBundleWiring();
		if (wiring == null)
			return;

		if (wiring.getBundle() != bndlib)
			return;

		List<String> extra;
		synchronized (this) {
			extra = delta;
			delta = null;
		}
		if (extra != null)
			wovenClass.getDynamicImports()
				.addAll(extra);
	}

	@Override
	public void close() throws IOException {
		if (closed.getAndSet(true) == false) {
			hook.unregister();
			tracker.close();
		}
	}

}
