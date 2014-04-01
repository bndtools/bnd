package biz.aQute.resolve.umbrella;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.osgi.service.indexer.*;
import org.osgi.service.indexer.ResourceIndexer.IndexResult;
import org.osgi.service.indexer.impl.*;
import org.osgi.service.repository.*;

import aQute.bnd.build.*;
import aQute.bnd.service.*;
import aQute.bnd.service.repository.*;
import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;
import aQute.lib.collections.*;
import aQute.lib.filter.*;
import aQute.lib.hex.*;
import aQute.lib.io.*;
import aQute.lib.json.*;
import aQute.lib.persistentmap.*;

public class UmbrellaRepository implements Repository {
	static JSONCodec						codec				= new JSONCodec();

	private File							cache;
	private boolean							inited;
	private Workspace						workspace;

	final Map<String,String>				failures			= new HashMap<String,String>();
	final RepoIndex							repoIndex			= new RepoIndex();
	final PersistentMap<DResource>			resources;

	public UmbrellaRepository(Workspace workspace) throws IOException {
		this.workspace = workspace;
		cache = IO.getFile("~/.bnd/cache/umbrella");
		cache.mkdirs();
		resources = new PersistentMap<DResource>(cache, DResource.class);
	}

	@SuppressWarnings({
			"unchecked", "rawtypes"
	})
	public Map<org.osgi.resource.Requirement,Collection<org.osgi.resource.Capability>> findProviders(
			Collection< ? extends org.osgi.resource.Requirement> requirements) {
		try {
			init();

			MultiMap<org.osgi.resource.Requirement,org.osgi.resource.Capability> caps = new MultiMap<org.osgi.resource.Requirement,org.osgi.resource.Capability>();

			for (DResource resource : resources.values()) {
				for (org.osgi.resource.Requirement req : requirements) {
					for (org.osgi.resource.Capability dcap : resource.capabilities.get(req.getNamespace())) {
						if (matches(req, dcap)) {
							caps.add(req, dcap);
						}
					}
				}
			}
			return (Map) caps;
		}
		catch (Exception e) {
			workspace.error("Failed findProviders for %s, reason %s", requirements, e);
			throw new RuntimeException(e);
		}
	}

	private boolean matches(org.osgi.resource.Requirement req, org.osgi.resource.Capability dcap) {
		String f = req.getDirectives().get("filter");
		if (f == null)
			return false;

		try {
			Filter filter = new Filter(f);
			return filter.matchMap(dcap.getAttributes());
		}
		catch (Exception e) {}
		return false;
	}

	/**
	 * Ensure we've got all the resources loaded and parsed
	 * 
	 * @throws Exception
	 */
	private void init() throws Exception {
		if (inited)
			return;

		inited = true;
		int count = 0;
		final Semaphore waiter = new Semaphore(0);

		//
		// We index InfoRepositories
		//

		Set<String> seen = new HashSet<String>();

		for (InfoRepository info : workspace.getPlugins(InfoRepository.class)) {

			SortedSet<ResourceDescriptor> descriptors = info.getResources();
			for (final ResourceDescriptor rd : descriptors) {

				final String key = Hex.toHexString(rd.id);
				seen.add(key);

				if (resources.containsKey(key))
					continue;

				if (failures.containsKey(key))
					continue;

				final DResource r = new DResource();
				resources.put(key, r);

				r.download = new RepositoryPlugin.DownloadListener() {

					public void success(File file) throws Exception {
						try {

							index(file, r);
							waiter.release();
						}
						catch (Exception e) {
							failure(file, "parsing resource " + e.getMessage());
						}
					}

					public void failure(File file, String reason) throws Exception {
						failures.put(key, reason);
						waiter.release();
					}

					public boolean progress(File file, int percentage) throws Exception {
						return true;
					}
				};

				//
				// Start fetching
				//

				info.get(rd.bsn, rd.version, null, r.download);
			}
		}

		if (failures.size() > 0) {
			workspace.error("Failed to download %s", failures);
		}
		//
		// We might have started downloads
		// so we need to wait for them
		//

		if (count > 0) {
			waiter.acquire(count);
		}
	}

	/*
	 * Index the file
	 */
	DResource index(File file, DResource resource) throws Exception {
		IndexResult result = repoIndex.indexFile(file);
		if (result.requirements != null && !result.requirements.isEmpty()) {
			resource.requirements = new MultiMap<String,DRequirement>();
			for (Requirement req : result.requirements) {
				resource.add(new DRequirement(req));
			}
		}

		if (result.capabilities != null && !result.capabilities.isEmpty()) {
			resource.capabilities = new MultiMap<String,DCapability>();
			for (Capability cap : result.capabilities) {
				resource.capabilities.add(cap.getNamespace(), new DCapability(cap));
			}
		}
		return resource;
	}
}
