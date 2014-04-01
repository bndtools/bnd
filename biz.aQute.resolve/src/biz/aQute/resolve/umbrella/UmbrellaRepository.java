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
import aQute.bnd.version.*;
import aQute.lib.collections.*;
import aQute.lib.filter.*;
import aQute.lib.hex.*;
import aQute.lib.io.*;
import aQute.lib.json.*;

public class UmbrellaRepository implements Repository {
	static JSONCodec						codec				= new JSONCodec();

	private static final Comparator<byte[]>	BYTEARRAYCOMPARATOR	= new Comparator<byte[]>() {

																	public int compare(byte[] o1, byte[] o2) {
																		if (o1.length > o2.length)
																			return 1;

																		if (o1.length < o2.length)
																			return -1;

																		for (int i = 0; i < o1.length; i++)
																			if (o1[i] > o2[i])
																				return 1;
																			else if (o1[i] < o2[i])
																				return 1;

																		return 0;
																	}
																};

	private File							cache;
	private boolean							inited;
	private Workspace						workspace;

	final TreeMap<byte[],DResource>			resources			= new TreeMap<byte[],DResource>(BYTEARRAYCOMPARATOR);
	final TreeMap<byte[],String>			failures			= new TreeMap<byte[],String>(BYTEARRAYCOMPARATOR);
	final RepoIndex							repoIndex			= new RepoIndex();

	public UmbrellaRepository(Workspace workspace) {
		this.workspace = workspace;
		cache = IO.getFile("~/.bnd/cache/shas");
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

		for (InfoRepository info : workspace.getPlugins(InfoRepository.class)) {
			if (info instanceof RepositoryPlugin) {

				// But they must also be a repository to iterate
				// over them

				RepositoryPlugin rp = (RepositoryPlugin) info;
				for (String bsn : rp.list(null)) {

					for (Version version : rp.versions(bsn)) {

						final ResourceDescriptor r = info.getDescriptor(bsn, version);

						// we should not get a null when the
						// repo provides the iteration

						assert r != null;

						//
						// Don't want to download failures repeatedly
						//

						if (failures.containsKey(r.id))
							continue;

						//
						// Check if we've already got this one
						//

						if (!resources.containsKey(r.id)) {

							//
							// Nope, check if we have this cached on disk
							//

							DResource dr = getCachedDResource(r.id);
							if (dr == null) {

								//
								// Ok, now we're going to download
								//
								count++;

								dr = new DResource();
								resources.put(r.id, dr);
								dr.download = new RepositoryPlugin.DownloadListener() {

									public void success(File file) throws Exception {
										try {
											DResource dresource = index(file);
											File dresourceFile = getDResourceFile(r.id);
											file.getParentFile().mkdirs();

											codec.enc().to(file).put(dresource);
											waiter.release();
										}
										catch (Exception e) {
											failure(file, "parsing resource " + e.getMessage());
										}
									}

									public void failure(File file, String reason) throws Exception {
										failures.put(r.id, reason);
										waiter.release();
									}

									public boolean progress(File file, int percentage) throws Exception {
										return true;
									}
								};

								//
								// Start fetching
								//
								rp.get(bsn, version, null, dr.download);
							}
						}
					}
				}
			}
		}

		if ( failures.size() > 0) {
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
	DResource index(File file) throws Exception {
		IndexResult result = repoIndex.indexFile(file);
		DResource resource = new DResource();
		for (Requirement req : result.requirements) {
			resource.add(new DRequirement(req));
		}
		for (Capability cap : result.capabilities) {
			resource.capabilities.add(cap.getNamespace(), new DCapability(cap));
		}
		return resource;
	}

	/*
	 * Check if the file systems contains a cache
	 */
	private DResource getCachedDResource(byte[] id) throws Exception {
		File file = getDResourceFile(id);
		if (!file.isFile())
			return null;

		DResource resource = codec.dec().from(file).get(DResource.class);
		resource.fixup();

		return resource;
	}

	/*
	 * Calculate the file name of the resource JSON
	 */
	File getDResourceFile(byte[] id) {
		String hex = Hex.toHexString(id);
		File dir = new File(cache, hex);
		if (!dir.isDirectory())
			return null;

		File file = new File(dir, "resource.json");
		return file;
	}

}
