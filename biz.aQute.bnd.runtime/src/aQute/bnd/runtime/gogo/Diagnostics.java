package aQute.bnd.runtime.gogo;

import java.io.Closeable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.service.command.Converter;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.lib.dtoformatter.DTOFormatter;
import aQute.libg.glob.Glob;

public class Diagnostics implements Closeable, Converter {

	private final BundleContext		context;
	private final FilterListener	fl;

	public Diagnostics(BundleContext context, DTOFormatter formatter) {
		this.context = context;
		this.fl = new FilterListener(context);
	}

	@Override
	public void close() {
		fl.close();
	}

	@Descriptor("Show all requirements. Iterates over all (or one) bundles and gathers their requirements.")
	public List<Requirement> reqs(@Descriptor("Only show the requirements of the given bundle") @Parameter(names = {
		"-b", "--bundle"
	}, absentValue = "*") Glob bundle,
		@Descriptor("Only show the requirements when the given namespace matches. You can use wildcards. A number of namespaces are shortcutted:\n"
			+ "  p = osgi.wiring.package\n" + "  i = osgi.wiring.identity\n" + "  h = osgi.wiring.host\n"
			+ "  b = osgi.wiring.bundle\n" + "  e = osgi.extender\n" + "  s = osgi.service\n"
			+ "  c = osgi.contract") @Parameter(names = {
				"-n", "--namespace"
		}, absentValue = "*") String ns) {

		Glob nsg = shortcuts(ns);

		List<Requirement> reqs = new ArrayList<>();

		for (Bundle b : context.getBundles()) {
			if (!bundle.matcher(b.getSymbolicName())
				.matches())
				continue;

			BundleWiring wiring = b.adapt(BundleWiring.class);

			List<BundleRequirement> requirements = wiring.getRequirements(null);
			for (Requirement r : requirements) {
				if (nsg.matcher(r.getNamespace())
					.matches()) {
					reqs.add(r);
				}
			}
		}
		return reqs;
	}

	@Descriptor("Show all capabilities of all bundles. It is possible to list by bundle and/or by a specific namespace.")
	public List<Capability> caps(@Descriptor("Only show the capabilities of the given bundle") @Parameter(names = {
		"-b", "--bundle"
	}, absentValue = "-1") long bundle,
		@Descriptor("Only show the capabilities when the given namespace matches. You can use wildcards. A number of namespaces are shortcutted:\n"
			+ "  p = osgi.wiring.package\n" + "  i = osgi.wiring.identity\n" + "  h = osgi.wiring.host\n"
			+ "  b = osgi.wiring.bundle\n" + "  e = osgi.extender\n" + "  s = osgi.service\n"
			+ "  c = osgi.contract") @Parameter(names = {
				"-n", "--namespace"
		}, absentValue = "*") String ns) {

		Glob nsg = shortcuts(ns);

		List<Capability> result = new ArrayList<>();

		for (Bundle b : context.getBundles()) {
			if (bundle != -1 && b.getBundleId() != bundle)
				continue;

			BundleWiring wiring = b.adapt(BundleWiring.class);

			List<BundleCapability> capabilities = wiring.getCapabilities(null);
			for (Capability r : capabilities) {
				if (nsg.matcher(r.getNamespace())
					.matches()) {
					result.add(r);
				}
			}
		}
		return result;
	}

	@Descriptor("Show bundles that are listening for certain services. This will check for the following fishy cases: \n"
		+ "* ? – No matching registered service found\n"
		+ "* ! – Matching registered service found in another classpace\n"
		+ "The first set show is the set of bundle that register such a service in the proper class space. The second "
		+ "set (only shown when not empty) shows the bundles that have a registered service for this but are not "
		+ "compatible because they are registered in another class space")
	public List<Search> wanted(@Descriptor("If specified will only show for the given bundle") @Parameter(names = {
		"-b", "--bundle"
	}, absentValue = "-1") long exporter,
		@Descriptor("If specified, this glob expression must match the name of the service class/interface name") @Parameter(names = {
			"-n", "--name"
		}, absentValue = "*") Glob name) throws InvalidSyntaxException {
		List<Search> searches = new ArrayList<>();
		synchronized (fl) {
			for (Map.Entry<String, List<BundleContext>> e : fl.listenerContexts.entrySet()) {

				String serviceName = e.getKey();

				if (!name.matcher(serviceName)
					.matches())
					continue;

				ServiceReference<?> refs[] = context.getAllServiceReferences(serviceName, null);
				for (BundleContext bc : e.getValue()) {

					if (exporter != -1 && exporter != bc.getBundle()
						.getBundleId())
						continue;

					BundleWiring wiring = bc.getBundle()
						.adapt(BundleWiring.class);

					Search s = new Search();
					s.serviceName = serviceName;
					s.searcher = wiring.getRevision();

					ClassLoader classLoader = wiring.getClassLoader();
					Class<?> type = load(classLoader, serviceName);

					if (refs != null) {
						for (ServiceReference<?> ref : refs) {
							Bundle registrar = ref.getBundle();

							Class<?> registeredClass = load(registrar, serviceName);
							long bundleId = registrar.getBundleId();
							if (type == null || registeredClass == null || type == registeredClass) {
								s.matched.add(bundleId);
							} else {
								s.mismatched.add(bundleId);
							}
						}
					}
					searches.add(s);
				}
			}
		}
		return searches;
	}

	@Descriptor("Show exported packages of all bundles that look fishy. Options are provided to filter for a specific bundle and/or the package name (glob). You can also specify -a for all packages")
	public Collection<Export> exports(
		@Descriptor("If specified will only show for the given bundle") @Parameter(names = {
			"-b", "--bundle"
		}, absentValue = "-1") long exporter,
		@Descriptor("If specified, this glob expression must match the name of the service class/interface name") @Parameter(names = {
			"-n", "--name"
		}, absentValue = "*") Glob name,
		@Descriptor("Show all packages, not just the ones that look fishy") @Parameter(names = {
			"-a", "--all"
		}, absentValue = "false", presentValue = "true") boolean all,
		@Descriptor("Check exports against private packages") @Parameter(names = {
			"-p", "--private"
		}, absentValue = "false", presentValue = "true") boolean privatePackages) {
		Map<String, Export> map = new HashMap<>();

		List<Capability> caps = caps(-1, PackageNamespace.PACKAGE_NAMESPACE);

		for (Capability c : caps) {
			String packageName = (String) c.getAttributes()
				.get(PackageNamespace.PACKAGE_NAMESPACE);
			if (!name.matcher(packageName)
				.matches())
				continue;

			Resource resource = c.getResource();
			if (resource instanceof BundleRevision) {
				Bundle bundle = ((BundleRevision) resource).getBundle();
				if (exporter != -1 && bundle.getBundleId() != exporter)
					continue;
				Export e = map.get(packageName);
				if (e == null) {
					e = new Export(packageName);
					map.put(packageName, e);
				}
				e.exporters.add(bundle.getBundleId());
			}

		}

		for (Export e : map.values()) {
			for (Bundle b : context.getBundles()) {

				if (e.exporters.contains(b.getBundleId()))
					continue;

				if (hasPackage(b, e.pack)) {
					e.privates.add(b.getBundleId());
				}
			}
		}

		if (all) {
			return map.values();
		}

		Set<Export> s = new HashSet<>(map.values());
		s.removeIf(e -> e.exporters.size() == 1 && e.privates.isEmpty());
		return s;
	}

	private boolean hasPackage(Bundle b, String pack) {
		Enumeration<URL> entries = b.findEntries(pack.replace('.', '/'), "*", false);
		return entries != null && entries.hasMoreElements();
	}

	private Class<?> load(Bundle bundle, String name) {
		return load(bundle.adapt(BundleWiring.class)
			.getClassLoader(), name);
	}

	private Class<?> load(ClassLoader classLoader, String name) {
		try {
			return classLoader.loadClass(name);
		} catch (Exception e) {
			return null;
		}
	}

	private Glob shortcuts(String ns) {
		switch (ns) {
			case "p" :
				ns = "osgi.wiring.package";
				break;

			case "i" :
				ns = "osgi.wiring.identity";
				break;
			case "h" :
				ns = "osgi.wiring.host";
				break;
			case "b" :
				ns = "osgi.wiring.bundle";
				break;
			case "e" :
				ns = "osgi.extender";
				break;
			case "s" :
				ns = "osgi.service";
				break;
			case "c" :
				ns = "osgi.contract";
				break;
		}
		Glob nsg = new Glob(ns);
		return nsg;
	}

	@Override
	public Object convert(Class<?> arg0, Object arg1) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CharSequence format(Object arg0, int arg1, Converter arg2) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
