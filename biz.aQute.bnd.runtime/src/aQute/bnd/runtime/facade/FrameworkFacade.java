package aQute.bnd.runtime.facade;

import static aQute.bnd.runtime.util.Util.asBean;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.osgi.dto.DTO;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.dto.FrameworkDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.framework.hooks.service.ListenerHook;
import org.osgi.framework.hooks.service.ListenerHook.ListenerInfo;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.dto.BundleWiringDTO;
import org.osgi.util.tracker.ServiceTracker;

import aQute.bnd.header.Parameters;
import aQute.bnd.runtime.api.SnapshotProvider;
import aQute.bnd.runtime.util.Util;
import aQute.lib.collections.MultiMap;

public class FrameworkFacade implements SnapshotProvider {

	final BundleContext																								context;
	final MultiMap<Bundle, ListenerInfo>																			serviceListeners	= new MultiMap<>();
	@SuppressWarnings("deprecation")
	final ServiceTracker<org.osgi.service.packageadmin.PackageAdmin, org.osgi.service.packageadmin.PackageAdmin>	packageAdminTracker;
	final AtomicLong																								number				= new AtomicLong();
	final Map<String, PackageDTO>																					packages			= new ConcurrentHashMap<>();
	final TimeMeasurement																							timing;

	private ServiceRegistration<ListenerHook>																		listenerHook;

	public static class XBundleDTO extends BundleDTO {
		public String						location;
		public int							startLevel;
		public boolean						persistentlyStarted;
		public boolean						activationPolicyUsed;
		public Map<String, String>			headers				= new LinkedHashMap<>();
		public List<Map<String, Object>>	waiting				= new ArrayList<>();
		public Set<Long>					exports				= new HashSet<>();
		public Set<Long>					importingBundles	= new HashSet<>();
		public Set<Long>					registeredServices;
		public Set<Long>					usingServices;
		public int							revisions;
		public long							startTime;
	}

	public static class ServiceDTO extends ServiceReferenceDTO {
		public List<Long>	timings	= new ArrayList<>();
		public boolean		registered;
		public boolean		unregistered;
	}

	public static class PackageDTO extends DTO {
		public long								id;
		public String							name;
		public boolean							mandatory;
		public long								exportingBundle;
		public Map<String, Map<String, Object>>	versions			= new TreeMap<>();
		public List<Long>						importingBundles	= new ArrayList<>();
		public Map<String, Object>				properties			= new HashMap<>();
		public boolean							removalPending;
		public Set<Long>						duplicates			= new HashSet<>();
	}

	public static class XFrameworkDTO extends DTO {
		public int							startLevel;
		public int							initialBundleStartLevel;

		public Map<Long, XBundleDTO>		bundles		= new LinkedHashMap<>();
		public Map<Long, ServiceDTO>		services	= new LinkedHashMap<>();
		public Map<String, Object>			properties	= new LinkedHashMap<>();
		public Map<String, Object>			system		= new LinkedHashMap<>();
		public Map<Long, PackageDTO>		packages	= new LinkedHashMap<>();
		public Set<String>					errors		= new HashSet<>();
		public Map<Long, BundleWiringDTO>	wiring		= new LinkedHashMap<>();
	}

	public static class XFrameworkEventDTO extends DTO {
		public long		bundleId;
		public String	message;
		public int		type;
		public long		time;
	}

	public static class ServiceTiming extends DTO {
		public List<Long>	timings	= new ArrayList<>();
		public boolean		registered;
		public boolean		unregistered;

		public ServiceTiming getTiming(long id) {
			// TODO Auto-generated method stub
			return null;
		}
	}

	@SuppressWarnings("deprecation")
	public FrameworkFacade(BundleContext context) {
		this.context = context;
		packageAdminTracker = new ServiceTracker<>(context, org.osgi.service.packageadmin.PackageAdmin.class, null);
		packageAdminTracker.open();
		this.timing = new TimeMeasurement(context);

		ListenerHook listeners = new ListenerHook() {

			@Override
			public void added(Collection<ListenerInfo> listeners) {
				synchronized (listeners) {
					for (ListenerInfo li : listeners) {
						Bundle b = li.getBundleContext()
							.getBundle();
						serviceListeners.add(b, li);
					}
				}
			}

			@Override
			public void removed(Collection<ListenerInfo> listeners) {
				synchronized (listeners) {
					for (ListenerInfo li : listeners) {
						Bundle b = li.getBundleContext()
							.getBundle();
						serviceListeners.remove(b, li);
					}
				}
			}

		};
		listenerHook = context.registerService(ListenerHook.class, listeners, null);
	}

	@SuppressWarnings({
		"unchecked", "rawtypes", "deprecation"
	})
	public XFrameworkDTO getFrameworkDTO() throws InvalidSyntaxException {
		XFrameworkDTO xframework = new XFrameworkDTO();
		org.osgi.service.packageadmin.PackageAdmin packageAdmin = packageAdminTracker.getService();

		org.osgi.service.packageadmin.RequiredBundle[] requiredBundles = null;

		requiredBundles = packageAdmin.getRequiredBundles(null);

		FrameworkDTO adapt = context.getBundle(0)
			.adapt(FrameworkDTO.class);

		xframework.properties = adapt.properties;
		// xframework.system.putAll((Map) System.getProperties());

		for (ServiceReferenceDTO sref : adapt.services) {
			ServiceDTO sdto = new ServiceDTO();
			sdto.id = sref.id;
			sdto.bundle = sref.bundle;
			sdto.properties = sref.properties;
			sdto.usingBundles = sref.usingBundles;

			ServiceTiming timing = this.timing.getTiming(sdto.id);
			if (timing != null) {
				sdto.registered = timing.registered;
				sdto.unregistered = timing.unregistered;
				sdto.timings = timing.timings;
			}
			xframework.services.put(sdto.id, sdto);
		}

		Set<String> frameworkExports = getExports(context.getBundle()).keySet();

		for (BundleDTO bundle : adapt.bundles) {
			XBundleDTO xbundle = Util.copy(XBundleDTO.class, bundle);
			Bundle b = context.getBundle(xbundle.id);
			xbundle.location = b.getLocation();

			xbundle.startTime = timing.getStart(bundle.id);

			Dictionary<String, String> headers = b.getHeaders();
			Enumeration<String> e = headers.keys();
			while (e.hasMoreElements()) {
				String key = e.nextElement();
				xbundle.headers.put(key, headers.get(key));
			}

			BundleStartLevel startLevel = b.adapt(BundleStartLevel.class);
			if (startLevel != null) {
				xbundle.startLevel = startLevel.getStartLevel();
				xbundle.activationPolicyUsed = startLevel.isActivationPolicyUsed();
				xbundle.persistentlyStarted = startLevel.isPersistentlyStarted();
			}

			BundleRevisions revisions = b.adapt(BundleRevisions.class);
			xbundle.revisions = revisions.getRevisions()
				.size();

			if (serviceListeners.containsKey(b)) {
				for (ListenerInfo li : serviceListeners.get(b)) {
					Map<String, Object> bean = asBean(ListenerInfo.class, li);
					ServiceReference<?>[] allRefs = context.getAllServiceReferences(null, li.getFilter());
					List<ServiceReference<?>> all = new ArrayList<>(asList(allRefs));
					List<ServiceReference<?>> partial = asList(b.getBundleContext()
						.getAllServiceReferences(null, li.getFilter()));
					all.removeAll(partial);
					List<Long> hidden = all.stream()
						.map(sr -> (Long) sr.getProperty(Constants.SERVICE_ID))
						.collect(Collectors.toList());
					bean.put("hidden", hidden);
					xbundle.waiting.add(bean);
				}
			}

			verifyThatExportedPackagesAreImportableWhenExportedByFramework(xframework, frameworkExports, b);

			xbundle.registeredServices = adapt.services.stream()
				.filter(r -> r.bundle == b.getBundleId())
				.map(r -> r.id)
				.collect(Collectors.toSet());

			xbundle.usingServices = adapt.services.stream()
				.filter(r -> Util.in(r.usingBundles, b.getBundleId()))
				.map(r -> r.id)
				.collect(Collectors.toSet());

			xframework.bundles.put(b.getBundleId(), xbundle);
		}

		doPackages(xframework, packageAdmin);

		if (System.getProperty("snapshot.wiring") != null) {
			for (Bundle b : context.getBundles()) {
				xframework.wiring.put(b.getBundleId(), b.adapt(BundleWiringDTO.class));
			}
		}
		return xframework;
	}

	/*
	 * Do a sanity check that any exports from bundle b are imported when the
	 * framework exports them.
	 */
	private void verifyThatExportedPackagesAreImportableWhenExportedByFramework(XFrameworkDTO xframework,
		Set<String> frameworkExports, Bundle b) {
		if (b.getBundleId() != 0L) {
			Set<String> imports = getImports(b).keySet();
			Set<String> exports = getExports(b).keySet();
			exports.removeAll(imports);
			exports.retainAll(frameworkExports);
			if (!exports.isEmpty()) {
				xframework.errors.add("bundle " + b + " exports packages " + exports + ".\n"
					+ "These packages are not imported by this bundle " + b
					+ " but they are exported by the framework.\n"
					+ "Since they are not imported they will remain in their own class space. The JUnitFramework will\n"
					+ "likely not work for these packages");
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void doPackages(XFrameworkDTO xframework, org.osgi.service.packageadmin.PackageAdmin packageAdmin) {
		if (packageAdmin != null) {
			org.osgi.service.packageadmin.ExportedPackage[] exportedPackages = packageAdmin
				.getExportedPackages((Bundle) null);
			if (exportedPackages != null) {
				MultiMap<String, PackageDTO> duplicates = new MultiMap<>();

				for (org.osgi.service.packageadmin.ExportedPackage ep : exportedPackages) {

					doPackage(xframework, duplicates, number, packages, ep);
				}

				for (List<PackageDTO> l : duplicates.values()) {
					if (setDuplicates(l)) {
						xframework.errors.add("Duplicate package " + l.stream()
							.map(p -> p.name)
							.findFirst()
							.orElse("?"));
					}
				}
			} else {
				xframework.errors.add("No packages in package admin found?");
			}
		} else {
			xframework.errors.add("No package admin found");
		}
	}

	private boolean setDuplicates(List<PackageDTO> l) {
		Set<Long> ids = l.stream()
			.map(p -> p.id)
			.collect(Collectors.toSet());
		if (l.size() <= 1)
			return false;

		for (PackageDTO p : l) {
			p.duplicates.addAll(ids);
		}
		return true;
	}

	@SuppressWarnings("deprecation")
	private void doPackage(XFrameworkDTO xframework, MultiMap<String, PackageDTO> duplicates, AtomicLong number,
		Map<String, PackageDTO> packages, org.osgi.service.packageadmin.ExportedPackage ep) {
		String packageId = ep.getName() + "-" + ep.getExportingBundle() + "-" + ep.isRemovalPending();

		PackageDTO packageDto = packages.computeIfAbsent(packageId, k -> {
			PackageDTO pdto = new PackageDTO();
			pdto.id = number.getAndIncrement();
			pdto.name = ep.getName();
			pdto.exportingBundle = ep.getExportingBundle()
				.getBundleId();
			pdto.removalPending = ep.isRemovalPending();
			return pdto;
		});
		packageDto.versions.put(ep.getVersion()
			.toString(), new HashMap<>());

		XBundleDTO exporter = xframework.bundles.get(ep.getExportingBundle()
			.getBundleId());
		exporter.exports.add(packageDto.id);

		Bundle[] importingBundles = ep.getImportingBundles();

		if (importingBundles != null) {
			for (Bundle b : importingBundles) {
				XBundleDTO importer = xframework.bundles.get(b.getBundleId());
				importer.importingBundles.add(packageDto.id);
				packageDto.importingBundles.add(importer.id);
			}
		}

		duplicates.add(ep.getName(), packageDto);
		xframework.packages.put(packageDto.id, packageDto);
	}

	@Override
	public void close() throws IOException {
		listenerHook.unregister();
		packageAdminTracker.close();
		timing.close();
	}

	@Override
	public Object getSnapshot() throws Exception {
		return getFrameworkDTO();
	}

	private Parameters getExports(Bundle b) {
		return new Parameters(b.getHeaders()
			.get(Constants.EXPORT_PACKAGE));
	}

	private Parameters getImports(Bundle b) {
		return new Parameters(b.getHeaders()
			.get(Constants.IMPORT_PACKAGE));
	}

	private List<ServiceReference<?>> asList(ServiceReference<?>[] refs) {
		if (refs == null)
			return Collections.emptyList();

		return Arrays.asList(refs);
	}

}
