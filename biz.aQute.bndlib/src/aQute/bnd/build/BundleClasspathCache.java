package aQute.bnd.build;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import aQute.bnd.osgi.Resource;
import aQute.bnd.service.RepositoryPlugin.PutOptions;
import aQute.bnd.service.RepositoryPlugin.PutResult;
import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;
import aQute.bnd.version.Version;
import aQute.lib.deployer.FileRepo;
import aQute.libg.cryptography.SHA1;

/**
 * Caches the entries specified in the Bundle-ClassPath header in a file
 * repository so that these entries can be added to the build path.
 */
public class BundleClasspathCache {

	/** The backing repository for the cached files */
	private FileRepo repo;

	/**
	 * Initialise the repository
	 * 
	 * @param location
	 *            where the cached files will be stored
	 */
	void init(File location) {
		FileRepo files = new FileRepo();

		Map<String,String> config = new HashMap<>();
		config.put(FileRepo.LOCATION, location.getAbsolutePath());
		config.put(FileRepo.READONLY, "false");
		config.put(FileRepo.INDEX, "true");
		files.setProperties(config);

		repo = files;
	}

	/**
	 * Looks up the cached file in the file repository. If it doesn't exist it
	 * is added.
	 * 
	 * @param source
	 *            the container for the bundle using the 'Bundle-ClassPath'
	 *            header
	 * @param resource
	 *            the resource embedded in 'source'
	 * @param entryName
	 *            the name of the entry the 'resource' hold the data for
	 * @return
	 * @throws Exception
	 */
	public File getEntry(Container source, Resource resource, String entryName) throws Exception {

		SHA1 digest = SHA1.digest(resource.openInputStream());

		ResourceDescriptor entry = repo.getResource(digest.toByteArray());

		// we need to put the entry in the cache if the resource was not found
		if (entry == null) {
			InputStream stream = resource.openInputStream();
			PutOptions options = createOptions(source, entryName, digest.hashCode());
			PutResult result = repo.put(stream, options);
			return new File(result.artifact);
		} else {
			return new File(entry.url);
		}
	}

	private PutOptions createOptions(Container source, String entry, int id) {
		PutOptions options = new PutOptions();

		// use a bsn that encodes the source and the entry within it
		options.bsn = encodeEntryBsn(source.getBundleSymbolicName(), entry);

		String versionString = source.getVersion();
		if (Version.isVersion(versionString)) {
			options.version = Version.parseVersion(versionString);
		}

		options.type = PutOptions.BUNDLE;
		return options;
	}

	private String encodeEntryBsn(String containerBsn, String entryPath) {
		return containerBsn + "-" + entryPath;
	}
}
