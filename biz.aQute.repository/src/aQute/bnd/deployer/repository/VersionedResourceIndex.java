package aQute.bnd.deployer.repository;

import static aQute.bnd.deployer.repository.RepoResourceUtils.getResourceIdentity;
import static aQute.bnd.deployer.repository.RepoResourceUtils.getResourceVersion;
import static aQute.bnd.deployer.repository.RepoResourceUtils.narrowVersionsByVersionRange;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import org.osgi.resource.Resource;

import aQute.bnd.version.Version;
import aQute.lib.collections.SortedList;

public class VersionedResourceIndex {

	private final Map<String, SortedMap<Version, Resource>> map = new HashMap<>();

	public synchronized void clear() {
		map.clear();
	}

	public synchronized Set<String> getIdentities() {
		return map.keySet();
	}

	public synchronized SortedSet<Version> getVersions(String bsn) {
		SortedMap<Version, Resource> versionMap = map.get(bsn);
		if (versionMap == null || versionMap.isEmpty())
			return SortedList.empty();
		return new SortedList<>(versionMap.keySet());
	}

	public synchronized List<Resource> getRange(String bsn, String range) {
		SortedMap<Version, Resource> versionMap = map.get(bsn);
		if (versionMap == null || versionMap.isEmpty())
			return null;
		return narrowVersionsByVersionRange(versionMap, range);
	}

	public synchronized void put(Resource resource) {
		String id = getResourceIdentity(resource);
		if (id == null)
			throw new IllegalArgumentException("Missing identity capability on resource");

		Version version = getResourceVersion(resource);
		SortedMap<Version, Resource> versionMap = map.get(id);
		if (versionMap == null) {
			versionMap = new TreeMap<>();
			map.put(id, versionMap);
		}
		versionMap.put(version, resource);
	}

	public synchronized Resource getExact(String identity, Version version) {
		SortedMap<Version, Resource> versions = map.get(identity);
		if (versions == null)
			return null;

		return findVersion(version, versions);
	}

	private static Resource findVersion(Version version, SortedMap<Version, Resource> versions) {
		if (version.getQualifier() != null && version.getQualifier()
			.length() > 0) {
			return versions.get(version);
		}

		Resource latest = null;
		for (Map.Entry<Version, Resource> entry : versions.entrySet()) {
			if (version.getMicro() == entry.getKey()
				.getMicro()
				&& version.getMinor() == entry.getKey()
					.getMinor()
				&& version.getMajor() == entry.getKey()
					.getMajor()) {
				latest = entry.getValue();
				continue;
			}
			if (compare(version, entry.getKey()) < 0) {
				break;
			}
		}
		return latest;
	}

	private static int compare(Version v1, Version v2) {

		if (v1.getMajor() != v2.getMajor())
			return v1.getMajor() - v2.getMajor();

		if (v1.getMinor() != v2.getMinor())
			return v1.getMinor() - v2.getMinor();

		if (v1.getMicro() != v2.getMicro())
			return v1.getMicro() - v2.getMicro();

		return 0;
	}

}
