package biz.aQute.resolve.repository;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.osgi.resource.*;
import org.osgi.service.indexer.ResourceIndexer.IndexResult;
import org.osgi.service.indexer.impl.*;
import org.osgi.service.repository.*;

import aQute.bnd.build.*;
import aQute.bnd.osgi.resource.*;
import aQute.bnd.service.repository.*;
import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;
import aQute.bnd.version.*;
import aQute.lib.hex.*;
import aQute.lib.persistentmap.*;

public class InfoRepositoryWrapper implements Repository {
	final InfoRepository					repo;
	final RepoIndex							repoIndexer;
	final File								cache;
	final PersistentMap<PersistentResource>	persistent;

	private boolean							inited;

	public InfoRepositoryWrapper(RepoIndex repoIndex, InfoRepository repo, File cache) throws IOException {
		this.repoIndexer = repoIndex;
		this.repo = repo;
		this.cache = cache;
		this.persistent = new PersistentMap<PersistentResource>(cache, PersistentResource.class);
	}

	boolean init() throws Exception {
		if (inited)
			return false;

		inited = true;

		Map<String,ResourceDescriptor> map = collectKeys(repo);
		
		persistent.keySet().retainAll(map.keySet());
		map.keySet().removeAll(persistent.keySet());
		
		//
		// Initiate downloads
		//
		
		Map<String,DownloadBlocker> blockers = new HashMap<String,DownloadBlocker>();
		
		for ( Map.Entry<String,ResourceDescriptor> entry : map.entrySet()) {
			
			final ResourceDescriptor rd = entry.getValue();
			
			DownloadBlocker blocker = new DownloadBlocker(null) {
				
				//
				// We steal the thread of the downloader to index
				//
				
				@Override
				public void success(File file) throws Exception {
					IndexResult index = repoIndexer.indexFile(file);
					
					//PersistentResource pr = new PersistentResource(rd.id, index.capabilities, index.requirements);
				}
			};
			blockers.put(entry.getKey(), blocker);
			
			
			repo.get(rd.bsn, rd.version, null, blocker);
		}
		
		
		
		return true;
	}

	public Map<Requirement,Collection<Capability>> findProviders(Collection< ? extends Requirement> requirements) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * Get all the shas from the repo
	 */
	private Map<String,ResourceDescriptor> collectKeys(InfoRepository repo) throws Exception {
		Map<String,ResourceDescriptor> map = new HashMap<String,ResourceDescriptor>();
		
		for (String bsn : repo.list(null)) {
			for (Version version : repo.versions(bsn)) {
				ResourceDescriptor rd = repo.getDescriptor(bsn, version);
				map.put( Hex.toHexString(rd.id), rd);
			}
		}

		return map;
	}

}
