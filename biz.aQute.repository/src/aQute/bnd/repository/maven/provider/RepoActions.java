package aQute.bnd.repository.maven.provider;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import aQute.bnd.repository.maven.provider.IndexFile.BundleDescriptor;
import aQute.lib.io.IO;

class RepoActions {

	private MavenBndRepository repo;

	RepoActions(MavenBndRepository mavenBndRepository) {
		this.repo = mavenBndRepository;
	}


	Map<String,Runnable> getRevisionActions(final BundleDescriptor bd) {
		Map<String,Runnable> map = new LinkedHashMap<>();
		map.put("Clear", new Runnable() {

			@Override
			public void run() {
				File dir = repo.storage.toLocalFile(bd.archive).getParentFile();
				IO.delete(dir);
			}

		});
		map.put("Delete from Index", new Runnable() {

			@Override
			public void run() {
				try {
					repo.index.remove(bd.archive);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}

		});
		return map;
	}

}
