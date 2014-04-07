package biz.aQute.resolve.repository;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import org.osgi.resource.*;
import org.osgi.service.indexer.ResourceIndexer.IndexResult;
import org.osgi.service.indexer.impl.*;
import org.osgi.service.repository.*;

import aQute.bnd.build.*;
import aQute.bnd.osgi.resource.*;
import aQute.bnd.service.repository.*;
import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;
import aQute.bnd.version.*;
import aQute.lib.collections.*;
import aQute.lib.filter.*;
import aQute.lib.hex.*;
import aQute.lib.persistentmap.*;

public class InfoRepositoryWrapper implements Repository {
	final InfoRepository					repo;
	final RepoIndex							repoIndexer;
	final File								cache;
	final PersistentMap<PersistentResource>	persistent;

	private boolean							inited;

	public InfoRepositoryWrapper(InfoRepository repo, File cache) throws IOException {
		this.repoIndexer = new RepoIndex();
		this.repo = repo;
		this.cache = cache;
		this.persistent = new PersistentMap<PersistentResource>(cache, PersistentResource.class);
	}

	boolean init() {
		if (inited)
			return false;

		inited = true;

		Set<String> errors = new LinkedHashSet<String>();

		try {
			//
			// Get the current repo contents
			//
			Map<String,ResourceDescriptor> map = collectKeys(repo);

			//
			// Remove any keys not in the repo from the persistent set
			// and remove any keys in the persistent set from the repo content
			// (they do not need to be parsed)
			//

			persistent.keySet().retainAll(map.keySet());
			map.keySet().removeAll(persistent.keySet());

			//
			// Initiate downloads
			//

			Map<String,DownloadBlocker> blockers = new HashMap<String,DownloadBlocker>();

			for (final Map.Entry<String,ResourceDescriptor> entry : map.entrySet()) {
				final String id = entry.getKey();
				final ResourceDescriptor rd = entry.getValue();

				DownloadBlocker blocker = new DownloadBlocker(null) {

					//
					// We steal the thread of the downloader to index
					//

					@Override
					public void success(File file) throws Exception {
						IndexResult index = repoIndexer.indexFile(file);

						ResourceBuilder rb = new ResourceBuilder();

						//
						// Unfortunately, we need to convert the caps/reqs since
						// they are not real caps/reqs
						//
						for (org.osgi.service.indexer.Capability capability : index.capabilities) {
							CapReqBuilder cb = new CapReqBuilder(capability.getNamespace());
							cb.addAttributes(capability.getAttributes());
							cb.addDirectives(capability.getDirectives());
						}
						for (org.osgi.service.indexer.Requirement requirement : index.requirements) {
							CapReqBuilder cb = new CapReqBuilder(requirement.getNamespace());
							cb.addAttributes(requirement.getAttributes());
							cb.addDirectives(requirement.getDirectives());
						}

						Resource resource = rb.build();

						PersistentResource pr = new PersistentResource(rd.id, resource.getCapabilities(null),
								resource.getRequirements(null));
						persistent.put(id, pr);
						super.success(file);
					}
				};
				blockers.put(entry.getKey(), blocker);
				repo.get(rd.bsn, rd.version, null, blocker);
			}

			for (Entry<String,DownloadBlocker> entry : blockers.entrySet()) {
				String key = entry.getKey();
				DownloadBlocker blocker = entry.getValue();

				String reason = blocker.getReason();
				if (reason != null) {
					errors.add(key + ": " + reason);
				}
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

		if (!errors.isEmpty())
			throw new IllegalStateException("Cannot index " + repo.getName() + " due to " + errors);

		return true;
	}
	
	
	/**
	 * The repository method
	 */

	@SuppressWarnings({
			"unchecked", "rawtypes"
	})
	public Map<Requirement,Collection<Capability>> findProviders(Collection< ? extends Requirement> requirements) {

		MultiMap<Requirement,Capability> result = new MultiMap<Requirement,Capability>();

		nextReq: for (Requirement req : requirements) {
			String f = req.getDirectives().get("filter");
			if (f == null)
				continue nextReq;

			Filter filter = new Filter(f);

			for (Resource resource : persistent.values()) {
				for (Capability cap : resource.getCapabilities(req.getNamespace())) {
					if (filter.matchMap(cap.getAttributes()))
						result.add(req, cap);
				}
			}
		}
		return (Map) result;
	}

	/*
	 * Get all the shas from the repo
	 */
	private Map<String,ResourceDescriptor> collectKeys(InfoRepository repo) throws Exception {
		Map<String,ResourceDescriptor> map = new HashMap<String,ResourceDescriptor>();

		for (String bsn : repo.list(null)) {
			for (Version version : repo.versions(bsn)) {
				ResourceDescriptor rd = repo.getDescriptor(bsn, version);
				map.put(Hex.toHexString(rd.id), rd);
			}
		}
		return map;
	}

	public String toString() {
		return repo.getName();
	}
}
