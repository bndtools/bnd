package aQute.bnd.osgi.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.osgi.resource.CapabilityBuilder;
import aQute.bnd.osgi.resource.RequirementBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.ContentCapability;
import aQute.bnd.osgi.resource.ResourceUtils.IdentityCapability;
import aQute.bnd.version.Version;
import aQute.lib.exceptions.Exceptions;
import aQute.libg.glob.Glob;

/**
 * Bridge an OSGi repository (requirements) and a bnd repository (bsn/version)
 * by creating an index and providing suitable methods.
 * <p>
 * This class ignores duplicate bsn/version entries
 */
public class BridgeRepository {
	final static String			BND_INFO	= "bnd.info";
	final static Logger			logger		= LoggerFactory.getLogger(BridgeRepository.class);
	final static Requirement	allIdentity	= ResourceUtils.createWildcardRequirement();
	final static Requirement	allBndInfo;

	static {
		RequirementBuilder rb = new RequirementBuilder(BND_INFO);
		rb.addFilter("(name=*)");
		allBndInfo = rb.buildSyntheticRequirement();
	}

	private static final SortedSet<Version>					EMPTY_VERSIONS	= new TreeSet<>();

	private final Repository								repository;
	private final Map<String, Map<Version, ResourceInfo>>	index			= new HashMap<>();

	@ProviderType
	public interface InfoCapability extends Capability {
		String error();

		String name();

		String from();

		Version version();
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

			String bsn = null;
			Version version = null;

			if (ic != null) {
				bsn = ic.osgi_identity();
				version = ic.version();
			}

			InfoCapability info = getInfo();

			String sha256 = cc == null ? "<>" : cc.osgi_content();

			String error = null;
			String name = null;
			String from = null;

			if (info != null) {
				error = info.error();
				name = info.name();
				from = info.from();
				if (bsn == null) {
					bsn = name;
				}
				if (version == null)
					version = info.version();
			}

			if (version == null) {
				version = Version.LOWEST;
			}
			if (bsn == null) {
				bsn = "unknown";
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

			if (ic != null && ic.description(null) != null) {
				tsb.append(ic.description(null));
				tsb.append("\n");
			}

			tsb.append(bsn)
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
			return BridgeRepository.getInfo(resource);
		}

		public String getTitle() {
			init();
			return title;
		}

		public boolean isError() {
			init();
			return error;
		}

		public Resource getResource() {
			return resource;
		}

		@Override
		public String toString() {
			return "ResourceInfo [error=" + error + ", resource=" + resource + "]";
		}
	}

	public BridgeRepository(Repository repository) throws Exception {
		this.repository = repository;
		index();
	}

	public BridgeRepository(Collection<Resource> resources) throws Exception {
		this(new ResourcesRepository(resources));
	}

	public BridgeRepository() {
		this.repository = new ResourcesRepository();
	}

	private void index() throws Exception {

		Set<Resource> resources = new HashSet<>();

		find(resources, allIdentity);
		find(resources, allBndInfo);

		resources.forEach(this::index);
	}

	private void find(Set<Resource> resources, Requirement req) {
		repository.findProviders(Collections.singleton(req))
			.get(req)
			.stream()
			.map(Capability::getResource)
			.forEach(resources::add);
	}

	private void index(Resource r) {
		String bsn;
		Version version;

		IdentityCapability bc = ResourceUtils.getIdentityCapability(r);
		if (bc != null) {
			bsn = bc.osgi_identity();
			version = bc.version();
		} else {
			logger.debug("No identity for {}, trying info", r);
			InfoCapability info = getInfo(r);
			if (info == null) {
				// No way to index this
				logger.debug("Also no info, giving up indexing");
				return;
			}

			bsn = info.name();
			version = info.version();
			if (version == null)
				version = Version.LOWEST;
		}

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
		addInformationCapability(rb, name, Version.LOWEST, from, error == null ? null : error.toString());
	}

	public static void addInformationCapability(ResourceBuilder rb, String name, String from) {
		addInformationCapability(rb, name, Version.LOWEST, from, (String) null);
	}

	public static void addInformationCapability(ResourceBuilder rb, String name, Version version, String from,
		String error) {

		try {
			CapabilityBuilder c = new CapabilityBuilder(BND_INFO);
			c.addAttribute("name", name);
			if (from != null)
				c.addAttribute("from", from);
			if (error != null)
				c.addAttribute("error", error);
			if (version != null)
				c.addAttribute("version", version);

			rb.addCapability(c);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	public String tooltip(Object... target) throws Exception {
		if (target.length == 0) {
			return repository.toString();
		}
		if (target.length == 1) {
			return null;
		}
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

	private static InfoCapability getInfo(Resource resource) {
		List<Capability> capabilities = resource.getCapabilities(BND_INFO);
		InfoCapability info;
		if (capabilities.size() >= 1) {
			info = ResourceUtils.as(capabilities.get(0), InfoCapability.class);
		} else
			info = null;
		return info;
	}

	public Set<Resource> getResources() {
		return ResourceUtils.getAllResources(repository);
	}
}
