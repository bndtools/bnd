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

import aQute.bnd.osgi.resource.ResourceUtils;
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

	private static final Requirement		allRq			= ResourceUtils.createWildcardRequirement();
	private static final SortedSet<Version>	EMPTY_VERSIONS	= new TreeSet<>();

	private final Repository						repository;
	private final Map<String,Map<Version,Resource>>	index	= new HashMap<>();

	public BridgeRepository(Repository repository) throws Exception {
		this.repository = repository;
		index();
	}

	private void index() throws Exception {
		Map<Requirement,Collection<Capability>> all = repository.findProviders(Collections.singleton(allRq));
		for (Capability capability : all.get(allRq)) {
			Resource r = capability.getResource();
			index(r);
		}
	}

	private void index(Resource r) throws Exception {
		IdentityCapability bc = ResourceUtils.getIdentityCapability(r);
		String bsn = bc.osgi_identity();
		Version version = bc.version();

		Map<Version,Resource> map = index.get(bsn);
		if (map == null) {
			map = new HashMap<>();
			index.put(bsn, map);
		}
		map.put(version, r);
	}

	public Resource get(String bsn, Version version) throws Exception {
		Map<Version,Resource> map = index.get(bsn);
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
					if (g.matcher(bsn).find()) {
						bsns.add(bsn);
						continue outer;
					}
				}
			}
		}
		return bsns;
	}

	public SortedSet<Version> versions(String bsn) throws Exception {
		Map<Version,Resource> map = index.get(bsn);
		if (map == null || map.isEmpty()) {
			return EMPTY_VERSIONS;
		}
		return new TreeSet<>(map.keySet());
	}

	public Repository getRepository() {
		return repository;
	}

}
