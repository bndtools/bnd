package aQute.bnd.deployer.repository.wrapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.osgi.framework.FrameworkUtil;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.indexer.ResourceIndexer.IndexResult;
import org.osgi.service.indexer.impl.KnownBundleAnalyzer;
import org.osgi.service.indexer.impl.RepoIndex;

import aQute.bnd.build.DownloadBlocker;
import aQute.bnd.osgi.repository.BaseRepository;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.PersistentResource;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.service.repository.InfoRepository;
import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;
import aQute.bnd.version.Version;
import aQute.lib.collections.MultiMap;
import aQute.lib.filter.Filter;
import aQute.lib.hex.Hex;
import aQute.lib.persistentmap.PersistentMap;

public class InfoRepositoryWrapper extends BaseRepository {
	final RepoIndex								repoIndexer;
	final PersistentMap<PersistentResource>		persistent;
	final Collection< ? extends InfoRepository>	repos;;
	long										lastTime	= 0;
	private Properties							augments	= new Properties();

	// private boolean inited;

	public InfoRepositoryWrapper(File dir, Collection< ? extends InfoRepository> repos) throws Exception {
		this.repoIndexer = new RepoIndex();

		KnownBundleAnalyzer knownBundleAnalyzer = new KnownBundleAnalyzer();
		this.augments = new Properties();
		knownBundleAnalyzer.setKnownBundlesExtra(this.augments);

		this.repoIndexer.addAnalyzer(knownBundleAnalyzer, FrameworkUtil.createFilter("(name=*)"));
		this.repos = repos;
		this.persistent = new PersistentMap<>(dir, PersistentResource.class);
	}

	boolean init() {
		try {
			if (System.currentTimeMillis() < lastTime + 10000)
				return true;
		} finally {
			lastTime = System.currentTimeMillis();
		}

		Set<String> errors = new LinkedHashSet<>();

		try {
			//
			// Get the current repo contents
			//

			Set<String> toBeDeleted = new HashSet<>(persistent.keySet());
			Map<String,DownloadBlocker> blockers = new HashMap<>();

			for (InfoRepository repo : repos) {
				Map<String,ResourceDescriptor> map = collectKeys(repo);

				for (final Map.Entry<String,ResourceDescriptor> entry : map.entrySet()) {
					final String id = entry.getKey();

					toBeDeleted.remove(id);

					if (persistent.containsKey(id))
						continue;

					final ResourceDescriptor rd = entry.getValue();

					DownloadBlocker blocker = new DownloadBlocker(null) {

						//
						// We steal the thread of the downloader to index
						//

						@Override
						public void success(File file) throws Exception {
							IndexResult index = null;
							try {
								index = repoIndexer.indexFile(file);

								ResourceBuilder rb = new ResourceBuilder();

								//
								// Unfortunately, we need to convert the
								// caps/reqs
								// since they are not real caps/reqs
								//

								for (org.osgi.service.indexer.Capability capability : index.capabilities) {
									CapReqBuilder cb = new CapReqBuilder(capability.getNamespace());
									cb.addAttributes(capability.getAttributes());
									cb.addDirectives(capability.getDirectives());
									rb.addCapability(cb.buildSyntheticCapability());
								}
								for (org.osgi.service.indexer.Requirement requirement : index.requirements) {
									CapReqBuilder cb = new CapReqBuilder(requirement.getNamespace());
									cb.addAttributes(requirement.getAttributes());
									cb.addDirectives(requirement.getDirectives());
									rb.addRequirement(cb.buildSyntheticRequirement());
								}

								Resource resource = rb.build();

								PersistentResource pr = new PersistentResource(resource);
								persistent.put(id, pr);
							} finally {
								super.success(file);

								if (index != null) {
									index.resource.close();
								}
							}
						}
					};
					blockers.put(entry.getKey(), blocker);
					repo.get(rd.bsn, rd.version, null, blocker);
				}

			}

			for (Entry<String,DownloadBlocker> entry : blockers.entrySet()) {
				String key = entry.getKey();
				DownloadBlocker blocker = entry.getValue();

				String reason = blocker.getReason();
				if (reason != null) {
					errors.add(key + ": " + reason);
				}
			}
			persistent.keySet().removeAll(toBeDeleted);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		if (!errors.isEmpty())
			throw new IllegalStateException("Cannot index " + repos + " due to " + errors);

		return true;
	}

	/**
	 * The repository method
	 * 
	 * @throws Exception
	 */

	public void findProviders(Map<Requirement,List<Capability>> result, Collection< ? extends Requirement> requirements)
			throws Exception {
		init();

		for (Requirement req : requirements) {
			result.putIfAbsent(req, new ArrayList<>(1));

			String f = req.getDirectives().get("filter");
			if (f == null)
				continue;

			Filter filter = new Filter(f);

			for (PersistentResource presource : persistent.values()) {
				Resource resource = presource.getResource();
				for (Capability cap : resource.getCapabilities(req.getNamespace())) {
					if (filter.matchMap(cap.getAttributes())) {
						result.get(req)
							.add(cap);
					}
				}
			}
		}
	}

	@SuppressWarnings({
			"unchecked", "rawtypes"
	})
	public Map<Requirement,Collection<Capability>> findProviders(Collection< ? extends Requirement> requirements) {
		MultiMap<Requirement,Capability> result = new MultiMap<>();
		try {
			findProviders(result, requirements);
			return (Map) result;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/*
	 * Get all the shas from the repo
	 */
	private Map<String,ResourceDescriptor> collectKeys(InfoRepository repo) throws Exception {
		Map<String,ResourceDescriptor> map = new HashMap<>();

		for (String bsn : repo.list(null)) {
			for (Version version : repo.versions(bsn)) {
				ResourceDescriptor rd = repo.getDescriptor(bsn, version);
				if (rd != null)
					map.put(Hex.toHexString(rd.id), rd);
			}
		}
		return map;
	}

	public String toString() {
		return "InfoRepositoryWrapper[" + repos.size() + "]";
	}

	public void close() throws IOException {
		persistent.close();
	}

	public void addAugment(Properties properties) {
		augments.putAll(properties);
	}

	public void clear() {
		this.persistent.clear();
	}

	/**
	 * Clear all files that were indexed before this date
	 * 
	 * @param whenOlder
	 */
	public void clear(long whenOlder) {
		persistent.clear(whenOlder);
	}

}
