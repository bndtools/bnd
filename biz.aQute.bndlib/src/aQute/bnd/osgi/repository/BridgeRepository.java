package aQute.bnd.osgi.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

import aQute.bnd.osgi.resource.CapabilityBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.ContentCapability;
import aQute.bnd.osgi.resource.ResourceUtils.IdentityCapability;
import aQute.bnd.version.Version;
import aQute.libg.glob.Glob;

/**
 * Bridge an OSGi repository (requirements) and a bnd repository (bsn/version)
 * by creating an index and providing suitable methods.
 * <p>
 * This class ignores duplicate bsn/version entries
 */
public class BridgeRepository {

	private static final Requirement						allRq			= ResourceUtils.createWildcardRequirement();
	private static final SortedSet<Version>					EMPTY_VERSIONS	= new TreeSet<>();

	private final Repository								repository;
	private final Map<String, Map<Version, ResourceInfo>>	index			= new HashMap<>();

	public interface InfoCapability extends Capability {
		String error();

		String name();

		String from();
	}

	static public class ResourceInfo {
		public ResourceInfo(Resource resource) {
			this.resource = resource;
		}

		boolean			inited;
		Resource		resource;
		boolean			error;
		String			tooltip;
		private String	title;

		public String getTooltip() {
			init();
			return tooltip;
		}

		private synchronized void init() {
			if (inited)
				return;
			inited = true;

			IdentityCapability ic = ResourceUtils.getIdentityCapability(resource);
			ContentCapability cc = ResourceUtils.getContentCapability(resource);
			String bsn = ic.osgi_identity();
			Version version = ic.version();

			InfoCapability info = getInfo();

			String sha256 = cc == null ? "<>" : cc.osgi_content();

			String error = null;
			String name = null;
			String from = null;

			if (info != null) {
				error = info.error();
				name = info.name();
				from = info.from();
			}

			if (error != null) {
				this.error = true;
				this.title = version + " [" + error + "]";
			} else
				this.title = version.toString();

			StringBuilder tsb = new StringBuilder();
			if (this.error)
				tsb.append("ERROR: ")
					.append(error)
					.append("\n");

			tsb.append(bsn)
				.append("\n");

			if (ic.description(null) != null) {
				tsb.append(ic.description(null));
				tsb.append("\n");
			}

			tsb.append(bsn)
				.append("\n");

			tsb.append("Coordinates: ")
				.append(name)
				.append("\n");
			tsb.append("SHA-256: ")
				.append(sha256)
				.append("\n");
			if (from != null) {
				tsb.append("From: ");
				tsb.append(from);
				tsb.append("\n");
			}
			this.tooltip = tsb.toString();
		}

		public InfoCapability getInfo() {
			List<Capability> capabilities = resource.getCapabilities("bnd.info");
			InfoCapability info;
			if (capabilities.size() >= 1) {
				info = ResourceUtils.as(capabilities.get(0), InfoCapability.class);
			} else
				info = null;
			return info;
		}

		public String getTitle() {
			init();
			return title;
		}

		public boolean isError() {
			init();
			return error;
		}
	}

	public BridgeRepository(Repository repository) throws Exception {
		this.repository = repository;
		index();
	}

	private void index() throws Exception {
		Map<Requirement, Collection<Capability>> all = repository.findProviders(Collections.singleton(allRq));
		for (Capability capability : all.get(allRq)) {
			Resource r = capability.getResource();
			index(r);
		}
	}

	private void index(Resource r) throws Exception {
		IdentityCapability bc = ResourceUtils.getIdentityCapability(r);
		String bsn = bc.osgi_identity();
		Version version = bc.version();

		Map<Version, ResourceInfo> map = index.get(bsn);
		if (map == null) {
			map = new HashMap<>();
			index.put(bsn, map);
		}
		map.put(version, new ResourceInfo(r));
	}

	public Resource get(String bsn, Version version) throws Exception {
		ResourceInfo resourceInfo = getInfo(bsn, version);
		if (resourceInfo == null)
			return null;

		return resourceInfo.resource;
	}

	public ResourceInfo getInfo(String bsn, Version version) throws Exception {
		Map<Version, ResourceInfo> map = index.get(bsn);
		if (map == null)
			return null;

		return map.get(version);
	}

	public List<String> list(String pattern) throws Exception {
		List<String> bsns = new ArrayList<>();
		if (pattern == null || pattern.equals("*") || pattern.equals("")) {
			bsns.addAll(index.keySet());
		} else {
			String[] split = pattern.split("\\s+");
			Glob globs[] = new Glob[split.length];
			for (int i = 0; i < split.length; i++) {
				globs[i] = new Glob(split[i]);
			}

			outer: for (String bsn : index.keySet()) {
				for (Glob g : globs) {
					if (g.matcher(bsn)
						.find()) {
						bsns.add(bsn);
						continue outer;
					}
				}
			}
		}
		return bsns;
	}

	public SortedSet<Version> versions(String bsn) throws Exception {
		Map<Version, ResourceInfo> map = index.get(bsn);
		if (map == null || map.isEmpty()) {
			return EMPTY_VERSIONS;
		}
		return new TreeSet<>(map.keySet());
	}

	public Repository getRepository() {
		return repository;
	}

	public static void addInformationCapability(ResourceBuilder rb, String name, String from, Throwable error) {
		try {
			CapabilityBuilder c = new CapabilityBuilder("bnd.info");
			c.addAttribute("name", name);
			if (from != null)
				c.addAttribute("from", from);
			if (error != null)
				c.addAttribute("error", error.toString());

			rb.addCapability(c);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String tooltip(Object... target) throws Exception {
		if (target.length == 2) {
			ResourceInfo ri = getInfo((String) target[0], (Version) target[1]);
			return ri.getTooltip();
		}
		return null;
	}

	public String title(Object... target) throws Exception {
		if (target.length == 2) {
			ResourceInfo ri = getInfo((String) target[0], (Version) target[1]);
			return ri.getTitle();
		}
		if (target.length == 1) {
			String bsn = (String) target[0];
			Map<Version, ResourceInfo> map = index.get(bsn);
			for (ResourceInfo ri : map.values()) {
				if (ri.isError())
					return bsn + " [!]";
			}
			return bsn;
		}
		return null;
	}
}
