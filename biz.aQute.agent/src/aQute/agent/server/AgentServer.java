package aQute.agent.server;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.dto.FrameworkDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;

import aQute.libg.shacache.ShaCache;
import aQute.libg.shacache.ShaSource;
import aQute.service.agent.Agent;
import aQute.service.agent.Supervisor;

public class AgentServer implements Agent {
	@SuppressWarnings("deprecation")
	static String keys[] = { Constants.FRAMEWORK_BEGINNING_STARTLEVEL,
			Constants.FRAMEWORK_BOOTDELEGATION, Constants.FRAMEWORK_BSNVERSION,
			Constants.FRAMEWORK_BUNDLE_PARENT,
			Constants.FRAMEWORK_TRUST_REPOSITORIES,
			Constants.FRAMEWORK_COMMAND_ABSPATH,
			Constants.FRAMEWORK_EXECPERMISSION,
			Constants.FRAMEWORK_EXECUTIONENVIRONMENT,
			Constants.FRAMEWORK_LANGUAGE,
			Constants.FRAMEWORK_LIBRARY_EXTENSIONS,
			Constants.FRAMEWORK_OS_NAME, Constants.FRAMEWORK_OS_VERSION,
			Constants.FRAMEWORK_PROCESSOR, Constants.FRAMEWORK_SECURITY,
			Constants.FRAMEWORK_STORAGE,
			Constants.FRAMEWORK_SYSTEMCAPABILITIES,
			Constants.FRAMEWORK_SYSTEMCAPABILITIES_EXTRA,
			Constants.FRAMEWORK_SYSTEMPACKAGES,
			Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, Constants.FRAMEWORK_UUID,
			Constants.FRAMEWORK_VENDOR, Constants.FRAMEWORK_VERSION,
			Constants.FRAMEWORK_WINDOWSYSTEM, };

	private Supervisor remote;
	private final BundleContext context;
	private final ShaCache cache;
	private final ShaSource source;
	private final Map<String, String> installed = new HashMap<String, String>();

	public AgentServer(BundleContext context, File cache) {
		this.context = context;
		this.cache = new ShaCache(cache);
		source = new ShaSource() {

			@Override
			public boolean isFast() {
				return false;
			}

			@Override
			public InputStream get(String sha) throws Exception {
				byte[] data = remote.getFile(sha);
				if (data == null)
					return null;

				return new ByteArrayInputStream(data);
			}
		};
	}

	@Override
	public FrameworkDTO getFramework() throws Exception {
		FrameworkDTO fw = new FrameworkDTO();
		fw.bundles = getBundles();
		fw.properties = getProperties();
		fw.services = getServiceReferences();
		return fw;
	}

	@Override
	public BundleDTO install(String location, String sha)
			throws FileNotFoundException, BundleException {
		InputStream in = cache.getStream(sha, source);
		if (in == null)
			return null;

		Bundle b = context.installBundle(location, in);
		installed.put(b.getLocation(), sha);
		return toDTO(b);
	}

	@Override
	public String start(long... ids) {
		StringBuilder sb = new StringBuilder();

		for (long id : ids) {
			Bundle bundle = context.getBundle(id);
			try {
				bundle.start();
			} catch (BundleException e) {
				sb.append(e.getMessage()).append("\n");
			}
		}
		return sb.length() == 0 ? null : sb.toString();
	}

	@Override
	public String stop(long... ids) {
		StringBuilder sb = new StringBuilder();

		for (long id : ids) {
			Bundle bundle = context.getBundle(id);
			try {
				bundle.stop();
			} catch (BundleException e) {
				sb.append(e.getMessage()).append("\n");
			}
		}
		return sb.length() == 0 ? null : sb.toString();
	}

	@Override
	public String uninstall(long... ids) {
		StringBuilder sb = new StringBuilder();

		for (long id : ids) {
			Bundle bundle = context.getBundle(id);
			try {
				bundle.uninstall();
				installed.remove(bundle.getBundleId());
			} catch (BundleException e) {
				sb.append(e.getMessage()).append("\n");
			}
		}
		return sb.length() == 0 ? null : sb.toString();
	}

	@Override
	public String update(Map<String, String> bundles) {
		Formatter out = new Formatter();

		Set<String> toBeDeleted = new HashSet<String>(installed.keySet());
		toBeDeleted.removeAll(bundles.keySet());

		Set<String> toBeInstalled = new HashSet<String>(bundles.keySet());
		toBeInstalled.removeAll(installed.keySet());

		Map<String, String> changed = new HashMap<String, String>(bundles);
		changed.values().removeAll(installed.values());
		changed.keySet().removeAll(toBeInstalled);

		Set<String> affected = new HashSet<String>(toBeDeleted);
		affected.addAll(changed.keySet());

		Set<Bundle> toBeStarted = new HashSet<Bundle>();

		for (String location : affected) {
			Bundle b = context.getBundle(location);
			if (b == null) {
				out.format("Could not location bundle %s to stop it", location);
				continue;
			}

			try {
				if (!isActive(b))
					toBeStarted.add(b);

				b.stop();
			} catch (BundleException e) {
				e.printStackTrace();
				out.format("Trying to stop bundle %s : %s", b, e);
			}

		}

		for (String location : toBeDeleted) {
			Bundle b = context.getBundle(location);
			if (b == null) {
				out.format("Could not find bundle %s to uninstall it", location);
			}

			try {
				b.uninstall();
				installed.remove(location);
				toBeStarted.remove(b);
			} catch (BundleException e) {
				e.printStackTrace();
				out.format("Trying to uninstall %s: %s", location, e);
			}
		}

		for (String location : toBeInstalled) {
			String sha = bundles.get(location);

			try {
				InputStream in = cache.getStream(sha, source);
				if (in == null) {
					out.format("Could not find file with sha %s for bundle %s",
							sha, location);
					continue;
				}

				Bundle b = context.installBundle(location, in);
				installed.put(location, sha);
				toBeStarted.add(b);

			} catch (Exception e) {
				e.printStackTrace();
				out.format("Trying to install %s: %s", location, e);
			}
		}

		for (Entry<String, String> e : changed.entrySet()) {
			String location = e.getKey();
			String sha = e.getValue();

			try {
				InputStream in = cache.getStream(sha, source);
				if (in == null) {
					out.format("Cannot find file for sha %s to update %s", sha,
							location);
					continue;
				}

				Bundle bundle = context.getBundle(location);
				if (bundle == null) {
					out.format(
							"No such bundle for location %s while trying to update it",
							location);
					continue;
				}

				bundle.update(in);
			} catch (Exception e1) {
				e1.printStackTrace();
				out.format("Trying to update %s: %s", location, e);
			}
		}

		for (Bundle b : toBeStarted) {
			try {
				b.start();
			} catch (BundleException e1) {
				e1.printStackTrace();
				out.format("Trying to start %s: %s", b, e1);
			}
		}

		String result = out.toString();
		out.close();
		if ( result.length() == 0)
			return null;
		
		return result;
	}

	private boolean isActive(Bundle b) {
		return b.getState() == Bundle.ACTIVE || b.getState() == Bundle.STARTING;
	}

	@Override
	public void redirect(boolean on) {
		// TODO Auto-generated method stub

	}

	@Override
	public void stdin(String s) {
		// TODO Auto-generated method stub

	}

	@Override
	public String shell(String cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	public void setSupervisor(Supervisor remote) {
		this.remote = remote;
	}

	private List<ServiceReferenceDTO> getServiceReferences() throws Exception {
		ServiceReference<?>[] refs = context
				.getAllServiceReferences(null, null);
		if (refs == null)
			return Collections.emptyList();

		ArrayList<ServiceReferenceDTO> list = new ArrayList<ServiceReferenceDTO>(
				refs.length);
		for (ServiceReference<?> r : refs) {
			ServiceReferenceDTO ref = new ServiceReferenceDTO();
			ref.bundle = r.getBundle().getBundleId();
			ref.id = Long.parseLong((String) r
					.getProperty(Constants.SERVICE_ID));
			ref.properties = getProperties(r);
			Bundle[] usingBundles = r.getUsingBundles();
			ref.usingBundles = new long[usingBundles.length];
			for (int i = 0; i < usingBundles.length; i++) {
				ref.usingBundles[i] = usingBundles[i].getBundleId();
			}
			list.add(ref);
		}
		return list;
	}

	private Map<String, Object> getProperties(ServiceReference<?> ref) {
		Map<String, Object> map = new HashMap<String, Object>();
		for (String key : ref.getPropertyKeys())
			map.put(key, ref.getProperty(key));
		return map;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map<String, Object> getProperties() {
		Map map = new HashMap();
		map.putAll(System.getenv());
		map.putAll(System.getProperties());
		for (String key : keys) {
			Object value = context.getProperty(key);
			if (value != null)
				map.put(key, value);
		}
		return map;
	}

	private List<BundleDTO> getBundles() {
		Bundle[] bundles = context.getBundles();
		ArrayList<BundleDTO> list = new ArrayList<BundleDTO>(bundles.length);
		for (Bundle b : bundles) {
			list.add(toDTO(b));
		}
		return list;
	}

	private BundleDTO toDTO(Bundle b) {
		BundleDTO bd = new BundleDTO();
		bd.id = b.getBundleId();
		bd.lastModified = b.getLastModified();
		bd.state = b.getState();
		bd.symbolicName = b.getSymbolicName();
		bd.version = b.getVersion() == null ? "0" : b.getVersion().toString();
		return bd;
	}

}
